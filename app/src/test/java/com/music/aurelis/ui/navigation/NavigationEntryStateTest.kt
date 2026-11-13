// Aurelis © 2026. GPL-3.0-or-later
package com.music.aurelis.ui.navigation

import androidx.activity.ComponentActivity
import androidx.compose.material3.TextField
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.saveable.LocalSaveableStateRegistry
import androidx.compose.runtime.saveable.SaveableStateRegistry
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.StateRestorationTester
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import com.music.aurelis.AurelisApplication
import com.music.aurelis.ui.RobolectricActivityRule
import com.music.aurelis.ui.theme.AurelisTheme
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33], qualifiers = "w360dp-h800dp-mdpi", application = AurelisApplication::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class NavigationEntryStateTest {
    @get:Rule(order = 0) val registerActivity = RobolectricActivityRule()
    @get:Rule(order = 1) val compose = createAndroidComposeRule<ComponentActivity>()
    private lateinit var navigator: AppNavigator
    private lateinit var registry: SaveableStateRegistry

    /** Real navigation host; lightweight fields make its state ownership observable. */
    private fun start(legacy: List<String>? = null): StateRestorationTester {
        val restoration = StateRestorationTester(compose)
        restoration.setContent {
            navigator = if (legacy == null) rememberAppNavigator() else {
                rememberSaveable(saver = AppNavigator.StateSaver) {
                    requireNotNull(AppNavigator.restoreState(legacy))
                }
            }
            registry = requireNotNull(LocalSaveableStateRegistry.current)
            AurelisTheme {
                AppNavigationHost(navigator, tabContent = {
                    var value by rememberSaveable { mutableStateOf("") }
                    TextField(value, { value = it }, Modifier.testTag("tab-value"))
                }, routeContent = {
                    var value by rememberSaveable { mutableStateOf("") }
                    TextField(value, { value = it }, Modifier.testTag("page-value"))
                }, bottomBar = {})
            }
        }
        return restoration
    }

    @Test fun duplicateRouteVisitsRemainIndependentThroughBackAndRecreation() {
        val restoration = start()
        compose.runOnIdle { navigator.selectTab(AppTab.LIBRARY); navigator.push(AppRoute.Settings) }
        compose.onNodeWithTag("page-value").performTextReplacement("first visit")
        compose.runOnIdle { navigator.push(AppRoute.Equalizer) }
        compose.onNodeWithTag("page-value").performTextReplacement("equalizer visit")
        compose.runOnIdle { navigator.push(AppRoute.Settings) }
        compose.onNodeWithTag("page-value").assertTextEquals("").performTextReplacement("second visit")
        val originalIds = compose.runOnIdle { navigator.entries.map { it.id } }
        restoration.emulateSavedInstanceStateRestore()
        compose.onNodeWithTag("page-value").assertTextEquals("second visit")
        compose.runOnIdle { assertEquals(originalIds, navigator.entries.map { it.id }); assertTrue(navigator.back()) }
        compose.onNodeWithTag("page-value").assertTextEquals("equalizer visit")
        compose.runOnIdle { assertTrue(navigator.back()) }
        compose.onNodeWithTag("page-value").assertTextEquals("first visit")
    }

    @Test fun closingAPageDropsItsSavedPayloadAndKeepsTheTabState() {
        val restoration = start()
        compose.runOnIdle { navigator.selectTab(AppTab.SEARCH) }
        compose.onNodeWithTag("tab-value").performTextReplacement("retained tab query")
        compose.runOnIdle { navigator.push(AppRoute.Settings) }
        compose.onNodeWithTag("page-value").performTextReplacement("closed-page-marker")
        compose.runOnIdle { assertTrue(navigator.back()) }
        compose.onNodeWithTag("tab-value").assertTextEquals("retained tab query")
        compose.runOnIdle { assertFalse(registry.performSave().toString().contains("closed-page-marker")) }
        restoration.emulateSavedInstanceStateRestore()
        compose.onNodeWithTag("tab-value").assertTextEquals("retained tab query")
        compose.runOnIdle { navigator.push(AppRoute.Settings) }
        compose.onNodeWithTag("page-value").assertTextEquals("")
    }

    @Test fun selectingATabClearsPagePayloadsWithoutClearingOtherTabs() {
        start()
        compose.runOnIdle { navigator.selectTab(AppTab.SEARCH) }
        compose.onNodeWithTag("tab-value").performTextReplacement("search query")
        compose.runOnIdle { navigator.push(AppRoute.Settings) }
        compose.onNodeWithTag("page-value").performTextReplacement("cleared-page-marker")
        compose.runOnIdle { navigator.selectTab(AppTab.LIBRARY) }
        compose.onNodeWithTag("tab-value").assertTextEquals("")
        compose.runOnIdle {
            assertTrue(navigator.entries.isEmpty())
            assertFalse(registry.performSave().toString().contains("cleared-page-marker"))
            navigator.selectTab(AppTab.SEARCH)
        }
        compose.onNodeWithTag("tab-value").assertTextEquals("search query")
    }

    @Test fun legacyPageKeySurvivesAnotherRecreationButTheNextVisitIsFresh() {
        val restoration = start(listOf("1", "LIBRARY", "settings"))
        compose.onNodeWithTag("page-value").performTextReplacement("legacy-page-marker")
        restoration.emulateSavedInstanceStateRestore()
        compose.onNodeWithTag("page-value").assertTextEquals("legacy-page-marker")
        compose.runOnIdle {
            assertEquals("page.settings", AppDestination.Route(navigator.entries.single()).key)
            navigator.back()
        }
        compose.onNodeWithTag("tab-value").assertExists()
        compose.runOnIdle {
            assertFalse(registry.performSave().toString().contains("legacy-page-marker"))
            navigator.push(AppRoute.Settings)
        }
        compose.onNodeWithTag("page-value").assertTextEquals("")
    }

    @Test fun queueAndPlayerRestorationKeepTheUnderlyingPageEntry() {
        val restoration = start()
        compose.runOnIdle { navigator.selectTab(AppTab.LIBRARY); navigator.push(AppRoute.Settings) }
        compose.onNodeWithTag("page-value").performTextReplacement("underlying settings")
        compose.runOnIdle { navigator.pushAll(AppRoute.NowPlaying, AppRoute.Queue) }
        restoration.emulateSavedInstanceStateRestore()
        compose.runOnIdle {
            assertEquals(AppRoute.Queue, navigator.visibleRoute()); navigator.back()
            assertEquals(AppRoute.NowPlaying, navigator.visibleRoute()); navigator.back()
        }
        compose.onNodeWithTag("page-value").assertTextEquals("underlying settings")
        compose.runOnIdle { assertEquals(AppRoute.Settings, navigator.visibleRoute()) }
    }
}
