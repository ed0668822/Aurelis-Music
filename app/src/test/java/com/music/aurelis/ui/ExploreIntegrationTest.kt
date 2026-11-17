// Aurelis © 2026. GPL-3.0-or-later
package com.music.aurelis.ui

import android.Manifest
import androidx.activity.ComponentActivity
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.lifecycle.ViewModelStore
import com.music.aurelis.*
import com.music.aurelis.R
import com.music.aurelis.data.*
import com.music.aurelis.data.model.LocalSong
import com.music.aurelis.playback.PlaybackViewModel
import com.music.aurelis.playback.RecordingPlayer
import com.music.aurelis.ui.navigation.*
import com.music.aurelis.ui.theme.AurelisTheme
import org.junit.*
import org.junit.Assert.*
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

@androidx.media3.common.util.UnstableApi
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33, 35], qualifiers = "en-rUS-w360dp-h800dp-mdpi", application = AurelisApplication::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class ExploreIntegrationTest {
    @get:Rule(order = 0) val activity = RobolectricActivityRule()
    @get:Rule(order = 1) val compose = createAndroidComposeRule<ComponentActivity>()
    private val app get() = RuntimeEnvironment.getApplication() as AurelisApplication
    private val models = ViewModelStore()
    @After fun close() { compose.runOnIdle { models.clear() } }

    @Test fun metadataGenreOpensOnlyItsCurrentLocalSongsAndBackReturnsToExplore() {
        val songs = listOf("Róck", " rock ", null).mapIndexed { i, genre ->
            LocalSong("$i", "Local track $i", "Artist", null, "content://media/$i", null,
                60_000L, null, i, 2026, genre = genre)
        }
        shadowOf(app).grantPermissions(Manifest.permission.READ_MEDIA_AUDIO)
        val library = LibraryViewModel(app, LocalLibrarySource { songs })
        val playback = PlaybackViewModel(app) { RecordingPlayer() }
        models.put("library", library); models.put("player", playback)
        lateinit var navigator: AppNavigator
        compose.setContent { AurelisTheme {
            navigator = rememberAppNavigator()
            AurelisNavigationContent(navigator, playback, library)
        } }
        compose.runOnIdle { library.loadLibrary() }
        compose.onNode(hasText(compose.activity.getString(R.string.explore)) and hasClickAction()).performClick()
        compose.onNodeWithTag("genre:tag:rock").performClick()
        compose.runOnIdle { assertEquals(AppRoute.Collection(LocalCollectionKind.GENRE, "tag:rock"), navigator.visibleRoute()) }
        compose.onNodeWithText("Local track 0").assertIsDisplayed()
        compose.onNodeWithText("Local track 1").assertIsDisplayed()
        compose.onNodeWithText("Local track 2").assertDoesNotExist()
        compose.runOnIdle { navigator.back() }
        compose.onNodeWithTag("genre:unknown").performClick()
        compose.onNodeWithText("Unclassified").assertIsDisplayed()
        compose.onNodeWithText("Local track 2").assertIsDisplayed()
        compose.onNodeWithText("Local track 0").assertDoesNotExist()
        compose.runOnIdle { assertEquals(AppRoute.Collection(LocalCollectionKind.GENRE, "unknown"), navigator.visibleRoute()) }
    }
}
