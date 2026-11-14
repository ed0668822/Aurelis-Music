package com.music.aurelis.playback

import android.os.Looper
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import com.music.aurelis.AurelisApplication
import com.music.aurelis.data.model.LocalSong
import com.music.aurelis.data.model.QueueTier
import com.music.aurelis.ui.player.splitQueue
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.Assert.*
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config

@UnstableApi
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33], manifest = Config.NONE, application = AurelisApplication::class)
class QueueCoordinatorTest {
    private lateinit var player: RecordingPlayer
    private lateinit var queue: QueueCoordinator
    private fun song(id: String) = LocalSong(id, "Canción $id", "Artista", "Álbum",
        "content://media/external/audio/media/$id", null, 60_000, null, null, null)
    private fun ids() = (0 until player.mediaItemCount).map { player.getMediaItemAt(it).mediaId }
    private fun entries() = (0 until player.mediaItemCount).map { player.getMediaItemAt(it).toLocalSong() }
    private fun start(ids: List<String> = listOf("1", "2", "3"), index: Int = 1) {
        PlayerConnection.playSongs(player, ids.map(::song), index)
        player.seekTo(12_345)
        player.pause()
    }
    @Before fun setup() { player = RecordingPlayer(); queue = QueueCoordinator { player } }
    @After fun cleanup() { player.release(); shadowOf(Looper.getMainLooper()).idle() }

    @Test fun playNextInsertsOrderedOccurrencesWithoutRestartingTheCurrentTrack() {
        start()
        val current = player.currentMediaItem!!.queueEntryId
        val before = Triple(player.replacements, player.prepares, player.seeks)
        assertTrue(queue.playNext(listOf(song("2"), song("4"), song("2"))))
        assertEquals(listOf("1", "2", "2", "4", "2", "3"), ids())
        assertEquals(current, player.currentMediaItem!!.queueEntryId)
        assertEquals(12_345L, player.currentPosition)
        assertFalse(player.playWhenReady)
        assertEquals(before, Triple(player.replacements, player.prepares, player.seeks))
        assertEquals(6, entries().map { it.queueEntryId }.toSet().size)
        assertTrue(entries().subList(2, 5).all { it.queueTier == QueueTier.USER_QUEUE })
    }

    @Test fun movingADuplicatePreservesTheExactPlayingOccurrenceAndItsPosition() {
        start(listOf("1", "2", "2", "3"), 2)
        player.play()
        player.repeatMode = Player.REPEAT_MODE_ALL
        player.shuffleModeEnabled = true
        val current = player.currentMediaItem!!.queueEntryId
        val other = player.getMediaItemAt(1).queueEntryId
        val before = Triple(player.replacements, player.prepares, player.seeks)
        assertTrue(queue.moveItem(1, 3))
        assertEquals(listOf("1", "2", "3", "2"), ids())
        assertEquals(current, player.currentMediaItem!!.queueEntryId)
        assertEquals(other, player.getMediaItemAt(3).queueEntryId)
        assertEquals(1, player.currentMediaItemIndex)
        assertEquals(12_345L, player.currentPosition)
        assertTrue(player.playWhenReady)
        assertTrue(player.shuffleModeEnabled)
        assertEquals(Player.REPEAT_MODE_ALL, player.repeatMode)
        assertEquals(before, Triple(player.replacements, player.prepares, player.seeks))
        assertTrue(queue.removeItem(3))
        assertEquals(listOf("1", "2", "3"), ids())
        assertEquals(current, player.currentMediaItem!!.queueEntryId)
        assertFalse(queue.removeItem(player.currentMediaItemIndex))
    }

    @Test fun movingThePlayingRowKeepsItsIdentityAndPausedPosition() {
        start()
        val current = player.currentMediaItem!!.queueEntryId
        assertTrue(queue.moveItem(1, 0))
        assertEquals(0, player.currentMediaItemIndex)
        assertEquals(current, player.currentMediaItem!!.queueEntryId)
        assertEquals(12_345L, player.currentPosition)
        assertFalse(player.playWhenReady)
    }

