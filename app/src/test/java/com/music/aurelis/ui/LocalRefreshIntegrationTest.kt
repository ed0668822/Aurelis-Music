// Aurelis © 2026. GPL-3.0-or-later
package com.music.aurelis.ui

import android.Manifest
import androidx.activity.ComponentActivity
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.ProgressBarRangeInfo
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModelStore
import com.music.aurelis.AurelisApplication
import com.music.aurelis.AurelisNavigationContent
import com.music.aurelis.LibraryViewModel
import com.music.aurelis.R
import com.music.aurelis.data.LibraryState
import com.music.aurelis.data.LocalLibrarySource
import com.music.aurelis.data.model.LocalSong
import com.music.aurelis.playback.PlaybackViewModel
import com.music.aurelis.playback.RecordingPlayer
import com.music.aurelis.ui.components.FrostedTopBar
import com.music.aurelis.ui.navigation.*
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
import java.io.IOException
import java.util.concurrent.atomic.AtomicInteger

@androidx.media3.common.util.UnstableApi
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33, 35], qualifiers = "en-rUS-w360dp-h900dp-mdpi", application = AurelisApplication::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class LocalRefreshIntegrationTest {
    @get:Rule(order = 0) val activity = RobolectricActivityRule()
    @get:Rule(order = 1) val compose = createAndroidComposeRule<ComponentActivity>()
    private val app get() = RuntimeEnvironment.getApplication() as AurelisApplication
    private val models = ViewModelStore()
    private lateinit var library: LibraryViewModel
    private lateinit var playback: PlaybackViewModel
    private lateinit var player: RecordingPlayer
    private lateinit var navigator: AppNavigator
    private var permissionRequests = 0
    private val song = LocalSong("1", "Real local song", "Artist", "Album", "content://media/1", null,
        60_000, null, null, null, genre = "Rock")
    @After fun close() { compose.runOnIdle { models.clear() } }

    private fun start(source: LocalLibrarySource) {
        shadowOf(app).grantPermissions(Manifest.permission.READ_MEDIA_AUDIO)
        library = LibraryViewModel(app, source)
        player = RecordingPlayer()
        playback = PlaybackViewModel(app) { player }
        models.put("library", library); models.put("player", playback)
        compose.setContent { AurelisTheme {
            navigator = rememberAppNavigator()
            AurelisNavigationContent(navigator, playback, library, onRequestAudioPermission = { permissionRequests++ })
        } }
        compose.runOnIdle { library.loadLibrary() }
        compose.waitUntil(5_000) { library.uiState.value.libraryState is LibraryState.Success }
    }

    private fun refreshAction(tab: AppTab, expected: Boolean = true) {
        val action = compose.onNodeWithTag("feed-refresh:${tab.name}").fetchSemanticsNode()
            .config[SemanticsActions.CustomActions].single { it.label == app.getString(R.string.home_refresh) }
        compose.runOnIdle { assertEquals(expected, action.action()) }
    }

    @Test fun downwardGestureRetainsCatalogueAndPlaybackUntilTheSingleReadCompletes() {
        val reads = AtomicInteger()
        val gate = CompletableDeferred<List<LocalSong>>()
        start(LocalLibrarySource { if (reads.incrementAndGet() == 1) listOf(song) else gate.await() })
        compose.runOnIdle { playback.playSongs(listOf(song)) }
        val headerHeight = compose.onNodeWithTag("feed-refresh-header").fetchSemanticsNode().boundsInRoot.height
        var replacements = 0
        compose.runOnIdle { replacements = player.replacements }
        compose.onNodeWithTag("local-home").performTouchInput {
            swipe(Offset(8f, height * 0.2f), Offset(8f, height * 0.7f), durationMillis = 600)
        }
        compose.waitUntil(5_000) { reads.get() == 2 }
        compose.onNodeWithTag("library-refresh-puck").assertIsDisplayed()
        assertEquals(headerHeight, compose.onNodeWithTag("feed-refresh-header").fetchSemanticsNode().boundsInRoot.height, 0.5f)
        refreshAction(AppTab.HOME, expected = false)
        compose.runOnIdle {
            assertEquals(listOf(song), (library.uiState.value.libraryState as LibraryState.Success).songs)
            assertEquals(song.id, player.currentMediaItem?.mediaId)
            assertEquals(replacements, player.replacements)
            gate.complete(listOf(song.copy(title = "Updated local title")))
        }
        compose.onNodeWithTag("library-refresh-puck").assertDoesNotExist()
        compose.runOnIdle {
            assertEquals(2, reads.get())
            assertFalse(library.uiState.value.isRefreshing)
            assertEquals("Updated local title", (library.uiState.value.libraryState as LibraryState.Success).songs.single().title)
            assertEquals(replacements, player.replacements)
        }
    }

    @Test fun exploreAndLibraryShareTheRefreshButSearchHasNoRefreshActionOrPuck() {
        val reads = AtomicInteger()
        val gate = CompletableDeferred<List<LocalSong>>()
        start(LocalLibrarySource { if (reads.incrementAndGet() == 1) listOf(song) else gate.await() })
        compose.runOnIdle { navigator.selectTab(AppTab.EXPLORE) }
        refreshAction(AppTab.EXPLORE)
        compose.onNodeWithTag("library-refresh-puck").assertIsDisplayed()
        compose.onNodeWithTag("genre:tag:rock").assertIsDisplayed()
        compose.runOnIdle { navigator.selectTab(AppTab.LIBRARY) }
        compose.onNodeWithTag("library-page").assertIsDisplayed()
        compose.onNodeWithTag("library-refresh-puck").assertIsDisplayed()
        refreshAction(AppTab.LIBRARY, expected = false)
        compose.runOnIdle { assertEquals(2, reads.get()); navigator.selectTab(AppTab.SEARCH) }
        compose.onNodeWithTag("library-refresh-puck").assertDoesNotExist()
        assertFalse(compose.onNodeWithTag("feed-refresh:SEARCH").fetchSemanticsNode().config.contains(SemanticsActions.CustomActions))
        compose.runOnIdle { gate.complete(listOf(song)); navigator.selectTab(AppTab.LIBRARY) }
        compose.onNodeWithTag("library-refresh-puck").assertDoesNotExist()
        // This tab's action starts a real subsequent read, not merely a selected visual state.
        refreshAction(AppTab.LIBRARY)
        compose.waitUntil(5_000) { reads.get() == 3 }
    }

    @Test fun emptyRefreshShowsLoadingThenScannerFailureOffersTheExistingRetry() {
        val reads = AtomicInteger()
        val gate = CompletableDeferred<List<LocalSong>>()
        start(LocalLibrarySource {
            when (reads.incrementAndGet()) { 1 -> emptyList(); 2 -> gate.await(); else -> listOf(song) }
        })
        compose.onNodeWithText(app.getString(R.string.home_refresh)).assertIsDisplayed()
        refreshAction(AppTab.HOME)
        compose.onNodeWithText(app.getString(R.string.home_loading)).assertIsDisplayed()
        compose.onNodeWithText(app.getString(R.string.home_refresh)).assertDoesNotExist()
        compose.runOnIdle { gate.completeExceptionally(IOException("Fixture scan error")) }
        compose.onNodeWithText("Fixture scan error").assertIsDisplayed()
        compose.onNodeWithText(app.getString(R.string.home_retry)).performClick()
        compose.onNodeWithTag("local-home").assertIsDisplayed()
        compose.runOnIdle { assertEquals(3, reads.get()); assertFalse(library.uiState.value.isRefreshing) }
    }

    @Test fun revokedPermissionUsesThePermissionActionAndCanRecoverWithoutLosingFavorites() {
        val reads = AtomicInteger()
        start(LocalLibrarySource { reads.incrementAndGet(); listOf(song) })
        compose.runOnIdle {
            app.userLibrary.toggleFavorite(song.id)
            shadowOf(app).denyPermissions(Manifest.permission.READ_MEDIA_AUDIO)
        }
        refreshAction(AppTab.HOME)
        compose.onNodeWithText(app.getString(R.string.home_grant_permission)).performScrollTo().performClick()
        compose.runOnIdle {
            assertEquals(1, permissionRequests)
            assertEquals(1, reads.get())
            assertTrue((library.uiState.value.libraryState as LibraryState.Error).permissionRequired)
            assertEquals(setOf(song.id), app.userLibrary.state.value.favorites)
            shadowOf(app).grantPermissions(Manifest.permission.READ_MEDIA_AUDIO)
        }
        refreshAction(AppTab.HOME)
        compose.onNodeWithTag("local-home").assertIsDisplayed()
        compose.runOnIdle { assertEquals(2, reads.get()); assertEquals(setOf(song.id), app.userLibrary.state.value.favorites) }
    }

    @Test fun sourcePuckAppearsBelowTheBarWithoutChangingItsHeightInEitherTheme() {
        var fraction by mutableFloatStateOf(0f)
        var refreshing by mutableStateOf(false)
        var dark by mutableStateOf(false)
        compose.setContent { AurelisTheme(darkTheme = dark) {
            FrostedTopBar("Home", { false }, Modifier.testTag("test-header"),
                refreshing = refreshing, pullFraction = { fraction })
        } }
        val initialHeight = compose.onNodeWithTag("test-header").fetchSemanticsNode().boundsInRoot.height
        for (night in listOf(false, true)) {
            compose.runOnIdle { dark = night; fraction = 0.5f }
            compose.onNodeWithTag("library-refresh-puck").assertIsDisplayed()
            compose.runOnIdle { fraction = 1f; refreshing = true }
            compose.onNodeWithTag("library-refresh-puck").assertWidthIsEqualTo(40.dp).assertHeightIsEqualTo(40.dp)
            compose.onNode(SemanticsMatcher.expectValue(SemanticsProperties.ProgressBarRangeInfo, ProgressBarRangeInfo.Indeterminate)).assertExists()
            assertEquals(initialHeight, compose.onNodeWithTag("test-header").fetchSemanticsNode().boundsInRoot.height, 0.5f)
            compose.runOnIdle { refreshing = false; fraction = 0f }
            compose.onNodeWithTag("library-refresh-puck").assertDoesNotExist()
        }
    }
}
