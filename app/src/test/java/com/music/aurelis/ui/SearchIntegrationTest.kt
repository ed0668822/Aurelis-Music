// Aurelis © 2026. GPL-3.0-or-later
package com.music.aurelis.ui

import android.Manifest
import androidx.activity.ComponentActivity
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.lifecycle.ViewModelStore
import com.music.aurelis.AurelisApplication
import com.music.aurelis.LibraryViewModel
import com.music.aurelis.data.LibraryState
import com.music.aurelis.data.LocalLibrarySource
import com.music.aurelis.data.model.LocalSong
import com.music.aurelis.playback.PlaybackViewModel
import com.music.aurelis.playback.RecordingPlayer
import com.music.aurelis.ui.navigation.*
import com.music.aurelis.ui.theme.AurelisTheme
import org.junit.After
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

@androidx.media3.common.util.UnstableApi
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33, 35], qualifiers = "en-rUS-w360dp-h800dp-mdpi", application = AurelisApplication::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class SearchIntegrationTest {
    @get:Rule(order = 0) val activity = RobolectricActivityRule()
    @get:Rule(order = 1) val compose = createAndroidComposeRule<ComponentActivity>()
    private val app get() = RuntimeEnvironment.getApplication() as AurelisApplication
    private val models = ViewModelStore()
    private lateinit var navigator: AppNavigator
    private lateinit var player: RecordingPlayer
    private val songs = listOf("Zeta", "Alpha", "Unrelated").mapIndexed { index, title ->
        LocalSong("$index", title, if (index < 2) "Nicolás artista" else "Other artist",
            if (index < 2) "Nicolás álbum" else "Other album", "content://media/$index", null,
            60_000, null, null, null, genre = if (index < 2) "Nicolás género" else null)
    }
    @After fun close() { compose.runOnIdle { models.clear() } }

    private fun start() {
        shadowOf(app).grantPermissions(Manifest.permission.READ_MEDIA_AUDIO)
        app.userLibrary.createPlaylist("Nicolás lista", listOf("0", "1"))
        val library = LibraryViewModel(app, LocalLibrarySource { songs })
        player = RecordingPlayer()
        val playback = PlaybackViewModel(app) { player }
        models.put("library", library); models.put("player", playback)
        compose.setContent { AurelisTheme {
            navigator = rememberAppNavigator()
            AurelisNavigationContent(navigator, playback, library)
        } }
        compose.runOnIdle { library.loadLibrary() }
        compose.waitUntil(5_000) { library.uiState.value.libraryState is LibraryState.Success }
        compose.runOnIdle { navigator.selectTab(AppTab.SEARCH) }
        compose.onNode(hasSetTextAction()).performTextInput("NICOLAS")
    }

    @Test fun accentInsensitiveResultsPlayOnlyTheCurrentSortedMatchingQueue() {
        start()
        compose.onNodeWithText("Unrelated").assertDoesNotExist()
        var replacementsBeforeClick = 0
        compose.runOnIdle { replacementsBeforeClick = player.replacements }
        compose.onNodeWithText("Zeta").performClick()
        compose.runOnIdle {
            assertEquals(listOf("1", "0"), (0 until player.mediaItemCount).map { player.getMediaItemAt(it).mediaId })
            assertEquals("0", player.currentMediaItem?.mediaId)
            assertEquals(1, player.currentMediaItemIndex)
            assertEquals(replacementsBeforeClick + 1, player.replacements)
        }
    }

    @Test fun eachCollectionCategoryOpensItsLocalRouteAndReturnsToTheSameQuery() {
        start()
        val cases = listOf(
            Triple("Artists", "Nicolás artista", LocalCollectionKind.ARTIST),
            Triple("Albums", "Nicolás álbum", LocalCollectionKind.ALBUM),
            Triple("Playlists", "Nicolás lista", LocalCollectionKind.PLAYLIST),
            Triple("Genres", "Nicolás género", LocalCollectionKind.GENRE),
        )
        for ((label, title, kind) in cases) {
            compose.onNode(isSelectable() and hasText(label)).performScrollTo().performClick().assertIsSelected()
            compose.onNodeWithText(title).assertIsDisplayed().performClick()
            compose.runOnIdle { assertEquals(kind, (navigator.visibleRoute() as AppRoute.Collection).kind) }
            compose.onNodeWithText("Zeta").assertIsDisplayed()
            compose.onNodeWithText("Alpha").assertIsDisplayed()
            compose.onNodeWithText("Unrelated").assertDoesNotExist()
            compose.runOnIdle { navigator.back() }
            compose.onNode(hasSetTextAction()).assertTextEquals("NICOLAS")
            compose.onNode(isSelectable() and hasText(label)).assertIsSelected()
        }
    }

    @Test fun unknownGenreIsSearchableUsingItsDisplayedTranslation() {
        start()
        compose.onNode(hasSetTextAction()).performTextReplacement("Unclassified")
        compose.onNode(isSelectable() and hasText("Genres")).performScrollTo().performClick()
        compose.onNodeWithTag("search-destination:GENRES:unknown").performClick()
        compose.runOnIdle { assertEquals(AppRoute.Collection(LocalCollectionKind.GENRE, "unknown"), navigator.visibleRoute()) }
        compose.onNodeWithText("Unrelated").assertIsDisplayed()
        compose.onNodeWithText("Zeta").assertDoesNotExist()
    }
}
