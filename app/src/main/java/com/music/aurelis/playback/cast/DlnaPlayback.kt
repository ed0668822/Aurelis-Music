package com.music.aurelis.playback.cast

import android.content.Context
import android.net.wifi.WifiManager
import android.util.Log
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
import java.net.DatagramPacket
import java.net.DatagramSocket
import java.net.InetAddress
import java.net.URL
import java.nio.charset.StandardCharsets

/** DLNA discovery and route selection. The service remains the owner of playback. */
internal object DlnaController {
    private val _devices = MutableStateFlow<List<DlnaRenderer>>(emptyList())
    val devices: StateFlow<List<DlnaRenderer>> = _devices
    private val _connected = MutableStateFlow<String?>(null)
    val connected: StateFlow<String?> = _connected
    private val _connectedId = MutableStateFlow<String?>(null)
    val connectedId: StateFlow<String?> = _connectedId

    enum class Failure { UNSUPPORTED, CONNECTION_FAILED, CONNECTION_LOST }
    private val _failure = MutableStateFlow<Failure?>(null)
    val failure: StateFlow<Failure?> = _failure

    private val _volume = MutableStateFlow(0.5f)
    val volume: StateFlow<Float> = _volume

    fun onFailure(error: Failure) { _failure.value = error }
    fun clearFailure() { _failure.value = null }
    fun onVolume(level: Float) { _volume.value = level.coerceIn(0f, 1f) }
    private var speaker: DlnaPlayback? = null
    private var scanJob: Job? = null
    private var app: Context? = null
    private var scope: CoroutineScope? = null

    fun attach(context: Context, owner: CoroutineScope, target: DlnaPlayback) {
        app = context.applicationContext
        scope = owner
        speaker = target
    }

    fun detach(target: DlnaPlayback) {
        if (speaker === target) speaker = null
        scanJob?.cancel()
        scanJob = null
        app = null
        scope = null
        _connected.value = null
        _connectedId.value = null
        _devices.value = emptyList()
    }

    fun scan() {
        val context = app ?: return
        val owner = scope ?: return
        scanJob?.cancel()
        scanJob = owner.launch {
            val found = withContext(Dispatchers.IO) { search(context) }
            _devices.value = found
        }
    }

    fun connect(renderer: DlnaRenderer) {
        clearFailure()
        speaker?.connect(renderer)
    }

    fun setVolume(level: Float) { speaker?.setVolume(level) }

    fun disconnect(resumeHere: Boolean) {
        if (resumeHere) clearFailure()
        speaker?.disconnect(resumeHere)
    }

    fun onConnected(name: String?, id: String? = null) {
        _connected.value = name
        _connectedId.value = id
    }

    private fun search(context: Context): List<DlnaRenderer> {
        val wifi = context.getSystemService(Context.WIFI_SERVICE) as? WifiManager
        val lock = runCatching {
            wifi?.createMulticastLock("AurelisDlnaScan")?.apply {
                setReferenceCounted(false)
                acquire()
            }
        }.getOrNull()
        return try {
            val matches = LinkedHashMap<String, URL>()
            DatagramSocket().use { socket ->
                socket.soTimeout = 350
                val query = (
                    "M-SEARCH * HTTP/1.1\r\n" +
                    "HOST: 239.255.255.250:1900\r\n" +
                    "MAN: \"ssdp:discover\"\r\n" +
                    "MX: 2\r\n" +
                    "ST: urn:schemas-upnp-org:device:MediaRenderer:1\r\n\r\n"
                ).toByteArray(StandardCharsets.US_ASCII)
                val address = InetAddress.getByName("239.255.255.250")
                repeat(2) { socket.send(DatagramPacket(query, query.size, address, 1900)) }
                val limit = System.nanoTime() + 2_500_000_000L
                val buffer = ByteArray(4096)
                while (System.nanoTime() < limit) {
                    val packet = DatagramPacket(buffer, buffer.size)
                    try {
                        socket.receive(packet)
                    } catch (_: java.net.SocketTimeoutException) {
                        continue
                    }
                    val response = String(packet.data, packet.offset, packet.length, StandardCharsets.UTF_8)
                    if (!response.startsWith("HTTP/1.1 200")) continue
                    val location = DlnaProtocol.location(response, packet.address) ?: continue
                    matches[location.toExternalForm()] = location
                    if (matches.size >= 24) break
                }
            }
            matches.values.mapNotNull { location ->
                val xml = DlnaProtocol.request(location, "GET") ?: return@mapNotNull null
                DlnaProtocol.parseDescription(xml, location)
            }.distinctBy { it.id }
        } catch (e: Exception) {
            Log.w("AurelisDlna", "Discovery failed", e)
            emptyList()
        } finally {
            runCatching { lock?.release() }
        }
    }
}

/**
 * AVTransport implementation: unlike Cast, a DLNA renderer has no guaranteed
 * queue support. Aurelis retains the queue locally and sends one URL per track.
 */
