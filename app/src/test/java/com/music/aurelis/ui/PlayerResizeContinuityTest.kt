// Aurelis © 2026. GPL-3.0-or-later
package com.music.aurelis.ui

import android.Manifest
import androidx.activity.ComponentActivity
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.StateRestorationTester
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModelStore
import androidx.media3.common.util.UnstableApi
import com.music.aurelis.AurelisApplication
import com.music.aurelis.AurelisNavigationContent
import com.music.aurelis.LibraryViewModel
import com.music.aurelis.data.LocalLibrarySource
import com.music.aurelis.data.SettingsRepository
import com.music.aurelis.data.model.LocalSong
import com.music.aurelis.playback.PlaybackViewModel
import com.music.aurelis.playback.RecordingPlayer
import com.music.aurelis.ui.navigation.rememberAppNavigator
import com.music.aurelis.ui.theme.AurelisTheme
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

@UnstableApi
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33], qualifiers = "w800dp-h800dp-mdpi", application = AurelisApplication::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class PlayerResizeContinuityTest {
    @get:Rule(order = 0) val registerActivity = RobolectricActivityRule()
    @get:Rule(order = 1) val compose = createAndroidComposeRule<ComponentActivity>()
    private val models = ViewModelStore()
    private var previousSettings: Pair<Boolean, Boolean>? = null
    private var previousHistory: Boolean? = null
    private var wide by mutableStateOf(true)
    private lateinit var vm: PlaybackViewModel
    private lateinit var player: RecordingPlayer

    @After fun cleanup() { compose.runOnIdle {
        models.clear()
        previousHistory?.let(SettingsRepository::setShowQueueHistory)
        previousSettings?.let { (motion, blur) ->
            SettingsRepository.setReduceAnimation(motion)
            SettingsRepository.setReduceDynamicBlur(blur)
        }
    } }

    private fun start(mesh: Boolean = false): StateRestorationTester {
        val app = RuntimeEnvironment.getApplication() as AurelisApplication
        previousSettings = SettingsRepository.reduceAnimation.value to SettingsRepository.reduceDynamicBlur.value
        SettingsRepository.setReduceAnimation(true)
        SettingsRepository.setReduceDynamicBlur(!mesh)
        previousHistory = SettingsRepository.showQueueHistory.value
        SettingsRepository.setShowQueueHistory(false)
        shadowOf(app).grantPermissions(Manifest.permission.READ_MEDIA_AUDIO)
        val rows = (1..40).map { id -> LocalSong("$id", "Tema $id", "Artista", "Álbum",
            "content://media/external/audio/media/$id", null, 60000, null, id, 2026) }
        player = RecordingPlayer()
        vm = PlaybackViewModel(app) { player }
        val library = LibraryViewModel(app, LocalLibrarySource { rows })
        models.put("player", vm); models.put("library", library)
        val restoration = StateRestorationTester(compose)
        restoration.setContent { AurelisTheme {
            Box(Modifier.size(if (wide) 800.dp else 360.dp, if (wide) 360.dp else 800.dp)) {
                AurelisNavigationContent(rememberAppNavigator(), vm, library)
            }
        } }
        compose.runOnIdle { library.loadLibrary(); vm.playSongs(rows) }
        compose.waitUntil(5000) { compose.runOnIdle { vm.state.value.isConnected && vm.state.value.queue.size == rows.size } }
        compose.runOnIdle { player.pause(); vm.seekTo(12345) }
        compose.onNodeWithContentDescription("Abrir reproductor").performClick()
        return restoration
    }

    @Test fun queueScrollAndPlaybackSurviveResizeAndSavedStateRestoration() {
        val restoration = start()
        compose.onNodeWithContentDescription("Abrir cola").performClick()
        compose.onNodeWithTag("landscape-queue").assertIsDisplayed()
        compose.onNode(hasScrollToIndexAction() and hasAnyAncestor(hasTestTag("playback-queue")))
            .performScrollToNode(hasText("Tema 25"))
        val queue = vm.state.value.queue.map { it.id to it.queueEntryId }
        val prepares = player.prepares; val replacements = player.replacements
        compose.runOnIdle { wide = false }
        compose.onNodeWithTag("landscape-queue").assertDoesNotExist()
        compose.onNodeWithContentDescription("Cerrar reproductor").assertDoesNotExist()
        compose.onNode(hasText("Tema 25") and hasAnyAncestor(hasTestTag("playback-queue"))).assertIsDisplayed()
        compose.runOnIdle { wide = true }
        compose.onNodeWithTag("landscape-queue").assertIsDisplayed()
        compose.onNode(hasText("Tema 25") and hasAnyAncestor(hasTestTag("playback-queue"))).assertIsDisplayed()
        restoration.emulateSavedInstanceStateRestore()
        compose.onNodeWithTag("landscape-queue").assertIsDisplayed()
        compose.onNode(hasText("Tema 25") and hasAnyAncestor(hasTestTag("playback-queue"))).assertIsDisplayed()
        compose.runOnIdle {
            assertEquals(queue, vm.state.value.queue.map { it.id to it.queueEntryId })
            assertEquals(12345L, vm.state.value.positionMs)
            assertFalse(player.playWhenReady)
            assertEquals(prepares, player.prepares); assertEquals(replacements, player.replacements)
        }
    }

    @Test fun lyricsPaneSurvivesEffectsChangesResizeAndRestorationWithoutRestartingPlayback() {
        val restoration = start(mesh = true)
        compose.onNodeWithContentDescription("Abrir letras").performClick()
        compose.onNodeWithTag("landscape-lyrics").assertIsDisplayed()
        compose.onNodeWithTag("player-mesh-background").assertExists()
        val queue = vm.state.value.queue.map { it.id to it.queueEntryId }
        val prepares = player.prepares; val replacements = player.replacements
        compose.runOnIdle {
            SettingsRepository.setReduceDynamicBlur(true)
            SettingsRepository.setReduceAnimation(false)
            wide = false
        }
        compose.onNodeWithTag("player-mesh-background").assertDoesNotExist()
        compose.onNodeWithTag("lyrics-overlay").assertIsDisplayed()
        compose.runOnIdle {
            SettingsRepository.setReduceAnimation(true)
            SettingsRepository.setReduceDynamicBlur(false)
            wide = true
        }
        restoration.emulateSavedInstanceStateRestore()
        compose.onNodeWithTag("landscape-lyrics").assertIsDisplayed()
        compose.onNodeWithTag("player-mesh-background").assertExists()
        compose.runOnIdle {
            assertEquals(queue, vm.state.value.queue.map { it.id to it.queueEntryId })
            assertEquals(12345L, vm.state.value.positionMs)
            assertFalse(player.playWhenReady)
            assertEquals(prepares, player.prepares); assertEquals(replacements, player.replacements)
        }
    }
}
