// Aurelis © 2026. GPL-3.0-or-later
package com.music.aurelis.ui

import androidx.activity.ComponentActivity
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.*
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Density
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.unit.dp
import com.music.aurelis.AurelisApplication
import com.music.aurelis.R
import com.music.aurelis.data.model.LocalSong
import com.music.aurelis.data.model.SortOption
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
@Config(sdk = [33, 35], qualifiers = "en-rUS-w360dp-h800dp-mdpi", application = AurelisApplication::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class SearchPresentationTest {
    @get:Rule(order = 0) val activity = RobolectricActivityRule()
    @get:Rule(order = 1) val compose = createAndroidComposeRule<ComponentActivity>()
    private val songs = (1..2).map { LocalSong("$it", "Match $it", "Local artist", "Local album",
        "content://media/$it", null, 60_000, null, null, null) }

    @Test fun f014KeyboardSearchSubmitsAndKeepsTypedQuery() {
        var query by mutableStateOf("")
        var submitted = 0
        compose.setContent { AurelisTheme {
            SearchScreen(query, { query = it }, emptyList(), rememberLazyListState(),
                false, {}, { _, _ -> }, {}, emptyList(), { submitted++ }, {}, {}, {},
                null, false, contentPadding = PaddingValues(), topPadding = 0.dp)
        } }
        val field = compose.onNode(hasSetTextAction())
        field.performTextInput("Local tracks")
        field.performImeAction()
        compose.runOnIdle {
            assertEquals("Local tracks", query)
            assertEquals(1, submitted)
        }
    }

    @Test fun f014LargeFontScaleKeepsSearchFieldAndClearControlAccessible() {
        var query by mutableStateOf("Music")
        compose.setContent {
            CompositionLocalProvider(LocalDensity provides Density(1f, 2f)) {
                AurelisTheme {
                    SearchScreen(query, { query = it }, emptyList(), rememberLazyListState(),
                        false, {}, { _, _ -> }, {}, emptyList(), {}, {}, {}, {},
                        null, false, contentPadding = PaddingValues(), topPadding = 0.dp)
                }
            }
        }
        compose.onNode(hasSetTextAction()).assertExists()
        compose.onNodeWithContentDescription(compose.activity.getString(R.string.clear_search)).assertExists()
    }

    @Test fun emptyCategoryKeepsTheSelectorAndReturnsToActualSongs() {
        var category by mutableStateOf(LocalSearchCategory.ALL)
        compose.setContent { AurelisTheme {
            SearchScreen("Match", {}, songs, rememberLazyListState(), false, {}, { _, _ -> }, {},
                emptyList(), {}, {}, {}, {}, null, false, category = category,
                onCategoryChange = { category = it }, contentPadding = PaddingValues(), topPadding = 0.dp)
        } }
        compose.onNode(isSelectable() and hasText("Genres")).performScrollTo().performClick().assertIsSelected()
        compose.onNodeWithText(compose.activity.getString(R.string.search_library_no_results, "Match")).assertIsDisplayed()
        compose.onNodeWithText("Match 1").assertDoesNotExist()
        compose.onNodeWithTag("local-search-order").assertDoesNotExist()
        compose.onNode(isSelectable() and hasText("All")).performScrollTo().performClick().assertIsSelected()
        compose.onNodeWithText("Match 1").assertIsDisplayed()
    }

    @Test fun allResultsFollowOriginalSectionOrderAndSongActionsKeepTheResultQueue() {
        var played: Pair<List<LocalSong>, Int>? = null
        var held: LocalSong? = null
        var opened = ""
        val destinations = listOf(LocalSearchCategory.ALBUMS, LocalSearchCategory.ARTISTS,
            LocalSearchCategory.PLAYLISTS, LocalSearchCategory.GENRES).map { category ->
            // Identical IDs across different collection kinds must not collide in LazyColumn.
            LocalSearchDestination("Result ${category.name}", "Local collection", category, "same") { opened = category.name }
        }
        compose.setContent { AurelisTheme {
            SearchScreen("Match", {}, songs, rememberLazyListState(), false, {},
                { list, index -> played = list to index }, { held = it }, emptyList(), {}, {}, {}, {}, songs[1], true,
                destinations = destinations, contentPadding = PaddingValues(bottom = 80.dp), topPadding = 0.dp)
        } }
        compose.onNodeWithText("Match 2").performClick()
        compose.runOnIdle { assertEquals(songs to 1, played) }
        compose.onNodeWithText("Match 1").performTouchInput { longClick() }
        compose.runOnIdle { assertEquals(songs[0], held) }
        val list = compose.onNodeWithTag("local-search-results")
        for (category in listOf(LocalSearchCategory.ARTISTS, LocalSearchCategory.ALBUMS,
            LocalSearchCategory.PLAYLISTS, LocalSearchCategory.GENRES)) {
            list.performScrollToNode(hasTestTag("search-section:${category.name}"))
            compose.onNodeWithTag("search-section:${category.name}").assertIsDisplayed()
            list.performScrollToNode(hasTestTag("search-destination:$category:same"))
            compose.onNodeWithTag("search-destination:$category:same").performClick()
            compose.runOnIdle { assertEquals(category.name, opened) }
        }
    }

    @Test fun singleCategoryHasNoRedundantHeadingAndSortUsesTheExistingCallback() {
        var category by mutableStateOf(LocalSearchCategory.ARTISTS)
        var sorting by mutableStateOf(SortOption.DURATION_DESC)
        val row = LocalSearchDestination("Artist hit", "Artist", LocalSearchCategory.ARTISTS, "artist") {}
        compose.setContent { AurelisTheme {
            SearchScreen("hit", {}, songs, rememberLazyListState(), false, {}, { _, _ -> }, {},
                emptyList(), {}, {}, {}, {}, null, false, filter = sorting, onFilterChange = { sorting = it },
                destinations = listOf(row), category = category, onCategoryChange = { category = it },
                contentPadding = PaddingValues(), topPadding = 0.dp)
        } }
        compose.onNodeWithText("Artist hit").assertIsDisplayed()
        compose.onNodeWithText("Match 1").assertDoesNotExist()
        compose.onNodeWithTag("search-section:ARTISTS").assertDoesNotExist()
        compose.onNode(isSelectable() and hasText("Songs")).performScrollTo().performClick()
        compose.onNodeWithTag("local-search-order").assertIsDisplayed()
        compose.onNode(hasText("Artist") and isSelectable()).performClick().assertIsSelected()
        compose.runOnIdle { assertEquals(SortOption.ARTIST_ASC, sorting) }
        compose.onNodeWithText("Artist hit").assertDoesNotExist()
        compose.onNodeWithText("Match 1").assertIsDisplayed()
    }
}
