/*
 * Modeled on Orchard's own TrackAnalyzer (https://github.com/SFG5453/Orchard).
 * Phase 1 was the DSP-only pass (native/analyzer/audio_analysis.cpp); Phase 2
 * adds the Beat This! ONNX model (see [BeatTracker]) and Phase 3 the
 * open-unmix vocal mask (see [VocalTracker]), both over the head and tail of
 * the track, which is the only part a transition ever reads.
 *
 * Copyright (C) 2026 Kushagra Singh
 *
 * This program is free software: you can redistribute it and/or modify it
 * under the terms of the GNU General Public License as published by the
 * Free Software Foundation, either version 3 of the License, or (at your
 * option) any later version.
 *
 * This program is distributed in the hope that it will be useful, but
 * WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General
 * Public License for more details.
 *
 * You should have received a copy of the GNU General Public License along
 * with this program. If not, see <https://www.gnu.org/licenses/>.
 */

package com.music.aurelis.playback.smart

import android.content.Context
import android.app.ActivityManager
import com.music.aurelis.playback.smart.AutomixThreadBudget
import android.media.MediaDataSource
import android.net.Uri
import android.os.Build
import android.os.PowerManager
import android.os.Process
import android.os.SystemClock
import android.util.Log
import androidx.media3.common.util.UnstableApi
import com.music.aurelis.data.listentogether.ListenTogether
import com.music.aurelis.data.settings.AppSettings
import com.music.aurelis.data.settings.AutomixPerformanceMode
import com.music.aurelis.playback.AudioCache
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import kotlin.math.abs
import kotlin.math.ceil
import kotlin.math.max
import kotlin.math.round
import java.util.Locale

/**
 * Produces [TrackAnalysis] for tracks that are about to be mixed, and hands it
 * to [TransitionPlanner].
 *
 * [analysisFor] is called from the crossfade watcher every tick, so it never
 * blocks or computes: it returns what is already known, and an unanalysed
 * track simply reads as no evidence, which the policy ladder answers with a
 * plain fade.
 */
