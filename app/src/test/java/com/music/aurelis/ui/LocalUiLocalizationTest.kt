// Aurelis © 2026. GPL-3.0-or-later
package com.music.aurelis.ui

import androidx.activity.ComponentActivity
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.*
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.unit.dp
import com.music.aurelis.AurelisApplication
import com.music.aurelis.R
import com.music.aurelis.data.LibraryState
import com.music.aurelis.data.model.LocalSong
import com.music.aurelis.data.model.SortOption
import com.music.aurelis.ui.screens.LocalMusicContent
import com.music.aurelis.ui.screens.SearchScreen
import com.music.aurelis.ui.screens.LocalSearchCategory
import com.music.aurelis.ui.theme.AurelisTheme
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33], qualifiers = "en-rUS-w360dp-h800dp-mdpi", application = AurelisApplication::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class LocalUiLocalizationTest {
    @get:Rule(order = 0) val registerActivity = RobolectricActivityRule()
    @get:Rule(order = 1) val compose = createAndroidComposeRule<ComponentActivity>()
    private val song = LocalSong("1", "Tema local", "Nicolás", "Disco", "content://media/1", null, 60_000, null, 1, 2026)

    @Test fun englishLocalHeaderUsesOneSongAndBothPillsKeepTheRealCallbacks() {
        val played = mutableListOf<List<LocalSong>>()
        var sorting by mutableStateOf(SortOption.TITLE_ASC)
        compose.setContent { AurelisTheme {
            LocalMusicContent(LibraryState.Success(listOf(song)), listOf(song), emptyList(), null, false,
                sorting, { sorting = it }, {}, {}, {},
                { songs, index -> assertEquals(0, index); played.add(songs) }, {}, {}, {})
        } }
        compose.onNodeWithText("Audio files on device").assertIsDisplayed()
        compose.onNodeWithText("1 song · 1 min").assertIsDisplayed()
        for (label in listOf("Play", "Shuffle")) {
            compose.onNodeWithText(label).performScrollTo().assertHeightIsEqualTo(50.dp).performClick()
        }
        compose.runOnIdle { assertEquals(listOf(listOf(song), listOf(song)), played) }
        compose.onNodeWithContentDescription("Sort").performClick()
        compose.onNodeWithText("Duration (longest)").performClick()
        compose.runOnIdle { assertEquals(SortOption.DURATION_DESC, sorting) }
        compose.onNodeWithTag("local-music-list").performScrollToNode(hasText("Tema local"))
        compose.onNodeWithText("Tema local").assertIsDisplayed()
        compose.onNodeWithText("Nicolás").assertIsDisplayed()
    }

    @Test fun englishSearchKeepsRawQueriesAndHistoryActionsWhileLocalizingItsChrome() {
        var query by mutableStateOf("")
        var history by mutableStateOf(emptyList<String>())
        var category by mutableStateOf(LocalSearchCategory.ALL)
        var cleared = 0
        val term = "Nicolás 100%"
        compose.setContent { AurelisTheme {
            SearchScreen(query = query, onQueryChange = { query = it },
                results = if (query.isEmpty()) null else emptyList(), listState = rememberLazyListState(),
                focusRequested = false, onFocusHandled = {}, onSongClick = { _, _ -> }, onSongLongPress = {},
                history = history, onSubmit = {}, onHistoryClick = { query = it },
                onHistoryRemove = { removed -> history = history.filterNot { it == removed } },
                onHistoryClear = { cleared++; history = emptyList() }, currentSong = null, isPlaying = false,
                contentPadding = PaddingValues(), topPadding = 0.dp, category = category, onCategoryChange = { category = it })
        } }
        compose.onNodeWithText("Search songs, artists, albums…").assertIsDisplayed()
        compose.onNodeWithText("Search your local music").assertIsDisplayed()
        compose.runOnIdle { history = listOf(term) }
        compose.onNodeWithText("Recent searches").assertIsDisplayed()
        compose.onNodeWithText(term).performClick()
        compose.onNode(hasSetTextAction()).assertTextEquals(term)
        compose.onNodeWithText(compose.activity.getString(R.string.search_library_no_results, term)).assertIsDisplayed()
        compose.onNodeWithText("Genres").performScrollTo()
        compose.onNodeWithText("Genres").performClick()
        compose.runOnIdle { assertEquals(LocalSearchCategory.GENRES, category) }
        compose.onNode(hasSetTextAction()).performTextClearance()
        compose.onNodeWithContentDescription("Remove search: $term").performClick()
        compose.onNodeWithText("Search your local music").assertIsDisplayed()
        compose.runOnIdle { history = listOf("Another query") }
        compose.onNodeWithText("Clear").performClick()
        compose.runOnIdle { assertEquals(1, cleared); assertTrue(history.isEmpty()) }
    }
}
