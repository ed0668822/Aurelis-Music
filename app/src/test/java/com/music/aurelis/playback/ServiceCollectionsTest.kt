package com.music.aurelis.playback

import android.os.Looper
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
class ServiceCollectionsTest {
    private val app get() = RuntimeEnvironment.getApplication() as AurelisApplication
    private fun song(id: String) = LocalSong(id, id, "artist", "album", "content://media/external/audio/media/$id", null, 60_000, null, 1, 2026)
    @Test fun recorderCountsElapsedAudioNotSeekDistanceAndSurvivesWithoutAViewModel() {
        var now = 0L
        val store = LocalUserLibrary(app, clock = { now })
        val queue = LocalQueueStore(app)
        val player = RecordingPlayer()
        val scope = CoroutineScope(SupervisorJob() + StandardTestDispatcher())
        val recorder = ServicePlaybackRecorder(player, store, queue, scope) { now }
        try {
            PlayerConnection.playSongs(player, listOf(song("1")), 0); shadowOf(Looper.getMainLooper()).idle()
            repeat(30) { now += 1000; recorder.tick() }
            player.seekTo(50_000); shadowOf(Looper.getMainLooper()).idle()
            player.pause(); shadowOf(Looper.getMainLooper()).idle()
            assertEquals(30_000L, store.state.value.listening["1"]?.listenedMs)
            assertEquals(1, store.state.value.listening["1"]?.playCount)
            now += 5000; recorder.tick(); recorder.close()
            assertEquals(30_000L, LocalUserLibrary(app).state.value.listening["1"]?.listenedMs)
            assertEquals(50_000L, queue.read()?.positionMs)
        } finally { recorder.close(); scope.cancel(); player.release() }
    }
    @Test fun savedQueueResolvesMissingItemsKeepsDuplicatesAndNeverStoresAutoPlay() {
        val saved = SavedLocalQueue(listOf("gone", "1", "2", "1"), 2, 1234, 2, true)
        val store = LocalQueueStore(app); store.save(saved)
        assertEquals(saved, LocalQueueStore(app).read())
        val (rows, index) = saved.resolve(listOf(song("1")))
        assertEquals(listOf("1", "1"), rows.map { it.id }); assertEquals(1, index)
        assertTrue(saved.resolve(emptyList()).first.isEmpty())
        val other = LocalQueueStore(app, ProfileId("another")); assertNull(other.read())
    }
    @Test fun unknownQueueVersionIsPreserved() {
        val prefs = ProfileStorage(app).open(ProfileId.DEFAULT, ProfileDataArea.QUEUE)
        prefs.edit().putString("queue_v1", "{\"version\":99}").commit()
        val store = LocalQueueStore(app); assertTrue(store.readOnly)
        store.save(SavedLocalQueue(listOf("1"), 0, 0, 0, false))
        assertEquals("{\"version\":99}", prefs.getString("queue_v1", null))
    }
    @Test fun playlistOrderAndClearingHistoryPreserveOtherPrivateData() {
        val store = LocalUserLibrary(app)
        store.toggleFavorite("1")
        val id = store.createPlaylist("Lista", listOf("1", "missing", "2"))
        store.movePlaylistSong(id, 2, 0)
        store.recordListening(song("1"), 5000, false); store.flush(); store.clearHistory()
        val loaded = LocalUserLibrary(app).state.value
        assertEquals(listOf("2", "1", "missing"), loaded.playlists.single().songIds)
        assertEquals(setOf("1"), loaded.favorites); assertTrue(loaded.listening.isEmpty())
    }
    @Test fun media3ReorderPreservesPlayingItemAndEnqueuePreservesPosition() {
        val player = RecordingPlayer()
        try {
            PlayerConnection.playSongs(player, listOf(song("1"), song("2"), song("3")), 1)
            player.seekTo(20_000); player.moveMediaItem(0, 2); shadowOf(Looper.getMainLooper()).idle()
            assertEquals("2", player.currentMediaItem?.mediaId); assertEquals(20_000, player.currentPosition)
            player.addMediaItems(listOf(song("4").toMediaItem())); shadowOf(Looper.getMainLooper()).idle()
            assertEquals(4, player.mediaItemCount); assertEquals(20_000, player.currentPosition)
        } finally { player.release() }
    }
}
