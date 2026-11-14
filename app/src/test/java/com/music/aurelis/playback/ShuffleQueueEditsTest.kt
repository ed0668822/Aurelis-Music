// Aurelis © 2026. GPL-3.0-or-later
package com.music.aurelis.playback

import android.os.Bundle
import android.os.Looper
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.source.ShuffleOrder.DefaultShuffleOrder
import androidx.media3.session.MediaController
import androidx.media3.session.MediaSession
import androidx.media3.session.SessionResult
import com.google.common.util.concurrent.ListenableFuture
import com.music.aurelis.AurelisApplication
import com.music.aurelis.data.model.LocalSong
import com.music.aurelis.data.model.QueueTier
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
class ShuffleQueueEditsTest {
    private fun song(id: String) = LocalSong(id, "Tema $id", "Artista", null,
        "content://media/external/audio/media/$id", null, 60_000, null, null, null)
    private fun ids(player: Player) = (0 until player.mediaItemCount).map { player.getMediaItemAt(it).mediaId }
    private fun visible(player: Player) = requireNotNull(queuePlaybackOrder(player)).map { player.getMediaItemAt(it).mediaId }
    private fun await(condition: () -> Boolean) {
        val deadline = System.nanoTime() + 5_000_000_000L
        while (!condition() && System.nanoTime() < deadline) {
            shadowOf(Looper.getMainLooper()).idle(); Thread.yield()
        }
        assertTrue("Session did not publish expected state", condition())
    }
    private fun result(future: ListenableFuture<SessionResult>): Int {
        await { future.isDone }; return future.get().resultCode
    }
    private fun withSession(owner: String? = null, test: (ExoPlayer, MediaController) -> Unit) {
        val app = RuntimeEnvironment.getApplication()
        val player = ExoPlayer.Builder(app).build()
        player.setMediaItems(listOf("a", "b", "a", "d").map { song(it).toQueueMediaItem() }, 2, 12_345L)
        player.setShuffleOrder(DefaultShuffleOrder(intArrayOf(2, 0, 3, 1), 5L))
        player.shuffleModeEnabled = true
        val session = MediaSession.Builder(app, player)
            .setCallback(ShuffleQueueCallback(player, owner ?: app.packageName)).build()
        var controller: MediaController? = null
        try {
            val future = MediaController.Builder(app, session.token).buildAsync()
            await { future.isDone }; val connected = future.get(); controller = connected
            test(player, connected)
        } finally { controller?.release(); session.release(); player.release() }
    }

    @Test fun playNextAndAppendPinFreshDuplicateOccurrencesWithoutRestartingPlayback() = withSession { player, controller ->
        assertTrue(ShuffleQueueEdits.available(controller))
        val current = player.currentMediaItem!!.queueEntryId
        assertEquals(SessionResult.RESULT_SUCCESS, result(ShuffleQueueEdits.insert(controller, listOf(song("a"), song("x")), true)))
        await { controller.mediaItemCount == 6 }
        assertEquals(listOf("a", "a", "x", "a", "d", "b"), visible(player))
        assertEquals(player.nextMediaItemIndex, 4)
        assertEquals(SessionResult.RESULT_SUCCESS, result(ShuffleQueueEdits.insert(controller, listOf(song("z")), false)))
        await { controller.mediaItemCount == 7 }
        assertEquals(listOf("a", "a", "x", "z", "a", "d", "b"), visible(player))
        assertEquals(visible(player), visible(controller))
        assertEquals(7, (0 until player.mediaItemCount).map { player.getMediaItemAt(it).queueEntryId }.toSet().size)
        assertTrue((4..6).all { player.getMediaItemAt(it).queueTier == QueueTier.USER_QUEUE })
        assertEquals(SessionResult.RESULT_SUCCESS, result(ShuffleQueueEdits.insert(controller, listOf(song("first")), true)))
        await { controller.mediaItemCount == 8 }
        assertEquals(listOf("a", "first", "a", "x", "z", "a", "d", "b"), visible(player))
        assertEquals(current, player.currentMediaItem!!.queueEntryId)
        assertEquals(12_345L, player.currentPosition)
        assertFalse(player.playWhenReady); assertEquals(Player.STATE_IDLE, player.playbackState)
        controller.shuffleModeEnabled = false
        await { !player.shuffleModeEnabled }
        assertEquals(listOf("a", "b", "a", "d", "a", "x", "z", "first"), ids(player))
        assertEquals(current, player.currentMediaItem!!.queueEntryId); assertEquals(12_345L, player.currentPosition)
    }

    @Test fun visibleMoveAndManualSelectionUseOccurrenceIdentityAndPreservePhysicalOrder() = withSession { player, controller ->
        val original = (0 until player.mediaItemCount).map { player.getMediaItemAt(it).queueEntryId }
        assertEquals(SessionResult.RESULT_SUCCESS, result(ShuffleQueueEdits.move(controller, 1, 0)))
        await { queuePlaybackOrder(controller) == listOf(2, 1, 0, 3) }
        assertEquals(listOf(2, 1, 0, 3), queuePlaybackOrder(player))
        assertEquals(original, (0 until player.mediaItemCount).map { player.getMediaItemAt(it).queueEntryId })
        controller.seekTo(0, 4321L)
        await { player.currentMediaItemIndex == 0 && controller.currentMediaItemIndex == 0 }
        assertEquals(original[0], player.currentMediaItem!!.queueEntryId)
        assertEquals(3, player.nextMediaItemIndex)
        assertEquals(SessionResult.RESULT_SUCCESS, result(ShuffleQueueEdits.insert(controller, listOf(song("q")), true)))
        assertEquals(listOf(2, 1, 0, 4, 3), queuePlaybackOrder(player))
        assertEquals(4321L, player.currentPosition); assertFalse(player.playWhenReady)
        controller.removeMediaItem(3)
        await { player.mediaItemCount == 4 && controller.mediaItemCount == 4 }
        assertEquals(listOf(2, 1, 0, 3), queuePlaybackOrder(player))
        assertEquals(original[0], player.currentMediaItem!!.queueEntryId)
        assertEquals(4321L, player.currentPosition)
    }

