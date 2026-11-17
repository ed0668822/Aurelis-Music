// Aurelis © 2026. GPL-3.0-or-later
package com.music.aurelis.ui

import androidx.activity.ComponentActivity
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.unit.*
import com.music.aurelis.AurelisApplication
import com.music.aurelis.ui.components.PillSegmentedControl
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
class PillSegmentedControlTest {
    @get:Rule(order = 0) val activity = RobolectricActivityRule()
    @get:Rule(order = 1) val compose = createAndroidComposeRule<ComponentActivity>()

    @Test fun selectionInvokesOnceAndDisabledControlsCannotChangeIt() {
        var selected by mutableIntStateOf(0)
        var enabled by mutableStateOf(true)
        var calls = 0
        compose.setContent { AurelisTheme {
            PillSegmentedControl(listOf("One", "Two"), selected, { selected = it; calls++ }, enabled = enabled)
        } }
        compose.onNodeWithText("One").assertIsSelected().performClick()
        compose.runOnIdle { assertEquals(0, calls) }
        compose.onNodeWithText("Two").performClick().assertIsSelected()
        compose.runOnIdle { assertEquals(1, calls); enabled = false }
        compose.onNodeWithText("One").assertIsNotEnabled().performClick()
        compose.runOnIdle { assertEquals(1, calls); assertEquals(1, selected) }
    }

    @Test fun overflowWithLargeFontKeepsTheSelectedSegmentReachableInRtl() {
        var selected by mutableIntStateOf(3)
        compose.setContent { AurelisTheme {
            val density = LocalDensity.current
            CompositionLocalProvider(LocalDensity provides Density(density.density, 1.5f),
                LocalLayoutDirection provides LayoutDirection.Rtl) {
                PillSegmentedControl(listOf("All results", "Songs", "Artists", "Local playlists"), selected,
                    { selected = it }, Modifier.width(240.dp))
            }
        } }
        compose.onNodeWithText("Local playlists").assertIsSelected().assertIsDisplayed()
        compose.onNodeWithText("All results").performScrollTo().performClick().assertIsSelected()
        compose.runOnIdle { assertEquals(0, selected) }
    }

    @Test fun equalSegmentsFillTheTrackAndInvalidSelectionDoesNotSelectAnotherItem() {
        var selected by mutableIntStateOf(-1)
        compose.setContent { AurelisTheme {
            PillSegmentedControl(listOf("One", "Two", "Three"), selected, { selected = it }, Modifier.width(320.dp))
        } }
        val bounds = listOf("One", "Two", "Three").map {
            compose.onNodeWithText(it).assertIsNotSelected().fetchSemanticsNode().boundsInRoot
        }
        assertEquals(bounds[0].width, bounds[1].width, 1f)
        assertEquals(bounds[1].width, bounds[2].width, 1f)
        assertTrue(bounds[0].right <= bounds[1].left + 1f)
        compose.onNodeWithText("Three").performClick().assertIsSelected()
        compose.runOnIdle { assertEquals(2, selected) }
    }
}