@UnstableApi
class TrackAnalyzer internal constructor(
    private val context: Context,
    private val cache: AudioCache,
    private val executor: ExecutorService,
) {
    constructor(context: Context, cache: AudioCache) : this(context, cache, analysisExecutor("aurelis-smart-analysis", 2))

    data class Request(
        val trackId: String,
        val uri: Uri,
        val durationSeconds: Double,
        val prioritizeIncomingHead: Boolean = false,
    )

    private class PendingAnalysis(
        val storageKey: String,
        val recordingKey: String,
        val run: (threads: Int, parallel: Boolean) -> Unit,
    )

    // File identity and saved results must be available even during a long decode.
    private val restoreExecutor = analysisExecutor("aurelis-smart-restore")

    private val models by lazy { AutomixModelDepot(context.applicationContext) }
    private val nativeGuard by lazy { AutomixNativeGuard(context) }

    /** One adaptive budget, used by both ONNX models and both analysis passes. */
    private fun inferenceThreads(): Int {
        thermalWatch
        val lowRam = context.getSystemService(ActivityManager::class.java)?.isLowRamDevice == true
        return AutomixThreadBudget.choose(
            requested = AppSettings.automixPerformanceMode.value.inferenceThreads,
            availableCpus = Runtime.getRuntime().availableProcessors(),
            lowRam = lowRam,
            thermalModerate = thermalStatus >= PowerManager.THERMAL_STATUS_MODERATE,
            thermalSevere = thermalStatus >= PowerManager.THERMAL_STATUS_SEVERE,
        )
    }

    // Each task owns its native sessions until its last model call returns.
    private inner class AnalysisModels(threads: Int, private val parallel: Boolean) {
        // A cancelled decode stays cancelled even if the thermal status changes
        // again before it returns. Its samples must never count as truncated audio.
        private var cancelled = false

        fun isCancelled(): Boolean {
            if (cancelled) return true
            val parallelUnavailable = parallel &&
                (thermalStatus >= PowerManager.THERMAL_STATUS_MODERATE ||
                    AppSettings.automixPerformanceMode.value != AutomixPerformanceMode.PERFORMANCE)
            cancelled = stopped || parallelUnavailable
            return cancelled
        }

        private val threadLimit = { minOf(threads, inferenceThreads()) }
        val tracker = BeatTracker(
            modelPath = {
                java.io.File(context.filesDir, BeatTracker.RETIRED_MODEL_ASSET).delete()
                models.path(BeatTracker.MODEL_ASSET)
            },
            inferenceThreads = threadLimit,
            allowSpinning = false,
        )
        val vocals = VocalTracker(
            modelPath = { models.path(VocalTracker.MODEL_ASSET) },
            inferenceThreads = threadLimit,
            allowSpinning = false,
        )

        private fun inferenceDiagnostics(): String =
            "mode=${AppSettings.automixPerformanceMode.value.name} threads=${threadLimit()} " +
                "activeAnalyses=${running.size} thermalStatus=$thermalStatus"

        fun beats(samples: FloatArray, offset: Double): BeatTracker.Grid? =
            nativeGuard.infer(AutomixNativeGuard.Model.BEAT, ::isCancelled, ::inferenceDiagnostics) {
                tracker.track(samples, offsetSeconds = offset)
            }

        fun vocals(stereo: AudioDecoder.StereoPcm): FloatArray? =
            nativeGuard.infer(AutomixNativeGuard.Model.VOCALS, ::isCancelled, ::inferenceDiagnostics) {
                vocalValues(stereo, vocals)
            }

        fun release() = nativeGuard.closeSessions {
            tracker.release()
            vocals.release()
        }
    }

    init {
        // The shared trackers report model failures and their timings through
        // this; with no sink they went nowhere on the phone, which is how a
        // beat model that found nothing went unnoticed.
        AnalysisLog.install { message, error -> Log.w(TAG, message, error) }
    }

    // Models stay bundled for offline AutoMix. The depot validates their hashes,
    // writes atomically, and only downloads if an asset is absent or corrupt.

    /**
     * Whether analysis is switched off entirely right now.
     *
     * A party never mixes — [com.music.aurelis.playback.CrossfadeController]
     * refuses to plan a transition while one is running, because every member
     * would start the next track at a moment their own copy of the audio
     * decided on. Measuring for a transition that cannot happen is then pure
     * cost, and not a small one: a whole-track decode, two ONNX passes, and for
     * an uncached track a megabyte fetched over the same connection the party's
     * own sync is sharing.
     *
     * Read fresh at every entry point and between the stages of a pass in
     * flight, rather than latched when the party starts, so joining one stops
     * the work already running instead of only the work not yet queued.
     */
    @Volatile private var released = false
    private val stopped: Boolean get() = released || ListenTogether.state.value.inParty

    /** The last thermal status the system reported; see [overheated]. */
    @Volatile private var thermalStatus = 0

    /**
     * Registered on first use rather than at construction, for the reason
     * [resolver] gives: there is no base context to ask for a service yet.
     */
    private val thermalWatch by lazy {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            context.getSystemService(PowerManager::class.java)?.let { power ->
                thermalStatus = power.currentThermalStatus
                power.addThermalStatusListener(Runnable::run) { thermalStatus = it }
            }
        }
    }

    /**
     * Whether the phone has asked apps to shed load. No new pass is started
     * while it has: a pass is tens of seconds of decoding and inference, and
     * started on a phone already throttled to half its clocks it is the load
     * that keeps it there — measured, a session of back-to-back analyses held
     * a phone at SEVERE with every frame of the UI missing its vsync. A track
     * left unmeasured gets a plain crossfade, and is measured on a later tick
     * once the phone has cooled. A pass already running finishes.
     */
    private val overheated: Boolean
        get() {
            thermalWatch
            return thermalStatus >= PowerManager.THERMAL_STATUS_SEVERE
        }

    /**
     * Resolved on first use, not at construction, for the reason [AnalysisStore]
     * documents about its own directory: this class is a field initializer on
     * the playback service, which runs before the service has a base context
     * attached, and asking a context for anything there returns null.
     */
    private val resolver by lazy { context.contentResolver }

    private val results = ConcurrentHashMap<String, TrackAnalysis>()
    private val running = ConcurrentHashMap.newKeySet<String>()

    /** Tracks whose result came from [analyzeHead] and is waiting to be superseded. */
    private val provisional = ConcurrentHashMap.newKeySet<String>()

    /**
     * Cached prefix size, in bytes, at the last head attempt on each
     * *rendition*. See [headWorthTrying].
     *
     * Keyed by rendition rather than by track because the growth guard is
     * asking "have I already decoded roughly this much of this copy", and a
     * track can move between copies: a first attempt on a half-cached lossless
     * rendition recorded six megabytes, and a much lighter Opus head arriving
     * afterwards — the one that would actually have produced a result — was then
     * refused for being smaller than a number belonging to a different file.
     */
    private val headAttempts = ConcurrentHashMap<String, Long>()

    /**
     * Track-and-rendition pairs that have already reported waiting for a head,
     * so the tick doesn't spam. Not keyed by track alone: a track that gains a
     * second copy of itself is in a genuinely new situation, and the first
     * version of this hid exactly that.
     */
    private val headSkipLogged = ConcurrentHashMap.newKeySet<String>()

    /**
     * How many times each track's whole-track pass has been refused for
     * decoding short. Counted rather than flagged so a rendition still filling
     * in its holes gets a few more chances, while one that is genuinely
     * truncated stops being re-decoded on every tick.
     */
    private val shortDecodes = ConcurrentHashMap<String, Int>()

    /**
     * Local files whose container would not state a runtime, asked about without
     * one.
     *
     * A file on the device queued *behind* the playing track has no runtime the
     * player knows yet, so the analyzer has to read it off the container — and a
     * container that will not say used to write the track off for the session as
     * a ready-but-empty result. The same file analyses perfectly once it is the
     * playing one, because the player then supplies the runtime. So it is not a
     * failure: nothing is recorded, and the next request that *does* carry a
     * runtime goes through. Until one does, asking again is pointless.
     */
    private val unknownRuntime = ConcurrentHashMap.newKeySet<String>()

    /** A present key marks a lookup in progress (false) or completed (true). */
    private val restoreLookups = ConcurrentHashMap<String, Boolean>()

    /** Results that survive the process, so a track is measured once and stays measured. */
    private val store = AnalysisStore { context.filesDir }

    /**
     * Cache keys of renditions that decoded short despite the cache calling them
     * complete. Keyed by rendition rather than by track, because the point is to
     * send the next attempt at the *same* track to a different copy of it.
     */
    private val badRenditions = ConcurrentHashMap.newKeySet<String>()

    /**
     * Cache keys already put through the whole-track pass, whatever came of it.
     *
     * What reopens a track that was written off: a copy of it nobody has read
     * yet. Without this the write-off is final for the session, and a rendition
     * that arrives seconds later — a quality upgrade, or the head fetch the
     * analyzer itself asked for — is never looked at. Measured, a track was
     * given up on at 22:09:24 and its `#hifi` copy finished downloading at
     * 22:09:41.
     */
    private val triedRenditions = ConcurrentHashMap.newKeySet<String>()

    /**
     * How many times each rendition has been thrown off disk for being
     * undecodable, so a clean slate stays a one-off.
     *
     * The refetch that follows a discard is not guaranteed to be any better —
     * a source can serve the same broken file twice — and without a bound the
     * two halves feed each other: refuse, delete, refetch, refuse, delete, on a
     * 250ms tick, spending the listener's data in a loop. One clean-slate retry
     * is enough for the case this exists for, which is an entry spliced from two
     * different encodings and unrecoverable only because nothing would ever
     * overwrite it.
     */
    private val discarded = ConcurrentHashMap<String, Int>()

    private fun discardsOf(key: String): Int = discarded[key] ?: 0

    private data class LocalRevision(val checkedAt: Long, val stamp: String?)
    private val localRevisions = ConcurrentHashMap<Uri, LocalRevision>()
    private val checkingLocalRevisions = ConcurrentHashMap.newKeySet<Uri>()

    /** Poll on the restore worker, keeping provider I/O off playback ticks. */
    private fun checkedLocalRevision(uri: Uri): LocalRevision? {
        val previous = localRevisions[uri]
        if (previous != null && SystemClock.elapsedRealtime() - previous.checkedAt < LOCAL_REVISION_TTL_MS) {
            return previous
        }
        if (checkingLocalRevisions.add(uri)) {
            restoreExecutor.execute {
                try {
                    val stamp = LocalAudioSource.revision(resolver, uri)
                    localRevisions[uri] = LocalRevision(SystemClock.elapsedRealtime(), stamp)
                } finally {
                    checkingLocalRevisions.remove(uri)
                }
            }
        }
        return null
    }

    private fun analysisKey(trackId: String, uri: Uri, playbackKey: String? = null): String {
        if (LocalAudioSource.isLocal(uri)) {
            return AutomixAnalysisSource.localStorageKey(trackId, uri.toString(), localRevisions[uri]?.stamp)
        }
        val key = playbackKey ?: cache.cacheKeyOf(uri)
        return AutomixAnalysisSource.playbackStorageKey(trackId, key)
    }

    /** A source's stereo and alternate-mix rows can share a media ID, but not a beat grid. */
    fun analysisFor(trackId: String, uri: Uri?): TrackAnalysis =
        uri?.let { results[analysisKey(trackId, it)] } ?: TrackAnalysis(trackId = trackId)

    /** Wait for the saved result before requesting audio bytes or scheduling a decode. */
    private fun restoreComplete(trackId: String, storageKey: String, durationSeconds: Double): Boolean {
        if (results.containsKey(storageKey) || restoreLookups[storageKey] == true) return true
        if (restoreLookups.putIfAbsent(storageKey, false) == null) {
            restoreExecutor.execute {
                try {
                    val stored = store.load(storageKey)
                    if (stored != null) {
                        if (stored.isConsistentWithPlayback(durationSeconds)) {
                            Log.d(TAG, "Restored validated analysis for $trackId: bpm=${stored.bpm} conf=${stored.beatConfidence}")
                            results.putIfAbsent(storageKey, stored.copy(trackId = trackId))
                        } else {
                            Log.d(TAG, "AutoMix will refresh inconsistent saved analysis for $trackId")
                        }
                    }
                } finally {
                    restoreLookups[storageKey] = true
                }
            }
        }
        return false
    }

    /** True once [trackId] has a result, including a failure. Nothing more will arrive. */
    fun isAnalysed(trackId: String, uri: Uri? = null): Boolean =
        results.containsKey(uri?.let { analysisKey(trackId, it) } ?: trackId)

    /**
     * True while a decode and inference for [trackId] is actually in flight.
     *
     * Distinct from "not analysed": a track waiting on bytes and a track being
     * worked on right now are the same absence of a result, and the difference
     * is the difference between something being wrong and something simply
     * taking the several seconds it takes.
     */
    fun isAnalysing(trackId: String, uri: Uri?): Boolean =
        uri != null && analysisKey(trackId, uri) in running

    /**
     * Restore both saved results even while a batch is running. A fresh batch
     * reserves the budget once, so a late request cannot start alongside an
     * existing four-thread task. Waiting tracks are reconsidered on the next
     * playback tick, using the current queue and available audio.
     */
    @Synchronized
    fun requestPair(outgoing: Request?, incoming: Request?, allowParallel: Boolean) {
        if (stopped) return
        val canStart = running.isEmpty()
        val first = outgoing?.let { prepare(it, canStart) }
        val second = incoming?.let { prepare(it, canStart) }
        val pending = listOfNotNull(first, second).distinctBy { it.recordingKey }
        if (pending.isEmpty()) return

        val allocations = AutomixThreadBudget.allocate(inferenceThreads(), pending.size, allowParallel)
        val batch = pending.zip(allocations)
        val parallel = batch.size > 1
        Log.d(TAG, "AutoMix analysis batch: ${allocations.joinToString(" + ")} threads")
        for ((analysis, _) in batch) running.add(analysis.storageKey)
        for ((analysis, threads) in batch) {
            executor.execute { analysis.run(threads, parallel) }
        }
    }

    private fun prepare(request: Request, canStart: Boolean): PendingAnalysis? {
        val (trackId, uri, durationSeconds, prioritizeIncomingHead) = request
        if (trackId.isBlank()) return null
        // Never find a substitute recording by title. Source-backed tracks
        // must be measured from the same URI (and cache entry) used to play them.
        val analysisUri = uri
        val local = LocalAudioSource.isLocal(uri)
        val revision = if (local) checkedLocalRevision(uri) ?: return null else null
        val playbackKey = if (local) null else cache.cacheKeyOf(uri)
        val storageKey = analysisKey(trackId, uri, playbackKey)
        if (storageKey in running) return null
        if ((!local || revision?.stamp != null) && !restoreComplete(trackId, storageKey, durationSeconds)) return null

        val recorded = results[storageKey]
        if (recorded?.isUsable == true && storageKey !in provisional) return null
        // A saved result can still be restored while the phone is too hot to decode.
        if (overheated) return null

        // See [unknownRuntime]: no runtime from the caller and none from the
        // container last time, so there is nothing new to try.
        if (local && !(durationSeconds.isFinite() && durationSeconds > 0.0) && storageKey in unknownRuntime) return null

        // Complete *and* not already written off. A copy that decoded to
        // nothing is not a copy worth routing to: counting it as "fully cached"
        // sent this down the whole-track path on every tick with nothing left
        // for that path to read, and each of those empty passes was then
        // counted as a failed decode.
        //
        // Not asked of a local file, whose bytes the cache has never seen and
        // whose completeness is not a question: it is whole from the first tick.
        val complete = playbackKey?.let(cache::playbackRendition)
            ?.takeIf { it.isComplete && it.key !in badRenditions }
        val usableComplete = local || complete != null

        // Two things are worth superseding, and nothing else is. A provisional
        // head result, because replacing it with the whole-track pass is the
        // entire point of it — and a recorded failure, but only once a copy of
        // the track nobody has read yet turns up. Re-deciding a failure against
        // the same renditions that produced it would just spend the decode
        // again for the same answer.
        //
        // A failed local revision is retried only when the file changes.
        val untried = if (local) {
            storageKey !in triedRenditions
        } else {
            complete != null && complete.key !in triedRenditions
        }
        val supersedable = when {
            recorded == null -> false
            storageKey in provisional -> usableComplete
            else -> untried
        }
        if (recorded != null && !supersedable) {
            // One exception to returning empty-handed. A provisional result is a
            // placeholder, not an answer — it says the opening decoded, not that
            // the track is measured — and the thing that supersedes it is bytes.
            // Without this nudge the byte escalation stops at whatever the first
            // successful head happened to cost, nothing else ever asks for the
            // rest, and a queued track reaches its own transition carrying an
            // entry-only estimate: no content end, no mix-out anchor, no vocal
            // mask, which is most of what the outgoing half of a blend reads.
            if (storageKey in provisional && !usableComplete) playbackKey?.let { cache.requestPlaybackAnalysisAudio(analysisUri, it) }
            return null
        }
        // One head attempt per track: a partial container that will not parse
        // now is unlikely to parse ten ticks later, and retrying a decode every
        // 250ms would cost more than the analysis it is trying to bring
        // forward. This byte check only applies to an incomplete stream.
        val headRendition = if (usableComplete) null else headWorthTrying(trackId, requireNotNull(playbackKey), durationSeconds)
        if (!usableComplete) playbackKey?.let { cache.requestPlaybackAnalysisAudio(analysisUri, it) }
        if (!usableComplete && headRendition == null) return null
        if (!canStart) return null

        return PendingAnalysis(storageKey, playbackKey ?: storageKey) run@{ threads, parallel ->
            val session = AnalysisModels(threads, parallel)
            try {
                if (session.isCancelled() || overheated) return@run
                if (results[storageKey]?.isUsable == true && storageKey !in provisional) return@run
                // Efficient mode yields to decoding and playback rather than
                // competing for a core. Thread count remains the speed knob.
                Process.setThreadPriority(
                    if (AppSettings.automixPerformanceMode.value == AutomixPerformanceMode.EFFICIENT) {
                        Process.THREAD_PRIORITY_BACKGROUND
                    } else {
                        Process.THREAD_PRIORITY_DEFAULT
                    },
                )
                if (playbackKey != null && cache.cacheKeyOf(uri) != playbackKey) return@run
                // A reopened recording starts with a fresh short-decode count.
                if (recorded != null && !recorded.isUsable) shortDecodes.remove(storageKey)
                if (usableComplete) {
                    val outcome = analyze(
                        trackId, analysisUri, durationSeconds, storageKey, playbackKey,
                        prioritizeIncomingHead && recorded == null, session,
                    ) {
                        if (local) LocalAudioSource.revision(resolver, uri) == revision?.stamp
                        else cache.cacheKeyOf(uri) == playbackKey
                    }
                    if (session.isCancelled()) return@run
                    if (local && LocalAudioSource.revision(resolver, uri) != revision?.stamp) {
                        localRevisions.remove(uri)
                        return@run
                    }
                    val whole = outcome.analysis
                    if (whole != null) {
                        results[storageKey] = whole
                        provisional.remove(storageKey)
                        shortDecodes.remove(storageKey)
                        // Only the whole-track pass is persisted. A head result
                        // is missing everything past its window — the outro, the
                        // mix-out anchor, the energy curve — and storing one
                        // would freeze a deliberately partial answer in place of
                        // the complete one that supersedes it minutes later.
                        if (!local || revision?.stamp != null) {
                            synchronized(store) { store.save(storageKey, whole) }
                        }
                    } else if (outcome.decodedShort &&
                        shortDecodes.merge(storageKey, 1, Int::plus)!! >= MAX_SHORT_DECODE_ATTEMPTS
                    ) {
                        // Bounded, so a container that is genuinely truncated
                        // isn't re-decoded on every tick for the rest of the
                        // session. Any provisional head result already published
                        // stays: a partial analysis beats an empty one.
                        Log.w(TAG, "Giving up on $trackId after $MAX_SHORT_DECODE_ATTEMPTS short decodes")
                        if (storageKey !in provisional) {
                            results[storageKey] = TrackAnalysis(
                                status = TrackAnalysis.STATUS_READY,
                                trackId = trackId,
                                duration = durationSeconds,
                            )
                        }
                    }
                } else {
                    // Marked before it is published, so a reader on the playback
                    // thread can never see a provisional result that is not
                    // flagged as one.
                    val rendition = checkNotNull(headRendition)
                    val head = try {
                        analyzeHead(trackId, durationSeconds, session) {
                            cache.renditionDataSource(analysisUri, rendition)
                        }
                    } finally {
                        if (!session.isCancelled()) headAttempts[rendition.key] = rendition.cachedPrefix
                    }
                    if (session.isCancelled()) return@run
                    if (head != null) {
                        provisional.add(storageKey)
                        results[storageKey] = head
                    }
                }
            } catch (error: Throwable) {
                if (session.isCancelled()) return@run
                // Throwable, not Exception: decode leans on MediaCodec, and an
                // OOM or a codec-level Error uncaught on a pool thread that is
                // nobody's parent takes the whole app down for work whose
                // entire failure mode is meant to be "this track goes
                // unanalysed".
                Log.w(TAG, "Analysis of $trackId failed", error)
                // A failed head pass records nothing: the whole-track pass reads
                // a different, complete file and deserves its own attempt.
                // The failed attempt is recorded against that cached prefix.
                if (usableComplete) {
                    // Recorded as ready-but-empty so a track that cannot be
                    // analysed is not retried on every tick for the rest of the
                    // session.
                    results[storageKey] = TrackAnalysis(
                        status = TrackAnalysis.STATUS_READY,
                        trackId = trackId,
                        duration = durationSeconds,
                    )
                    provisional.remove(storageKey)
                }
            } finally {
                try {
                    session.release()
                } finally {
                    running.remove(storageKey)
                }
            }
        }
    }

    /**
     * Whether enough of the playback recording is cached to try an opening pass.
     *
     * The byte threshold is derived from the rendition's own average bitrate
     * where the duration is known, because "30 seconds of audio" is a wildly
     * different number of bytes at 96 kbps and at lossless.
     * [HEAD_BYTES_MARGIN] covers the container header and the fact that a
     * track's opening is rarely at its own average bitrate.
     *
     * Where the duration isn't known — which is the common case, since callers
     * request analysis before anything has read the container — the estimate is
     * unavailable and [MIN_HEAD_BYTES] stands in. That is about 30 s of a
     * typical stream but only a few seconds of lossless, so a single attempt
     * gated on it would be spent on too little audio for exactly the tracks
     * that carry the most bytes per second.
     *
     * Hence retrying on growth rather than attempting once: an attempt is
     * allowed again only when the cached prefix has [HEAD_RETRY_GROWTH]-fold
     * grown since the last one. A track therefore gets a handful of tries
     * spread across its download instead of either one try or one per tick.
     */
    private fun headWorthTrying(
        trackId: String,
        playbackKey: String,
        durationSeconds: Double,
    ): AudioCache.Rendition? {
        val candidate = cache.playbackRendition(playbackKey)?.takeUnless { it.key in badRenditions }
            ?: return null
        val previous = headAttempts[candidate.key]
        if (previous != null && candidate.cachedPrefix < previous * HEAD_RETRY_GROWTH) return null

        val prefix = candidate.cachedPrefix
        val total = candidate.contentLength
        val needed = if (durationSeconds.isFinite() && durationSeconds > MIN_HEAD_SECONDS && total > 0) {
            val bytesPerSecond = total / durationSeconds
            // Sized to [MIN_HEAD_SECONDS] — the shortest decode [analyzeHead]
            // will accept — not to the model's full window. Gating on the full
            // window meant demanding two and a half times the input the analysis
            // would actually settle for: a lossless rendition needs nine
            // megabytes on disk for thirty seconds of audio, and a track sitting
            // at six was refused outright despite holding twice what was needed
            // to produce a result. Whatever *is* cached still gets decoded — the
            // read simply runs out — so a larger prefix is used when there is
            // one, and [HEAD_RETRY_GROWTH] comes back for a better look as the
            // rest arrives.
            (MIN_HEAD_SECONDS * bytesPerSecond * HEAD_BYTES_MARGIN).toLong()
                .coerceAtLeast(MIN_HEAD_BYTES)
                .coerceAtMost(total)
        } else {
            MIN_HEAD_BYTES
        }
        if (prefix < needed) {
            // Once per track, not per tick: a head pass that never fires is
            // invisible otherwise, which is exactly how the first version of
            // this shipped doing nothing at all.
            if (headSkipLogged.add("$trackId@${candidate.key}")) {
                Log.d(
                    TAG,
                    "Head pass for $trackId waiting: ${prefix / 1024}kB cached of " +
                        "${needed / 1024}kB needed (rendition ${candidate.key})",
                )
            }
            return null
        }

        return candidate
    }

    /**
     * The opening window only: a beat grid, and nothing that would need the rest
     * of the file.
     *
     * Runs [TrackFeatures] over the head, but copies across only the fields
     * that describe an *entry*: where the file starts making sound, the pickup,
     * the end of the intro, and the mix-in candidates. Those are all measured
     * within the opening seconds, so a head-only pass measures them exactly as
     * a whole-track pass would.
     *
     * Everything that describes the rest of the track is dropped on the floor —
     * content end, outro, mix-out anchors, the energy curve. Over a 30 s head
     * that pass does not fail, it answers confidently about a track that is
     * mostly missing, and the planner has no way to tell the difference. Left at
     * their defaults they read as "no evidence": [contentEndTime] falls back to
     * the real duration and the mix-out list ranks as empty.
     *
     * The energy curve is dropped for the same reason even though it is
     * genuinely measured here: the policy indexes the vocal mask against it and
     * counts audible seconds from it, and a curve that stops at 30 s would have
     * this track's *outgoing* half scored against a window it does not cover.
     * A vocal mask therefore cannot come from this pass either, and waits for
     * the whole-track one.
     */
    private fun analyzeHead(
        trackId: String,
        durationSeconds: Double,
        session: AnalysisModels,
        openSource: () -> MediaDataSource?,
    ): TrackAnalysis? {
        val window = BeatTracker.WINDOW_SECONDS
        val head = region(openSource, 0.0, window, session, features = null, deriveFeatures = true)
            ?: run {
                // An abandoned decode comes back the same way a broken one
                // does, and only one of the two is worth a warning: see
                // [AnalysisModels.isCancelled]. Nothing is recorded either way — the caller
                // publishes only a non-null result — so the track is simply
                // left unmeasured, which is what it is.
                if (session.isCancelled()) return null
                Log.d(TAG, "Head pass for $trackId could not decode the playback recording")
                return null
            }
        // What was decoded, not what was asked for: the source stops where the
        // cache does. A tempo read off a few seconds is not a weaker measurement
        // than one read off thirty, it is a different and much more credulous
        // one, and the planner cannot see the difference — so it is refused here
        // and the next attempt gets more of the file.
        if (head.seconds < MIN_HEAD_SECONDS) {
            Log.d(TAG, "Head pass for $trackId decoded only ${"%.1f".format(Locale.ROOT, head.seconds)}s; too short")
            return null
        }
        val primary = head.grid
        // A partial cache can supply the first window without supplying a
        // shifted one. Leave the provisional result available in that case.
        val grid = if (primary != null && primary.canBeConfirmed() &&
            head.seconds >= window - 0.1 && durationSeconds >= window + BEAT_CONFIRMATION_SHIFT_SECONDS
        ) {
            val nearby = region(
                openSource, BEAT_CONFIRMATION_SHIFT_SECONDS, window + BEAT_CONFIRMATION_SHIFT_SECONDS,
                session, features = null,
            )?.grid
            primary.confirmedBy(nearby)
        } else {
            primary
        }
        if (session.isCancelled()) return null
        val entry = head.features
        if (grid == null && entry == null) {
            Log.d(TAG, "Head pass for $trackId produced nothing usable")
            return null
        }

        Log.d(
            TAG,
            "Analysed head of $trackId: bpm=${grid?.bpm ?: entry?.bpm} " +
                "conf=${grid?.beatConfidence ?: entry?.beatConfidence} " +
                "audibleStart=${entry?.audibleStartTime} pickup=${entry?.pickupTime} " +
                "introEnd=${entry?.introEndTime} mixInCandidates=${entry?.mixInCandidates?.size ?: 0} " +
                "over ${"%.1f".format(Locale.ROOT, head.seconds)}s",
        )

        return TrackAnalysis(
            status = TrackAnalysis.STATUS_READY,
            trackId = trackId,
            duration = durationSeconds,
            bpm = grid?.bpm ?: entry?.bpm ?: 0.0,
            beatInterval = grid?.beatInterval ?: entry?.beatInterval ?: 0.0,
            beatConfidence = grid?.beatConfidence ?: entry?.beatConfidence ?: 0.0,
            downbeats = grid?.downbeats ?: entry?.downbeats.orEmpty(),
            firstBeat = grid?.firstBeat ?: entry?.firstBeat ?: 0.0,
            key = entry?.key.orEmpty(),
            keyConfidence = entry?.keyConfidence ?: 0.0,
            audibleStartTime = entry?.audibleStartTime,
            pickupTime = entry?.pickupTime,
            introEndTime = entry?.introEndTime ?: 0.0,
            mixInTime = entry?.mixInTime ?: 0.0,
            mixInCandidates = entry?.mixInCandidates.orEmpty(),
            headBeats = grid?.let { BeatSide(it.beats, it.downbeats, it.bpm, it.beatConfidence) },
        )
    }

    private class Copy(
        val key: String,
        val rendition: AudioCache.Rendition?,
        val closeSource: () -> Unit = {},
        val open: () -> MediaDataSource?,
    ) : java.io.Closeable {
        override fun close() = closeSource()
    }

    private fun copyToRead(
        trackId: String,
        uri: Uri,
        playbackKey: String? = null,
        abort: () -> Boolean = { stopped },
    ): Copy? {
        if (LocalAudioSource.isLocal(uri)) {
            val recording = LocalAudioSource.Recording(context, uri, abort)
            return Copy(analysisKey(trackId, uri), null, recording::close, recording::open)
        }
        val key = playbackKey ?: cache.cacheKeyOf(uri)
        val rendition = cache.playbackRendition(key)?.takeIf { it.isComplete && key !in badRenditions }
            ?: return null
        return Copy(key, rendition) { cache.renditionDataSource(uri, rendition) }
    }

    /**
     * What a whole-track pass came back with.
     *
     * [decodedShort] is the difference between "this copy is broken, strike it"
     * and "there was no copy to read", which the caller counts very differently:
     * three strikes writes a track off for the session. Conflating the two spent
     * all three in 922ms on a track whose only complete copy had just been
     * rejected — the following two attempts decoded nothing because there was
     * nothing left to decode, and were counted as though they had tried.
     */
    private class WholeTrack(val analysis: TrackAnalysis?, val decodedShort: Boolean = false)

    /**
     * What Pass 1 came back with.
     *
     * [decodedShort] has to survive the return rather than collapsing into a null [features]: it is
     * the same distinction [WholeTrack.decodedShort] draws, between a broken copy and no copy, and
     * only one of the two is a strike.
     */
    private class Structural(
        val features: TrackFeatures.Features?,
        val decodedShort: Boolean = false,
    )

    /**
     * Decodes the whole track and reduces it to DSP features.
     *
     * A method rather than a block in [analyze] for a reason that is about memory, not tidiness —
     * see the call site. Everything it decodes is dead by the time it returns, and returning is
     * what makes that true of the heap as well as of the program.
     */
    private fun structure(
        trackId: String,
        uri: Uri,
        copy: Copy,
        effectiveDuration: Double,
        storageKey: String,
        session: AnalysisModels,
    ): Structural {
        val structRate = TrackFeatures.sampleRate
        val decoded = copy.open()?.use { AudioDecoder.decodeRegion(it, 0.0, effectiveDuration, session::isCancelled) }
            ?: return Structural(null)
        if (session.isCancelled()) return Structural(null)
        val (pcm, _) = decoded

        // A decode that stops early is indistinguishable, downstream, from a
        // track that simply goes quiet: [TrackFeatures] is handed the
        // container's full duration alongside a short buffer, reads the
        // difference as trailing silence, and puts the mix-out anchor where the
        // bytes ran out. Nothing about the result looks wrong — it is a complete
        // analysis with a plausible contentEnd — and the audible symptom is the
        // track being faded out minutes early. Refused outright rather than
        // published, because a missing analysis degrades to a plain crossfade
        // while a confidently wrong one does not degrade at all.
        val decodedSeconds = if (pcm.sampleRate > 0) pcm.samples.size / pcm.sampleRate else 0.0
        if (decodedSeconds < effectiveDuration * MIN_DECODED_FRACTION) {
            Log.w(
                TAG,
                "Analysis of $trackId refused: ${copy.key} decoded " +
                    "${"%.1f".format(Locale.ROOT, decodedSeconds)}s of a " +
                    "${"%.1f".format(Locale.ROOT, effectiveDuration)}s " +
                    // The two causes are different enough to be worth naming. A
                    // cached rendition stops at the first hole read-ahead left in
                    // it; a file on the device has no holes, so a short decode
                    // there means the container itself is truncated or damaged.
                    if (copy.rendition != null) "container — cached with holes?" else "container — truncated file?",
            )
            // Both halves of this are about *renditions*, so both are conditional
            // on there being one. A local file has no sibling copy to be routed to
            // instead and is not ours to delete either way; the three-strike count
            // in [request] is the whole bound there, and it is enough, because a
            // file on disk that decodes short will decode short again.
            copy.rendition?.let { rendition ->
                // Remembered, so the retry reaches for a *different* rendition. This
                // is the whole reason the lightest one is only a preference: a
                // rendition the cache index calls complete can still decode short if
                // it was written badly, and without this the retries would pick the
                // same broken copy three times over and give up on a track whose
                // heavier rendition would have analysed perfectly well.
                badRenditions.add(rendition.key)
                // And thrown off disk, not merely remembered. A file the cache calls
                // complete and the decoder gives up on partway is not going to
                // improve: nothing else will ever write to it, because as far as the
                // cache is concerned it is finished. Remembering it only helps for as
                // long as this process lives — a restart clears the set, the same
                // bytes are read again, and the same seconds are spent reaching the
                // same conclusion. Deleting it is what lets a clean copy be fetched.
                // Skipped for whatever the player is reading from; see
                // [AudioCache.discardBadRendition].
                if (rendition.isComplete && discardsOf(rendition.key) < MAX_RENDITION_DISCARDS &&
                    cache.discardBadRendition(uri, rendition.key)
                ) {
                    // Every memory of that copy goes with the bytes. Deleting the
                    // file and then still refusing its key is the worst of both: the
                    // clean copy [AudioCache.requestAnalysisHead] fetches in its place
                    // is filtered straight back out by [badRenditions], the track is
                    // written off for the session anyway, and the download was spent
                    // on nothing. The strike count goes too — the next attempt reads
                    // genuinely different bytes, so it starts level.
                    discarded.merge(rendition.key, 1, Int::plus)
                    badRenditions.remove(rendition.key)
                    triedRenditions.remove(rendition.key)
                    shortDecodes.remove(storageKey)
                }
            }
            return Structural(null, decodedShort = true)
        }

        val samples = if (abs(pcm.sampleRate - structRate) > 1.0) {
            TrackFeatures.resample(pcm.samples, pcm.sampleRate, structRate) ?: return Structural(null)
        } else {
            pcm.samples
        }

        return Structural(TrackFeatures.analyze(samples, effectiveDuration))
    }

    /**
     * The whole-track pass. A null [WholeTrack.analysis] means "not now, try
     * again"; see the short-decode guard below, which is the one condition that
     * produces a confident-looking analysis that is wrong by minutes rather than
     * merely absent, and the only one that counts as a strike.
     */
    private fun analyze(
        trackId: String,
        uri: Uri,
        durationSeconds: Double,
        storageKey: String,
        playbackKey: String?,
        prioritizeIncomingHead: Boolean,
        session: AnalysisModels,
        recordingStillMatches: () -> Boolean,
    ): WholeTrack {
        val copy = copyToRead(trackId, uri, playbackKey, session::isCancelled) ?: return WholeTrack(null)
        return copy.use { source ->
            val knownDuration = if (prioritizeIncomingHead && durationSeconds <= 0) {
                source.open()?.use(AudioDecoder::containerDurationSeconds) ?: durationSeconds
            } else {
                durationSeconds
            }
            if (prioritizeIncomingHead && knownDuration >= MIN_HEAD_SECONDS) {
                val head = try {
                    analyzeHead(trackId, knownDuration, session, source.open)
                } catch (error: Exception) {
                    Log.w(TAG, "Could not prepare the entry of $trackId", error)
                    null
                }
                if (head?.isUsable == true && !session.isCancelled() && recordingStillMatches()) {
                    provisional.add(storageKey)
                    results[storageKey] = head
                }
            }
            if (session.isCancelled()) return WholeTrack(null)
            val firstAttempt = triedRenditions.add(source.key)
            try {
                analyzeCopy(trackId, uri, source, knownDuration, storageKey, session)
            } finally {
                if (firstAttempt && session.isCancelled()) triedRenditions.remove(source.key)
            }
        }
    }

    private fun analyzeCopy(
        trackId: String,
        uri: Uri,
        copy: Copy,
        durationSeconds: Double,
        storageKey: String,
        session: AnalysisModels,
    ): WholeTrack {
        val openSource = copy.open
        val effectiveDuration = openSource()?.use(AudioDecoder::containerDurationSeconds)
            ?: durationSeconds.takeIf { it.isFinite() && it > 0 } ?: 0.0
        if (session.isCancelled()) return WholeTrack(null)
        if (effectiveDuration <= 0) {
            Log.d(TAG, "Skipping $trackId: ${copy.key} has no readable duration")
            if (copy.rendition == null) {
                // A file on the device whose container won't state a runtime is
                // not a broken file: see [unknownRuntime]. Left unrecorded, and
                // forgotten as tried, so it is measured once a runtime arrives
                // instead of being written off while it is still only queued.
                unknownRuntime.add(storageKey)
                triedRenditions.remove(copy.key)
                return WholeTrack(null)
            }
            return WholeTrack(empty(trackId, 0.0))
        }
        unknownRuntime.remove(storageKey)

        // Pass 1 (Phase 1, DSP-only): the analyzer needs the whole track — the energy curve,
        // phrase structure and mix-out anchor all read the tail, not just a window of it — at its
        // own low sample rate, so this is a much smaller decode than a full-rate pass would be.
        //
        // In a frame of its own, and handing back only the features, because of what it allocates
        // to get them: the whole track decoded to mono at the container's rate (35 MB for a
        // 3.5-minute song) plus the resampled copy the DSP reads (8 MB). Neither is touched again
        // after this line, but a local holding either stays reachable to the end of the method, and
        // the rest of the method is Pass 2 — the most allocation-heavy part of the analysis.
        // Measured on the API 28 emulator: those two buffers were 43 MB of an 82 MB live set, still
        // held while the models ran, in a process that was reaching a 256 MB heap limit and had
        // died on it. Returning is what releases them — a `val` cannot be nulled, and a narrower
        // scope alone does not make ART treat one as dead.
        val structural = structure(trackId, uri, copy, effectiveDuration, storageKey, session)
        // A cancelled analysis can be retried when conditions allow it.
        if (session.isCancelled()) return WholeTrack(null)
        if (structural.decodedShort) return WholeTrack(null, decodedShort = true)
        val features = structural.features ?: return WholeTrack(empty(trackId, effectiveDuration))

        // Pass 2 (Phases 2 and 3, models): the Beat This! grid and the open-unmix vocal mask, over
        // the opening and the ending only. A transition only ever reads the tail of the outgoing
        // track and the head of the incoming one, and a track is both of those at different
        // moments, so the middle is never decoded for this. Both models read the same decoded
        // window, so its stereo buffer is paid for once. The spans match the loop mix's searches:
        // its incoming loop is looked for in the first 45 s, its outgoing one in the last 60.
        val span = modelWindowSeconds()
        // Spans that the windows tile exactly, overlaps included: one window more
        // here is a whole extra pass of both models.
        val headEnd = minOf(effectiveDuration, HEAD_MODEL_WINDOWS * span - (HEAD_MODEL_WINDOWS - 1) * WINDOW_OVERLAP_SECONDS)
        val tailStart = max(0.0, effectiveDuration - (TAIL_MODEL_WINDOWS * span - (TAIL_MODEL_WINDOWS - 1) * WINDOW_OVERLAP_SECONDS))
        val regions = ArrayList<Region>()
        for ((from, to) in modelWindows(headEnd, tailStart, effectiveDuration, span)) {
            // Checked between windows: an abort leaves a window missing, which
            // is indistinguishable from one the models found nothing in — and
            // that would be published, and persisted, with the DSP estimate
            // standing in. A result assembled out of a half-finished analysis
            // is worse than none.
            if (session.isCancelled()) return WholeTrack(null)
            region(openSource, from, to, session, features)?.let(regions::add)
        }
        if (session.isCancelled()) return WholeTrack(null)

        val measured = regions.mapNotNull { region -> region.grid?.let { region to it } }.sortedBy { it.first.start }
        val confirmed = measured.mapIndexed { index, (region, grid) ->
            val previous = measured.getOrNull(index - 1)?.takeIf { (other, _) ->
                other.start + other.seconds > region.start
            }?.second
            val next = measured.getOrNull(index + 1)?.takeIf { (other, _) ->
                region.start + region.seconds > other.start
            }?.second
            region to grid.confirmedBy(previous).confirmedBy(next)
        }
        val headSide = beatSide(confirmed, 0.0, headEnd)
        val tailSide = beatSide(confirmed, tailStart, effectiveDuration)
        val headGrid = headSide?.toGrid()
        val tailGrid = tailSide?.toGrid()

        // The tail governs where the outgoing track is mixed out, so it takes precedence; the
        // head is what a track uses when it is the *incoming* side of a different transition.
        val leading = tailGrid ?: headGrid

        Log.d(
            TAG,
            "Analysed $trackId: bpm=${leading?.bpm ?: features.bpm} " +
                "conf=${leading?.beatConfidence ?: features.beatConfidence} " +
                "key=${features.key} contentEnd=${features.contentEndTime} " +
                "mixOutCandidates=${features.mixOutCandidates.size} " +
                "grid=${if (leading != null) "model" else "dsp"} " +
                "beats=${headGrid?.beats?.size ?: 0}+${tailGrid?.beats?.size ?: 0} " +
                "downbeats=${headGrid?.downbeats?.size ?: 0}+${tailGrid?.downbeats?.size ?: 0} " +
                "vocalMask=${if (regions.any { it.vocalMask != null }) "model" else "dsp"} " +
                "windows=${regions.size} vocalWindows=${regions.count { it.vocalWindow != null }}",
        )

        return WholeTrack(
                TrackAnalysis(
                    status = TrackAnalysis.STATUS_READY,
                    trackId = trackId,
                    duration = effectiveDuration,
                    contentEndTime = features.contentEndTime.takeIf { it > 0 } ?: effectiveDuration,
                    bpm = leading?.bpm ?: features.bpm,
                beatInterval = leading?.beatInterval ?: features.beatInterval,
                beatConfidence = leading?.beatConfidence ?: features.beatConfidence,
                downbeats = (headGrid?.downbeats.orEmpty() + tailGrid?.downbeats.orEmpty())
                    .ifEmpty { features.downbeats }
                    .sorted(),
                firstBeat = headGrid?.firstBeat ?: features.firstBeat,
                phraseBoundaries = features.phraseBoundaries,
                key = features.key,
                keyConfidence = features.keyConfidence,
                audibleStartTime = features.audibleStartTime,
                pickupTime = features.pickupTime,
                introEndTime = features.introEndTime,
                outroStartTime = features.outroStartTime,
                mixInTime = features.mixInTime,
                mixOutTime = features.mixOutTime,
                mixInCandidates = features.mixInCandidates,
                mixOutCandidates = features.mixOutCandidates,
                energyCurve = features.energyCurve,
                lowEnergyCurve = features.lowEnergyCurve,
                // The model's mask where it ran, the DSP heuristic's where it didn't. Falling back to
                // the heuristic rather than to nothing matters because the policy reads an
                // absent mask and a neutral one identically — as "no evidence" — so a failed model
                // pass would otherwise silently discard the estimate Phase 1 already had.
                vocalActivityMask = mergeMasks(features.energyCurve.size, regions.mapNotNull { it.vocalMask })
                    ?: features.vocalActivityMask.map(::heuristicVocalOnModelScale),
                vocalProbability = features.vocalProbability,
                headBeats = headSide,
                tailBeats = tailSide,
                vocalWindows = regions.mapNotNull { it.vocalWindow },
                levelWindows = regions.mapNotNull { it.levelWindow },
            ),
        )
    }

    /**
     * Everything a decoded region contributes, once its audio has been let go
     * of. [seconds] is what was actually decoded, which for a partially cached
     * file is not what was asked for.
     */
    private class Region(
        val grid: BeatTracker.Grid?,
        val vocalMask: DoubleArray?,
        val seconds: Double,
        /** Only populated when the caller asked for it; see [region]'s `deriveFeatures`. */
        val features: TrackFeatures.Features? = null,
        /** Where the decode really began (the extractor seeks to a sync sample at or before the ask). */
        val start: Double = 0.0,
        /** The vocal model's raw output over this window, for the loop mix's vocal check. */
        val vocalWindow: CurveWindow? = null,
        /** RMS level over this window, for the loop mix's loudness check. */
        val levelWindow: CurveWindow? = null,
    )

    /**
     * Seconds of audio one model window covers: the vocal model's fixed input,
     * which is the shorter of the two models' (the beat model's 1500 frames run
     * to ~30 s). One window is then exactly one inference of each.
     */
    private fun modelWindowSeconds(): Double =
        if (VocalSpectrogram.available) {
            (VocalTracker.FIXED_FRAMES - 2) * VocalSpectrogram.hop / VocalSpectrogram.sampleRate
        } else {
            BeatTracker.WINDOW_SECONDS
        }

    /**
     * The windows the models run over: [HEAD_MODEL_WINDOWS] from the start and
     * [TAIL_MODEL_WINDOWS] up to the end, each [span] long and overlapping its
     * neighbour by [WINDOW_OVERLAP_SECONDS] so the beat at a seam is seen from
     * both sides. A short track, where the two spans meet, is covered once.
     */
    private fun modelWindows(headEnd: Double, tailStart: Double, duration: Double, span: Double): List<Pair<Double, Double>> {
        val spans = if (tailStart <= headEnd) {
            listOf(0.0 to duration)
        } else {
            listOf(0.0 to headEnd, tailStart to duration)
        }
        val windows = ArrayList<Pair<Double, Double>>()
        for ((from, to) in spans) {
            val length = to - from
            if (length < 1.0) continue
            if (length <= span) {
                windows += from to to
                continue
            }
            // As few windows as cover the span with at least the overlap between
            // neighbours, spread evenly from its start to its end.
            val count = ceil((length - WINDOW_OVERLAP_SECONDS) / (span - WINDOW_OVERLAP_SECONDS) - 1e-6).toInt()
            val stride = (length - span) / (count - 1)
            for (index in 0 until count) {
                val start = from + index * stride
                windows += start to minOf(to, start + span)
            }
        }
        return windows
    }

    private fun BeatTracker.Grid.canBeConfirmed(): Boolean =
        beatConfidence >= BEAT_CONFIRMATION_MIN_CONFIDENCE && beatConfidence < MIN_BEATMATCH_CONFIDENCE

    private fun BeatTracker.Grid.confirmedBy(other: BeatTracker.Grid?): BeatTracker.Grid {
        if (!canBeConfirmed() || other == null || other.beatConfidence < MIN_BEATMATCH_CONFIDENCE) return this
        if (!agreesWithNearbyBeatGrid(beats, bpm, other.beats, other.bpm)) return this
        // Keep the measured beats and downbeats of this region. A matching
        // pulse supports beat matching but cannot relocate a bar or cue.
        return copy(beatConfidence = MIN_BEATMATCH_CONFIDENCE)
    }

    /** Keeps the beats on either side of each seam and scores each end independently. */
    private fun beatSide(
        measured: List<Pair<Region, BeatTracker.Grid>>,
        from: Double,
        to: Double,
    ): BeatSide? {
        val beats = ArrayList<Double>()
        val downbeats = ArrayList<Double>()
        var weighted = 0.0
        for ((index, entry) in measured.withIndex()) {
            val (region, grid) = entry
            val previous = measured.getOrNull(index - 1)?.first
            val next = measured.getOrNull(index + 1)?.first
            val after = previous?.let { (it.start + it.seconds + region.start) / 2 }
                ?.takeIf { it > region.start } ?: Double.NEGATIVE_INFINITY
            val before = next?.let { (region.start + region.seconds + it.start) / 2 }
                ?.takeIf { it < region.start + region.seconds } ?: Double.POSITIVE_INFINITY
            val kept = grid.beats.filter { it >= after && it < before && it in from..to }
            beats += kept
            downbeats += grid.downbeats.filter { it >= after && it < before && it in from..to }
            weighted += grid.beatConfidence * kept.size
        }
        val ordered = beats.sorted()
        val bpm = BeatTracker.tempoFromBeats(ordered)
        if (bpm <= 0) return null
        return BeatSide(ordered, downbeats.sorted(), bpm, weighted / ordered.size)
    }

    private fun BeatSide.toGrid() = BeatTracker.Grid(
        beats = beats,
        downbeats = downbeats,
        bpm = bpm,
        beatInterval = 60 / bpm,
        firstBeat = beats.first(),
        beatConfidence = confidence,
    )

    /**
     * Decodes one stereo region and runs both models over it, returning only their results.
     *
     * The point of the function boundary is the audio: the stereo buffer, its mono mix and the
     * resampled copies are all local, so they become collectible the moment this returns rather
     * than staying live until the whole analysis finishes. A 30 s stereo region is several
     * megabytes before either model's own working set is counted.
     *
     * Null, or a null field, means "no model evidence for this window" — a codec that will not
     * configure, a region too short, a missing model — which [analyze] already falls back on.
     *
     * The extractor seeks to a sync sample at or before what was asked for, so the region's real
     * start (not [startSeconds]) is what its beat times must be stated against.
     */
    private fun region(
        openSource: () -> MediaDataSource?,
        startSeconds: Double,
        endSeconds: Double,
        session: AnalysisModels,
        features: TrackFeatures.Features?,
        deriveFeatures: Boolean = false,
    ): Region? {
        if (session.isCancelled()) return null
        val decoded = openSource()?.use { AudioDecoder.decodeRegionStereo(it, startSeconds, endSeconds, session::isCancelled) }
            ?: run {
                // The two ways this comes back empty mean opposite things and
                // were reported identically, which cost a round of guessing:
                // this one is the extractor refusing the container outright, so
                // the bytes are wrong or not enough of them are there to parse.
                Log.d(TAG, "Region [$startSeconds, $endSeconds) would not open")
                return null
            }
        if (session.isCancelled()) return null
        val (stereo, actualStart) = decoded
        if (stereo.left.size < stereo.sampleRate) {
            // And this one is a container that parsed fine and yielded under a
            // second of audio — a decode that started and ran out, not one that
            // never started.
            Log.d(TAG, "Region [$startSeconds, $endSeconds) decoded ${stereo.left.size} frames; too few")
            return null
        }

        val seconds = stereo.left.size / stereo.sampleRate
        // In a frame of its own so the full-rate mono downmix is released before either model runs.
        // It is 23 MB for this window at 48 kHz — the same size as each of the two channels it
        // averages — and it is read exactly twice, to make the resampled model input and the DSP
        // one. As a local it would nonetheless stay reachable through `tracker.track` and
        // `vocalMask` below, which is where the analysis allocates most heavily and where the
        // process was dying. Same reasoning [derived] already had, one level further out.
        val inputs = regionInputs(stereo, seconds, deriveFeatures)

        if (session.isCancelled()) return null
        val vocalValues = if (features != null) session.vocals(stereo) else null
        if (session.isCancelled()) return null
        return Region(
            grid = inputs.forModel?.let { session.beats(it, actualStart) },
            vocalMask = if (features != null && vocalValues != null) vocalMask(vocalValues, features, actualStart) else null,
            seconds = seconds,
            features = inputs.derived,
            start = actualStart,
            vocalWindow = vocalValues?.let { CurveWindow(actualStart, VocalSpectrogram.frameRate, it) },
            levelWindow = if (features != null) levelCurve(stereo, actualStart) else null,
        )
    }

    /**
     * RMS of the stereo mix in frames of 1 / [LEVEL_FRAME_RATE] s, on the
     * [levelFromRms] scale (stored in hundredths, so it has to fit 0..1). Every
     * fourth sample: plenty for a level, and a quarter of the reads.
     */
    private fun levelCurve(stereo: AudioDecoder.StereoPcm, actualStart: Double): CurveWindow? {
        val frame = (stereo.sampleRate / LEVEL_FRAME_RATE).toInt()
        if (frame <= 0) return null
        val count = stereo.left.size / frame
        if (count <= 0) return null
        val values = FloatArray(count)
        for (index in 0 until count) {
            var sum = 0.0
            var n = 0
            var sample = index * frame
            val end = sample + frame
            while (sample < end) {
                val mixed = (stereo.left[sample] + stereo.right[sample]) * 0.5f
                sum += mixed * mixed
                n++
                sample += 4
            }
            values[index] = levelFromRms(kotlin.math.sqrt(sum / n)).toFloat()
        }
        return CurveWindow(actualStart, LEVEL_FRAME_RATE, values)
    }

    /** A region's model input, and its DSP features when the caller asked for them. */
    private class RegionInputs(val forModel: FloatArray?, val derived: TrackFeatures.Features?)

    /**
     * Reduces a decoded region to the buffers the models and the DSP actually read.
     *
     * Both come off one full-rate mono downmix, which is why they are made together rather than on
     * demand: that downmix is the largest single allocation in the analysis, and returning is the
     * only way to be rid of it before the models run.
     */
    private fun regionInputs(
        stereo: AudioDecoder.StereoPcm,
        seconds: Double,
        deriveFeatures: Boolean,
    ): RegionInputs {
        val mono = FloatArray(stereo.left.size) { index -> (stereo.left[index] + stereo.right[index]) * 0.5f }
        val forModel = if (abs(stereo.sampleRate - MelSpectrogram.sampleRate) > 1.0) {
            MelSpectrogram.resample(mono, stereo.sampleRate, MelSpectrogram.sampleRate)
        } else {
            mono
        }

        // Derived here rather than by the caller so the mono buffer is still
        // live: handing it back would keep several megabytes reachable for the
        // rest of the analysis, which is the one thing this function exists to
        // avoid.
        val derived = if (deriveFeatures) {
            val forFeatures = if (abs(stereo.sampleRate - TrackFeatures.sampleRate) > 1.0) {
                TrackFeatures.resample(mono, stereo.sampleRate, TrackFeatures.sampleRate)
            } else {
                mono
            }
            forFeatures?.let { TrackFeatures.analyze(it, seconds) }
        } else {
            null
        }

        return RegionInputs(forModel, derived)
    }

    /**
     * A vocal-presence value for every point on the energy curve, filled only where the model
     * actually ran.
     *
     * The policy indexes the mask against energy-curve sample times and requires the two to be the
     * same length, but the model's window is fixed at about 22 seconds, far less than a track. So
     * the mask is built at full length and filled only over this region.
     *
     * Everywhere else stays at [NEUTRAL_VOCAL]. That is not a guess dressed up as data: it sits
     * below the policy's own VOCAL_ACTIVE_THRESHOLD, so unmeasured material can never trip vocal
     * logic in either direction.
     */
    private fun vocalValues(stereo: AudioDecoder.StereoPcm, vocals: VocalTracker): FloatArray? {
        if (!VocalSpectrogram.available) return null
        // [VocalTracker.track] refuses anything wider than its graph, so a window that came back a
        // little long is trimmed. Two frames of margin absorb the ±1 sample a rate conversion can
        // land on.
        val maxSeconds = (VocalTracker.FIXED_FRAMES - 2) * VocalSpectrogram.hop / VocalSpectrogram.sampleRate
        val maxSamples = (maxSeconds * stereo.sampleRate).toInt().coerceAtMost(stereo.left.size)
        if (maxSamples <= 0) return null
        val left = if (maxSamples < stereo.left.size) stereo.left.copyOf(maxSamples) else stereo.left
        val right = if (maxSamples < stereo.right.size) stereo.right.copyOf(maxSamples) else stereo.right
        return vocals.track(left, right, stereo.sampleRate)
    }

    private fun vocalMask(
        values: FloatArray,
        features: TrackFeatures.Features,
        actualStart: Double,
    ): DoubleArray? {
        val curve = features.energyCurve
        if (curve.isEmpty()) return null

        val mask = DoubleArray(curve.size) { NEUTRAL_VOCAL }
        for (index in curve.indices) {
            val frame = ((curve[index].time - actualStart) * VocalSpectrogram.frameRate).toInt()
            if (frame in values.indices) mask[index] = values[frame].toDouble()
        }
        return mask
    }

    /**
     * Overlays the head and tail masks onto one full-length curve, or null when neither ran —
     * which the caller answers by keeping the DSP heuristic rather than reporting a mask of
     * nothing but [NEUTRAL_VOCAL].
     */
    private fun mergeMasks(size: Int, masks: List<DoubleArray>): List<Double>? {
        if (size <= 0 || masks.isEmpty()) return null
        val merged = DoubleArray(size) { NEUTRAL_VOCAL }
        for (source in masks) {
            for (index in merged.indices) {
                if (index < source.size && source[index] != NEUTRAL_VOCAL) merged[index] = source[index]
            }
        }
        return merged.toList()
    }

    /** Recorded ready-but-empty so a track that cannot be decoded is not retried every tick. */
    private fun empty(trackId: String, durationSeconds: Double) = TrackAnalysis(
        status = TrackAnalysis.STATUS_READY,
        trackId = trackId,
        duration = durationSeconds,
    )

    /**
     * Stereo audio of [fromSeconds]..[toSeconds] of [trackId] at [sampleRate],
     * for a loop mix deck, read off the copy analysis measured (so the deck's
     * loop is cut from the same audio its downbeats were found in). Null for a
     * source-backed track: its analysis reads a matched YouTube recording, not
     * what the player plays, and a deck spliced in from another master would
     * land off the song it takes over from. Blocking — call it off the main
     * thread.
     */
    /**
     * Loop decks must contain the very recording that owns the beat grid and
     * is currently playing. A different YouTube master may have the same song
     * title, duration and even BPM, but its downbeat positions can differ.
     *
     * Source-backed tracks and YouTube substitutions are safe only after
     * their own complete cached rendition was analysed under its own key.
     * An Opus grid cannot be borrowed by a #alt FLAC recording.
     */
    fun canLoopMix(trackId: String, uri: Uri): Boolean {
        val playbackKey = if (LocalAudioSource.isLocal(uri)) null else cache.cacheKeyOf(uri)
        val storageKey = analysisKey(trackId, uri, playbackKey)
        if (results[storageKey]?.isUsable != true || storageKey in provisional) return false
        return playbackKey == null || cache.playbackRendition(playbackKey)?.isComplete == true
    }

    fun loopRegion(
        trackId: String,
        uri: Uri,
        durationSeconds: Double,
        fromSeconds: Double,
        toSeconds: Double,
        sampleRate: Int,
    ): com.music.aurelis.playback.loopmix.LoopRegion? {
        val began = SystemClock.elapsedRealtime()
        val copy = runCatching { copyToRead(trackId, uri) }.getOrNull() ?: return null
        val (stereo, start) = copy.use {
            it.open()?.use { source ->
                AudioDecoder.decodeRegionStereo(source, max(0.0, fromSeconds), toSeconds) { stopped }
            }
        } ?: return null
        if (stereo.left.isEmpty()) return null
        val rate = sampleRate.toDouble()
        val left: FloatArray
        val right: FloatArray
        if (abs(stereo.sampleRate - rate) > 0.5) {
            left = MelSpectrogram.resample(stereo.left, stereo.sampleRate, rate) ?: return null
            right = MelSpectrogram.resample(stereo.right, stereo.sampleRate, rate) ?: return null
        } else {
            left = stereo.left
            right = stereo.right
        }
        Log.d(
            TAG,
            "Loop region $trackId from ${copy.key}: asked ${"%.2f".format(Locale.ROOT, fromSeconds)}s, " +
                "got ${"%.3f".format(Locale.ROOT, start)}s + ${"%.2f".format(Locale.ROOT, left.size / rate)}s " +
                "at ${stereo.sampleRate.toInt()}->$sampleRate Hz in ${SystemClock.elapsedRealtime() - began}ms",
        )
        return com.music.aurelis.playback.loopmix.LoopRegion(left, right, start)
    }

    /**
     * A three-band waveform of [fromSeconds]..[toSeconds] of [trackId], for the
     * Automix popup, read off the same copy analysis measured; null when there
     * is no such copy to read. Blocking — call it off the main thread.
     */
    fun mixWaveform(
        trackId: String,
        uri: Uri,
        durationSeconds: Double,
        fromSeconds: Double,
        toSeconds: Double,
        bins: Int,
    ): MixWaveform.Bands? {
        // Read exactly the playback recording, never an unrelated YouTube search match.
        val began = SystemClock.elapsedRealtime()
        val copy = runCatching { copyToRead(trackId, uri) }.getOrNull() ?: return null
        val chosen = SystemClock.elapsedRealtime()
        val bands = copy.use {
            it.open()?.use { source -> MixWaveform.build(source, fromSeconds, toSeconds, bins) { stopped } }
        }
        Log.d(
            TAG,
            "Mix waveform $trackId: copy ${chosen - began}ms, decode ${SystemClock.elapsedRealtime() - chosen}ms, " +
                "asked ${"%.1f".format(Locale.ROOT, fromSeconds)}s, decoder started " +
                "${bands?.decodedFrom?.let { "%.1f".format(Locale.ROOT, it) } ?: "-"}s",
        )
        return bands
    }

    @Synchronized
    fun release() {
        released = true
        executor.shutdownNow()
        restoreExecutor.shutdownNow()
        headAttempts.clear()
        unknownRuntime.clear()
        localRevisions.clear()
        checkingLocalRevisions.clear()
        headSkipLogged.clear()
        provisional.clear()
        restoreLookups.clear()
        shortDecodes.clear()
        badRenditions.clear()
        triedRenditions.clear()
        discarded.clear()
        running.clear()
    }

    private companion object {
        const val TAG = "AurelisTrackAnalyzer"

        fun analysisExecutor(name: String, workers: Int = 1): ExecutorService = Executors.newFixedThreadPool(workers) { runnable ->
            Thread(runnable, name).apply {
                isDaemon = true
                priority = Thread.NORM_PRIORITY
            }
        }

        /**
         * What an unmeasured instant reads as. Below the policy's VOCAL_ACTIVE_THRESHOLD by
         * design, so absence of measurement is never mistaken for absence of a vocal, or for the
         * presence of one.
         */
        const val NEUTRAL_VOCAL = 0.2

        /** Model windows over the opening (~45 s) and the ending (~68 s); see [modelWindowSeconds]. */
        const val HEAD_MODEL_WINDOWS = 2
        const val TAIL_MODEL_WINDOWS = 3

        /** Overlap between neighbouring model windows, so a beat on a seam is seen from both sides. */
        const val WINDOW_OVERLAP_SECONDS = 1.0

        /**
         * How much more than the average-bitrate estimate of the opening window
         * to insist on before decoding it. Covers the container header and the
         * fact that a track's opening is rarely at its own average bitrate.
         */
        const val HEAD_BYTES_MARGIN = 1.35

        /**
         * Floor under the computed threshold, and the whole requirement when the
         * duration is unknown. Roughly fifteen seconds at 128 kbps — a little
         * over [MIN_HEAD_SECONDS], so it guarantees a parsable container and a
         * usable decode without quietly reinstating the thirty-second demand the
         * bitrate estimate was just lowered away from.
         */
        const val MIN_HEAD_BYTES = 256L * 1024L

        /**
         * How much of the container's stated duration must actually decode
         * before the whole-track pass is trusted. Not 1.0: a decoder legitimately
         * comes up a frame or two short of the container's rounding, and
         * refusing over that would refuse everything.
         */
        const val MIN_DECODED_FRACTION = 0.95

        /** Refusals before a track is written off as truncated rather than still filling in. */
        const val MAX_SHORT_DECODE_ATTEMPTS = 3

        /**
         * Clean-slate retries per rendition. One: a discard is worth doing when
         * the bytes on disk are unrecoverable and nothing would otherwise
         * overwrite them, and worth doing exactly once, because a second identical
         * answer means the source is serving that file rather than the cache
         * having mangled it.
         */
        const val MAX_RENDITION_DISCARDS = 1

        const val LOCAL_REVISION_TTL_MS = 15_000L

        /**
         * The least decoded audio a head-only tempo estimate is allowed to rest
         * on. Twelve seconds is around 24 beats at 120 bpm — enough for the
         * grid's own confidence measure to mean something.
         */
        const val MIN_HEAD_SECONDS = 12.0

        /**
         * How much more of the file has to be cached before the head is worth
         * decoding again. Doubling bounds the attempts to a handful over a whole
         * download while still catching up quickly on a high-bitrate rendition
         * whose first attempt covered only a few seconds.
         */
        const val HEAD_RETRY_GROWTH = 2
    }
}