    @Test fun staleOccurrenceAndChangedCurrentRequestsNeverMoveADifferentSong() = withSession { player, controller ->
        val before = queuePlaybackOrder(player)
        val args = Bundle().apply {
            putString("operation", "move"); putString("from", "removed-entry")
            putString("to", player.getMediaItemAt(0).queueEntryId)
            putString("current", player.currentMediaItem!!.queueEntryId)
        }
        assertEquals(SessionResult.RESULT_ERROR_BAD_VALUE, result(controller.sendCustomCommand(ShuffleQueueEdits.command, args)))
        args.putString("from", player.getMediaItemAt(1).queueEntryId)
        args.putString("current", "previous-track")
        assertEquals(SessionResult.RESULT_ERROR_INVALID_STATE, result(controller.sendCustomCommand(ShuffleQueueEdits.command, args)))
        assertEquals(before, queuePlaybackOrder(player)); assertEquals(4, player.mediaItemCount)
    }

    @Test fun invalidPayloadNetworkSourcesAndShuffleOffAreRejectedBeforeMutating() = withSession { player, controller ->
        assertEquals(SessionResult.RESULT_ERROR_BAD_VALUE, result(ShuffleQueueEdits.insert(controller, emptyList(), true)))
        assertEquals(SessionResult.RESULT_ERROR_BAD_VALUE, result(ShuffleQueueEdits.insert(controller, List(201) { song("x") }, true)))
        assertEquals(SessionResult.RESULT_ERROR_BAD_VALUE, result(ShuffleQueueEdits.insert(controller,
            listOf(song("x").copy(contentUri = "https://example.com/audio")), true)))
        assertEquals(SessionResult.RESULT_ERROR_BAD_VALUE, result(controller.sendCustomCommand(ShuffleQueueEdits.command, Bundle())))
        player.shuffleModeEnabled = false
        await { !controller.shuffleModeEnabled }
        assertEquals(SessionResult.RESULT_ERROR_INVALID_STATE, result(ShuffleQueueEdits.insert(controller, listOf(song("x")), true)))
        assertEquals(4, player.mediaItemCount); assertEquals(12_345L, player.currentPosition)
    }

    @Test fun insertingIntoAnEmptyShuffledQueueDoesNotStartOrPreparePlayback() = withSession { player, controller ->
        player.clearMediaItems()
        await { controller.mediaItemCount == 0 }
        assertEquals(SessionResult.RESULT_SUCCESS, result(ShuffleQueueEdits.insert(controller, listOf(song("x"), song("x")), true)))
        await { controller.mediaItemCount == 2 }
        assertEquals(listOf(0, 1), queuePlaybackOrder(player))
        assertNotEquals(player.getMediaItemAt(0).queueEntryId, player.getMediaItemAt(1).queueEntryId)
        assertFalse(player.playWhenReady); assertEquals(Player.STATE_IDLE, player.playbackState)
    }

    @Test fun otherPackagesAreNotGrantedThePrivateQueueCommand() = withSession("another.package") { player, controller ->
        assertFalse(ShuffleQueueEdits.available(controller))
        assertEquals(SessionResult.RESULT_ERROR_NOT_SUPPORTED, result(ShuffleQueueEdits.insert(controller, listOf(song("x")), true)))
        assertNotEquals(SessionResult.RESULT_SUCCESS, result(controller.sendCustomCommand(ShuffleQueueEdits.command, Bundle())))
        assertEquals(4, player.mediaItemCount)
    }

    @Test fun clearUsesShuffledFutureRatherThanPhysicalIndexAndPreservesHistoryAndCurrent() = withSession { player, controller ->
        val items = listOf("past", "future", "now", "context").map {
            song(it).copy(queueTier = if (it == "context") QueueTier.CONTEXT else QueueTier.USER_QUEUE).toQueueMediaItem()
        }
        player.setMediaItems(items, 2, 9876L)
        player.setShuffleOrder(DefaultShuffleOrder(intArrayOf(0, 2, 1, 3), 5L))
        await { controller.currentMediaItem?.mediaId == "now" }
        val current = player.currentMediaItem!!.queueEntryId
        assertEquals(SessionResult.RESULT_SUCCESS, result(ShuffleQueueEdits.clear(controller)))
        await { player.mediaItemCount == 3 && controller.mediaItemCount == 3 }
        assertEquals(listOf("past", "now", "context"), ids(player))
        assertEquals(listOf("past", "now", "context"), visible(player))
        assertEquals(current, player.currentMediaItem!!.queueEntryId); assertEquals(9876L, player.currentPosition)
        assertFalse(player.playWhenReady)
    }
}
