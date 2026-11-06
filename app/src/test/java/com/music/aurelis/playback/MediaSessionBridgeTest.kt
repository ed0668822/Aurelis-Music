package com.music.aurelis.playback

import android.os.Looper
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.session.MediaController
import androidx.media3.session.MediaSession
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
class MediaSessionBridgeTest {
    @Test fun mediaControllerUsesOneSessionForMetadataTransportAndRelease() {
        val app = RuntimeEnvironment.getApplication()
        val player = RecordingPlayer()
        val song = LocalSong("1", "Audio local", "Artista", "Álbum", "content://media/external/audio/media/1",
            null, 60_000, null, 1, 2026, albumId = "4", artistId = "7")
        PlayerConnection.playSongs(player, listOf(song, song.copy(id = "2")), 0)
        val session = MediaSession.Builder(app, player).build()
        var controller: MediaController? = null
        try {
            val future = MediaController.Builder(app, session.token).buildAsync()
            await { future.isDone }
            controller = future.get()
            assertEquals("Audio local", controller.mediaMetadata.title.toString())
            assertEquals(song, controller.currentMediaItem!!.toLocalSong().copy(queueEntryId = null))
            assertNotNull(controller.currentMediaItem!!.queueEntryId)
            assertTrue(controller.isCommandAvailable(Player.COMMAND_PLAY_PAUSE))
            controller.pause(); await { !player.playWhenReady }
            controller.seekToNextMediaItem(); await { player.currentMediaItemIndex == 1 }
            controller.repeatMode = Player.REPEAT_MODE_ALL; await { player.repeatMode == Player.REPEAT_MODE_ALL }
            controller.shuffleModeEnabled = true; await { player.shuffleModeEnabled }
            controller.play(); await { player.playWhenReady }
            controller.release(); controller = null
            assertFalse("Releasing UI controller must retain session player", player.released)
            assertTrue(player.playWhenReady)
        } finally { controller?.release(); session.release(); player.release() }
    }
    @Test fun queueEditsCrossTheSessionWithoutReplacingOrSeekingTheCurrentOccurrence() {
        val app = RuntimeEnvironment.getApplication()
        val player = RecordingPlayer()
        val song = LocalSong("1", "Audio local", "Artista", "Álbum", "content://media/external/audio/media/1",
            null, 60_000, null, 1, 2026)
        PlayerConnection.playSongs(player, listOf(song, song.copy(id = "2")), 0)
        player.pause(); player.seekTo(12_345)
        val session = MediaSession.Builder(app, player).build()
        var controller: MediaController? = null
        try {
            val future = MediaController.Builder(app, SessionToken(app, session.token)).buildAsync()
            await { future.isDone }; val connected = future.get(); controller = connected
            val queue = QueueCoordinator { connected }
            val current = player.currentMediaItem!!.queueEntryId
            val before = Triple(player.replacements, player.prepares, player.seeks)
            assertTrue(queue.canEdit)
            assertTrue(queue.playNext(listOf(song)))
            await { player.mediaItemCount == 3 && connected.mediaItemCount == 3 }
            assertEquals(current, player.currentMediaItem!!.queueEntryId)
            assertNotEquals(current, connected.getMediaItemAt(1).queueEntryId)
            assertEquals(player.getMediaItemAt(1).queueEntryId, connected.getMediaItemAt(1).queueEntryId)
            assertTrue(queue.moveItem(1, 2))
            await { player.getMediaItemAt(2).mediaId == "1" && connected.getMediaItemAt(2).mediaId == "1" }
            assertEquals(current, player.currentMediaItem!!.queueEntryId)
            assertEquals(12_345L, player.currentPosition)
            assertFalse(player.playWhenReady)
            assertEquals(before, Triple(player.replacements, player.prepares, player.seeks))
        } finally { controller?.release(); session.release(); player.release() }
    }

    private fun await(condition: () -> Boolean) {
        val deadline = System.nanoTime() + 5_000_000_000
        while (!condition() && System.nanoTime() < deadline) {
            shadowOf(Looper.getMainLooper()).idle()
            Thread.sleep(5)
        }
        assertTrue("MediaSession command did not complete", condition())
    }
}
