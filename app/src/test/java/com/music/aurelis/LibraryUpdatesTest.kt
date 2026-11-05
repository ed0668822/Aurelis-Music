package com.music.aurelis

import android.Manifest
import com.music.aurelis.data.*
import com.music.aurelis.data.model.LocalSong
import com.music.aurelis.data.model.SortOption
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.test.*
import org.junit.Assert.*
import org.junit.Before
import org.junit.After
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33], manifest = Config.NONE, application = AurelisApplication::class)
class LibraryUpdatesTest {
    private val dispatcher = StandardTestDispatcher()
    private val app get() = RuntimeEnvironment.getApplication() as AurelisApplication
    private fun song(id: String, title: String = id) = LocalSong(id, title, "Artist", "Album",
        "content://media/external/audio/media/$id", null, 60_000, null, 1, 2026)

    @Before fun setup() { Dispatchers.setMain(dispatcher); shadowOf(app).grantPermissions(Manifest.permission.READ_MEDIA_AUDIO) }
    @After fun teardown() { Dispatchers.resetMain() }

    private class MutableCatalogue(var rows: List<LocalSong>) : LocalLibrarySource {
        var reads = 0
        var lastFilter = true
        val updates = MutableSharedFlow<Unit>(extraBufferCapacity = 20)
        override val changes: Flow<Unit> = flow { emit(Unit); emitAll(updates) }
        override suspend fun songs(filterNonMusic: Boolean): List<LocalSong> { reads++; lastFilter = filterNonMusic; return rows }
    }

    @Test fun updatesAreCoalescedReuseUnchangedRowsAndResumeWithAFreshSnapshot() = runTest(dispatcher) {
        val one = song("1"); val two = song("2")
        val source = MutableCatalogue(listOf(one, two))
        val model = LibraryViewModel(app, source)
        model.onForeground(); runCurrent()
        assertEquals(1, source.reads)
        source.rows = listOf(one.copy(title = "Renamed"), two.copy(), song("3"))
        repeat(8) { source.updates.tryEmit(Unit) }
        runCurrent(); advanceTimeBy(349); runCurrent()
        assertEquals(1, source.reads)
        advanceTimeBy(2); runCurrent()
        assertEquals(2, source.reads)
        var state = model.uiState.value.libraryState as LibraryState.Success
        assertSame(two, state.songs.first { it.id == "2" })
        assertEquals("Renamed", state.songs.first { it.id == "1" }.title)
        source.rows = listOf(two)
        model.onBackground(); runCurrent()
        source.updates.tryEmit(Unit); advanceTimeBy(1000); runCurrent()
        assertEquals(2, source.reads)
        model.onForeground(); runCurrent()
        state = model.uiState.value.libraryState as LibraryState.Success
        assertEquals(listOf("2"), state.songs.map { it.id })
        assertEquals(3, source.reads)
        model.onBackground()
    }

    @Test fun changesDuringAnActiveReadScheduleOneFollowupInsteadOfBeingLost() = runTest(dispatcher) {
        val gate = CompletableDeferred<Unit>()
        var calls = 0
        var rows = listOf(song("1"))
        val model = LibraryViewModel(app, LocalLibrarySource {
            val snapshot = rows
            if (++calls == 1) gate.await()
            snapshot
        })
        model.loadLibrary(); runCurrent()
        rows = listOf(song("2"))
        repeat(5) { model.refresh() }
        gate.complete(Unit); runCurrent()
        assertEquals(2, calls)
        assertEquals(listOf("2"), (model.uiState.value.libraryState as LibraryState.Success).songs.map { it.id })
        assertFalse(model.uiState.value.isRefreshing)
    }

    @Test fun permissionRevocationClearsVisibleAudioButKeepsPrivateCollectionsAndCanRecover() = runTest(dispatcher) {
        app.userLibrary.toggleFavorite("1")
        val source = MutableCatalogue(listOf(song("1")))
        val model = LibraryViewModel(app, source)
        model.onForeground(); runCurrent()
        shadowOf(app).denyPermissions(Manifest.permission.READ_MEDIA_AUDIO)
        model.onForeground(); runCurrent()
        assertTrue((model.uiState.value.libraryState as LibraryState.Error).permissionRequired)
        assertTrue(model.uiState.value.filteredSongs.isEmpty())
        assertEquals(setOf("1"), app.userLibrary.state.value.favorites)
        source.updates.tryEmit(Unit); advanceTimeBy(1000); runCurrent()
        assertEquals(1, source.reads)
        shadowOf(app).grantPermissions(Manifest.permission.READ_MEDIA_AUDIO)
        model.onForeground(); runCurrent()
        assertTrue(model.uiState.value.libraryState is LibraryState.Success)
        assertEquals(2, source.reads)
        model.onBackground()
    }

    @Test fun filterChangesRefreshAndAlbumsUseMediaIdsForCompilations() = runTest(dispatcher) {
        val a = song("1").copy(albumId = "10", artistId = "1")
        val b = song("2").copy(artist = "Other", albumId = "10", artistId = "2")
        val source = MutableCatalogue(listOf(a, b))
        val model = LibraryViewModel(app, source)
        model.onForeground(); runCurrent()
        val state = model.uiState.value.libraryState as LibraryState.Success
        assertEquals(1, state.albums.size)
        assertEquals("Varios artistas", state.albums.single().artist)
        assertEquals("media:10", state.albums.single().key)
        assertEquals(2, state.artists.size)
        SettingsRepository.setFilterNonMusicAudio(false)
        runCurrent(); advanceTimeBy(351); runCurrent()
        assertEquals(2, source.reads)
        assertFalse(source.lastFilter)
        model.onBackground()
    }

    @Test fun folderPolicyChangesAreObservedAndTriggerAFreshCatalogue() = runTest(dispatcher) {
        val mediaSource = MediaStoreLibrarySource(app)
        var events = 0
        val job = backgroundScope.launch(kotlinx.coroutines.Dispatchers.Unconfined) { mediaSource.changes.collect { events++ } }
        runCurrent(); val before = events
        SettingsRepository.setLocalFolderSelection(emptySet(), false)
        runCurrent()
        assertTrue(events > before)
        val refreshed = events
        SettingsRepository.setLocalFolderSelection(emptySet(), false)
        runCurrent()
        assertTrue("Regranting the same scope must still invalidate the catalogue", events > refreshed)
        job.cancel()
    }

    @Test fun localSearchAndSortAreConsistentForAccentsAndAlbumMetadata() = runTest(dispatcher) {
        val source = LocalLibrarySource { listOf(song("1", "Árbol"), song("2", "Barco").copy(durationMs = 90_000)) }
        val model = LibraryViewModel(app, source)
        model.loadLibrary(); runCurrent()
        model.onSearchQueryChanged("ARBOL")
        assertEquals(listOf("1"), model.uiState.value.filteredSongs.map { it.id })
        model.onSearchQueryChanged("álbum")
        assertEquals(2, model.uiState.value.filteredSongs.size)
        model.onSearchQueryChanged("ALBUM")
        assertEquals(2, model.uiState.value.filteredSongs.size)
        model.onSortOptionChanged(SortOption.DURATION_DESC)
        assertEquals(listOf("2", "1"), model.uiState.value.filteredSongs.map { it.id })
    }
}
