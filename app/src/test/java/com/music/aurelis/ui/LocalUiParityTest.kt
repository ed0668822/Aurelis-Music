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
        val first = compose.onAllNodesWithText("Local Music").onFirst().fetchSemanticsNode().boundsInRoot.top
        compose.onNodeWithTag("local-music-list").performScrollToNode(hasText("Track 30"))
        compose.onNodeWithContentDescription("Opciones de Track 30").assertIsDisplayed().performClick()
        compose.runOnIdle { assertEquals("30", more) }
        assertTrue(first >= 0f)
        compose.onAllNodesWithText("Local Music").assertCountEquals(0)
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
}
