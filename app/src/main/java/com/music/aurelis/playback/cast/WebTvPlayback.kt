package com.music.aurelis.playback.cast

import android.content.Context
import android.util.Log
import android.os.SystemClock
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject

/** Published web pairing address, visible in the existing output picker. */
internal object WebTvController {
    enum class Failure { START_FAILED, CONNECTION_LOST }
    data class State(
        val url: String? = null,
        val active: Boolean = false,
        val starting: Boolean = false,
        val failure: Failure? = null,
    )
    private val _state = MutableStateFlow(State())
    val state: StateFlow<State> = _state
    private var speaker: WebTvPlayback? = null

    fun attach(target: WebTvPlayback) { speaker = target }
    fun detach(target: WebTvPlayback) {
        if (speaker === target) speaker = null
        _state.value = State()
    }
    fun start() { speaker?.startServer() }
    fun stop(resumeHere: Boolean) { speaker?.stop(resumeHere) }
    fun set(url: String? = _state.value.url, active: Boolean = _state.value.active,
        starting: Boolean = false) {
        _state.value = State(url, active, starting)
    }

    fun fail(reason: Failure) { _state.value = State(failure = reason) }
}

/**
 * Phone stays the only owner of the library/queue. A TV browser receives a
 * temporary web URL, obtains a separate media URL from the existing source
 * resolver, and reports its playhead. No second Android playback engine.
 */
