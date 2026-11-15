// Aurelis © 2026. GPL-3.0-or-later
// Aurelis © 2026. GPL-3.0-or-later.
package com.music.aurelis.playback

import android.app.PendingIntent
import android.content.Intent
import android.util.Log
import androidx.annotation.OptIn
import androidx.media3.common.Player
import androidx.media3.common.PlaybackException
import androidx.media3.common.util.UnstableApi
import androidx.media3.datasource.DefaultDataSource
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.source.DefaultMediaSourceFactory
import androidx.media3.session.MediaSession
import androidx.media3.session.MediaSessionService
import com.music.aurelis.MainActivity
import com.music.aurelis.BuildConfig
import kotlinx.coroutines.*
import com.music.aurelis.playback.audio.DspChain
import com.music.aurelis.playback.dsd.DsdExtractorsFactory

@OptIn(UnstableApi::class)
class AurelisPlaybackService : MediaSessionService() {

    private var mediaSession: MediaSession? = null
    private var coordinator: LocalPlaybackCoordinator? = null
    private var dspChain: DspChain? = null
    private var widgetBinding: com.music.aurelis.widget.MediaWidgetBinding? = null
    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)

    override fun onCreate() {
        super.onCreate()

        val chain = DspChain()
        dspChain = chain

        val renderersFactory = AurelisRenderersFactory(this, chain)
        val mediaSourceFactory = DefaultMediaSourceFactory(DefaultDataSource.Factory(this), DsdExtractorsFactory())

        val player = ExoPlayer.Builder(this, renderersFactory)
            .withLocalAudioPolicy()
            .setMediaSourceFactory(mediaSourceFactory)
            .build()

        // Debug-only tracking on the underlying ExoPlayer: an attached controller
        // cannot always distinguish audio-focus loss from a remote pause.
        if (BuildConfig.DEBUG) {
            player.addListener(object : Player.Listener {
                override fun onPlayWhenReadyChanged(playWhenReady: Boolean, reason: Int) {
                    Log.d("P3Media3Service", "playWhenReady=" + playWhenReady +
                        " reason=" + reason + " state=" + player.playbackState +
                        " suppression=" + player.playbackSuppressionReason)
                }
                override fun onPlaybackStateChanged(playbackState: Int) {
                    Log.d("P3Media3Service", "playbackState=" + playbackState +
                        " playWhenReady=" + player.playWhenReady +
                        " mediaId=" + player.currentMediaItem?.mediaId +
                        " count=" + player.mediaItemCount)
                }
                override fun onPlayerError(error: PlaybackException) {
                    Log.e("P3Media3Service", "Media3 player error=" + error.errorCodeName, error)
                }
            })
        }

        bindAudioSettings(serviceScope, chain.equalizer) { chain.spatial.enabled = it }

        val sessionActivityIntent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP
        }
        val sessionActivity = PendingIntent.getActivity(
            this, 0, sessionActivityIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )

        val controls = LocalPlaybackCoordinator(player)
        coordinator = controls
        widgetBinding = com.music.aurelis.widget.MediaWidgetBinding(this, player)
        mediaSession = MediaSession.Builder(this, player)
            .setSessionActivity(sessionActivity)
            .setCallback(ShuffleQueueCallback(player, packageName, controls))
            .build()
        controls.bind(requireNotNull(mediaSession))
        controls.bindProfile(this, serviceScope)
        controls.bindAutoplay(serviceScope) {
            withContext(Dispatchers.IO) {
                if (com.music.aurelis.data.AudioPermission.isGranted(this@AurelisPlaybackService))
                    com.music.aurelis.data.LocalMediaRepository.getLibrarySongs(this@AurelisPlaybackService,
                        com.music.aurelis.data.SettingsRepository.filterNonMusicAudio.value)
                else emptyList()
            }
        }
    }

    override fun onGetSession(controllerInfo: MediaSession.ControllerInfo): MediaSession? = mediaSession

    override fun onTaskRemoved(rootIntent: Intent?) {
        if (BuildConfig.DEBUG) {
            Log.d("P3Media3Service", "onTaskRemoved playWhenReady=" +
                mediaSession?.player?.playWhenReady +
                " count=" + mediaSession?.player?.mediaItemCount)
        }
        val player = mediaSession?.player
        if (player == null || !player.playWhenReady || player.mediaItemCount == 0 ||
            player.playbackState == Player.STATE_IDLE || player.playbackState == Player.STATE_ENDED ||
            player.playerError != null) stopSelf()
        // READY with transient focus suppression retains play intent for Media3 to resume.
    }

    override fun onDestroy() {
        if (BuildConfig.DEBUG) Log.d("P3Media3Service", "onDestroy playback service")
        widgetBinding?.close(); widgetBinding = null
        coordinator?.close(); coordinator = null
        serviceScope.cancel()
        mediaSession?.run { player.release(); release() }
        mediaSession = null
        dspChain = null
        super.onDestroy()
    }
}
