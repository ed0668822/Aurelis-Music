// Aurelis © 2026. GPL-3.0-or-later
package com.music.aurelis.ui.navigation

import androidx.activity.ComponentActivity
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.unit.dp
import com.music.aurelis.AurelisApplication
import com.music.aurelis.R
import com.music.aurelis.ui.RobolectricActivityRule
import com.music.aurelis.ui.components.FrostedTopBar
import com.music.aurelis.ui.components.LocalProfileButton
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
class ChromeContractTest {
    @get:Rule(order = 0) val registerActivity = RobolectricActivityRule()
    @get:Rule(order = 1) val compose = createAndroidComposeRule<ComponentActivity>()

    @Test fun pushedPagesReserveChromeAndPlayerHidesItsAccessibilityActions() {
        lateinit var nav: AppNavigator
        compose.setContent { AurelisTheme {
            nav = rememberAppNavigator()
            AppNavigationHost(nav,
                modifier = Modifier.size(360.dp, 640.dp), pageBottomPadding = 100.dp,
                tabContent = { Text("Tab") },
                routeContent = { Box(Modifier.fillMaxSize().testTag("page")) },
                bottomBar = {
                    TextButton(onClick = { nav.selectTab(AppTab.HOME) },
                        modifier = Modifier.align(Alignment.BottomCenter).height(80.dp).testTag("chrome")) {
                        Text("Global controls")
                    }
                },
                playbackOverlays = {
                    if (nav.visibleRoute() == AppRoute.NowPlaying) Text("Player overlay")
                })
        } }
        for (page in listOf(AppRoute.LocalMusic, AppRoute.Settings, AppRoute.Equalizer,
            AppRoute.Collection(LocalCollectionKind.FAVORITES), AppRoute.Replay)) {
            compose.runOnIdle { nav.reset(page) }
            val content = compose.onNodeWithTag("page").fetchSemanticsNode().boundsInRoot
            val controls = compose.onNodeWithTag("chrome").fetchSemanticsNode().boundsInRoot
            assertTrue("Route content must not be trapped beneath chrome", content.bottom <= controls.top)
        }
        val chromeCenter = compose.onNodeWithTag("chrome").fetchSemanticsNode().boundsInRoot.center
        compose.runOnIdle { nav.push(AppRoute.NowPlaying) }
        compose.onNodeWithText("Global controls").assertDoesNotExist()
        compose.onNodeWithText("Player overlay").assertIsDisplayed()
        compose.onRoot().performTouchInput { click(chromeCenter) }
        compose.runOnIdle { assertEquals(AppRoute.NowPlaying, nav.visibleRoute()) }
        compose.runOnIdle { nav.back() }
        compose.onNodeWithTag("chrome").performClick()
        compose.runOnIdle { assertFalse(nav.canGoBack); assertEquals(AppTab.HOME, nav.selectedTab) }
    }

    @Test fun sharedHeaderActuallyInvokesBackProfileAndAccessoryActions() {
        var backs = 0
        var profiles = 0
        var accessory by mutableStateOf(false)
        compose.setContent { AurelisTheme {
            val accessoryContent: (@Composable () -> Unit)? = if (accessory) { { Text("Search accessory") } } else null
            FrostedTopBar(title = "Heading", scrolled = { true },
                onBack = if (accessory) null else { { backs++ } },
                actions = { LocalProfileButton { profiles++ } },
                accessory = accessoryContent)
        } }
        compose.onNodeWithContentDescription("Atrás").performClick()
        compose.onNodeWithContentDescription("Perfil").performClick()
        compose.runOnIdle { assertEquals(1, backs); assertEquals(1, profiles); accessory = true }
        compose.onNodeWithText("Search accessory").assertIsDisplayed()
        compose.onNodeWithContentDescription("Perfil").performClick()
        compose.runOnIdle { assertEquals(2, profiles) }
    }

    @Test fun settingsGlassHeaderIsOutsideItsRecordedBodyAndHostDoesNotRecordItAgain() {
        compose.setContent { AurelisTheme {
            val nav = rememberAppNavigator()
            SideEffect { nav.push(AppRoute.Settings) }
            AppNavigationHost(nav,
                pageModifier = Modifier.testTag("host-backdrop"),
                pageHandlesBackdrop = { it == AppRoute.Settings },
                tabContent = { Text("Root") },
                routeContent = { com.music.aurelis.ui.screens.SettingsScreen(true, { nav.pop() },
                    contentBackdropModifier = Modifier.testTag("body-backdrop")) },
                bottomBar = {})
        } }
        compose.onNodeWithTag("host-backdrop").assertDoesNotExist()
        compose.onNodeWithTag("body-backdrop", useUnmergedTree = true)
            .assert(hasAnyDescendant(hasText("Nombre")))
            .assert(!hasAnyDescendant(hasContentDescription("Atrás")))
        compose.onNodeWithContentDescription("Atrás").assertIsDisplayed()
    }

    @Test fun emptyAndFilteredSearchRecordTheirListOutsideTheGlassHeader() {
        var query by mutableStateOf("")
        compose.setContent { AurelisTheme {
            com.music.aurelis.ui.screens.SearchScreen(
                query = query, onQueryChange = { query = it },
                results = if (query.isBlank()) null else emptyList(),
                listState = androidx.compose.foundation.lazy.rememberLazyListState(),
                focusRequested = false, onFocusHandled = {},
                onSongClick = { _, _ -> }, onSongLongPress = {}, history = emptyList(),
                onSubmit = {}, onHistoryClick = {}, onHistoryRemove = {}, onHistoryClear = {},
                currentSong = null, isPlaying = false,
                contentPadding = PaddingValues(bottom = 80.dp), topPadding = 12.dp,
                contentBackdropModifier = Modifier.testTag("search-body-backdrop"))
        } }
        val body = compose.onNodeWithTag("search-body-backdrop", useUnmergedTree = true)
        body.assert(hasAnyDescendant(hasText(compose.activity.getString(R.string.local_search_start))))
            .assert(!hasAnyDescendant(hasSetTextAction()))
        assertTrue("Empty search must still record a non-empty body", body.fetchSemanticsNode().boundsInRoot.height > 0f)
        compose.onNode(hasSetTextAction()).performTextInput("missing")
        body.assert(hasAnyDescendant(hasText(compose.activity.getString(R.string.search_library_no_results, "missing"))))
            .assert(!hasAnyDescendant(hasSetTextAction()))
    }
}
