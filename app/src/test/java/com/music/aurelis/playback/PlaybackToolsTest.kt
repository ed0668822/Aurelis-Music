// Aurelis © 2026. GPL-3.0-or-later
package com.music.aurelis.playback

import android.os.Bundle
import androidx.media3.common.C
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.session.MediaSession
import androidx.media3.session.SessionResult
import com.music.aurelis.AurelisApplication
import com.music.aurelis.data.model.LocalSong
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config

@UnstableApi
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33], manifest = Config.NONE, application = AurelisApplication::class)
class PlaybackToolsTest {
    private fun withControls(test: (ExoPlayer, LocalPlaybackCoordinator, MediaSession) -> Unit) {
        val app = RuntimeEnvironment.getApplication()
        val player = ExoPlayer.Builder(app).build()
        val song = LocalSong("1", "Tema", "Artista", null, "content://media/1", null, 60_000, null, null, null)
        player.setMediaItems(listOf(song.toQueueMediaItem()), 0, 12345L)
        val controls = LocalPlaybackCoordinator(player)
        val session = MediaSession.Builder(app, player).build()
        controls.bind(session)
        try { test(player, controls, session) } finally { controls.close(); session.release(); player.release() }
    }
    @Test fun speedAndSilenceSetActualPlayerPropertiesWithoutSeekingOrPreparing() = withControls { player, controls, session ->
        val current = player.currentMediaItem!!.queueEntryId
        assertEquals(SessionResult.RESULT_SUCCESS, controls.execute(Bundle().apply { putString("action", "speed"); putFloat("speed", 1.5f) }).resultCode)
        assertEquals(1.5f, player.playbackParameters.speed)
        assertEquals(SessionResult.RESULT_SUCCESS, controls.execute(Bundle().apply { putString("action", "silence"); putBoolean("enabled", true) }).resultCode)
        assertTrue(player.skipSilenceEnabled)
        assertEquals(1.5f, session.sessionExtras.getFloat("speed"))
        assertTrue(session.sessionExtras.getBoolean("skip_silence"))
        assertEquals(current, player.currentMediaItem!!.queueEntryId); assertEquals(12345L, player.currentPosition)
        assertFalse(player.playWhenReady); assertEquals(Player.STATE_IDLE, player.playbackState)
        controls.execute(Bundle().apply { putString("action", "speed"); putFloat("speed", 1f) })
        controls.execute(Bundle().apply { putString("action", "silence"); putBoolean("enabled", false) })
        assertEquals(1f, player.playbackParameters.speed); assertFalse(player.skipSilenceEnabled)
    }
    @Test fun invalidSpeedAndUnknownActionsAreRejectedWithoutMutation() = withControls { player, controls, _ ->
        for (value in listOf(Float.NaN, Float.POSITIVE_INFINITY, 0f, 0.49f, 2.01f)) {
            assertEquals(SessionResult.RESULT_ERROR_BAD_VALUE, controls.execute(Bundle().apply { putString("action", "speed"); putFloat("speed", value) }).resultCode)
        }
        assertEquals(SessionResult.RESULT_ERROR_BAD_VALUE, controls.execute(Bundle()).resultCode)
        assertEquals(1f, player.playbackParameters.speed); assertFalse(player.skipSilenceEnabled)
    }
    @Test fun floatAndHighResolutionPcmAreExplicitlyExcludedFromProcessingControls() {
        assertTrue(unsupportedControlPcm(C.ENCODING_PCM_FLOAT))
        assertTrue(unsupportedControlPcm(C.ENCODING_PCM_24BIT))
        assertTrue(unsupportedControlPcm(C.ENCODING_PCM_32BIT))
        assertFalse(unsupportedControlPcm(C.ENCODING_PCM_16BIT))
        assertFalse(unsupportedControlPcm(null))
    }
}
