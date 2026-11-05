// Aurelis © 2026. GPL-3.0-or-later
// Aurelis © 2026. GPL-3.0-or-later.
package com.music.aurelis.playback

import android.app.PendingIntent
import android.content.Intent
import androidx.annotation.OptIn
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.common.util.UnstableApi
import androidx.media3.datasource.DefaultDataSource
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.source.DefaultMediaSourceFactory
import androidx.media3.session.MediaSession
import androidx.media3.session.MediaSessionService
import com.music.aurelis.MainActivity
import com.music.aurelis.AurelisApplication
import com.music.aurelis.data.LocalQueueStore
import com.music.aurelis.data.LocalMediaRepository
import com.music.aurelis.data.AudioPermission
import kotlinx.coroutines.*
import com.music.aurelis.data.SettingsRepository
import com.music.aurelis.playback.audio.DspChain
import com.music.aurelis.playback.dsd.DsdExtractorsFactory

@OptIn(UnstableApi::class)
class AurelisPlaybackService : MediaSessionService() {

    private var mediaSession: MediaSession? = null
    private var dspChain: DspChain? = null
    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private var recorder: ServicePlaybackRecorder? = null

    override fun onCreate() {
        super.onCreate()

        val chain = DspChain()
        dspChain = chain

        val renderersFactory = AurelisRenderersFactory(this, chain)
        val mediaSourceFactory = DefaultMediaSourceFactory(DefaultDataSource.Factory(this), DsdExtractorsFactory())

        val player = ExoPlayer.Builder(this, renderersFactory)
            .setAudioAttributes(
                AudioAttributes.Builder()
                    .setUsage(C.USAGE_MEDIA)
                    .setContentType(C.AUDIO_CONTENT_TYPE_MUSIC)
                    .build(),
                true,
            )
            .setHandleAudioBecomingNoisy(true)
            .setWakeMode(C.WAKE_MODE_LOCAL)
            .setMediaSourceFactory(mediaSourceFactory)
            .build()

        bindAudioSettings(serviceScope, chain.equalizer) { chain.spatial.enabled = it }

        val sessionActivityIntent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP
        }
        val sessionActivity = PendingIntent.getActivity(
            this, 0, sessionActivityIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )

        mediaSession = MediaSession.Builder(this, player)
            .setSessionActivity(sessionActivity)
            .build()
        val savedQueue = LocalQueueStore(this)
        val snapshot = savedQueue.read()
        recorder = ServicePlaybackRecorder(player, (application as AurelisApplication).userLibrary, savedQueue, serviceScope)
        serviceScope.launch {
            if (snapshot != null && AudioPermission.isGranted(this@AurelisPlaybackService)) {
                try {
                    val songs = LocalMediaRepository.getAllSongs(this@AurelisPlaybackService, SettingsRepository.filterNonMusicAudio.value)
                    val (available, index) = snapshot.resolve(songs)
                    if (player.mediaItemCount == 0 && available.isNotEmpty()) {
                        val position = if (available[index].id == snapshot.ids.getOrNull(snapshot.index))
                            snapshot.positionMs.coerceAtMost(available[index].durationMs.coerceAtLeast(0)) else 0L
                        player.setMediaItems(available.map { it.toMediaItem() }, index, position)
                        player.repeatMode = snapshot.repeat; player.shuffleModeEnabled = snapshot.shuffle
                        // Idle/paused: only an explicit user command prepares and starts playback.
                    }
                } catch (cancelled: CancellationException) { throw cancelled }
                catch (_: Exception) { /* Permission/provider unavailable: preserve the stored snapshot. */ }
            }
        }
    }

    override fun onGetSession(controllerInfo: MediaSession.ControllerInfo): MediaSession? = mediaSession

    override fun onTaskRemoved(rootIntent: Intent?) {
        val player = mediaSession?.player
        if (player == null || !player.playWhenReady || player.mediaItemCount == 0) stopSelf()
    }

    override fun onDestroy() {
        recorder?.close(); recorder = null
        serviceScope.cancel()
        mediaSession?.run { player.release(); release() }
        mediaSession = null
        dspChain = null
        super.onDestroy()
    }
}
