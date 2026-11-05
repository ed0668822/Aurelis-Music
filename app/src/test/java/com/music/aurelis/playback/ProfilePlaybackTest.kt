package com.music.aurelis.playback

import android.os.Looper
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import com.music.aurelis.AurelisApplication
import com.music.aurelis.data.*
import com.music.aurelis.data.model.LocalSong
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
    @Test fun restartsRestoreTheSameLocalQueuePausedIncludingPreferencesAndPosition() {
        val dispatcher = StandardTestDispatcher(); val scope = CoroutineScope(SupervisorJob() + dispatcher)
        LocalQueueStore(app).save(SavedLocalQueue(listOf("1", "2"), 1, 3456, Player.REPEAT_MODE_ONE, true))
        try {
            repeat(2) {
                val player = RecordingPlayer(); val binding = ProfilePlaybackBinding(app, player, scope, songs = { listOf(song("1"), song("2")) })
                try {
                    dispatcher.scheduler.runCurrent(); shadowOf(Looper.getMainLooper()).idle()
                    assertEquals("2", player.currentMediaItem?.mediaId); assertEquals(3456L, player.currentPosition)
                    assertFalse(player.playWhenReady); assertEquals(Player.REPEAT_MODE_ONE, player.repeatMode)
                    assertTrue(player.shuffleModeEnabled)
                } finally { binding.close(); binding.close(); player.release() }
            }
        } finally { scope.cancel() }
    }
    @Test fun closingTheServiceCancelsLateRestoreAndPreservesSnapshot() {
        val dispatcher = StandardTestDispatcher(); val scope = CoroutineScope(SupervisorJob() + dispatcher)
        val player = RecordingPlayer(); val gate = CompletableDeferred<Unit>()
        val saved = SavedLocalQueue(listOf("1"), 0, 100, 0, false); LocalQueueStore(app).save(saved)
        val binding = ProfilePlaybackBinding(app, player, scope, songs = {
            withContext(NonCancellable) { gate.await() }; listOf(song("1"))
        })
        try {
            dispatcher.scheduler.runCurrent(); binding.close(); gate.complete(Unit)
            dispatcher.scheduler.runCurrent(); shadowOf(Looper.getMainLooper()).idle()
            assertEquals(0, player.mediaItemCount); assertEquals(saved, LocalQueueStore(app).read())
        } finally { binding.close(); scope.cancel(); player.release() }
    }
}
