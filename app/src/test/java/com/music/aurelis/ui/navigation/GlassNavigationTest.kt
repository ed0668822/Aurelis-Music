package com.music.aurelis.ui.navigation

import androidx.compose.foundation.layout.size
import com.music.aurelis.ui.components.backdrop.backdrops.layerBackdrop
import androidx.activity.ComponentActivity
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.runtime.*
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.unit.dp
import com.music.aurelis.AurelisApplication
import com.music.aurelis.ui.components.BottomTab
import com.music.aurelis.ui.components.GlassNavBar
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
class GlassNavigationTest {
    @get:Rule(order = 0) val registerActivity = com.music.aurelis.ui.RobolectricActivityRule()
    @get:Rule(order = 1) val compose = createAndroidComposeRule<ComponentActivity>()
    @Test fun pillSelectionKeepsTabsAccessibleAndInvokesOneNavigationAction() {
        var selected by mutableIntStateOf(0)
        var clicks = 0
        compose.setContent { AurelisTheme {
            CompositionLocalProvider(LocalReduceMotion provides true) {
                GlassNavBar(listOf("Home", "Explore", "Library", "Search").map { BottomTab(it, Icons.Default.Home) },
                    selected, { selected = it; clicks++ }, null, false, false, {}, {}, {}, {})
            }
        } }
        compose.onNode(hasText("Search") and isSelectable()).assertHeightIsAtLeast(48.dp).performClick().assertIsSelected()
        compose.onNode(hasText("Home") and isSelectable()).assertIsNotSelected()
        compose.runOnIdle { assertEquals(3, selected); assertEquals(1, clicks) }
    }

    @Test fun pageBackedGlassAttachesAndAnimatedSelectionCanReturnHome() {
        var selected by mutableIntStateOf(0)
        compose.setContent { AurelisTheme {
            val page = com.music.aurelis.ui.components.backdrop.backdrops.rememberLayerBackdrop()
            androidx.compose.foundation.layout.Box {
                androidx.compose.foundation.layout.Box(androidx.compose.ui.Modifier
                    .size(360.dp, 200.dp).layerBackdrop(page))
                CompositionLocalProvider(com.music.aurelis.ui.components.LocalAppBackdrop provides page) {
                    GlassNavBar(listOf("Home", "Explore", "Library", "Search").map { BottomTab(it, Icons.Default.Home) },
                        selected, { selected = it }, null, false, false, {}, {}, {}, {})
                }
            }
        } }
        compose.onNode(hasText("Search") and isSelectable()).performClick().assertIsSelected()
        compose.onNode(hasText("Home") and isSelectable()).performClick().assertIsSelected()
    }

    @Test fun changingGlassAndReducedBlurKeepsMiniPlayerAndNavigationInteractive() {
        val settings = com.music.aurelis.data.SettingsRepository
        val oldReduced = settings.reduceDynamicBlur.value
        var glass by mutableStateOf(true)
        var selected by mutableIntStateOf(0)
        var plays = 0
        var next = 0
        var expands = 0
        val song = com.music.aurelis.data.model.LocalSong(
            "fixture", "Local fixture", "Test artist", null,
            "content://media/external/audio/media/1", null, 120_000L, null, null, null,
        )
        try {
            compose.setContent { AurelisTheme {
                CompositionLocalProvider(
                    com.music.aurelis.ui.components.LocalLiquidGlassEnabled provides glass,
                    com.music.aurelis.ui.components.LocalGlassHardware provides false,
                    LocalReduceMotion provides true,
                ) {
                    GlassNavBar(listOf("Home", "Explore", "Library", "Search").map { BottomTab(it, Icons.Default.Home) },
                        selected, { selected = it }, song, false, false,
                        { plays++ }, { next++ }, {}, { expands++ })
                }
            } }
            // Software fallback exercises actual surfaces and callbacks, not GPU parity.
            listOf(true to false, true to true, false to false).forEach { (enabled, reduced) ->
                compose.runOnIdle { glass = enabled; settings.setReduceDynamicBlur(reduced) }
                compose.onNodeWithTag("mini-player").assertIsDisplayed()
                compose.onNodeWithContentDescription("Reproducir").performClick()
                compose.onNodeWithContentDescription("Siguiente").performClick()
                compose.onNodeWithContentDescription("Abrir reproductor").performClick()
                compose.onNode(hasText("Search") and isSelectable()).performClick().assertIsSelected()
                compose.onNode(hasText("Home") and isSelectable()).performClick().assertIsSelected()
            }
            compose.runOnIdle {
                assertEquals(3, plays); assertEquals(3, next); assertEquals(3, expands)
                assertEquals(0, selected)
            }
        } finally {
            settings.setReduceDynamicBlur(oldReduced)
        }
    }
}
