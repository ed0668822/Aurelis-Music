package com.music.aurelis.playback

import android.os.Looper
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import com.music.aurelis.AurelisApplication
import com.music.aurelis.data.*
import com.music.aurelis.data.model.*
import kotlinx.coroutines.*
import kotlinx.coroutines.test.*
import org.junit.*
import org.junit.Assert.*
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config

@UnstableApi
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33], manifest = Config.NONE, application = AurelisApplication::class)
class ProfilePlaybackTest {
    private val app get() = RuntimeEnvironment.getApplication() as AurelisApplication
    private fun song(id: String) = LocalSong(id, id, "artist", "album", "content://media/external/audio/media/$id", null, 60_000, null, 1, 2026)
    @Test fun switchFlushesOutgoingQueueRestoresNewQueuePausedAndKeepsSinglePlayer() {
        val dispatcher = StandardTestDispatcher()
        val scope = CoroutineScope(SupervisorJob() + dispatcher)
        val player = RecordingPlayer()
        val second = ProfileRepository.create("Segundo")
        LocalQueueStore(app, second).save(SavedLocalQueue(listOf("2"), 0, 3456, Player.REPEAT_MODE_ONE, true))
        val binding = ProfilePlaybackBinding(app, player, scope, songs = { listOf(song("1"), song("2")) })
        try {
            PlayerConnection.playSongs(player, listOf(song("1")), 0)
            player.seekTo(1234); shadowOf(Looper.getMainLooper()).idle()
            ProfileRepository.switchTo(second)
            dispatcher.scheduler.runCurrent(); shadowOf(Looper.getMainLooper()).idle()
            assertEquals(listOf("1"), LocalQueueStore(app, ProfileId.DEFAULT).read()?.ids)
            assertEquals(1234L, LocalQueueStore(app, ProfileId.DEFAULT).read()?.positionMs)
            assertEquals("2", player.currentMediaItem?.mediaId)
            assertEquals(3456L, player.currentPosition)
            assertFalse(player.playWhenReady); assertFalse(player.released)
            assertEquals(Player.REPEAT_MODE_ONE, player.repeatMode); assertTrue(player.shuffleModeEnabled)
            ProfileRepository.switchTo(ProfileId.DEFAULT)
            dispatcher.scheduler.runCurrent(); shadowOf(Looper.getMainLooper()).idle()
            assertEquals("1", player.currentMediaItem?.mediaId)
            assertEquals(1234L, player.currentPosition); assertFalse(player.playWhenReady)
        } finally { binding.close(); scope.cancel(); player.release() }
    }
    @Test fun lateRestoreCannotReplaceNewProfileQueue() {
        val dispatcher = StandardTestDispatcher()
        val scope = CoroutineScope(SupervisorJob() + dispatcher)
        val player = RecordingPlayer()
        val gate = CompletableDeferred<Unit>()
        val second = ProfileRepository.create("Otro")
        LocalQueueStore(app).save(SavedLocalQueue(listOf("1"), 0, 0, 0, false))
        LocalQueueStore(app, second).save(SavedLocalQueue(listOf("2"), 0, 0, 0, false))
        var calls = 0
        val binding = ProfilePlaybackBinding(app, player, scope, songs = {
            if (++calls == 1) withContext(NonCancellable) { gate.await() }
            listOf(song("1"), song("2"))
        })
        try {
            dispatcher.scheduler.runCurrent()
            ProfileRepository.switchTo(second)
            dispatcher.scheduler.runCurrent(); shadowOf(Looper.getMainLooper()).idle()
            gate.complete(Unit); dispatcher.scheduler.runCurrent(); shadowOf(Looper.getMainLooper()).idle()
            assertEquals("2", player.currentMediaItem?.mediaId)
            assertFalse(player.playWhenReady)
        } finally { binding.close(); scope.cancel(); player.release() }
    }
}