private const val BEAT_CONFIRMATION_MIN_CONFIDENCE = 0.25
private const val BEAT_CONFIRMATION_SHIFT_SECONDS = 6.0

/**
 * Compares measured beats on the same recording timeline. A shared pulse
 * must still agree at both ends of the primary window, after extrapolation.
 * Based on Mornye's neighboring-window consensus, Copyright 2026 zarzet.
 */
internal fun agreesWithNearbyBeatGrid(
    beats: List<Double>,
    bpm: Double,
    nearbyBeats: List<Double>,
    nearbyBpm: Double,
): Boolean {
    if (bpm !in MIN_BPM..MAX_BPM || nearbyBpm !in MIN_BPM..MAX_BPM ||
        abs(nearbyBpm / bpm - 1.0) > 0.02
    ) return false
    val start = beats.firstOrNull() ?: return false
    val end = beats.last()
    val nearbyStart = nearbyBeats.firstOrNull() ?: return false
    val nearbyEnd = nearbyBeats.last()
    val overlapStart = maxOf(start, nearbyStart)
    val overlapEnd = minOf(end, nearbyEnd)
    if (end - start < 6.0 || overlapEnd - overlapStart < 3.0) return false

    val middle = (overlapStart + overlapEnd) / 2.0
    val reference = nearbyBeats.minBy { abs(it - middle) }
    val period = 60.0 / nearbyBpm
    fun phaseError(position: Double): Double {
        val elapsed = position - reference
        return abs(elapsed - round(elapsed / period) * period)
    }
    return phaseError(start) <= 0.04 && phaseError(end) <= 0.04 &&
        phaseError(beats.minBy { abs(it - middle) }) <= 0.04
}

/** Cheap enough for saved local and downloaded tracks, even before decoding starts. */
internal fun TrackAnalysis.isConsistentWithPlayback(playbackDuration: Double): Boolean {
    if (!isUsable || bpm !in MIN_BPM..MAX_BPM || beatConfidence !in 0.0..1.0 ||
        !duration.isFinite() || duration <= 0.0
    ) return false
    if (playbackDuration.isFinite() && playbackDuration > 0.0 &&
        abs(duration - playbackDuration) > maxOf(3.0, playbackDuration * 0.02)
    ) return false

    val timeline = 0.0..(duration + 0.25)
    val audibleStart = audibleStartTime
    if (contentEndTime !in timeline || firstBeat !in timeline ||
        (audibleStart != null && audibleStart !in timeline)
    ) return false

    fun measured(times: List<Double>): Boolean =
        times.indices.all { index -> times[index] in timeline && (index == 0 || times[index] >= times[index - 1]) }

    if (!measured(downbeats)) return false
    return listOfNotNull(headBeats, tailBeats).all { side ->
        side.bpm in MIN_BPM..MAX_BPM && side.confidence in 0.0..1.0 &&
            measured(side.beats) && measured(side.downbeats)
    }
}
