// Aurelis © 2026. GPL-3.0-or-later
package com.music.aurelis.playback

import android.os.Looper
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import com.music.aurelis.AurelisApplication
import com.music.aurelis.data.model.LocalSong
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config
import java.util.concurrent.TimeUnit

@UnstableApi
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33], manifest = Config.NONE, application = AurelisApplication::class)
class SleepTimerTest {
    private fun withPlayer(block: (RecordingPlayer) -> Unit) {
        val player = RecordingPlayer()
        val song = LocalSong("1", "Tema", "Artista", null, "content://media/1", null, 60_000, null, null, null)
        PlayerConnection.playSongs(player, listOf(song, song.copy(id = "2")))
        try { block(player) } finally { player.release() }
    }
    @Test fun serviceTimerSurvivesControllerReleaseAndPublishesActualState() {
        val app = org.robolectric.RuntimeEnvironment.getApplication()
        val player = androidx.media3.exoplayer.ExoPlayer.Builder(app).build()
        player.play()
        val controls = LocalPlaybackCoordinator(player)
        val session = androidx.media3.session.MediaSession.Builder(app, player)
            .setCallback(ShuffleQueueCallback(player, app.packageName, controls)).build()
        controls.bind(session)
        var controller: androidx.media3.session.MediaController? = null
        val store = androidx.lifecycle.ViewModelStore()
        fun await(condition: () -> Boolean) {
            val end = System.nanoTime() + 5_000_000_000L
            while (!condition() && System.nanoTime() < end) {
                shadowOf(Looper.getMainLooper()).idle(); Thread.yield()
            }
            assertTrue(condition())
        }
        try {
            val future = androidx.media3.session.MediaController.Builder(app, session.token)
                .setListener(PlayerConnection.controllerListener {}).buildAsync()
            await { future.isDone }; val connected = future.get(); controller = connected
            assertTrue(connected.isSessionCommandAvailable(LocalPlaybackCoordinator.COMMAND))
            val vm = PlaybackViewModel(app as AurelisApplication) { connected }
            store.put("player", vm); vm.connect()
            await { vm.state.value.canUsePlaybackTools }
            vm.setSleepTimer(1000L)
            await { connected.sessionExtras.getLong("sleep_deadline") > 0 && vm.state.value.sleepRemainingMs > 0 }
            controls.execute(android.os.Bundle().apply { putString("action", "sleep_cancel") })
            await { connected.sessionExtras.getLong("sleep_deadline") == 0L && vm.state.value.sleepRemainingMs == 0L }
            vm.setSleepTimer(1000L)
            await { connected.sessionExtras.getLong("sleep_deadline") > 0 }
            store.clear(); controller = null
            assertTrue(player.playWhenReady)
            shadowOf(Looper.getMainLooper()).idleFor(1, TimeUnit.SECONDS)
            assertFalse(player.playWhenReady)
        } finally { store.clear(); controller?.release(); controls.close(); session.release(); player.release() }
    }

    @Test fun deadlineRearmsAndOnlyPausesAtTheNewMonotonicDeadline() = withPlayer { player ->
        var now = 100L
        val timer = SleepTimer(player, clock = { now })
        try {
            assertTrue(timer.start(1000)); assertTrue(timer.start(3000))
            now = 1100; shadowOf(Looper.getMainLooper()).idleFor(1, TimeUnit.SECONDS)
            assertTrue(player.playWhenReady)
            now = 3100; shadowOf(Looper.getMainLooper()).idleFor(1, TimeUnit.SECONDS)
            assertFalse(player.playWhenReady); assertEquals(0L, timer.deadlineMs)
            player.play(); shadowOf(Looper.getMainLooper()).idleFor(5, TimeUnit.SECONDS)
            assertTrue(player.playWhenReady)
        } finally { timer.close() }
    }
    @Test fun cancelAndCloseRemoveCallbacksAndRejectInvalidDurations() = withPlayer { player ->
        var now = 0L
        val timer = SleepTimer(player, clock = { now })
        assertFalse(timer.start(0)); assertFalse(timer.start(43_200_001))
        timer.start(1000); timer.cancel(); now = 5000
        shadowOf(Looper.getMainLooper()).idleFor(5, TimeUnit.SECONDS)
        assertTrue(player.playWhenReady)
        timer.start(1000); timer.close(); now = 10_000
        shadowOf(Looper.getMainLooper()).idleFor(5, TimeUnit.SECONDS)
        assertTrue(player.playWhenReady); assertFalse(timer.start(1000))
    }
    @Test fun afterTrackPausesOnAutomaticRepeatOrEndButManualSelectionCancels() = withPlayer { player ->
        val timer = SleepTimer(player)
        try {
            assertTrue(timer.startAfterTrack())
            timer.onMediaItemTransition(player.currentMediaItem, Player.MEDIA_ITEM_TRANSITION_REASON_AUTO)
            assertFalse(player.playWhenReady); assertFalse(timer.afterTrack)
            player.play(); timer.startAfterTrack()
            timer.onMediaItemTransition(player.currentMediaItem, Player.MEDIA_ITEM_TRANSITION_REASON_REPEAT)
            assertFalse(player.playWhenReady)
            player.play(); timer.startAfterTrack(); player.seekTo(1, 0)
            assertFalse(timer.afterTrack); assertTrue(player.playWhenReady)
            timer.startAfterTrack(); timer.onPlaybackStateChanged(Player.STATE_ENDED)
            assertFalse(player.playWhenReady)
        } finally { timer.close() }
    }
}