    @Test fun appendToAnEmptyTimelineDoesNotStartPlaybackOrDeduplicateSongs() {
        assertTrue(queue.append(listOf(song("1"), song("1"))))
        assertEquals(listOf("1", "1"), ids())
        assertFalse(player.playWhenReady)
        assertEquals(0, player.prepares)
        assertEquals(Player.STATE_IDLE, player.playbackState)
        assertNotEquals(player.getMediaItemAt(0).queueEntryId, player.getMediaItemAt(1).queueEntryId)
    }

    @Test fun clearOnlyRemovesUpcomingUserEntriesAndNeverHistoryOrContext() {
        player.setMediaItems(listOf(
            song("1").copy(queueTier = QueueTier.USER_QUEUE).toQueueMediaItem(),
            song("2").copy(queueTier = QueueTier.USER_QUEUE).toQueueMediaItem(),
            song("3").toQueueMediaItem(),
        ), 1, 4_567)
        player.prepare()
        assertTrue(queue.playNext(listOf(song("4"))))
        assertTrue(queue.append(listOf(song("5"))))
        assertTrue(queue.clearUserQueue())
        assertEquals(listOf("1", "2", "3"), ids())
        assertEquals(1, player.currentMediaItemIndex)
        assertEquals(4_567L, player.currentPosition)
        assertFalse(queue.clearUserQueue())
    }

    @Test fun invalidIndexesAndEmptyRequestsAreNoOps() {
        start()
        val before = entries()
        assertFalse(queue.moveItem(-1, 0)); assertFalse(queue.moveItem(0, 99))
        assertFalse(queue.moveItem(1, 1)); assertFalse(queue.removeItem(99))
        assertFalse(queue.append(emptyList())); assertFalse(queue.playNext(emptyList()))
        assertEquals(before, entries())
    }

    @Test fun disconnectedOrRestrictedControllersCannotEditTheTimeline() {
        start()
        val before = entries()
        assertFalse(QueueCoordinator { null }.canEdit)
        assertFalse(QueueCoordinator { null }.append(listOf(song("4"))))
        player.publish(player.snapshot.buildUpon().setAvailableCommands(
            Player.Commands.Builder().addAllCommands().remove(Player.COMMAND_CHANGE_MEDIA_ITEMS).build()).build())
        assertFalse(queue.canEdit)
        assertFalse(queue.append(listOf(song("4"))))
        assertFalse(queue.moveItem(0, 2)); assertFalse(queue.removeItem(0))
        assertFalse(queue.clearUserQueue())
        assertEquals(before, entries())
    }

    @Test fun playNextDoesNotSilentlyChangeShuffleOrPromiseAnUncontrolledSuccessor() {
        start()
        player.shuffleModeEnabled = true
        val before = entries()
        assertFalse(queue.playNext(listOf(song("4"))))
        assertEquals(before, entries()); assertTrue(player.shuffleModeEnabled)
        assertTrue(queue.append(listOf(song("4"))))
    }

    @Test fun queueKeysFollowDuplicateOccurrencesThroughAReorder() {
        start(listOf("1", "2", "2", "3"), 0)
        val before = splitQueue(entries(), 0).context.associate { it.song.queueEntryId to it.key }
        queue.moveItem(1, 3)
        val after = splitQueue(entries(), 0).context.associate { it.song.queueEntryId to it.key }
        assertEquals(3, before.size)
        assertEquals(before, after)
    }

    @Test fun flatQueuePresentationMatchesTheCanonicalTimelineAcrossUserAndContextEntries() {
        start(listOf("1", "2", "3"), 0)
        queue.append(listOf(song("4")))
        queue.playNext(listOf(song("5")))
        val flat = splitQueue(entries(), 0, groupByTier = false)
        assertEquals(listOf("5", "2", "3", "4"), flat.context.map { it.song.id })
        assertTrue(flat.user.isEmpty()); assertTrue(flat.autoplay.isEmpty())
    }

    @Test fun mediaMetadataRoundTripPreservesExplicitIdentityAndTier() {
        val original = song("4").copy(queueEntryId = "local-entry", queueTier = QueueTier.USER_QUEUE)
        assertEquals(original, original.toMediaItem().toLocalSong())
        assertNotEquals(original.toQueueMediaItem().queueEntryId, original.toQueueMediaItem().queueEntryId)
    }
}