internal class DlnaPlayback(
    private val scope: CoroutineScope,
    private val localPlayer: () -> ExoPlayer?,
    private val resolve: suspend (MediaItem) -> CastStream?,
    private val onReleased: () -> Unit = {},
) : RemoteSpeaker {
    override var sink: CastSink? = null
    override var active = false
        private set
    private var renderer: DlnaRenderer? = null
    private var connectJob: Job? = null
    private var ticker: Job? = null
    private var generation = 0
    private var paused = true
    private var position = 0L
    private var duration = C.TIME_UNSET
    private var mimeSink: String? = null
    private var stoppedReadings = 0
    private var missedStatusChecks = 0
    private var volume = 50

    override val playWhenReady: Boolean get() = active && !paused
    override val isPlaying: Boolean get() = active && !paused
    override val playbackState: Int get() = if (active) Player.STATE_READY else Player.STATE_IDLE
    override val positionMs: Long get() = position
    override val durationMs: Long get() = duration
    override val deviceVolume: Int get() = volume

    fun connect(target: DlnaRenderer) {
        if (CastController.state.value.casting || WebTvController.state.value.active ||
            com.music.aurelis.wear.WearBridge.watchOutput.value.active
        ) return
        if (active || connectJob?.isActive == true) disconnect(false)
        val player = localPlayer() ?: return
        val selected = player.currentMediaItem ?: run {
            DlnaController.onFailure(DlnaController.Failure.CONNECTION_FAILED)
            return
        }
        val wasPlaying = player.playWhenReady
        val startPosition = player.currentPosition.coerceAtLeast(0)
        val id = ++generation
        connectJob = scope.launch {
            try {
                val media = resolve(selected) ?: run {
                    DlnaController.onFailure(DlnaController.Failure.UNSUPPORTED)
                    return@launch
                }
                val supports = withContext(Dispatchers.IO) {
                    val sink = target.connectionUrl?.let { url ->
                        target.connectionType?.let { type ->
                            val u = URL(url)
                            DlnaProtocol.request(u, "POST", DlnaProtocol.soap(type, "GetProtocolInfo", emptyMap()),
                                "$type#GetProtocolInfo")
                        }
                    }?.let { DlnaProtocol.xmlValue(it, "Sink") }
                    mimeSink = sink
                    DlnaProtocol.supports(media.mimeType, sink) &&
                        sendUri(target, media, selected, startPosition, wasPlaying)
                }
                if (id != generation) return@launch
                if (!supports) {
                    DlnaController.onFailure(DlnaController.Failure.UNSUPPORTED)
                    return@launch
                }
                // A system Cast / watch route may have started while the
                // slow UPnP network setup was running. Stop the orphaned TV.
                if (CastController.state.value.casting || WebTvController.state.value.active ||
                    com.music.aurelis.wear.WearBridge.watchOutput.value.active
                ) {
                    withContext(Dispatchers.IO) {
                        DlnaProtocol.call(target, "Stop", mapOf("InstanceID" to "0"))
                    }
                    return@launch
                }
                renderer = target
                missedStatusChecks = 0
                stoppedReadings = 0
                position = startPosition
                duration = selected.mediaMetadata.durationMs?.takeIf { it > 0 } ?: C.TIME_UNSET
                paused = !wasPlaying
                active = true
                player.pause()
                DlnaController.onConnected(target.name, target.id)
                sink?.onRemoteRouteChanged()
                sink?.onRemotePlaybackChanged()
                startTicker()
            } catch (c: CancellationException) {
                throw c
            } catch (e: Exception) {
                Log.w("AurelisDlna", "Cannot connect DLNA renderer", e)
                if (id == generation) DlnaController.onFailure(DlnaController.Failure.CONNECTION_FAILED)
            }
        }
    }

    private fun sendUri(
        target: DlnaRenderer, media: CastStream, item: MediaItem,
        startMs: Long, beginPlaying: Boolean,
    ): Boolean {
        val title = DlnaProtocol.xmlEscape(item.mediaMetadata.title?.toString() ?: "Aurelis Music")
        val protocolInfo = "http-get:*:${media.mimeType}:*"
        val didl = "<DIDL-Lite xmlns=\"urn:schemas-upnp-org:metadata-1-0/DIDL-Lite/\" " +
            "xmlns:dc=\"http://purl.org/dc/elements/1.1/\">" +
            "<item id=\"0\" parentID=\"0\" restricted=\"1\">" +
            "<dc:title>$title</dc:title><res protocolInfo=\"$protocolInfo\">" +
            DlnaProtocol.xmlEscape(media.url) + "</res></item></DIDL-Lite>"
        if (DlnaProtocol.call(target, "SetAVTransportURI", mapOf(
                "InstanceID" to "0",
                "CurrentURI" to media.url,
                "CurrentURIMetaData" to didl,
            )) == null
        ) return false
        if (startMs > 1000) {
            DlnaProtocol.call(target, "Seek", mapOf(
                "InstanceID" to "0",
                "Unit" to "REL_TIME",
                "Target" to DlnaProtocol.relativeTime(startMs),
            ))
        }
        return !beginPlaying || DlnaProtocol.call(target, "Play", mapOf(
            "InstanceID" to "0", "Speed" to "1",
        )) != null
    }

    override fun play() {
        val target = renderer ?: return
        paused = false
        launchCommand(target, "Play", mapOf("InstanceID" to "0", "Speed" to "1"))
        sink?.onRemotePlaybackChanged()
    }
    override fun pause() {
        val target = renderer ?: return
        paused = true
        launchCommand(target, "Pause", mapOf("InstanceID" to "0"))
        sink?.onRemotePlaybackChanged()
    }
    override fun seekTo(positionMs: Long) {
        val target = renderer ?: return
        position = positionMs.coerceAtLeast(0)
        launchCommand(target, "Seek", mapOf(
            "InstanceID" to "0", "Unit" to "REL_TIME",
            "Target" to DlnaProtocol.relativeTime(position),
        ))
        sink?.onRemotePlaybackChanged()
    }

    override fun setVolume(fraction: Float) {
        val target = renderer ?: return
        val url = target.renderingUrl ?: return
        val type = target.renderingType ?: return
        volume = (fraction.coerceIn(0f, 1f) * 100).toInt()
        DlnaController.onVolume(volume / 100f)
        scope.launch(Dispatchers.IO) {
            runCatching {
                DlnaProtocol.request(URL(url), "POST", DlnaProtocol.soap(type, "SetVolume", mapOf(
                    "InstanceID" to "0", "Channel" to "Master", "DesiredVolume" to volume.toString(),
                )), "$type#SetVolume")
            }
        }
        sink?.onRemoteVolumeChanged()
    }

    private fun launchCommand(target: DlnaRenderer, action: String, params: Map<String, String>) {
        scope.launch(Dispatchers.IO) { DlnaProtocol.call(target, action, params) }
    }

    override fun onLocalTransition(item: MediaItem?, reason: Int) {
        val target = renderer ?: return
        if (!active || item == null) return
        val id = ++generation
        scope.launch {
            val stream = resolve(item) ?: return@launch
            val success = withContext(Dispatchers.IO) {
                DlnaProtocol.supports(stream.mimeType, mimeSink) && sendUri(target, stream, item, 0, true)
            }
            if (id == generation && success) {
                position = 0
                paused = false
                duration = item.mediaMetadata.durationMs?.takeIf { it > 0 } ?: C.TIME_UNSET
                sink?.onRemotePlaybackChanged()
            }
        }
    }
    override fun onLocalQueueChanged() = Unit
    override fun onLocalRepeatChanged(repeatMode: Int) = Unit
    override fun onLocalStartedPlaying(player: Player) {
        if (!active) return
        player.pause()
        play()
    }

    private fun startTicker() {
        ticker?.cancel()
        ticker = scope.launch {
            while (active) {
                val target = renderer ?: break
                val result = withContext(Dispatchers.IO) {
                    val pos = DlnaProtocol.call(target, "GetPositionInfo", mapOf("InstanceID" to "0"))
                    val transport = DlnaProtocol.call(target, "GetTransportInfo", mapOf("InstanceID" to "0"))
                    pos to transport
                }
                DlnaProtocol.parseTime(result.first?.let { DlnaProtocol.xmlValue(it, "RelTime") })?.let {
                    position = it
                    sink?.onRemotePlaybackChanged()
                }
                val state = result.second?.let { DlnaProtocol.xmlValue(it, "CurrentTransportState") }
                if (result.first == null && result.second == null) {
                    missedStatusChecks++
                    if (RemoteRecoveryPolicy.dlnaUnreachable(missedStatusChecks)) {
                        DlnaController.onFailure(DlnaController.Failure.CONNECTION_LOST)
                        disconnect(resumeHere = false)
                        break
                    }
                } else {
                    missedStatusChecks = 0
                }
                if (!paused && state == "STOPPED") {
                    stoppedReadings++
                    if (stoppedReadings >= 2) {
                        stoppedReadings = 0
                        val player = localPlayer()
                        if (player?.hasNextMediaItem() == true) {
                            player.seekToNextMediaItem()
                        } else {
                            paused = true
                            sink?.onRemotePlaybackChanged()
                        }
                    }
                } else {
                    stoppedReadings = 0
                    if (state == "PAUSED_PLAYBACK" && !paused) {
                        paused = true
                        sink?.onRemotePlaybackChanged()
                    }
                }
                delay(1500)
            }
        }
    }

    fun disconnect(resumeHere: Boolean) {
        ++generation
        connectJob?.cancel()
        connectJob = null
        ticker?.cancel()
        ticker = null
        val target = renderer
        val shouldResume = resumeHere && active && !paused
        renderer = null
        active = false
        paused = true
        DlnaController.onConnected(null)
        if (target != null) launchCommand(target, "Stop", mapOf("InstanceID" to "0"))
        localPlayer()?.let { player ->
            if (target != null) player.seekTo(position)
            if (shouldResume) player.play()
        }
        onReleased()
        sink?.onRemoteRouteChanged()
        sink?.onRemotePlaybackChanged()
    }
}
