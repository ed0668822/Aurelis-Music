// Aurelis © 2026. GPL-3.0-or-later
package com.music.aurelis.ui

import android.Manifest
import androidx.activity.ComponentActivity
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.lifecycle.ViewModelStore
import com.music.aurelis.AurelisApplication
import com.music.aurelis.AurelisNavigationContent
import com.music.aurelis.LibraryViewModel
import com.music.aurelis.R
import com.music.aurelis.data.LibraryState
import com.music.aurelis.data.LocalLibrarySource
import com.music.aurelis.data.LocalSearchHistory
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
@Config(sdk = [33, 35], qualifiers = "en-rUS-w360dp-h900dp-mdpi", application = AurelisApplication::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class SearchHighlightsIntegrationTest {
    @get:Rule(order = 0) val activity = RobolectricActivityRule()
    @get:Rule(order = 1) val compose = createAndroidComposeRule<ComponentActivity>()
    private val app get() = RuntimeEnvironment.getApplication() as AurelisApplication
    private val models = ViewModelStore()
    private lateinit var navigator: AppNavigator
    private lateinit var player: RecordingPlayer
    private val songs = listOf("Alpha live", "Alpha remix", "Beta").mapIndexed { index, title ->
        LocalSong("$index", title, "Local artist", "Local album", "content://media/$index", null,
            60_000, null, null, null)
    }
    @After fun close() { compose.runOnIdle { models.clear() } }
    private fun start() {
        shadowOf(app).grantPermissions(Manifest.permission.READ_MEDIA_AUDIO)
        LocalSearchHistory(app).clear()
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
    }

    @Test fun fillAndSearchAreDistinctAndFeaturedPlaybackUsesTheLocalResultQueue() {
        start()
        compose.onNode(hasSetTextAction()).performTextInput("Alpha")
        compose.onNodeWithContentDescription(compose.activity.getString(R.string.local_search_edit_suggestion, "Alpha live")).performClick()
        compose.onNode(hasSetTextAction()).assertTextEquals("Alpha live").assertIsFocused()
        compose.runOnIdle { assertTrue(LocalSearchHistory(app).entries.isEmpty()); assertEquals(0, player.mediaItemCount) }
        compose.onNode(hasSetTextAction()).performTextReplacement("Alpha")
        compose.onNodeWithTag("search-suggestion:Alpha remix").performClick()
        compose.onNodeWithTag("search-top-result:1").assertIsDisplayed()
        compose.onNodeWithTag("search-suggestion:Alpha live").assertDoesNotExist()
        compose.runOnIdle { assertEquals(listOf("Alpha remix"), LocalSearchHistory(app).entries); assertEquals(0, player.mediaItemCount) }
        compose.onNode(hasSetTextAction()).performTextReplacement("Alpha")
        compose.onNode(hasSetTextAction()).performImeAction()
        compose.onNodeWithTag("search-top-play").performClick()
        compose.runOnIdle {
            assertEquals(listOf("0", "1"), (0 until player.mediaItemCount).map { player.getMediaItemAt(it).mediaId })
            assertEquals("0", player.currentMediaItem?.mediaId)
        }
    }

    @Test fun featuredPlaylistActionWritesTheExistingStoreWithoutStartingPlayback() {
        start()
        var id = ""
        compose.runOnIdle { id = app.userLibrary.createPlaylist("Destino") }
        compose.onNode(hasSetTextAction()).performTextInput("Alpha live")
        compose.onNodeWithTag("search-top-playlist").performClick()
        compose.onNodeWithText("Añadir a Destino").performClick()
        compose.runOnIdle {
            assertEquals(listOf("0"), app.userLibrary.state.value.playlists.single { it.id == id }.songIds)
            assertEquals(listOf("Alpha live"), LocalSearchHistory(app).entries)
            assertEquals(0, player.mediaItemCount)
        }
    }

    @Test fun editingAgainAndChangingCategoryCannotLeaveAnOldFeaturedSongVisible() {
        start()
        compose.onNode(hasSetTextAction()).performTextInput("Alpha")
        compose.onNode(hasSetTextAction()).performImeAction()
        compose.onNodeWithTag("search-top-result:0").assertIsDisplayed()
        compose.onNode(hasSetTextAction()).performTextReplacement("Beta")
        compose.onNodeWithTag("search-top-result:0").assertDoesNotExist()
        compose.onNodeWithTag("search-top-result:2").assertIsDisplayed()
        compose.onNode(isSelectable() and hasText("Songs")).performScrollTo().performClick()
        compose.onNodeWithTag("search-top-result:2").assertDoesNotExist()
        compose.onNodeWithTag("search-song:2:0").assertIsDisplayed()
        compose.onNode(hasSetTextAction()).performTextReplacement("Missing completely")
        compose.onNodeWithTag("search-top-result:2").assertDoesNotExist()
        compose.onNodeWithTag("local-search-categories").assertIsDisplayed()
        compose.onNodeWithText(compose.activity.getString(R.string.search_library_no_results, "Missing completely")).assertIsDisplayed()
        compose.onNode(hasSetTextAction()).performTextClearance()
        compose.onNodeWithText("Alpha").assertIsDisplayed() // saved history is restored
    }
}
