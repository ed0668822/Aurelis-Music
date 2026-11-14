package com.music.aurelis.data

import com.music.aurelis.AurelisApplication
import com.music.aurelis.data.model.LocalSong
import com.music.aurelis.data.model.ProfileId
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33], manifest = Config.NONE, application = AurelisApplication::class)
class LocalUserLibraryTest {
    private val context get() = RuntimeEnvironment.getApplication()
    private fun song(id: String, title: String = id, artist: String = "Artist") = LocalSong(
        id, title, artist, "Album", "content://media/external/audio/media/$id", null,
        60_000, null, 1, 2026,
    )

    @Test fun favoritesAndPlaylistsPersistWithoutOverwritingUnknownKeys() {
        val prefs = ProfileStorage(context).open(ProfileId.DEFAULT, ProfileDataArea.PLAYLISTS)
        prefs.edit().putString("unknown", "preserve").commit()
        val store = LocalUserLibrary(context)
        store.toggleFavorite("1")
        val id = store.createPlaylist("  My list  ", listOf("1", "1", "missing"))
        store.renamePlaylist(id, "Renamed")
        store.setPlaylistSongs(id, listOf("missing", "2", "2"))
        val restored = LocalUserLibrary(context).state.value
        assertEquals(setOf("1"), restored.favorites)
        assertEquals(LocalPlaylist(id, "Renamed", listOf("missing", "2")), restored.playlists.single())
        assertEquals("preserve", prefs.getString("unknown", null))
        store.deletePlaylist(id)
        assertTrue(LocalUserLibrary(context).state.value.playlists.isEmpty())
        store.toggleFavorite("1")
        assertTrue(LocalUserLibrary(context).state.value.favorites.isEmpty())
    }

    @Test fun profileCollectionsAndHistoryAreIsolated() {
        val alice = LocalUserLibrary(context, ProfileId("alice"))
        val bob = LocalUserLibrary(context, ProfileId("bob"))
        alice.toggleFavorite("1"); alice.createPlaylist("Private")
        alice.recordListening(song("1"), 1000, false); alice.flush()
        assertTrue(bob.state.value.favorites.isEmpty())
        assertTrue(bob.state.value.playlists.isEmpty())
        assertTrue(bob.state.value.listening.isEmpty())
        assertEquals(setOf("1"), LocalUserLibrary(context, ProfileId("alice")).state.value.favorites)
    }

    @Test fun corruptedOrUnknownVersionDataRemainsUntouchedAndRejectsWrites() {
        val prefs = ProfileStorage(context).open(ProfileId.DEFAULT, ProfileDataArea.PLAYLISTS)
        for (raw in listOf("broken-json", "{\"version\":99,\"items\":[]}")) {
            prefs.edit().putString("playlists_v1", raw).commit()
            val store = LocalUserLibrary(context)
            assertTrue(store.state.value.readOnly)
            assertThrows(IllegalStateException::class.java) { store.createPlaylist("Must not overwrite") }
            store.recordListening(song("1"), 1000, true); store.flush()
            assertEquals(raw, prefs.getString("playlists_v1", null))
        }
    }

    @Test fun playCountsUseListeningTimeNotTicksOrSeeksAndPersistAcrossReopen() {
        var time = 100_000L
        val store = LocalUserLibrary(context, clock = { time })
        val track = song("1")
        repeat(6) { time += 5000; store.recordListening(track, 5000, false) }
        store.recordListening(track, 10_000, false) // seek: ignored
        store.recordListening(track, -500, false)
        store.recordListening(track, 5000, false)
        store.flush(); store.flush()
        var saved = LocalUserLibrary(context).state.value.listening.getValue("1")
        assertEquals(35_000, saved.listenedMs)
        assertEquals(1, saved.playCount)
        store.startListeningSession("1") // same-track repeat is a new session
        repeat(6) { time += 5000; store.recordListening(track, 5000, false) }
        store.flush()
        saved = LocalUserLibrary(context).state.value.listening.getValue("1")
        assertEquals(65_000, saved.listenedMs)
        assertEquals(2, saved.playCount)
    }

    @Test fun homeResolvesOnlyAvailableMediaAndBuildsDeterministicLocalQueues() {
        val songs = listOf(song("1"), song("2"), song("3", "1"))
        val data = LocalUserLibraryState(setOf("2", "removed"), listening = mapOf(
            "1" to LocalListening(40_000, 1, 100), "removed" to LocalListening(90_000, 4, 200)))
        val home = LocalHomeData.build(songs, data, "1")
        assertEquals(listOf("1"), home.recent.map { it.id })
        assertEquals(listOf("1"), home.mostPlayed.map { it.id })
        assertEquals(listOf("2"), home.favorites.map { it.id })
        assertEquals(listOf("1", "2"), home.mix.map { it.id })
        assertTrue(home.mix.all { it.contentUri.startsWith("content://") })
        assertEquals(home, LocalHomeData.build(songs, data, "1"))
        assertTrue(LocalHomeData.build(emptyList(), data, null).mix.isEmpty())
    }

    @Test fun shortLocalTracksQualifyWithoutAnImpossibleMinimumListeningTime() {
        val store = LocalUserLibrary(context)
        store.recordListening(song("short").copy(durationMs = 500), 250, false)
        store.flush()
        assertEquals(1, LocalUserLibrary(context).state.value.listening.getValue("short").playCount)
    }
}
