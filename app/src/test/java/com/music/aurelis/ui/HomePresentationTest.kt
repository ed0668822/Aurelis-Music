// Aurelis © 2026. GPL-3.0-or-later
package com.music.aurelis.ui

import androidx.activity.ComponentActivity
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import com.music.aurelis.AurelisApplication
import com.music.aurelis.data.LibraryState
import com.music.aurelis.data.model.LocalArtist
import com.music.aurelis.data.model.LocalSong
import com.music.aurelis.ui.components.*
import com.music.aurelis.ui.components.home.ArtistCard
import com.music.aurelis.ui.screens.HomeScreen
import com.music.aurelis.ui.theme.AurelisTheme
import com.music.aurelis.ui.theme.LocalReduceMotion
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import kotlin.math.abs

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33, 35], qualifiers = "es-rES-w360dp-h800dp-mdpi", application = AurelisApplication::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class HomePresentationTest {
    @get:Rule(order = 0) val activity = RobolectricActivityRule()
    @get:Rule(order = 1) val compose = createAndroidComposeRule<ComponentActivity>()
    private val song = LocalSong("1", "Tema local", "Artista", "Álbum",
        "content://media/external/audio/media/1", null, 60_000L, null, 1, 2026)

    @Composable private fun Frame(content: @Composable () -> Unit) {
        AurelisTheme {
            CompositionLocalProvider(LocalGlassHardware provides false, LocalReduceMotion provides true,
                LocalPlayingBarsMotion provides false, content = content)
        }
    }

    private fun home(play: (LocalSong) -> Unit = {}, hold: (LocalSong) -> Unit = {}) {
        compose.setContent { Frame {
            HomeScreen(emptyList(), emptyList(), listOf(song), emptyList(), emptyList(),
                rememberLazyListState(), play, hold, {}, {}, null, false,
                contentPadding = PaddingValues(), libraryState = LibraryState.Success(listOf(song)))
        } }
    }

    @Test fun phoneHeroUsesSourceWidthRatioAndKeepsTapAndHoldIndependent() {
        var plays = 0
        var holds = 0
        home(play = { assertEquals(song, it); plays++ }, hold = { assertEquals(song, it); holds++ })
        val hero = compose.onNodeWithTag("home-leading-hero")
        val bounds = hero.fetchSemanticsNode().boundsInRoot
        assertEquals(252f, bounds.width, 0.5f)
        assertEquals(0.92f, bounds.width / bounds.height, 0.01f)
        assertEquals(16f, bounds.left, 0.5f)
        compose.onNodeWithContentDescription("Aurelis Music").assertExists()
        compose.onNodeWithText("Inicio").assertDoesNotExist()
        hero.performClick()
        hero.performTouchInput { longClick() }
        compose.runOnIdle { assertEquals(1, plays); assertEquals(1, holds) }
    }

    @Test
    @Config(sdk = [33, 35], qualifiers = "es-rES-w720dp-h1000dp-mdpi")
    fun wideHeroUsesTheSourceCapInsteadOfStretchingWithTheWindow() {
        home()
        compose.onNodeWithTag("home-leading-hero").assertWidthIsEqualTo(320.dp)
        val hero = compose.onNodeWithTag("home-leading-hero").fetchSemanticsNode().boundsInRoot
        assertEquals(0.92f, hero.width / hero.height, 0.01f)
    }

    @Test fun sourceShelfActuallySnapsAfterAFling() {
        lateinit var state: LazyListState
        compose.setContent { Frame {
            state = rememberLazyListState()
            ShelfRow(PaddingValues(horizontal = 16.dp), Arrangement.spacedBy(14.dp),
                Modifier.testTag("source-shelf"), state) {
                items(30) { Text("Card $it", Modifier.width(150.dp).height(80.dp)
                    .background(MaterialTheme.colorScheme.surfaceVariant)) }
            }
        } }
        compose.onNodeWithTag("source-shelf").performTouchInput { swipeLeft() }
        compose.waitForIdle()
        compose.runOnIdle {
            assertTrue(state.firstVisibleItemIndex > 0)
            assertFalse(state.isScrollInProgress)
            val layout = state.layoutInfo
            val center = (layout.viewportStartOffset + layout.viewportEndOffset) / 2f
            assertTrue("The original snap behavior must settle a card, not stop arbitrarily",
                layout.visibleItemsInfo.any { abs(it.offset + it.size / 2f - center) <= 1.5f })
        }
    }

    @Test fun rtlShelfScrollsInItsLogicalDirectionAndKeepsTheVisibleCardClickable() {
        lateinit var state: LazyListState
        var chosen = -1
        compose.setContent { Frame {
            CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
                state = rememberLazyListState()
                ShelfRow(PaddingValues(horizontal = 16.dp), Arrangement.spacedBy(14.dp),
                    Modifier.testTag("rtl-shelf"), state) {
                    items(30) { index -> Text("Card $index", Modifier.width(150.dp).height(80.dp)
                        .clickable { chosen = index }.testTag("card-$index")) }
                }
            }
        } }
        compose.onNodeWithTag("rtl-shelf").performTouchInput { swipeRight() }
        compose.waitForIdle()
        val item = compose.runOnIdle {
            assertTrue(state.firstVisibleItemIndex > 0)
            state.layoutInfo.visibleItemsInfo.first { it.offset >= 0 && it.offset + it.size <= state.layoutInfo.viewportEndOffset }.index
        }
        compose.onNodeWithTag("card-$item").performClick()
        compose.runOnIdle { assertEquals(item, chosen) }
    }

    @Test fun artistCardMatchesSquareShelfGeometryAndUsesLocalizedRealCounts() {
        var clicked = false
        compose.setContent { Frame {
            ArtistCard(LocalArtist("Artista", null, 1, 1, listOf(song))) { clicked = true }
        } }
        compose.onNodeWithTag("home-artist-card").assertWidthIsEqualTo(150.dp).performClick()
        compose.onNodeWithText("1 canción disponible").assertIsDisplayed()
        compose.runOnIdle { assertTrue(clicked) }
    }
}
