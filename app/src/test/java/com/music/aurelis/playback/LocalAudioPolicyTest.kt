// Aurelis © 2026. GPL-3.0-or-later
package com.music.aurelis.playback

import android.content.Intent
import android.media.AudioManager
import android.os.Looper
import androidx.media3.common.C
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import com.music.aurelis.AurelisApplication
import com.music.aurelis.data.model.LocalSong
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config

@UnstableApi
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33], manifest = Config.NONE, application = AurelisApplication::class)
class LocalAudioPolicyTest {
    private fun await(condition: () -> Boolean) {
        val deadline = System.nanoTime() + 5_000_000_000L
        while (!condition() && System.nanoTime() < deadline) {
            shadowOf(Looper.getMainLooper()).idle(); Thread.yield()
        }
        assertTrue("Media3 receiver did not reach the expected state", condition())
    }
    @Test fun frameworkNoisyEventPausesWithoutReplacingTheCurrentOccurrenceOrSeeking() {
        val app = RuntimeEnvironment.getApplication()
        val player = ExoPlayer.Builder(app).withLocalAudioPolicy().build()
        val song = LocalSong("1", "Tema", "Artista", null, "content://media/1", null, 60_000, null, null, null)
        try {
            player.setMediaItems(listOf(song.toQueueMediaItem()), 0, 12345L)
            val entry = player.currentMediaItem!!.queueEntryId
            var cause = Player.PLAY_WHEN_READY_CHANGE_REASON_USER_REQUEST
            player.addListener(object : Player.Listener {
                override fun onPlayWhenReadyChanged(playWhenReady: Boolean, reason: Int) { cause = reason }
            })
            player.play()
            assertTrue(player.playWhenReady)
            // Robolectric models a framework-delivered broadcast, not a physical unplug.
            await { shadowOf(app).registeredReceivers.any { it.intentFilter.hasAction(AudioManager.ACTION_AUDIO_BECOMING_NOISY) } }
            app.sendBroadcast(Intent(AudioManager.ACTION_AUDIO_BECOMING_NOISY))
            await { !player.playWhenReady }
            assertEquals(Player.PLAY_WHEN_READY_CHANGE_REASON_AUDIO_BECOMING_NOISY, cause)
            assertEquals(entry, player.currentMediaItem!!.queueEntryId)
            assertEquals(12345L, player.currentPosition)
            assertEquals(C.USAGE_MEDIA, player.audioAttributes.usage)
            assertEquals(C.AUDIO_CONTENT_TYPE_MUSIC, player.audioAttributes.contentType)
            assertEquals(Player.STATE_IDLE, player.playbackState) // No prepare/decode in this unit test.
            shadowOf(Looper.getMainLooper()).idle()
            assertFalse(player.playWhenReady)
            player.play(); assertTrue(player.playWhenReady)
        } finally { player.release() }
    }
    @Test fun noisyEventWhilePausedNeverStartsOrPreparesThePlayer() {
        val app = RuntimeEnvironment.getApplication()
        val player = ExoPlayer.Builder(app).withLocalAudioPolicy().build()
        try {
            await { shadowOf(app).registeredReceivers.any { it.intentFilter.hasAction(AudioManager.ACTION_AUDIO_BECOMING_NOISY) } }
            app.sendBroadcast(Intent(AudioManager.ACTION_AUDIO_BECOMING_NOISY))
            shadowOf(Looper.getMainLooper()).idle()
            assertFalse(player.playWhenReady); assertEquals(Player.STATE_IDLE, player.playbackState)
            assertEquals(0, player.mediaItemCount)
        } finally { player.release() }
    }
}
