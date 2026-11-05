package com.music.aurelis

import android.Manifest
import android.app.Application
import com.music.aurelis.data.LibraryState
import com.music.aurelis.data.LocalLibrarySource
import com.music.aurelis.data.SettingsRepository
import com.music.aurelis.data.model.LocalSong
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33], manifest = Config.NONE, application = AurelisApplication::class)
class LibraryViewModelTest {
    private val dispatcher = StandardTestDispatcher()
    private val application: Application get() = RuntimeEnvironment.getApplication()

    @Before fun setup() {
        Dispatchers.setMain(dispatcher)
        shadowOf(application).grantPermissions(Manifest.permission.READ_MEDIA_AUDIO)
    }

    @After fun teardown() { Dispatchers.resetMain() }

    @Test fun loadsThroughContractAndOnlyRescansWhenForced() = runTest(dispatcher) {
        var calls = 0
        SettingsRepository.setFilterNonMusicAudio(false)
        val model = LibraryViewModel(application, LocalLibrarySource { filter ->
            assertFalse(filter)
            calls++
            emptyList()
        })
        model.loadLibrary()
        runCurrent()
        assertEquals(LibraryState.Success(emptyList()), model.uiState.value.libraryState)
        model.loadLibrary()
        runCurrent()
        assertEquals(1, calls)
        model.refresh()
        runCurrent()
        assertEquals(2, calls)
    }

    @Test fun deniedPermissionNeverCallsSourceAndCanBeRetried() = runTest(dispatcher) {
        shadowOf(application).denyPermissions(Manifest.permission.READ_MEDIA_AUDIO)
        var calls = 0
        val model = LibraryViewModel(application, LocalLibrarySource { calls++; emptyList() })
        model.loadLibrary()
        runCurrent()
        assertTrue(model.uiState.value.libraryState is LibraryState.Error)
        assertEquals(0, calls)
        shadowOf(application).grantPermissions(Manifest.permission.READ_MEDIA_AUDIO)
        model.loadLibrary()
        runCurrent()
        assertTrue(model.uiState.value.libraryState is LibraryState.Success)
        assertEquals(1, calls)
    }

    @Test fun loadedLocalSongsReachGroupingAndSearchWithoutChangingTheirIdentity() = runTest(dispatcher) {
        val song = LocalSong(
            id = "42", title = "Local track", artist = "Local artist", album = "Local album",
            contentUri = "content://media/external/audio/media/42", artworkUri = null,
            durationMs = 180_000, filePath = null, trackNumber = 1, year = 2026,
            dateAddedMs = 1234,
        )
        val model = LibraryViewModel(application, LocalLibrarySource { listOf(song) })
        model.loadLibrary()
        runCurrent()
        val library = model.uiState.value.libraryState as LibraryState.Success
        assertEquals(listOf(song), library.songs)
        assertEquals(listOf(song), library.albums.single().songs)
        assertEquals(listOf(song), library.artists.single().songs)
        assertEquals(listOf(song), library.recentlyAdded)
        model.onSearchQueryChanged("LOCAL ARTIST")
        assertEquals(listOf(song), model.uiState.value.filteredSongs)
        model.onSearchQueryChanged("missing")
        assertTrue(model.uiState.value.filteredSongs.isEmpty())
    }

    @Test fun cancellationIsNotReportedAsFailureAndAllowsRetry() = runTest(dispatcher) {
        var calls = 0
        val model = LibraryViewModel(application, LocalLibrarySource {
            if (++calls == 1) throw CancellationException("cancelled scan")
            emptyList()
        })
        model.loadLibrary()
        runCurrent()
        assertEquals(LibraryState.Loading, model.uiState.value.libraryState)
        model.loadLibrary()
        runCurrent()
        assertTrue(model.uiState.value.libraryState is LibraryState.Success)
        assertEquals(2, calls)
    }

    @Test fun sourceFailureIsReportedAndAllowsRetry() = runTest(dispatcher) {
        var calls = 0
        val model = LibraryViewModel(application, LocalLibrarySource {
            if (++calls == 1) error("scanner unavailable")
            emptyList()
        })
        model.loadLibrary()
        runCurrent()
        assertEquals(LibraryState.Error("scanner unavailable"), model.uiState.value.libraryState)
        model.loadLibrary()
        runCurrent()
        assertTrue(model.uiState.value.libraryState is LibraryState.Success)
    }

    @Test fun androidFactoryConstructorRemainsAvailable() {
        assertNotNull(LibraryViewModel::class.java.getConstructor(Application::class.java))
    }
}