internal class WebTvPlayback(
    private val context: Context,
    private val scope: CoroutineScope,
    private val localPlayer: () -> ExoPlayer?,
    private val resolve: suspend (MediaItem) -> CastStream?,
    private val onReleased: () -> Unit,
) : RemoteSpeaker {
    override var sink: CastSink? = null
    override var active = false
        private set
    private var server: LocalTvWebServer? = null
    private var startJob: Job? = null
    private var mediaJob: Job? = null
    private var ticker: Job? = null
    private var generation = 0
    private var mediaId: String? = null
    private var streamUrl: String? = null
    private var mimeType: String? = null
    private var title = ""
    private var artist = ""
    private var artworkUrl: String? = null
    private var position = 0L
    private var duration = C.TIME_UNSET
    private var paused = true
    private var browserSeen = false
    private var lastBrowserHeartbeatMs = 0L
    private var commandSeq = 0
    private var command = "none"
    private var commandPosition = 0L
    private var lyricRevision = 0
    private var lyricLines: List<com.music.aurelis.data.lyrics.LyricLine>? = null
    private var lyricOffset = 0
    private var lyricMediaId: String? = null

    @Volatile private var stateJson = "{}"
    @Volatile private var lyricsJson = "{}"

    override val playWhenReady: Boolean get() = active && !paused
    override val isPlaying: Boolean get() = active && !paused
    override val playbackState: Int get() = if (active) Player.STATE_READY else Player.STATE_IDLE
    override val positionMs: Long get() = position
    override val durationMs: Long get() = duration
    override val deviceVolume: Int get() = 50

    fun startServer() {
        if (server != null || startJob?.isActive == true) return
        val serial = ++generation
        WebTvController.set(starting = true)
        startJob = scope.launch {
            val ready = withContext(Dispatchers.IO) {
                val ip = TvLan.address(context) ?: return@withContext null
                val page = runCatching { context.assets.open("webtv/index.html").use { it.readBytes() } }
                    .getOrNull() ?: return@withContext null
                val http = LocalTvWebServer(
                    ip, page, { stateJson }, { lyricsJson },
                    { event -> scope.launch { onBrowserEvent(event) } },
                    { scope.launch { onBrowserHeartbeat() } },
                )
                val url = http.start()
                if (url == null) { http.close(); null } else http to url
            }
            if (serial != generation) {
                ready?.first?.close()
                return@launch
            }
            if (ready == null) {
                WebTvController.fail(WebTvController.Failure.START_FAILED)
                return@launch
            }
            server = ready.first
            WebTvController.set(url = ready.second, starting = false)
            refreshMedia(localPlayer()?.currentMediaItem)
            ticker?.cancel()
            ticker = scope.launch {
                while (server != null) {
                    if (RemoteRecoveryPolicy.browserUnreachable(
                            active, SystemClock.elapsedRealtime(), lastBrowserHeartbeatMs,
                        )) {
                        stop(resumeHere = false)
                        WebTvController.fail(WebTvController.Failure.CONNECTION_LOST)
                        break
                    }
                    updateState()
                    delay(500)
                }
            }
        }
    }

    private fun refreshMedia(item: MediaItem?) {
        mediaJob?.cancel()
        val expectedId = item?.mediaId
        mediaId = expectedId
        title = item?.mediaMetadata?.title?.toString().orEmpty()
        artist = item?.mediaMetadata?.artist?.toString().orEmpty()
        artworkUrl = item?.mediaMetadata?.artworkUri?.toString()
            ?.takeIf { it.startsWith("https://") || it.startsWith("http://") }
        duration = item?.mediaMetadata?.durationMs?.takeIf { it > 0 } ?: C.TIME_UNSET
        streamUrl = null
        mimeType = null
        // Starting a browser session must not erase lyrics that the phone
        // already resolved for this exact track before the TV page was opened.
        if (lyricMediaId != expectedId) {
            lyricMediaId = expectedId
            lyricLines = null
            lyricRevision++
            updateLyricsJson()
        }
        updateState()
        if (item == null) return
        mediaJob = scope.launch {
            try {
                val stream = resolve(item) ?: return@launch
                if (mediaId == expectedId && server != null) {
                    streamUrl = stream.url
                    mimeType = stream.mimeType
                    ++commandSeq
                    command = "load"
                    updateState()
                }
            } catch (c: CancellationException) {
                throw c
            } catch (e: Exception) {
                Log.w("AurelisWebTv", "Track cannot be streamed to browser", e)
            }
        }
    }

    fun updateLyrics(id: String?, lines: List<com.music.aurelis.data.lyrics.LyricLine>?, offset: Int) {
        lyricMediaId = id
        lyricLines = lines
        lyricOffset = offset
        lyricRevision++
        updateLyricsJson()
        updateState()
    }

    private fun updateLyricsJson() {
        val packetList = CastLyricsPackets.build(
            lyricMediaId.orEmpty(), lyricLines, lyricOffset, lyricRevision,
        )
        val json = JSONArray()
        packetList.forEach { json.put(JSONObject(it)) }
        lyricsJson = JSONObject()
            .put("id", lyricMediaId ?: "")
            .put("revision", lyricRevision)
            .put("packets", json)
            .toString()
    }

    private fun updateState() {
        stateJson = JSONObject()
            .put("id", mediaId ?: "")
            .put("title", title)
            .put("artist", artist)
            .put("artwork", artworkUrl ?: "")
            .put("url", streamUrl ?: "")
            .put("mime", mimeType ?: "")
            .put("durationMs", if (duration == C.TIME_UNSET) 0 else duration)
            .put("positionMs", position)
            .put("playing", active && !paused)
            .put("active", active)
            .put("commandSeq", commandSeq)
            .put("command", command)
            .put("commandPosition", commandPosition)
            .put("lyricRevision", lyricRevision)
            .toString()
    }

    private fun onBrowserHeartbeat() {
        if (server != null) lastBrowserHeartbeatMs = SystemClock.elapsedRealtime()
    }

    private fun onBrowserEvent(message: String) {
        val json = runCatching { JSONObject(message) }.getOrNull() ?: return
        if (mediaId == null || json.optString("id") != mediaId) return
        if (server == null) return
        onBrowserHeartbeat()
        val action = json.optString("action")
        val newPosition = json.optLong("positionMs", position).coerceAtLeast(0L)
        if (action !in setOf("playing", "paused", "progress", "ended")) return
        position = newPosition
        browserSeen = true
        if (action == "playing" && !active && streamUrl != null) {
            if (CastController.state.value.casting || DlnaController.connected.value != null ||
                com.music.aurelis.wear.WearBridge.watchOutput.value.active
            ) return
            active = true
            lastBrowserHeartbeatMs = SystemClock.elapsedRealtime()
            localPlayer()?.pause()
            WebTvController.set(active = true)
            sink?.onRemoteRouteChanged()
        }
        if (!active) return
        if (action == "ended") {
            paused = true
            localPlayer()?.let { player ->
                if (player.hasNextMediaItem()) player.seekToNextMediaItem()
            }
        } else if (action == "playing") {
            paused = false
        } else if (action == "paused") {
            paused = true
        }
        updateState()
        sink?.onRemotePlaybackChanged()
    }

    override fun play() {
        if (!active) return
        paused = false
        command = "play"
        commandSeq++
        updateState()
        sink?.onRemotePlaybackChanged()
    }

    override fun pause() {
        if (!active) return
        paused = true
        command = "pause"
        commandSeq++
        updateState()
        sink?.onRemotePlaybackChanged()
    }

    override fun seekTo(positionMs: Long) {
        if (!active) return
        position = positionMs.coerceAtLeast(0)
        commandPosition = position
        command = "seek"
        commandSeq++
        updateState()
        sink?.onRemotePlaybackChanged()
    }

    override fun setVolume(fraction: Float) = Unit // TV browser controls its own audio volume.

    override fun onLocalTransition(item: MediaItem?, reason: Int) {
        if (server == null) return
        position = 0
        refreshMedia(item)
    }
    override fun onLocalQueueChanged() = Unit
    override fun onLocalRepeatChanged(repeatMode: Int) = Unit
    override fun onLocalStartedPlaying(player: Player) {
        if (!active) return
        player.pause()
        play()
    }

    fun stop(resumeHere: Boolean) {
        generation++
        startJob?.cancel()
        startJob = null
        mediaJob?.cancel()
        mediaJob = null
        ticker?.cancel()
        ticker = null
        server?.close()
        server = null
        val shouldResume = resumeHere && active && !paused
        active = false
        paused = true
        streamUrl = null
        onReleased()
        WebTvController.set(url = null, active = false)
        localPlayer()?.let { player ->
            if (browserSeen) player.seekTo(position)
            if (shouldResume) player.play()
        }
        browserSeen = false
        sink?.onRemoteRouteChanged()
        sink?.onRemotePlaybackChanged()
    }
}
