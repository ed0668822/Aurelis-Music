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
import com.music.aurelis.data.LibraryState
import com.music.aurelis.data.model.LocalSong
import com.music.aurelis.ui.components.LocalGlassHardware
import com.music.aurelis.ui.screens.ExploreScreen
import com.music.aurelis.ui.screens.LocalGenre
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
class ExplorePresentationTest {
    @get:Rule(order = 0) val activity = RobolectricActivityRule()
    @get:Rule(order = 1) val compose = createAndroidComposeRule<ComponentActivity>()
    private val genres = listOf(LocalGenre("Rock", 1, null, "tag:rock"),
        LocalGenre("Pop", 2, null, "tag:pop"), LocalGenre("Sin clasificar", 1, null, "unknown"))
    @Composable private fun Frame(content: @Composable () -> Unit) {
        AurelisTheme { CompositionLocalProvider(LocalGlassHardware provides false, content = content) }
    }
    @Test fun originalSleeveGeometryKeepsShortRowsAndAccessibleRealCounts() {
        var selected = ""
        compose.setContent { Frame { ExploreScreen(genres, rememberLazyListState(), { selected = it.key }, PaddingValues()) } }
        val first = compose.onNodeWithTag("genre:tag:rock").fetchSemanticsNode().boundsInRoot
        val last = compose.onNodeWithTag("genre:unknown").fetchSemanticsNode().boundsInRoot
        assertEquals(158f, first.width, 0.5f)
        assertEquals(16f, first.left, 0.5f)
        assertEquals(1.72f, first.width / first.height, 0.01f)
        assertEquals(first.width, last.width, 0.5f)
        val cover = compose.onNodeWithTag("genre-cover:tag:rock", useUnmergedTree = true).fetchSemanticsNode().boundsInRoot
        assertEquals(first.width * 0.72f, cover.width, 0.5f)
        compose.onNodeWithTag("genre:tag:rock").assert(SemanticsMatcher.expectValue(
            SemanticsProperties.StateDescription, "1 canción disponible")).performClick()
        compose.onNodeWithText("1 canciones").assertDoesNotExist()
        compose.runOnIdle { assertEquals("tag:rock", selected) }
    }
    @Test @Config(sdk = [33, 35], qualifiers = "es-rES-w1000dp-h1000dp-mdpi")
    fun largeWindowUsesFourColumnsInsteadOfStretchingTwoCards() {
        val many = (0..6).map { LocalGenre("Género $it", 1, null, "tag:$it") }
        compose.setContent { Frame { ExploreScreen(many, rememberLazyListState(), {}, PaddingValues()) } }
        val first = compose.onNodeWithTag("genre:tag:0").fetchSemanticsNode().boundsInRoot
        val fourth = compose.onNodeWithTag("genre:tag:3").fetchSemanticsNode().boundsInRoot
        val fifth = compose.onNodeWithTag("genre:tag:4").fetchSemanticsNode().boundsInRoot
        assertEquals(233f, first.width, 0.5f)
        assertEquals(first.top, fourth.top, 0.5f)
        assertTrue(fifth.top > first.bottom)
    }
    @Test fun loadingPermissionErrorRetryAndEmptyStatesDoNotOfferStaleGenres() {
        var state: LibraryState by mutableStateOf(LibraryState.Loading)
        var grants = 0; var retries = 0
        compose.setContent { Frame { ExploreScreen(genres, rememberLazyListState(), {}, PaddingValues(),
            libraryState = state, onRetry = { retries++ }, onRequestPermission = { grants++ }) } }
        compose.onNodeWithTag("explore-loading").assertIsDisplayed()
        compose.onNodeWithTag("genre:tag:rock").assertDoesNotExist()
        compose.runOnIdle { state = LibraryState.Error("Permiso requerido", true) }
        compose.onNodeWithText("Conceder acceso a música").performClick()
        compose.runOnIdle { assertEquals(1, grants); state = LibraryState.Error("Error local") }
        compose.onNodeWithText("Reintentar").performClick()
        compose.runOnIdle {
            assertEquals(1, retries)
            state = LibraryState.Success(listOf(LocalSong("1", "Tema", "Artista", null,
                "content://media/1", null, 60_000L, null, null, null)))
        }
        compose.onNodeWithTag("genre:tag:rock").assertIsDisplayed()
        compose.runOnIdle { state = LibraryState.Success(emptyList()) }
        compose.onNodeWithTag("genre:tag:rock").assertDoesNotExist()
        compose.onNodeWithText("No hay música local para explorar").assertIsDisplayed()
    }
    @Test fun emptyLibraryOffersRefreshWithoutInventingCategories() {
        var refreshes = 0
        compose.setContent { Frame { ExploreScreen(emptyList(), rememberLazyListState(), {}, PaddingValues(), onRetry = { refreshes++ }) } }
        compose.onNodeWithText("No hay música local para explorar").assertIsDisplayed()
        compose.onNodeWithTag("genre:unknown").assertDoesNotExist()
        compose.onNodeWithText("Actualizar biblioteca").performClick()
        compose.runOnIdle { assertEquals(1, refreshes) }
    }
    @Test fun rtlLargeTextKeepsLocalizedCategoryIdentityClickable() {
        var selected = ""
        compose.setContent { Frame {
            CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl,
                LocalDensity provides Density(1f, 2f)) {
                ExploreScreen(genres, rememberLazyListState(), { selected = it.key }, PaddingValues())
            }
        } }
        compose.onNodeWithTag("genre:unknown").assertIsDisplayed().performClick()
        compose.runOnIdle { assertEquals("unknown", selected) }
    }
}
