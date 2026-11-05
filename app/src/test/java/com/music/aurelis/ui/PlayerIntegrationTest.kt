package com.music.aurelis.ui

import android.Manifest
import org.robolectric.Shadows.shadowOf
import android.graphics.Bitmap
import android.graphics.Canvas
import androidx.activity.ComponentActivity
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.lifecycle.ViewModelStore
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import com.music.aurelis.*
import com.music.aurelis.data.*
import com.music.aurelis.data.model.LocalSong
import com.music.aurelis.playback.*
import com.music.aurelis.ui.navigation.*
import com.music.aurelis.ui.theme.AurelisTheme
import java.io.File
import org.junit.*
import org.junit.Assert.*
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

@UnstableApi
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33], qualifiers = "w360dp-h800dp-mdpi", application = AurelisApplication::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class PlayerIntegrationTest {
    @get:Rule val compose = createAndroidComposeRule<ComponentActivity>()
    private val app get() = RuntimeEnvironment.getApplication() as AurelisApplication
    private val store = ViewModelStore()
    private lateinit var vm: PlaybackViewModel
    private lateinit var player: RecordingPlayer
    private fun song(id: String) = LocalSong(id, "Canción local $id", "Artista local", "Álbum local",
        "content://media/external/audio/media/$id", null, 60_000, null, 1, 2026)
    @After fun cleanup() { compose.runOnIdle { store.clear() } }
    private fun start() {
        SettingsRepository.setReduceAnimation(true)
        SettingsRepository.lyricsSources.value = emptySet()
        player = RecordingPlayer()
        vm = PlaybackViewModel(app) { player }; store.put("player", vm)
        shadowOf(app).grantPermissions(Manifest.permission.READ_MEDIA_AUDIO)
        val library = LibraryViewModel(app, LocalLibrarySource { listOf(song("1"), song("2"), song("3")) }); store.put("library", library)
        compose.setContent { AurelisTheme {
            AurelisNavigationContent(rememberAppNavigator(), vm, library)
        } }
        compose.runOnIdle { library.loadLibrary(); vm.playSongs(listOf(song("1"), song("2"), song("3"))) }
        compose.waitForIdle()
        compose.runOnIdle { player.pause() }
    }
    @Test fun miniPlayerOpensRealControlsAndQueueActionsStaySynchronized() {
        start()
        compose.onNodeWithTag("mini-player").assertIsDisplayed()
        compose.onNodeWithContentDescription("Abrir reproductor").assertIsDisplayed().performClick()
        compose.onNodeWithContentDescription("Cerrar reproductor").assertIsDisplayed()
        compose.onNodeWithContentDescription("Aleatorio").performScrollTo().performClick()
        compose.runOnIdle { assertTrue(player.shuffleModeEnabled) }
        compose.onNodeWithContentDescription("Repetición").performScrollTo().performClick()
        compose.runOnIdle { assertEquals(Player.REPEAT_MODE_ALL, player.repeatMode) }
        compose.onNodeWithContentDescription("Posición de reproducción").performScrollTo()
            .performSemanticsAction(androidx.compose.ui.semantics.SemanticsActions.SetProgress) { it(0.5f) }
        compose.runOnIdle { assertEquals(30_000, vm.state.value.positionMs) }
        compose.onNodeWithContentDescription("Abrir cola").performScrollTo().performClick()
        compose.onNodeWithText("Canción local 2").performClick()
        compose.runOnIdle { assertEquals("2", vm.state.value.currentSong?.id); player.pause() }
        compose.onAllNodesWithContentDescription("Quitar de la cola")[0].performClick()
        compose.runOnIdle { assertEquals(listOf("2", "3"), vm.state.value.queue.map { it.id }) }
        compose.onNodeWithContentDescription("Cerrar").performClick()
        compose.onNodeWithContentDescription("Cerrar reproductor").performScrollTo().performClick()
        compose.onNodeWithTag("mini-player").assertIsDisplayed()
        compose.onNodeWithTag("mini-player").assertTextContains("Canción local 2")
    }
    @Test fun playerHasUsableArtworkTimeControlsAndCompactBarAt360dp() {
        start()
        capture("mini-player")
        compose.onNodeWithContentDescription("Abrir reproductor").performClick()
        compose.onNodeWithContentDescription("Repetición").performScrollTo().assertIsDisplayed()
        compose.onNodeWithText("1:00").assertIsDisplayed()
        compose.onNodeWithContentDescription("Siguiente").performScrollTo().assertIsDisplayed()
        capture("now-playing")
    }
    @Test
    @Config(qualifiers = "w640dp-h360dp-mdpi")
    fun landscapeKeepsTransportAccessibleByScrolling() {
        start()
        compose.onNodeWithContentDescription("Abrir reproductor").performClick()
        compose.onNodeWithContentDescription("Repetición").performScrollTo().assertIsDisplayed()
        compose.onNodeWithContentDescription("Siguiente").performScrollTo().performClick()
        compose.runOnIdle { assertEquals("2", vm.state.value.currentSong?.id) }
        compose.onNodeWithContentDescription("Cerrar reproductor").performScrollTo().performClick()
        compose.onNodeWithTag("mini-player").assertIsDisplayed()
    }
    private fun capture(name: String) {
        val bitmap = compose.runOnIdle {
            val view = compose.activity.window.decorView
            Bitmap.createBitmap(view.width, view.height, Bitmap.Config.ARGB_8888).also { view.draw(Canvas(it)) }
        }
        val file = File("build/reports/b07-visual/$name.png")
        file.parentFile!!.mkdirs()
        file.outputStream().use { assertTrue(bitmap.compress(Bitmap.CompressFormat.PNG, 100, it)) }
    }
}
