// Aurelis © 2026. GPL-3.0-or-later
package com.music.aurelis.playback

import android.os.Looper
import androidx.media3.session.MediaSession
import androidx.media3.session.MediaController
import org.robolectric.Shadows.shadowOf
import androidx.media3.common.Player
import androidx.media3.common.Timeline
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.source.ForwardingTimeline
import androidx.media3.exoplayer.source.ShuffleOrder.DefaultShuffleOrder
import com.music.aurelis.AurelisApplication
import com.music.aurelis.data.model.LocalSong
import com.music.aurelis.ui.player.splitQueue
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config

@UnstableApi
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33], manifest = Config.NONE, application = AurelisApplication::class)
class QueuePlaybackOrderTest {
    private fun withQueue(test: (ExoPlayer) -> Unit) {
        val player = ExoPlayer.Builder(RuntimeEnvironment.getApplication()).build()
        try {
            val songs = listOf("same", "b", "same", "d").map { id ->
                LocalSong(id, "Tema $id", "Artista", null, "content://media/external/audio/media/$id",
                    null, 60000, null, null, null)
            }
            player.setMediaItems(songs.map { it.toQueueMediaItem() }, 2, 12345L)
            player.setShuffleOrder(DefaultShuffleOrder(intArrayOf(2, 0, 3, 1), 7L))
            test(player)
        } finally { player.release() }
    }

    @Test fun visibleOrderMatchesExoPlayerAndToggleRestoresPhysicalOrderWithoutResettingCurrent() = withQueue { player ->
        val original = (0 until player.mediaItemCount).map(player::getMediaItemAt)
        val current = player.currentMediaItem
        val position = player.currentPosition
        assertEquals(listOf(0, 1, 2, 3), queuePlaybackOrder(player))
        player.shuffleModeEnabled = true
        assertEquals(listOf(2, 0, 3, 1), queuePlaybackOrder(player))
        val rows = original.map { it.toLocalSong() }
        val display = splitQueue(rows, player.currentMediaItemIndex, true, false, queuePlaybackOrder(player))
        assertEquals(listOf(0, 3, 1), display.context.map { it.timelineIndex })
        assertEquals(player.nextMediaItemIndex, display.context.first().timelineIndex)
        assertNotEquals(display.nowPlaying!!.song.queueEntryId, display.context.first().song.queueEntryId)
        player.shuffleModeEnabled = false
        assertEquals(listOf(0, 1, 2, 3), queuePlaybackOrder(player))
        assertEquals(original, (0 until player.mediaItemCount).map(player::getMediaItemAt))
        assertEquals(current, player.currentMediaItem); assertEquals(position, player.currentPosition)
        assertFalse(player.playWhenReady); assertEquals(Player.STATE_IDLE, player.playbackState)
    }

    @Test fun choosingAVisibleOccurrenceUsesItsPhysicalIndexAndFollowingActualSuccessor() = withQueue { player ->
        player.shuffleModeEnabled = true
        val rows = (0 until player.mediaItemCount).map { player.getMediaItemAt(it).toLocalSong() }
        val display = splitQueue(rows, player.currentMediaItemIndex, false, false, queuePlaybackOrder(player))
        val chosen = display.context.first()
        player.seekTo(chosen.timelineIndex, 4321L)
        assertEquals(chosen.song.queueEntryId, player.currentMediaItem!!.queueEntryId)
        val after = splitQueue(rows, player.currentMediaItemIndex, true, false, queuePlaybackOrder(player))
        assertEquals(listOf(2), after.history.map { it.timelineIndex })
        assertEquals(player.nextMediaItemIndex, after.context.first().timelineIndex)
        player.shuffleModeEnabled = false
        assertEquals(chosen.song.queueEntryId, player.currentMediaItem!!.queueEntryId)
    }

    @Test fun incompleteCyclicAndInvalidOrdersNeverBecomeAClaimedPlaybackOrder() = withQueue { player ->
        val timeline = player.currentTimeline
        assertNull(timelinePlaybackOrder(timeline, 3))
        val cycle = object : ForwardingTimeline(timeline) {
            override fun getFirstWindowIndex(shuffleModeEnabled: Boolean) = 0
            override fun getNextWindowIndex(windowIndex: Int, repeatMode: Int, shuffleModeEnabled: Boolean) = windowIndex
        }
        val invalid = object : ForwardingTimeline(timeline) {
            override fun getFirstWindowIndex(shuffleModeEnabled: Boolean) = 99
        }
        assertNull(timelinePlaybackOrder(cycle, 4)); assertNull(timelinePlaybackOrder(invalid, 4))
        assertEquals(emptyList<Int>(), timelinePlaybackOrder(Timeline.EMPTY, 0))
        val rows = (0 until player.mediaItemCount).map { player.getMediaItemAt(it).toLocalSong() }
        assertTrue(splitQueue(rows, 2, playbackOrder = listOf(0, 0, 2, 3)).context.isEmpty())
    }

    @Test fun shuffleWithoutTimelinePermissionIsUnknownRatherThanPhysicalOrder() {
        val player = RecordingPlayer()
        try {
            val song = LocalSong("1", "Tema", "Artista", null, "content://media/1", null, 1000, null, null, null)
            player.setMediaItems(listOf(song.toQueueMediaItem()))
            player.shuffleModeEnabled = true
            player.publish(player.snapshot.buildUpon().setAvailableCommands(
                Player.Commands.Builder().addAllCommands().remove(Player.COMMAND_GET_TIMELINE).build()).build())
            assertNull(queuePlaybackOrder(player))
        } finally { player.release() }
    }

    @Test fun mediaControllerReceivesTheSameShuffleOrderAndCanRestoreTheOriginal() = withQueue { player ->
        player.shuffleModeEnabled = true
        val app = RuntimeEnvironment.getApplication()
        val session = MediaSession.Builder(app, player).build()
        var controller: MediaController? = null
        try {
            val future = MediaController.Builder(app, session.token).buildAsync()
            await { future.isDone }
            val connected = future.get()
            controller = connected
            assertEquals(queuePlaybackOrder(player), queuePlaybackOrder(connected))
            assertEquals(listOf(2, 0, 3, 1), queuePlaybackOrder(connected))
            connected.shuffleModeEnabled = false
            await { !player.shuffleModeEnabled && !connected.shuffleModeEnabled }
            assertEquals(listOf(0, 1, 2, 3), queuePlaybackOrder(connected))
        } finally { controller?.release(); session.release() }
    }

    private fun await(condition: () -> Boolean) {
        val deadline = System.nanoTime() + 5_000_000_000L
        while (!condition() && System.nanoTime() < deadline) {
            shadowOf(Looper.getMainLooper()).idle()
            Thread.yield()
        }
        assertTrue("MediaSession did not publish the expected state", condition())
    }

}
