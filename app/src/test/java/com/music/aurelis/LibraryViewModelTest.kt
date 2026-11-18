// SPDX-FileCopyrightText: 2026 Aurelis
// SPDX-License-Identifier: LGPL-3.0-or-later
package com.music.aurelis

import android.Manifest
import android.app.Application
import android.os.Looper
import android.provider.MediaStore
import com.music.aurelis.data.MediaStoreLibrarySource
import com.music.aurelis.data.LibraryState
import com.music.aurelis.data.LocalLibrarySource
import com.music.aurelis.data.SettingsRepository
import com.music.aurelis.data.model.LocalSong
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.take
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceTimeBy
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


    @Test fun albumsKeepMediaStoreIdsAndUseDiscThenTrackOrder() = runTest(dispatcher) {
        fun row(id: String, albumId: String, artistId: String, disc: Int, track: Int) = LocalSong(
            id = id, title = "Pista $id", artist = "Mismo artista", album = "Mismo álbum",
            contentUri = "content://media/external/audio/media/$id", artworkUri = null,
            durationMs = 60_000L, filePath = null, trackNumber = track, year = null,
            albumId = albumId, artistId = artistId, discNumber = disc,
        )
        val tracks = listOf(
            row("a3", "10", "50", 2, 1),
            row("a2", "10", "50", 1, 4),
            row("b1", "11", "50", 1, 1),
            row("a1", "10", "50", 1, 1),
        )
        val model = LibraryViewModel(application, LocalLibrarySource { tracks })
        model.loadLibrary()
        runCurrent()

        val library = model.uiState.value.libraryState as LibraryState.Success
        assertEquals(setOf("media:10", "media:11"), library.albums.map { it.key }.toSet())
        assertEquals(listOf("a1", "a2", "a3"),
            library.albums.single { it.mediaId == "10" }.songs.map { it.id })
        assertEquals(listOf("b1"), library.albums.single { it.mediaId == "11" }.songs.map { it.id })
        assertEquals(2, library.artists.single().albumCount)
        assertEquals(tracks.toSet(), library.artists.single().songs.toSet())
    }

    @Test fun identicalAlbumNamesWithoutMediaIdsStaySeparatedByArtist() = runTest(dispatcher) {
        fun row(id: String, artist: String) = LocalSong(
            id = id, title = id, artist = artist, album = "Recopilación",
            contentUri = "content://media/external/audio/media/$id", artworkUri = null,
            durationMs = 60_000L, filePath = null, trackNumber = null, year = null,
        )
        val first = row("first", "Artista A")
        val second = row("second", "Artista B")
        val model = LibraryViewModel(application, LocalLibrarySource { listOf(first, second) })
        model.loadLibrary()
        runCurrent()

        val library = model.uiState.value.libraryState as LibraryState.Success
        assertEquals(2, library.albums.size)
        assertEquals(2, library.albums.map { it.key }.toSet().size)
        assertEquals(setOf(listOf(first), listOf(second)), library.albums.map { it.songs }.toSet())
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


    @Test fun foregroundCatalogueChangesRefreshAndBackgroundStopsWatching() = runTest(dispatcher) {
        SettingsRepository.setFilterNonMusicAudio(false)
        val events = MutableSharedFlow<Unit>(extraBufferCapacity = 2)
        var calls = 0
        val source = object : LocalLibrarySource {
            override val changes: Flow<Unit> = events.onStart { emit(Unit) }
            override suspend fun songs(filterNonMusic: Boolean): List<LocalSong> {
                assertFalse(filterNonMusic)
                calls++
                return emptyList()
            }
        }
        val model = LibraryViewModel(application, source)
        model.onForeground()
        runCurrent()
        assertEquals(1, calls)
        assertTrue(events.tryEmit(Unit))
        advanceTimeBy(351)
        runCurrent()
        assertEquals(2, calls)
        model.onBackground()
        assertTrue(events.tryEmit(Unit))
        advanceTimeBy(500)
        runCurrent()
        assertEquals("A background screen must not rescan", 2, calls)
        model.onForeground()
        runCurrent()
        assertEquals("The observer must register anew and emit an initial refresh", 3, calls)
        model.onBackground()
    }

    @Test fun mediaStoreUriNotificationsAreObservedWithoutPolling() = runTest(dispatcher) {
        val seen = mutableListOf<Unit>()
        val job = launch { MediaStoreLibrarySource(application).changes.take(2).toList(seen) }
        runCurrent()
        assertEquals("Registration must trigger the first catalogue read", 1, seen.size)
        application.contentResolver.notifyChange(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, null)
        shadowOf(Looper.getMainLooper()).idle()
        runCurrent()
        assertEquals("Android URI notifications must invalidate the catalogue", 2, seen.size)
        job.cancel()
    }

    @Test fun androidFactoryConstructorRemainsAvailable() {
        assertNotNull(LibraryViewModel::class.java.getConstructor(Application::class.java))
    }
}
