// Aurelis © 2026. GPL-3.0-or-later
package com.music.aurelis.ui

import androidx.activity.ComponentActivity
import androidx.compose.foundation.layout.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.junit4.StateRestorationTester
import androidx.compose.ui.unit.dp
import com.music.aurelis.AurelisApplication
import com.music.aurelis.data.*
import com.music.aurelis.data.model.*
import com.music.aurelis.ui.screens.LocalMusicContent
import com.music.aurelis.ui.screens.SettingsScreen
import com.music.aurelis.ui.navigation.*
import com.music.aurelis.ui.theme.AurelisTheme
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33], qualifiers = "es-rES-w360dp-h800dp-mdpi", application = AurelisApplication::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class LocalUiParityTest {
    @get:Rule(order = 0) val registerActivity = RobolectricActivityRule()
    @get:Rule(order = 1) val compose = createAndroidComposeRule<ComponentActivity>()
    private val songs = (1..30).map { LocalSong("$it", "Track $it", "Artist", "Album", "content://media/$it",
        null, 60_000L, null, it, null, volumeName = "external_primary", relativePath = "Music/Test/") }

    @Test fun listSearchKeepsTheEditorAndEmptyArtistAlbumResultsVisible() {
        val state = LibraryState.Success(songs,
            albums = listOf(LocalAlbum("Album", "Artist", null, songs.size, songs)),
            artists = listOf(LocalArtist("Artist", null, songs.size, 1, songs)))
        compose.setContent { AurelisTheme { Box(Modifier.size(360.dp, 640.dp)) {
            LocalMusicContent(state, songs, emptyList(), null, false, SortOption.TITLE_ASC, {},
                {}, {}, {}, { _, _ -> }, {}, {}, {})
        } } }
        compose.onNodeWithContentDescription("Buscar en esta lista").performClick()
        compose.onNode(hasSetTextAction()).assertIsDisplayed().performTextInput("not-in-library")
        compose.onNode(isSelectable() and hasText("Artistas")).performScrollTo().performClick()
        compose.onNodeWithText("Sin resultados").assertIsDisplayed()
        compose.onNode(isSelectable() and hasText("Álbumes")).performScrollTo().performClick()
        compose.onNodeWithText("Sin resultados").assertIsDisplayed()
    }

    @Test fun gridPreservesRealSongActionsAndAlbumRoutes() {
        var played: List<String> = emptyList()
        var actions: String? = null
        var opened: AppRoute.Collection? = null
        val state = LibraryState.Success(songs,
            albums = listOf(LocalAlbum("Album", "Artist", null, songs.size, songs)),
            artists = listOf(LocalArtist("Artist", null, songs.size, 1, songs)))
        compose.setContent { AurelisTheme { Box(Modifier.size(360.dp, 640.dp)) {
            LocalMusicContent(state, songs, emptyList(), null, false, SortOption.TITLE_ASC, {},
                {}, {}, {}, { tracks, _ -> played = tracks.map { it.id } },
                { opened = it }, { actions = it.id }, {})
        } } }
        compose.onNodeWithTag("local-music-view-toggle").performClick()
        compose.onNodeWithTag("local-music-card:song:1").performClick()
        compose.runOnIdle { assertEquals(songs.map { it.id }, played) }
        compose.onNodeWithContentDescription("Opciones de Track 1").performClick()
        compose.runOnIdle { assertEquals("1", actions) }
        compose.onNode(isSelectable() and hasText("Álbumes")).performScrollTo().performClick()
        compose.onNodeWithTag("local-music-card:album:5:AlbumArtist").performClick()
        compose.runOnIdle {
            assertEquals(AppRoute.Collection(LocalCollectionKind.ALBUM, state.albums.single().key), opened)
        }
        compose.onNodeWithTag("local-music-view-toggle").performClick()
        compose.onNodeWithTag("local-music-card:album:5:AlbumArtist").assertDoesNotExist()
    }

    @Test fun restoringTheLocalListDoesNotResetItsSavedScrollToTheHeader() {
        val restore = StateRestorationTester(compose)
        restore.setContent { AurelisTheme {
            LocalMusicContent(LibraryState.Success(songs), songs, emptyList(), null, false, SortOption.TITLE_ASC, {},
                {}, {}, {}, { _, _ -> }, {}, {}, {})
        } }
        compose.onNodeWithTag("local-music-list").performScrollToNode(hasText("Track 30"))
        restore.emulateSavedInstanceStateRestore()
        compose.onNodeWithText("Track 30").assertIsDisplayed()
    }

    @Test fun headerScrollsAwayAndLastTrackActionsRemainReachableInAShortWindow() {
        var more: String? = null
        compose.setContent { AurelisTheme { Box(Modifier.size(640.dp, 360.dp)) {
            LocalMusicContent(LibraryState.Success(songs), songs, emptyList(), null, false, SortOption.TITLE_ASC, {},
                {}, {}, {}, { _, _ -> }, {}, { more = it.id }, {})
        } } }
        val first = compose.onAllNodesWithText(compose.activity.getString(com.music.aurelis.R.string.local_music)).onFirst().fetchSemanticsNode().boundsInRoot.top
        compose.onNodeWithTag("local-music-list").performScrollToNode(hasText("Track 30"))
        compose.onNodeWithContentDescription("Opciones de Track 30").assertIsDisplayed().performClick()
        compose.runOnIdle { assertEquals("30", more) }
        assertTrue(first >= 0f)
        compose.onAllNodesWithText(compose.activity.getString(com.music.aurelis.R.string.local_music)).assertCountEquals(0)
    }

    @Test fun folderTabOpensTheSameLocalCollectionAndDoesNotStartAnotherPlayer() {
        var route: AppRoute.Collection? = null
        var plays = 0
        compose.setContent { AurelisTheme {
            LocalMusicContent(LibraryState.Success(songs), songs, emptyList(), null, false, SortOption.TITLE_ASC, {},
                {}, {}, {}, { _, _ -> plays++ }, { route = it }, {}, {})
        } }
        compose.onNodeWithTag("local-music-list").performScrollToNode(hasText("Carpetas"))
        compose.onNodeWithText("Carpetas").performScrollTo().performClick()
        compose.onNodeWithText("Music/Test").performScrollTo().performClick()
        compose.runOnIdle {
            assertEquals(AppRoute.Collection(LocalCollectionKind.FOLDER, LocalFolderRepository.groups(songs).single().key), route)
            assertEquals(0, plays)
        }
    }

    @Test fun settingsSearchFindsExistingControlsAndCategoryBackReturnsToTheRoot() {
        compose.setContent { AurelisTheme { SettingsScreen(true, {}) } }
        compose.onNode(hasSetTextAction()).performTextInput("LRCLIB")
        compose.onNodeWithContentDescription("Buscar letras en LRCLIB").performScrollTo().assertExists()
        compose.onNode(hasSetTextAction()).performTextReplacement("")
        compose.onNodeWithText("Apariencia").performScrollTo().performClick()
        compose.onNodeWithContentDescription("Liquid Glass").performScrollTo().assertExists()
        compose.onNodeWithContentDescription("Atrás").performClick()
        compose.onNode(hasSetTextAction()).assertExists()
    }
    @Test fun libraryLocalLinksOpenRealDestinationsWithoutExtraSettingsAccess() {
        var opened = ""
        compose.setContent { AurelisTheme {
            com.music.aurelis.ui.screens.LibraryScreen(
                listState = androidx.compose.foundation.lazy.rememberLazyListState(),
                links = listOf(
                    com.music.aurelis.ui.screens.LibraryLink("Música local", com.music.aurelis.ui.icons.AurelisIcons.Folder) { opened = "local" },
                    com.music.aurelis.ui.screens.LibraryLink("Favoritos", com.music.aurelis.ui.icons.AurelisIcons.Heart) { opened = "favorites" },
                ), onShowAll = {}, contentPadding = PaddingValues(),
                onAlbumOpen = {}, onPlaylistOpen = {}, onNewPlaylist = {},
            )
        } }
        compose.onNodeWithText("Favoritos").performClick()
        compose.runOnIdle { assertEquals("favorites", opened) }
        compose.onNodeWithText("Música local").performClick()
        compose.runOnIdle { assertEquals("local", opened) }
        compose.onNodeWithText("Ajustes").assertDoesNotExist()
    }

    @Test fun libraryPreviewsCapTheirCardsAndKeepCreationAndOpenActionsConnected() {
        val albums = (1..6).map { LocalAlbum("Album $it", "Artist", null, 1, songs.take(1), "album$it") }
        val playlists = (1..6).map { LocalPlaylist("p$it", "Playlist $it", emptyList()) }
        var opened = ""
        var created = false
        compose.setContent { AurelisTheme { Box(Modifier.size(360.dp, 640.dp)) {
            com.music.aurelis.ui.screens.LibraryScreen(
                listState = androidx.compose.foundation.lazy.rememberLazyListState(),
                links = emptyList(), onShowAll = { opened = it }, contentPadding = PaddingValues(),
                albums = albums, playlists = playlists, songs = songs,
                onAlbumOpen = { opened = it.key }, onPlaylistOpen = { opened = it },
                onNewPlaylist = { created = true })
        } } }
        compose.onNodeWithText("Album 1").performClick()
        compose.runOnIdle { assertEquals("media:album1", opened) }
        compose.onNodeWithTag("library-show-all-albums").assertExists()
        compose.onNodeWithTag("library-row-albums").performScrollToIndex(4)
        compose.onNodeWithText("Album 5").assertIsDisplayed()
        compose.onNodeWithText("Album 6").assertDoesNotExist()
        compose.onNodeWithTag("library-page").performScrollToNode(hasTestTag("library-shelf-playlists"))
        compose.onNodeWithText("Nueva lista").performClick()
        compose.runOnIdle { assertTrue(created) }
        compose.onNodeWithText("Playlist 1").performClick()
        compose.runOnIdle { assertEquals("p1", opened) }
        compose.onNodeWithTag("library-row-playlists").performScrollToIndex(4)
        compose.onNodeWithText("Playlist 4").assertIsDisplayed()
        compose.onNodeWithText("Playlist 5").assertDoesNotExist()
        compose.onNode(hasText("Ver todo") and hasAnyAncestor(hasTestTag("library-shelf-playlists"))).performClick()
        compose.runOnIdle { assertEquals("playlists", opened) }
    }

}
