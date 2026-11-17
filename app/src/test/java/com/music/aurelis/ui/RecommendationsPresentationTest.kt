// Aurelis © 2026. GPL-3.0-or-later
package com.music.aurelis.ui

import androidx.activity.ComponentActivity
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.unit.*
import com.music.aurelis.AurelisApplication
import com.music.aurelis.data.model.LocalSong
import com.music.aurelis.ui.components.LocalGlassHardware
import com.music.aurelis.ui.components.LocalPlayingBarsMotion
import com.music.aurelis.ui.components.home.LocalRecommendationsShelf
import com.music.aurelis.ui.theme.AurelisTheme
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33, 35], qualifiers = "es-rES-w360dp-h800dp-mdpi", application = AurelisApplication::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class RecommendationsPresentationTest {
    @get:Rule(order = 0) val activity = RobolectricActivityRule()
    @get:Rule(order = 1) val compose = createAndroidComposeRule<ComponentActivity>()
    private val songs = (1..3).map { LocalSong("$it", "Tema $it", "Artista $it", null,
        "content://media/external/audio/media/$it", null, 60_000L, null, it, 2026) }
    @Composable private fun Frame(content: @Composable () -> Unit) {
        AurelisTheme { CompositionLocalProvider(LocalGlassHardware provides false,
            LocalPlayingBarsMotion provides false, content = content) }
    }

    @Test fun sourcePortraitGeometryAndCaptionsUseLocalSongs() {
        compose.setContent { Frame { LocalRecommendationsShelf(songs, null, false, { _, _ -> }, {}) } }
        val card = compose.onNodeWithTag("recommendation:1").fetchSemanticsNode().boundsInRoot
        assertEquals(216f, card.width, 0.5f)
        assertEquals(0.75f, card.width / card.height, 0.01f)
        assertEquals(16f, card.left, 0.5f)
        compose.onNodeWithText("Selecciones para ti").assertIsDisplayed()
        compose.onAllNodesWithText("De tu biblioteca")[0].assertIsDisplayed()
    }

    @Test @Config(sdk = [33, 35], qualifiers = "es-rES-w720dp-h1000dp-mdpi")
    fun wideWindowUsesOriginal280dpCeiling() {
        compose.setContent { Frame { LocalRecommendationsShelf(songs, null, false, { _, _ -> }, {}) } }
        compose.onNodeWithTag("recommendation:1").assertWidthIsEqualTo(280.dp)
    }

    @Test fun scrollAndLongPressKeepCurrentQueueIndicesAfterDataChanges() {
        var rows by mutableStateOf(songs)
        var queue = emptyList<LocalSong>(); var index = -1; var plays = 0; var held = ""
        compose.setContent { Frame { LocalRecommendationsShelf(rows, songs[1], false,
            { q, i -> queue = q; index = i; plays++ }, { held = it.id }) } }
        compose.onNodeWithTag("recommendation-row").performScrollToNode(hasTestTag("recommendation:2"))
        compose.onNodeWithTag("recommendation:2").assertIsSelected().performClick()
        compose.onNodeWithTag("recommendation:2").performTouchInput { longClick() }
        compose.runOnIdle { assertEquals(songs, queue); assertEquals(1, index); assertEquals(1, plays); assertEquals("2", held); rows = songs.reversed() }
        compose.onNodeWithTag("recommendation-row").performScrollToNode(hasTestTag("recommendation:3"))
        compose.onNodeWithTag("recommendation:3").performClick()
        compose.runOnIdle { assertEquals(songs.reversed(), queue); assertEquals(0, index); assertEquals(2, plays) }
    }

    @Test fun emptyRecommendationsRemoveTheShelfAndKeepCallbacksSilent() {
        var rows by mutableStateOf(songs)
        compose.setContent { Frame { LocalRecommendationsShelf(rows, null, false, { _, _ -> fail("No play expected") }, {}) } }
        compose.onNodeWithTag("home-recommendations").assertExists()
        compose.runOnIdle { rows = emptyList() }
        compose.onNodeWithTag("home-recommendations").assertDoesNotExist()
    }

    @Test fun rtlLargeTextKeepsTheFirstCardActionReachable() {
        var clicked = false
        compose.setContent { Frame {
            CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl,
                LocalDensity provides Density(1f, 2f)) {
                LocalRecommendationsShelf(songs, null, false, { _, i -> assertEquals(0, i); clicked = true }, {})
            }
        } }
        compose.onNodeWithTag("recommendation:1").assertIsDisplayed().performClick()
        compose.runOnIdle { assertTrue(clicked) }
    }
}
