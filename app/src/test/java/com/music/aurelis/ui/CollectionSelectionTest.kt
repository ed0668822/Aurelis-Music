// Aurelis © 2026. GPL-3.0-or-later
package com.music.aurelis.ui

import androidx.activity.ComponentActivity
import androidx.compose.runtime.*
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.StateRestorationTester
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import com.music.aurelis.AurelisApplication
import com.music.aurelis.data.LibraryState
import com.music.aurelis.data.model.LocalSong
import com.music.aurelis.data.model.LocalAlbum
import com.music.aurelis.data.model.LocalArtist
import com.music.aurelis.ui.screens.localArtistAlbums
import com.music.aurelis.ui.screens.resolveLocalArtist
import com.music.aurelis.ui.screens.resolveLocalAlbum
import com.music.aurelis.ui.screens.sortCollectionEntries
import com.music.aurelis.ui.navigation.AppRoute
import com.music.aurelis.ui.navigation.LocalCollectionKind
import com.music.aurelis.ui.screens.LocalCollectionScreen
import com.music.aurelis.ui.theme.AurelisTheme
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33], qualifiers = "w360dp-h800dp-mdpi", application = AurelisApplication::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class CollectionSelectionTest {
    @get:Rule(order = 0) val registerActivity = RobolectricActivityRule()
    @get:Rule(order = 1) val compose = createAndroidComposeRule<ComponentActivity>()
    private val app get() = RuntimeEnvironment.getApplication() as AurelisApplication
    private val rows = listOf("a" to "Árbol", "c" to "Cielo", "z" to "Zorro").map { (id, title) ->
        LocalSong(id, title, "Artista", "Álbum", "content://media/external/audio/media/$id",
            null, 60000, null, 1, 2026)
    }
    private var library: LibraryState by mutableStateOf(LibraryState.Success(rows))
    private var played = emptyList<String>()
    private var startIndex = -1

    private fun start(): StateRestorationTester {
        rows.forEach { if (it.id !in app.userLibrary.state.value.favorites) app.userLibrary.toggleFavorite(it.id) }
        val restoration = StateRestorationTester(compose)
        restoration.setContent { AurelisTheme {
            LocalCollectionScreen(AppRoute.Collection(LocalCollectionKind.FAVORITES), library,
                app.userLibrary, null, false, onBack = {}, onOpen = {},
                onPlay = { songs, index -> played = songs.map { it.id }; startIndex = index })
        } }
        return restoration
    }

    private fun playSelection() {
        // The first action plays the whole collection; the second plays only marked rows.
        val buttons = compose.onAllNodes(hasText("Reproducir") and hasClickAction())
        buttons.assertCountEquals(2)
        buttons[1].performClick()
    }

    @Test fun artistPageIncludesOnlyRelatedLocalAlbumsAndPreservesMediaIds() {
        fun song(id: String, artist: String) = LocalSong(id, "Pista " + id, artist,
            "Disco", "content://media/" + id, null, 60000L, null, 1, 2026)
        val selected = song("10", "Grupo")
        val second = song("11", "Grupo")
        val other = song("12", "Otro")
        val artist = LocalArtist("Grupo", null, 2, 2, listOf(selected, second), "77")
        val albums = listOf(
            LocalAlbum("Disco", "Grupo", null, 1, listOf(selected), "21"),
            LocalAlbum("Disco", "Grupo", null, 1, listOf(second), "22"),
            LocalAlbum("Disco", "Otro", null, 1, listOf(other), "23"),
        )
        assertEquals(listOf("media:21", "media:22"),
            localArtistAlbums(artist, albums).map { it.key })
        assertEquals(artist, resolveLocalArtist(listOf(artist), "media:77"))
        assertNull(resolveLocalArtist(listOf(artist), "media:999"))
    }

    @Test fun artistAlbumCardOpensTheCorrectLocalAlbum() {
        val song = rows.first()
        val artist = LocalArtist("Artista", null, 1, 1, listOf(song), "200")
        val album = LocalAlbum("Álbum", "Artista", null, 1, listOf(song), "300")
        val state = LibraryState.Success(listOf(song),
            albums = listOf(album), artists = listOf(artist))
        var opened: AppRoute.Collection? = null
        compose.setContent { AurelisTheme {
            LocalCollectionScreen(AppRoute.Collection(LocalCollectionKind.ARTIST, artist.key),
                state, app.userLibrary, null, false, onBack = {},
                onOpen = { opened = it }, onPlay = { _, _ -> })
        } }
        compose.onNodeWithTag("collection-tracks").performScrollToNode(
            hasTestTag("artist-album-shelf"))
        compose.onNodeWithTag("artist-album:" + album.key).performClick()
        compose.runOnIdle {
            assertEquals(AppRoute.Collection(LocalCollectionKind.ALBUM, album.key), opened)
        }
    }

    @Test fun sameNameAlbumsResolveByIdentityAndRejectAmbiguousLegacyRoutes() {
        fun album(id: String, artist: String) = LocalAlbum("Mismo nombre", artist,
            null, 1, listOf(LocalSong(id, id, artist, "Mismo nombre",
                "content://media/" + id, null, 60_000L, null, 1, 2026)),
            mediaId = id)
        val first = album("10", "Artista A")
        val second = album("11", "Artista A")
        assertEquals(first, resolveLocalAlbum(listOf(first, second), first.key))
        assertEquals(second, resolveLocalAlbum(listOf(first, second), second.key))
        assertNull(resolveLocalAlbum(listOf(first, second),
            "12:Mismo nombreArtista A"))
        assertNull(resolveLocalAlbum(listOf(first), "media:nonexistent"))
    }

    @Test fun albumHeaderKeepsOriginalTrackNumberWhenSortingChanges() {
        val rows = listOf(
            LocalSong("a", "Alfa", "Artista", "Lanzamiento", "content://media/a",
                null, 60_000L, null, 2, 2024, discNumber = 2),
            LocalSong("b", "Beta", "Artista", "Lanzamiento", "content://media/b",
                null, 60_000L, null, 1, 2024, discNumber = 1),
        )
        val release = LocalAlbum("Lanzamiento", "Artista", null, rows.size, rows, "10")
        val state = LibraryState.Success(rows, albums = listOf(release))
        var played = emptyList<String>()
        compose.setContent { AurelisTheme {
            LocalCollectionScreen(AppRoute.Collection(LocalCollectionKind.ALBUM, release.key), state,
                app.userLibrary, null, false, onBack = {}, onOpen = {},
                onPlay = { songs, _ -> played = songs.map { it.id } })
        } }
        compose.onNodeWithTag("collection-tracks").performScrollToNode(
            hasTestTag("album-track-number:b"))
        compose.onNodeWithTag("album-track-number:b").assertTextEquals("1")
        compose.onNodeWithText("Título Z–A").performClick()
        compose.onNodeWithTag("collection-tracks").performScrollToNode(
            hasTestTag("album-track-number:a"))
        compose.onNodeWithTag("album-track-number:a").assertTextEquals("2.2")
        compose.onNodeWithText("Orden original").assertExists()
    }

    @Test fun collectionDateSortKeepsSourceIndexesAndPlacesUnknownDatesLast() {
        val values = listOf(
            rows[0].copy(dateAddedMs = 100L, dateModifiedMs = 0L),
            rows[1].copy(dateAddedMs = 0L, dateModifiedMs = 400L),
            rows[2].copy(dateAddedMs = 300L, dateModifiedMs = 200L),
        ).mapIndexed { index, song -> IndexedValue(index, song) }
        assertEquals(listOf("z", "a", "c"),
            sortCollectionEntries(values, false, "added").map { it.value.id })
        assertEquals(listOf("c", "z", "a"),
            sortCollectionEntries(values, false, "modified").map { it.value.id })
        assertEquals(listOf(0, 1, 2),
            sortCollectionEntries(values, false, "none").map { it.index })
        assertEquals(listOf("z", "c", "a"),
            sortCollectionEntries(values, true, "none").map { it.value.id })
    }

    @Test fun folderDirectoryViewChoiceSurvivesRecreation() {
        val restoration = StateRestorationTester(compose)
        restoration.setContent { AurelisTheme {
            LocalCollectionScreen(AppRoute.Collection(LocalCollectionKind.FOLDERS),
                LibraryState.Success(rows), app.userLibrary,
                null, false, onBack = {}, onOpen = {}, onPlay = { _, _ -> })
        } }
        compose.onNodeWithTag("collection-view-toggle").performClick()
        compose.onNodeWithText("Mostrar lista").assertExists()
        restoration.emulateSavedInstanceStateRestore()
        compose.onNodeWithText("Mostrar lista").assertExists()
    }

    @Test fun bulkPlaybackAndPlaylistAppendUseVisibleOrderWithoutReorderingSavedTracks() {
        val target = app.userLibrary.createPlaylist("Destino", listOf("c"))
        start()
        compose.onNodeWithText("Título Z–A").performClick()
        compose.onNodeWithText("Seleccionar").performClick()
        compose.onNodeWithContentDescription("Seleccionar Árbol").performClick()
        compose.onNodeWithContentDescription("Seleccionar Zorro").performClick()
        playSelection()
        compose.runOnIdle { assertEquals(listOf("z", "a"), played); assertEquals(0, startIndex) }
        compose.onNodeWithText("Añadir selección a playlist").performClick()
        compose.onNodeWithText("Destino").performClick()
        compose.runOnIdle {
            assertEquals(listOf("c", "z", "a"), app.userLibrary.state.value.playlists.single { it.id == target }.songIds)
            assertEquals(setOf("a", "c", "z"), app.userLibrary.state.value.favorites)
        }
        compose.onNodeWithText("Orden original").performClick()
        playSelection()
        compose.runOnIdle {
            assertEquals(listOf("a", "z"), played)
            assertEquals(listOf("c", "z", "a"), app.userLibrary.state.value.playlists.single { it.id == target }.songIds)
        }
    }

    @Test fun selectionCanClearAndSelectVisibleTracksWithoutEditingTheirPlaylist() {
        val id = app.userLibrary.createPlaylist("Lista para selección", listOf("a", "c", "z"))
        val restoration = start()
        compose.onNodeWithText("Seleccionar").performClick()
        compose.onNodeWithText("Todas").performClick()
        compose.onNodeWithTag("collection-selection-count").assertTextContains("3", substring = true)
        compose.onNodeWithText("Ninguna").performClick()
        compose.onNodeWithTag("collection-selection-count").assertTextContains("0", substring = true)
        compose.runOnIdle { assertEquals(listOf("a", "c", "z"), app.userLibrary.state.value.playlists.single { it.id == id }.songIds) }
        restoration.emulateSavedInstanceStateRestore()
        compose.onNodeWithTag("collection-selection-count").assertTextContains("0", substring = true)
    }

    @Test fun markedIdsAndVisibleSortSurviveRecreationAndCancelClearsThem() {
        val restoration = start()
        compose.onNodeWithText("Título Z–A").performClick()
        compose.onNodeWithText("Seleccionar").performClick()
        compose.onNodeWithContentDescription("Seleccionar Árbol").performClick()
        compose.onNodeWithContentDescription("Seleccionar Zorro").performClick()
        restoration.emulateSavedInstanceStateRestore()
        compose.onNodeWithContentDescription("Seleccionar Árbol").assertIsOn()
        compose.onNodeWithContentDescription("Seleccionar Cielo").assertIsOff()
        compose.onNodeWithContentDescription("Seleccionar Zorro").assertIsOn()
        compose.onNodeWithText("Orden original").assertExists()
        playSelection()
        compose.runOnIdle { assertEquals(listOf("z", "a"), played) }
        compose.onNodeWithText("Cancelar selección").performClick()
        compose.onNodeWithText("Seleccionar").performClick()
        rows.forEach { compose.onNodeWithContentDescription("Seleccionar ${it.title}").assertIsOff() }
    }

    @Test fun restoredSelectionResolvesOnlyAvailableMediaAndDisablesEmptyActions() {
        val restoration = start()
        compose.onNodeWithText("Seleccionar").performClick()
        compose.onNodeWithText("Todas").performClick()
        compose.runOnIdle { library = LibraryState.Success(rows.filter { it.id == "c" }) }
        restoration.emulateSavedInstanceStateRestore()
        playSelection()
        compose.runOnIdle { assertEquals(listOf("c"), played) }
        compose.runOnIdle { library = LibraryState.Success(emptyList()) }
        compose.onAllNodes(hasText("Reproducir") and hasClickAction())[1].assertIsNotEnabled()
        compose.onNodeWithText("Añadir selección a playlist").assertIsNotEnabled()
        compose.runOnIdle { assertEquals(setOf("a", "c", "z"), app.userLibrary.state.value.favorites) }
    }
}
