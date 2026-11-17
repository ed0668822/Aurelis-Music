// Aurelis © 2026. GPL-3.0-or-later
package com.music.aurelis.ui

import androidx.activity.ComponentActivity
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.*
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import com.music.aurelis.AurelisApplication
import com.music.aurelis.R
import com.music.aurelis.data.model.LocalSong
import com.music.aurelis.ui.screens.*
import com.music.aurelis.ui.theme.AurelisTheme
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33, 35], qualifiers = "en-rUS-w360dp-h900dp-mdpi", application = AurelisApplication::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class SearchHighlightsTest {
    @get:Rule(order = 0) val activity = RobolectricActivityRule()
    @get:Rule(order = 1) val compose = createAndroidComposeRule<ComponentActivity>()
    private val songs = (1..2).map { LocalSong("$it", "Track $it", "Local artist", null,
        "content://media/$it", null, 60_000, null, null, null) }

    @Test fun featuredSongOccursOnceAndUsesItsOriginalQueueIndexAndActions() {
        var category by mutableStateOf(LocalSearchCategory.ALL)
        var played: Pair<List<LocalSong>, Int>? = null
        var playlist: LocalSong? = null
        var actions: LocalSong? = null
        compose.setContent { AurelisTheme {
            SearchScreen("Track", {}, songs, rememberLazyListState(), false, {},
                { queue, index -> played = queue to index }, { actions = it }, emptyList(), {}, {}, {}, {}, null, false,
                contentPadding = PaddingValues(), topPadding = 0.dp, category = category, onCategoryChange = { category = it },
                topResult = songs[1], onTopResultPlaylist = { playlist = it })
        } }
        compose.onAllNodesWithText("Track 2").assertCountEquals(1)
        compose.onNodeWithTag("search-top-art", useUnmergedTree = true).assertWidthIsEqualTo(72.dp).assertHeightIsEqualTo(72.dp)
        compose.onNodeWithTag("search-top-play").performClick()
        compose.runOnIdle { assertEquals(songs to 1, played) }
        compose.onNodeWithTag("search-top-playlist").performClick()
        compose.onNodeWithTag("search-top-actions").performClick()
        compose.runOnIdle { assertEquals(songs[1], playlist); assertEquals(songs[1], actions) }
        compose.onNode(isSelectable() and hasText("Songs")).performScrollTo().performClick()
        compose.onNodeWithTag("search-top-result:2").assertDoesNotExist()
        compose.onNodeWithText("Track 2").assertIsDisplayed()
        compose.onNodeWithText("Track 1").performClick()
        compose.runOnIdle { assertEquals(songs to 0, played) }
    }

    @Test fun suggestionArrowEditsWhileTextCommitsAndOnlyThreeAlternativesRender() {
        var query by mutableStateOf("query")
        var suggestions by mutableStateOf(listOf("query", "One", "Two", "Three", "Four"))
        var searched = ""
        var filled = ""
        compose.setContent { AurelisTheme {
            SearchScreen(query, { query = it }, emptyList(), rememberLazyListState(), false, {}, { _, _ -> }, {},
                emptyList(), {}, {}, {}, {}, null, false, contentPadding = PaddingValues(), topPadding = 0.dp,
                suggestions = suggestions,
                onSuggestionFill = { filled = it; query = it },
                onSuggestionSearch = { searched = it; query = it; suggestions = emptyList() })
        } }
        compose.onNodeWithTag("search-suggestion:Four").assertDoesNotExist()
        compose.onNodeWithContentDescription(compose.activity.getString(R.string.local_search_edit_suggestion, "One")).performClick()
        compose.onNode(hasSetTextAction()).assertTextEquals("One").assertIsFocused()
        compose.runOnIdle { assertEquals("One", filled); assertEquals("", searched) }
        compose.onNodeWithTag("search-suggestion:Two").performClick()
        compose.onNode(hasSetTextAction()).assertTextEquals("Two").assertIsNotFocused()
        compose.onNodeWithTag("search-suggestion:One").assertDoesNotExist()
        compose.runOnIdle { assertEquals("Two", searched) }
    }

    @Test @Config(sdk = [33, 35], qualifiers = "es-rES-w320dp-h1000dp-mdpi")
    fun narrowSpanishWindowWithLargeTextKeepsFeaturedActionsInsideThePage() {
        compose.setContent { AurelisTheme {
            val density = LocalDensity.current
            CompositionLocalProvider(LocalDensity provides Density(density.density, 1.5f)) {
                SearchScreen("Track", {}, songs, rememberLazyListState(), false, {}, { _, _ -> }, {},
                    emptyList(), {}, {}, {}, {}, null, false, contentPadding = PaddingValues(), topPadding = 0.dp,
                    topResult = songs[0])
            }
        } }
        val card = compose.onNodeWithTag("search-top-result:1").fetchSemanticsNode().boundsInRoot
        for (tag in listOf("search-top-play", "search-top-playlist", "search-top-actions")) {
            val bounds = compose.onNodeWithTag(tag).assertIsDisplayed().fetchSemanticsNode().boundsInRoot
            assertTrue("$tag must remain within the card", bounds.left >= card.left && bounds.right <= card.right)
        }
    }

    @Test fun submittingFromTheEndOfTheListReturnsToTheFeaturedResultWithoutPlaying() {
        val many = (1..20).map { songs[0].copy(id = "$it", title = "Track $it") }
        var submitted = 0
        var played = 0
        compose.setContent { AurelisTheme {
            SearchScreen("Track", {}, many, rememberLazyListState(), false, {}, { _, _ -> played++ }, {},
                emptyList(), { submitted++ }, {}, {}, {}, null, false,
                contentPadding = PaddingValues(), topPadding = 0.dp, topResult = many[0])
        } }
        compose.onNodeWithTag("local-search-results").performScrollToNode(hasText("Track 20"))
        compose.onNodeWithText("Track 20").assertIsDisplayed()
        compose.onNode(hasSetTextAction()).performClick().performImeAction()
        compose.onNodeWithTag("search-top-result:1").assertIsDisplayed()
        compose.runOnIdle { assertEquals(1, submitted); assertEquals(0, played) }
    }
}
