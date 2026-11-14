// Aurelis © 2026. GPL-3.0-or-later
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

@OptIn(ExperimentalCoroutinesApi::class)
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
    private fun settle(dispatcher: kotlinx.coroutines.test.TestDispatcher) {
        dispatcher.scheduler.runCurrent(); shadowOf(Looper.getMainLooper()).idle(); dispatcher.scheduler.runCurrent()
    }

    @Test fun realExoPlayerRestoresPermutationAndDuplicateIdentityWithoutPreparingOrPlaying() {
        val dispatcher = StandardTestDispatcher(); val scope = CoroutineScope(SupervisorJob() + dispatcher)
        val saved = SavedLocalQueue(listOf("1", "2", "1"), 2, 12345, Player.REPEAT_MODE_ALL, true,
            listOf("first", "second", "playing"), listOf(com.music.aurelis.data.model.QueueTier.CONTEXT,
                com.music.aurelis.data.model.QueueTier.USER_QUEUE, com.music.aurelis.data.model.QueueTier.CONTEXT), listOf(2, 0, 1))
        LocalQueueStore(app).save(saved)
        val player = androidx.media3.exoplayer.ExoPlayer.Builder(app).build()
        val binding = ProfilePlaybackBinding(app, player, scope, songs = { listOf(song("1"), song("2")) })
        try {
            settle(dispatcher)
            assertEquals("playing", player.currentMediaItem!!.queueEntryId)
            assertEquals(12345L, player.currentPosition)
            assertEquals(listOf(2, 0, 1), queuePlaybackOrder(player))
            assertFalse(player.playWhenReady); assertEquals(Player.STATE_IDLE, player.playbackState)
            assertEquals(saved, LocalQueueStore(app).read())
        } finally { binding.close(); scope.cancel(); player.release() }
    }

    @Test fun clearingARestoredQueuePersistsEmptyInsteadOfResurrectingItOnRestart() {
        val dispatcher = StandardTestDispatcher(); val scope = CoroutineScope(SupervisorJob() + dispatcher)
        LocalQueueStore(app).save(SavedLocalQueue(listOf("1"), 0, 500, 0, false))
        val player = RecordingPlayer()
        val binding = ProfilePlaybackBinding(app, player, scope, songs = { listOf(song("1")) })
        try {
            settle(dispatcher); assertEquals(1, player.mediaItemCount)
            player.clearMediaItems(); settle(dispatcher)
            assertTrue(requireNotNull(LocalQueueStore(app).read()).ids.isEmpty())
        } finally { binding.close(); player.release() }
        val next = RecordingPlayer()
        val restarted = ProfilePlaybackBinding(app, next, scope, songs = { listOf(song("1")) })
        try { settle(dispatcher); assertEquals(0, next.mediaItemCount); assertFalse(next.playWhenReady) }
        finally { restarted.close(); scope.cancel(); next.release() }
    }

    @Test fun aUserCommandCancelsALateRestoreEvenWhenThePlayerRemainsEmpty() {
        val dispatcher = StandardTestDispatcher(); val scope = CoroutineScope(SupervisorJob() + dispatcher)
        val gate = CompletableDeferred<Unit>()
        val saved = SavedLocalQueue(listOf("1"), 0, 500, 0, false); LocalQueueStore(app).save(saved)
        val player = RecordingPlayer()
        val binding = ProfilePlaybackBinding(app, player, scope, songs = {
            withContext(NonCancellable) { gate.await() }; listOf(song("1"))
        })
        try {
            settle(dispatcher); binding.cancelRestore(); gate.complete(Unit); settle(dispatcher)
            assertEquals(0, player.mediaItemCount); assertFalse(player.playWhenReady)
            binding.close()
            assertEquals(saved, LocalQueueStore(app).read())
        } finally { binding.close(); scope.cancel(); player.release() }
    }

    @Test fun replacingQueueWhileCatalogIsLoadingPreservesNewPlaybackAndIgnoresTheLateResult() {
        val dispatcher = StandardTestDispatcher(); val scope = CoroutineScope(SupervisorJob() + dispatcher)
        val gate = CompletableDeferred<Unit>()
        LocalQueueStore(app).save(SavedLocalQueue(listOf("1"), 0, 500, 0, false))
        val player = RecordingPlayer()
        val binding = ProfilePlaybackBinding(app, player, scope, songs = {
            withContext(NonCancellable) { gate.await() }; listOf(song("1"))
        })
        try {
            settle(dispatcher)
            PlayerConnection.playSongs(player, listOf(song("new"))); player.seekTo(4321)
            val current = player.currentMediaItem!!.queueEntryId
            gate.complete(Unit); settle(dispatcher)
            assertEquals("new", player.currentMediaItem!!.mediaId)
            assertEquals(current, player.currentMediaItem!!.queueEntryId); assertEquals(4321L, player.currentPosition)
            assertTrue(player.playWhenReady); assertEquals(1, player.replacements)
        } finally { binding.close(); scope.cancel(); player.release() }
    }

    @Test fun missingCurrentStartsTheNearestRemainingTrackAtZeroAndAnUnavailableCatalogKeepsTheSnapshot() {
        val dispatcher = StandardTestDispatcher(); val scope = CoroutineScope(SupervisorJob() + dispatcher)
        val saved = SavedLocalQueue(listOf("gone", "2"), 0, 6000, 0, false)
        LocalQueueStore(app).save(saved)
        val player = RecordingPlayer()
        val binding = ProfilePlaybackBinding(app, player, scope, songs = { listOf(song("2")) })
        try { settle(dispatcher); assertEquals("2", player.currentMediaItem!!.mediaId); assertEquals(0L, player.currentPosition) }
        finally { binding.close(); player.release() }
        LocalQueueStore(app).save(saved)
        val empty = RecordingPlayer()
        val unavailable = ProfilePlaybackBinding(app, empty, scope, songs = { emptyList() })
        try { settle(dispatcher); unavailable.close(); assertEquals(saved, LocalQueueStore(app).read()) }
        finally { unavailable.close(); scope.cancel(); empty.release() }
    }

    @Test fun remotePreferenceCommandCancelsPendingRestoreWithoutErasingRecoverableQueue() {
        val dispatcher = StandardTestDispatcher(); val scope = CoroutineScope(SupervisorJob() + dispatcher)
        val gate = CompletableDeferred<Unit>()
        val saved = SavedLocalQueue(listOf("1"), 0, 500, 0, false); LocalQueueStore(app).save(saved)
        val player = androidx.media3.exoplayer.ExoPlayer.Builder(app).build()
        val controls = LocalPlaybackCoordinator(player)
        controls.bindProfile(app, scope) { withContext(NonCancellable) { gate.await() }; listOf(song("1")) }
        val session = androidx.media3.session.MediaSession.Builder(app, player)
            .setCallback(ShuffleQueueCallback(player, app.packageName, controls)).build()
        controls.bind(session)
        var controller: androidx.media3.session.MediaController? = null
        fun await(condition: () -> Boolean) {
            val deadline = System.nanoTime() + 5_000_000_000L
            while (!condition() && System.nanoTime() < deadline) { shadowOf(Looper.getMainLooper()).idle(); Thread.yield() }
            assertTrue("Session command did not complete", condition())
        }
        try {
            settle(dispatcher)
            val future = androidx.media3.session.MediaController.Builder(app, session.token).buildAsync()
            await { future.isDone }; val connected = future.get(); controller = connected
            connected.repeatMode = Player.REPEAT_MODE_ONE
            await { player.repeatMode == Player.REPEAT_MODE_ONE }
            gate.complete(Unit); settle(dispatcher)
            assertEquals(0, player.mediaItemCount); assertFalse(player.playWhenReady)
            assertEquals(Player.REPEAT_MODE_ONE, player.repeatMode)
            controls.close()
            assertEquals(saved, LocalQueueStore(app).read())
        } finally { gate.complete(Unit); controller?.release(); controls.close(); scope.cancel(); settle(dispatcher); session.release(); player.release() }
    }

    @Test fun externalUriOnlyItemsDoNotCrashOrOverwriteTheRecoverableLocalSnapshot() {
        val dispatcher = StandardTestDispatcher(); val scope = CoroutineScope(SupervisorJob() + dispatcher)
        val saved = SavedLocalQueue(listOf("1"), 0, 500, 0, false); LocalQueueStore(app).save(saved)
        val player = RecordingPlayer()
        val binding = ProfilePlaybackBinding(app, player, scope, songs = { emptyList() })
        try {
            settle(dispatcher)
            player.setMediaItems(listOf(androidx.media3.common.MediaItem.Builder().setUri("content://media/external/42").build()))
            settle(dispatcher); binding.close()
            assertEquals(saved, LocalQueueStore(app).read())
        } finally { binding.close(); scope.cancel(); player.release() }
    }

}
