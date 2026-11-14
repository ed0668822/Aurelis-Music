// Aurelis © 2026. GPL-3.0-or-later
package com.music.aurelis.ui

import android.Manifest
import androidx.activity.ComponentActivity
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.*
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import com.music.aurelis.AurelisApplication
import com.music.aurelis.LibraryViewModel
import com.music.aurelis.data.LibraryState
import com.music.aurelis.data.LocalLibrarySource
import com.music.aurelis.data.model.LocalSong
import com.music.aurelis.ui.screens.HomeScreen
import com.music.aurelis.ui.theme.AurelisTheme
import kotlinx.coroutines.CompletableDeferred
import org.junit.After
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33], qualifiers = "w360dp-h800dp-mdpi", application = AurelisApplication::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class HomeStateRecoveryTest {
    @get:Rule(order = 0) val registerActivity = RobolectricActivityRule()
    @get:Rule(order = 1) val compose = createAndroidComposeRule<ComponentActivity>()
    private var model: LibraryViewModel? = null
    @After fun cancelReads() { compose.runOnIdle { model?.onBackground() } }

    @Test
    @Config(qualifiers = "w640dp-h360dp-mdpi")
    fun longErrorsKeepRetryReachableOnShortScreensWithLargeTextInBothThemes() {
        val message = "No se pudo leer la carpeta local. ".repeat(40)
        var state: LibraryState by mutableStateOf(LibraryState.Error(message))
        var dark by mutableStateOf(false)
        var retries = 0
        compose.setContent { AurelisTheme(darkTheme = dark) {
            val density = LocalDensity.current
            CompositionLocalProvider(LocalDensity provides Density(density.density, 1.5f)) {
                HomeScreen(emptyList(), emptyList(), emptyList(), emptyList(), emptyList(),
                    rememberLazyListState(), {}, {}, {}, {}, null, false,
                    contentPadding = PaddingValues(bottom = 100.dp), libraryState = state,
                    onRetry = { retries++; state = LibraryState.Success(emptyList()) })
            }
        } }
        for ((index, night) in listOf(false, true).withIndex()) {
            compose.runOnIdle { dark = night; state = LibraryState.Error(message) }
            compose.onNodeWithText("Reintentar").performScrollTo().assertIsDisplayed().performClick()
            compose.onNodeWithText("Actualizar biblioteca").performScrollTo().assertIsDisplayed()
            compose.onNodeWithText(message).assertDoesNotExist()
            compose.onNodeWithTag("local-home").assertDoesNotExist()
            compose.runOnIdle { assertEquals(index + 1, retries) }
        }
    }

    @Test
    @Config(qualifiers = "w640dp-h360dp-mdpi")
    fun longPermissionMessageKeepsThePermissionActionSeparateFromRetry() {
        var requests = 0
        var retries = 0
        compose.setContent { AurelisTheme {
            HomeScreen(emptyList(), emptyList(), emptyList(), emptyList(), emptyList(),
                rememberLazyListState(), {}, {}, {}, {}, null, false,
                contentPadding = PaddingValues(bottom = 100.dp),
                libraryState = LibraryState.Error("Permiso de música requerido. ".repeat(60), true),
                onRequestPermission = { requests++ }, onRetry = { retries++ })
        } }
        compose.onNodeWithText("Conceder acceso a música").performScrollTo().assertIsDisplayed().performClick()
        compose.onNodeWithText("Reintentar").assertDoesNotExist()
        compose.runOnIdle { assertEquals(1, requests); assertEquals(0, retries) }
    }

    @Test fun emptyRefreshShowsLoadingAndPopulatedRefreshRetainsRealContentUntilCompletion() {
        val app = RuntimeEnvironment.getApplication() as AurelisApplication
        shadowOf(app).grantPermissions(Manifest.permission.READ_MEDIA_AUDIO)
        val emptyRefresh = CompletableDeferred<List<LocalSong>>()
        val populatedRefresh = CompletableDeferred<List<LocalSong>>()
        val song = LocalSong("1", "Canción real local", "Artista", "Álbum",
            "content://media/external/audio/media/1", null, 60000, null, 1, 2026)
        var reads = 0
        val library = LibraryViewModel(app, LocalLibrarySource {
            when (++reads) {
                1 -> emptyList()
                2 -> emptyRefresh.await()
                else -> populatedRefresh.await()
            }
        })
        model = library
        compose.setContent { AurelisTheme {
            val ui by library.uiState.collectAsState()
            HomeScreen(emptyList(), emptyList(), emptyList(), emptyList(), emptyList(),
                rememberLazyListState(), {}, {}, {}, {}, null, false,
                contentPadding = PaddingValues(bottom = 100.dp), libraryState = ui.libraryState,
                isRefreshing = ui.isRefreshing, onRetry = library::refresh)
        } }
        compose.runOnIdle { library.loadLibrary() }
        compose.onNodeWithText("Actualizar biblioteca").performScrollTo().performClick()
        compose.onNodeWithText("Cargando tu biblioteca…").assertIsDisplayed()
        compose.onNodeWithText("Actualizar biblioteca").assertDoesNotExist()
        compose.onNodeWithTag("local-home").assertDoesNotExist()
        compose.runOnIdle { assertTrue(library.uiState.value.isRefreshing); emptyRefresh.complete(listOf(song)) }
        compose.onNodeWithTag("local-home").assertIsDisplayed()
        compose.runOnIdle { library.refresh() }
        compose.onNodeWithTag("local-home").assertIsDisplayed()
        compose.onNodeWithTag("home-refresh").assertIsDisplayed()
        compose.runOnIdle {
            assertEquals(listOf(song), (library.uiState.value.libraryState as LibraryState.Success).songs)
            populatedRefresh.complete(emptyList())
        }
        compose.onNodeWithText("Actualizar biblioteca").performScrollTo().assertIsDisplayed()
        compose.onNodeWithTag("home-refresh").assertDoesNotExist()
        compose.onNodeWithText("Canción real local").assertDoesNotExist()
        compose.runOnIdle { assertEquals(3, reads); assertFalse(library.uiState.value.isRefreshing) }
    }
}
