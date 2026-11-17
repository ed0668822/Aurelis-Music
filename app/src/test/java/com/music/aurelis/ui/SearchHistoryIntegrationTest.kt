// Aurelis © 2026. GPL-3.0-or-later
package com.music.aurelis.ui

import android.Manifest
import androidx.activity.ComponentActivity
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.StateRestorationTester
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
@Config(sdk = [33, 35], qualifiers = "en-rUS-w360dp-h800dp-mdpi", application = AurelisApplication::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class SearchHistoryIntegrationTest {
    @get:Rule(order = 0) val activity = RobolectricActivityRule()
    @get:Rule(order = 1) val compose = createAndroidComposeRule<ComponentActivity>()
    private val app get() = RuntimeEnvironment.getApplication() as AurelisApplication
    private val models = ViewModelStore()
    private lateinit var navigator: AppNavigator
    private lateinit var player: RecordingPlayer
    private lateinit var restore: StateRestorationTester
    private val song = LocalSong("history-track", "Beyoncé Theme", "Beyoncé", "Local Album",
        "content://media/history-track", null, 60_000, null, null, null)
    @After fun close() { compose.runOnIdle { models.clear() } }

    private fun start(history: List<String> = emptyList()) {
        shadowOf(app).grantPermissions(Manifest.permission.READ_MEDIA_AUDIO)
        LocalSearchHistory(app).apply { clear(); history.asReversed().forEach { submit(it) } }
        val library = LibraryViewModel(app, LocalLibrarySource { listOf(song) })
        player = RecordingPlayer()
        val playback = PlaybackViewModel(app) { player }
        models.put("library", library); models.put("player", playback)
        restore = StateRestorationTester(compose)
        restore.setContent { AurelisTheme {
            navigator = rememberAppNavigator()
            AurelisNavigationContent(navigator, playback, library)
        } }
        compose.runOnIdle { library.loadLibrary() }
        compose.waitUntil(5_000) { library.uiState.value.libraryState is LibraryState.Success }
        compose.runOnIdle { navigator.selectTab(AppTab.SEARCH) }
    }

    private fun entries() = LocalSearchHistory(app).entries

    @Test fun typingSubmitReuseRemoveClearAndRestorationKeepTheirDistinctActions() {
        start(listOf("Older query"))
        val query = "Beyoncé 100%"
        compose.onNode(hasSetTextAction()).performTextInput(query)
        compose.runOnIdle { assertEquals(listOf("Older query"), entries()) }
        compose.onNode(hasSetTextAction()).performImeAction()
        compose.runOnIdle { assertEquals(listOf(query, "Older query"), entries()) }
        compose.onNode(hasSetTextAction()).performTextClearance()
        compose.onNodeWithText("Older query").performClick()
        compose.onNode(hasSetTextAction()).assertTextEquals("Older query")
        compose.runOnIdle {
            assertEquals(listOf(query, "Older query"), entries())
            assertEquals(0, player.mediaItemCount)
        }
        compose.onNode(hasSetTextAction()).performTextClearance()
        compose.onNodeWithContentDescription(compose.activity.getString(R.string.local_search_remove_history, query)).performClick()
        compose.onNode(hasSetTextAction()).assertTextEquals("")
        compose.runOnIdle { assertEquals(listOf("Older query"), entries()) }
        restore.emulateSavedInstanceStateRestore()
        compose.onNodeWithText("Older query").assertIsDisplayed()
        compose.onNodeWithText(query).assertDoesNotExist()
        compose.onNodeWithText(compose.activity.getString(R.string.local_search_clear_history)).performClick()
        compose.runOnIdle { assertTrue(entries().isEmpty()) }
        compose.onNodeWithText(compose.activity.getString(R.string.local_search_start)).assertIsDisplayed()
    }

    @Test fun literalHeaderQueryCanRenderAndBeRemovedWithoutSelectingIt() {
        start(listOf("header", "Another query"))
        compose.onNodeWithText(compose.activity.getString(R.string.local_search_recent)).assertIsDisplayed()
        compose.onNodeWithText("header").assertIsDisplayed()
        compose.onNodeWithContentDescription(compose.activity.getString(R.string.local_search_remove_history, "header")).performClick()
        compose.onNode(hasSetTextAction()).assertTextEquals("")
        compose.onNodeWithText("header").assertDoesNotExist()
        compose.onNodeWithText("Another query").assertIsDisplayed()
        compose.runOnIdle { assertEquals(listOf("Another query"), entries()); assertEquals(0, player.mediaItemCount) }
    }

    @Test fun playingAndOpeningAnAlbumRecordQueriesWithoutAddingHistoryPlayback() {
        start()
        compose.onNode(hasSetTextAction()).performTextInput("beyonce")
        compose.onNodeWithText(song.title).performClick()
        compose.runOnIdle {
            assertEquals(listOf("beyonce"), entries())
            assertEquals(song.id, player.currentMediaItem?.mediaId)
        }
        compose.onNode(hasSetTextAction()).performTextReplacement("Local Album")
        compose.onNodeWithTag("local-search-results").performScrollToNode(hasText("Local Album"))
        compose.onNode(hasText("Local Album") and hasAnyAncestor(hasTestTag("local-search-results"))).performClick()
        compose.runOnIdle {
            assertEquals(LocalCollectionKind.ALBUM, (navigator.visibleRoute() as AppRoute.Collection).kind)
            assertEquals(listOf("Local Album", "beyonce"), entries())
            navigator.back()
        }
        compose.onNode(hasSetTextAction()).performTextClearance()
        var previousReplacements = 0
        compose.runOnIdle { previousReplacements = player.replacements }
        compose.onNodeWithText("beyonce").performClick()
        compose.onNode(hasSetTextAction()).assertTextEquals("beyonce")
        compose.runOnIdle {
            assertEquals(previousReplacements, player.replacements)
            assertEquals(listOf("Local Album", "beyonce"), entries())
        }
    }
}
