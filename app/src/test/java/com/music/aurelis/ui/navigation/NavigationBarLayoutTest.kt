// Aurelis © 2026. GPL-3.0-or-later
package com.music.aurelis.ui.navigation

import android.graphics.Bitmap
import android.graphics.Canvas
import androidx.activity.ComponentActivity
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import com.music.aurelis.AurelisApplication
import com.music.aurelis.data.model.LocalSong
import com.music.aurelis.ui.RobolectricActivityRule
import com.music.aurelis.ui.components.*
import com.music.aurelis.ui.components.floatingtabbar.rememberFloatingTabBarScrollConnection
import com.music.aurelis.ui.components.floatingtabbar.FloatingTabBarScrollConnection
import com.music.aurelis.ui.icons.AurelisNavigationIcons
import com.music.aurelis.ui.theme.AurelisTheme
import com.music.aurelis.ui.theme.LocalReduceMotion
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35], qualifiers = "w360dp-h800dp-mdpi", application = AurelisApplication::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class NavigationBarLayoutTest {
    @get:Rule(order = 0) val activity = RobolectricActivityRule()
    @get:Rule(order = 1) val compose = createAndroidComposeRule<ComponentActivity>()
    private val tabs = listOf(BottomTab("Home", AurelisNavigationIcons.Home),
        BottomTab("Explore", AurelisNavigationIcons.TabExplore),
        BottomTab("Library", AurelisNavigationIcons.TabLibrary),
        BottomTab("Search", AurelisNavigationIcons.TabSearch))
    private fun tab(name: String) = compose.onNode(isSelectable() and (hasText(name) or hasContentDescription(name)))

    @Test fun softwareCapturesKeepBothPosesUsableWithoutHardwareOrWithReducedMotion() {
        var hardware by mutableStateOf(false)
        var reduced by mutableStateOf(false)
        var selected by mutableIntStateOf(0)
        val fixture = LocalSong("fixture", "Software capture", "Test artist", null,
            "content://media/external/audio/media/1", null, 120_000, null, null, null)
        var song by mutableStateOf<LocalSong?>(fixture)
        lateinit var scroll: FloatingTabBarScrollConnection
        compose.setContent { AurelisTheme {
            CompositionLocalProvider(LocalLiquidGlassEnabled provides true,
                LocalGlassHardware provides hardware, LocalReduceMotion provides reduced) {
                scroll = rememberFloatingTabBarScrollConnection()
                Box(Modifier.fillMaxSize()) {
                    GlassNavBar(tabs, selected, { selected = it }, song, false, false, {}, {}, {}, {},
                        Modifier.align(Alignment.BottomCenter), scrollConnection = scroll)
                }
            }
        } }
        fun captureSoftware() {
            compose.waitForIdle()
            compose.runOnIdle {
                val view = compose.activity.window.decorView
                val bitmap = Bitmap.createBitmap(view.width, view.height, Bitmap.Config.ARGB_8888)
                try {
                    val canvas = Canvas(bitmap)
                    assertFalse(canvas.isHardwareAccelerated)
                    // Same drawing path as the six CI180 failures; no exception is suppressed.
                    view.draw(canvas)
                } finally { bitmap.recycle() }
            }
            tab("Search").assertIsDisplayed()
        }
        // Software with motion enabled, and reduced motion with the hardware flag enabled.
        listOf(false to false, true to true).forEach { (gpu, reduce) ->
            listOf(fixture, null).forEach { current ->
                compose.runOnIdle { hardware = gpu; reduced = reduce; song = current; scroll.expand() }
                tab("Explore").assertIsDisplayed()
                captureSoftware()
                compose.runOnIdle { scroll.inline() }
                captureSoftware()
                tab("Home").performClick()
                tab("Library").performClick().assertIsSelected()
                tab("Home").performClick().assertIsSelected()
            }
        }
    }

    @Test fun glassSearchIsACircleOutsideTheGroupAndPlainModeKeepsFourTabs() {
        var glass by mutableStateOf(true)
        var selected by mutableIntStateOf(0)
        compose.setContent { AurelisTheme {
            CompositionLocalProvider(LocalLiquidGlassEnabled provides glass, LocalGlassHardware provides false,
                LocalReduceMotion provides true) {
                Box(Modifier.fillMaxSize()) {
                    GlassNavBar(tabs, selected, { selected = it }, null, false, false, {}, {}, {}, {},
                        Modifier.align(Alignment.BottomCenter))
                }
            }
        } }
        val group = compose.onNodeWithTag("navigation-group").fetchSemanticsNode().boundsInRoot
        val search = tab("Search").fetchSemanticsNode().boundsInRoot
        assertTrue("Search must sit outside the three-tab group", search.left > group.right)
        assertEquals(search.height, search.width, 1f)
        assertEquals("The source leaves an 8dp inter-component gap", 8f, search.left - group.right, 1f)
        tab("Search").performClick().assertIsSelected()
        compose.runOnIdle { glass = false }
        compose.onNodeWithTag("navigation-plain").assertIsDisplayed()
        tabs.forEach { tab(it.label).assertIsDisplayed() }
        tab("Search").assertIsSelected()
        tab("Library").performClick().assertIsSelected()
    }

    @Test fun realPageScrollFoldsTheAccessoryAndTappingItsTabExpandsTheBar() {
        var selected by mutableIntStateOf(0)
        var clicks = 0
        val song = LocalSong("fixture", "Local fixture", "Test artist", null,
            "content://media/external/audio/media/1", null, 120_000, null, null, null)
        compose.setContent { AurelisTheme {
            CompositionLocalProvider(LocalLiquidGlassEnabled provides true, LocalGlassHardware provides false,
                LocalReduceMotion provides true) {
                val scroll = rememberFloatingTabBarScrollConnection()
                val list = rememberLazyListState()
                Box(Modifier.fillMaxSize()) {
                    LazyColumn(Modifier.fillMaxSize().nestedScroll(scroll).testTag("page"), state = list) {
                        items(100) { Text("Row $it", Modifier.fillMaxWidth().height(56.dp)) }
                    }
                    GlassNavBar(tabs, selected, { selected = it; clicks++ }, song, false, false, {}, {}, {}, {},
                        Modifier.align(Alignment.BottomCenter), scrollConnection = scroll)
                }
            }
        } }
        val before = compose.onNodeWithTag("navigation-glass").fetchSemanticsNode().boundsInRoot.height
        compose.onNodeWithTag("page").performScrollToIndex(10)
        tab("Explore").assertIsDisplayed()
        compose.onNodeWithTag("page").performTouchInput {
            swipe(androidx.compose.ui.geometry.Offset(centerX, height * 0.6f),
                androidx.compose.ui.geometry.Offset(centerX, height * 0.2f), 300)
        }
        compose.onNodeWithTag("mini-player").assertIsDisplayed()
        val after = compose.onNodeWithTag("navigation-glass").fetchSemanticsNode().boundsInRoot.height
        assertTrue("The accessory and tabs must actually fold into one row", after < before)
        tab("Search").assertIsDisplayed()
        tab("Home").performClick()
        tab("Explore").assertIsDisplayed()
        tab("Library").assertIsDisplayed()
        compose.runOnIdle { assertEquals(0, selected); assertEquals(1, clicks) }
    }

    @Test fun swipingThePlainBarInRtlFollowsLogicalTabOrderAndCanReturnFromSearch() {
        var selected by mutableIntStateOf(3)
        compose.setContent { AurelisTheme {
            CompositionLocalProvider(LocalLiquidGlassEnabled provides false, LocalGlassHardware provides false,
                LocalReduceMotion provides true, LocalLayoutDirection provides LayoutDirection.Rtl) {
                Box(Modifier.fillMaxSize()) {
                    GlassNavBar(tabs, selected, { selected = it }, null, false, false, {}, {}, {}, {},
                        Modifier.align(Alignment.BottomCenter))
                }
            }
        } }
        compose.onNodeWithTag("navigation-plain").performTouchInput {
            swipe(center, center + androidx.compose.ui.geometry.Offset(100f, 0f), 300)
        }
        tab("Library").assertIsSelected()
        tab("Home").performClick().assertIsSelected()
        tab("Search").performClick().assertIsSelected()
    }
}
