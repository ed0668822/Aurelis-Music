// Aurelis © 2026. GPL-3.0-or-later
package com.music.aurelis.playback

import android.os.SystemClock
import androidx.media3.common.Player
import com.music.aurelis.data.LocalQueueStore
import com.music.aurelis.data.LocalUserLibrary
import com.music.aurelis.data.SavedLocalQueue
import kotlinx.coroutines.*

/** Owned by the service, so activity recreation cannot duplicate or stop accounting. */
internal class ServicePlaybackRecorder(
    private val player: Player,
    private val library: LocalUserLibrary,
    private val queue: LocalQueueStore,
    scope: CoroutineScope,
    private val canSaveEmpty: () -> Boolean = { true },
    private val event: (com.music.aurelis.data.stats.ListeningEvent) -> Unit = {},
    private val wallClock: () -> Long = System::currentTimeMillis,
    private val clock: () -> Long = SystemClock::elapsedRealtime,
) : Player.Listener {
    private var lastTick = clock()
    private var lastSave = lastTick
    private var song = player.currentMediaItem?.toLocalSong()
    private var advancing = player.isPlaying
    private var sessionId = java.util.UUID.randomUUID().toString()
    private var occurrence = song?.queueEntryId?.takeIf { it.isNotBlank() } ?: java.util.UUID.randomUUID().toString()
    private var sequence = 0L
    private var closed = false
    private val job: Job
    init {
        library.startListeningSession(song?.id)
        player.addListener(this)
        job = scope.launch { while (isActive) { delay(1000); tick() } }
    }
    internal fun tick() {
        if (closed) return
        val now = clock()
        val elapsed = now - lastTick
        lastTick = now // Baseline first: reentrant callbacks cannot count the interval twice.
        if (advancing && elapsed in 1..5000) song?.let { current ->
            library.recordListening(current, elapsed, false)
            val scheme = android.net.Uri.parse(current.contentUri).scheme
            if (scheme == "content" || scheme == "file") event(com.music.aurelis.data.stats.ListeningEvent(
                sessionId, sequence++, current.id, occurrence, wallClock(), elapsed, current.durationMs.coerceAtLeast(0)))
        }
        if (now - lastSave >= 15_000) saveQueue()
    }
    private fun newSession(current: com.music.aurelis.data.model.LocalSong?) {
        song = current
        sessionId = java.util.UUID.randomUUID().toString(); sequence = 0
        occurrence = current?.queueEntryId?.takeIf { it.isNotBlank() } ?: java.util.UUID.randomUUID().toString()
        library.startListeningSession(current?.id)
    }
    override fun onMediaItemTransition(mediaItem: androidx.media3.common.MediaItem?, reason: Int) {
        if (closed) return
        tick(); newSession(mediaItem?.toLocalSong()); advancing = player.isPlaying
    }
    override fun onIsPlayingChanged(isPlaying: Boolean) {
        if (closed) return
        tick(); advancing = isPlaying
    }
    override fun onPlaybackStateChanged(playbackState: Int) {
        if (closed) return
        tick(); advancing = player.isPlaying
    }
    override fun onPositionDiscontinuity(oldPosition: Player.PositionInfo, newPosition: Player.PositionInfo, reason: Int) {
        if (!closed) tick() // Measured time only, never the old/new position difference.
    }
    override fun onEvents(player: Player, events: Player.Events) {
        if (closed) return
        tick()
        val current = player.currentMediaItem?.toLocalSong()
        if (current?.id != song?.id || current?.queueEntryId != song?.queueEntryId) newSession(current) else song = current
        advancing = player.isPlaying
        if (!advancing) library.flush()
        saveQueue()
    }
    private fun saveQueue() {
        // Only an explicit queue edit may replace a recoverable pending restore with empty.
        if (player.mediaItemCount > 0 || canSaveEmpty()) {
            val items = (0 until player.mediaItemCount).map { player.getMediaItemAt(it) }
            val entries = items.mapNotNull { it.queueEntryId?.takeIf { value -> value.isNotBlank() } }
            val stableEntries = if (entries.size == items.size && entries.toSet().size == items.size) entries else emptyList()
            // External controllers may provide URI-only items without a local MediaStore ID.
            // Preserve the last recoverable snapshot rather than crash or shift its indices.
            if (items.all { it.mediaId.isNotBlank() }) queue.save(SavedLocalQueue(items.map { it.mediaId },
                if (items.isEmpty()) 0 else player.currentMediaItemIndex.coerceIn(items.indices),
                player.currentPosition.coerceAtLeast(0), player.repeatMode, player.shuffleModeEnabled,
                stableEntries, items.map { it.queueTier },
                if (player.shuffleModeEnabled) queuePlaybackOrder(player).orEmpty() else emptyList()))
        }
        lastSave = clock()
    }
    fun close() {
        if (closed) return
        tick(); closed = true; saveQueue(); library.flush(); player.removeListener(this); job.cancel()
    }
}
