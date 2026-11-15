// Aurelis © 2026. GPL-3.0-or-later
package com.music.aurelis.ui.navigation

import androidx.activity.ComponentActivity
import android.Manifest
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.StateRestorationTester
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.lifecycle.viewmodel.compose.viewModel
import com.music.aurelis.AurelisApplication
import com.music.aurelis.AurelisNavigationContent
import com.music.aurelis.playback.PlaybackViewModel
import com.music.aurelis.LibraryViewModel
import com.music.aurelis.ui.theme.AurelisTheme
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
@Config(sdk = [33], qualifiers = "es-rES-w360dp-h800dp-mdpi", application = AurelisApplication::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class NavigationIntegrationTest {
    @get:org.junit.Rule(order = 0) val registerActivity = com.music.aurelis.ui.RobolectricActivityRule()
    @get:Rule(order = 1) val compose = createAndroidComposeRule<ComponentActivity>()
    private lateinit var navigator: AppNavigator

    private fun startApp(restoration: StateRestorationTester = StateRestorationTester(compose)): StateRestorationTester {
        restoration.setContent {
            navigator = rememberAppNavigator()
            AurelisTheme {
                // Same UI graph as MainActivity, without updater, permission prompts or service startup.
                AurelisNavigationContent(navigator, viewModel<PlaybackViewModel>(), viewModel<LibraryViewModel>())
            }
        }
        return restoration
    }

    private fun tab(label: String) = compose.onNode(isSelectable() and (hasText(label) or hasContentDescription(label)))
    private fun back() = compose.runOnIdle { compose.activity.onBackPressedDispatcher.onBackPressed() }

    @Test fun libraryHeadingClearsTheProfileHeaderAndProfileOpensLocalSettings() {
        startApp()
        tab("Biblioteca").performClick()
        val heading = compose.onNode(hasText("Biblioteca") and !isSelectable()).fetchSemanticsNode().boundsInRoot
        val profile = compose.onNodeWithContentDescription("Perfil").fetchSemanticsNode().boundsInRoot
        assertTrue("Library must start below the shared header", heading.top >= profile.bottom)
        compose.onNodeWithContentDescription("Perfil").performClick()
        compose.runOnIdle { assertEquals(AppRoute.Settings, navigator.visibleRoute()) }
        // SettingsGroup renders section headers in uppercase with Locale.ROOT.
        compose.onNodeWithText("PERFIL DEL DISPOSITIVO").assertIsDisplayed()
        tab("Biblioteca").assertIsDisplayed()
        tab("Inicio").performClick().assertIsSelected()
        compose.runOnIdle { assertFalse(navigator.canGoBack) }
    }

    @Test fun realLibraryLinksAndNestedSettingsRespectAndroidBackAndRestore() {
        val restoration = startApp()
        tab("Biblioteca").performClick().assertIsSelected()
        compose.onNodeWithText("Ajustes").performClick()
        tab("Biblioteca").assertIsDisplayed().assertIsSelected()
        compose.onNodeWithText("Reproducción").performScrollTo().performClick()
        compose.onNodeWithText("Equalizer").performScrollTo().performClick()
        compose.onNodeWithText("Audio Equalizer & DSP").assertIsDisplayed()
        restoration.emulateSavedInstanceStateRestore()
        compose.onNodeWithText("Audio Equalizer & DSP").assertIsDisplayed()
        back()
        // Settings scroll state survives both the child page and saved-state recreation.
        compose.onNodeWithText("Equalizer").assertIsDisplayed()
        compose.runOnIdle { assertEquals(AppRoute.SettingsSection(SettingsCategory.AUDIO), navigator.visibleRoute()) }
        back()
        compose.runOnIdle { assertEquals(AppRoute.Settings, navigator.visibleRoute()) }
        back()
        tab("Biblioteca").assertIsSelected()
        compose.onNodeWithText("Replay").performClick()
        compose.onNodeWithText("Replay & Statistics").assertIsDisplayed()
        back()
        tab("Biblioteca").assertIsSelected()
        back()
        tab("Inicio").assertIsSelected()
        compose.runOnIdle { assertFalse(navigator.canGoBack) }
    }

    @Test fun searchTextSurvivesTabChangesPagesAndSavedStateRecreation() {
        val restoration = startApp()
        tab("Buscar").performClick()
        compose.onNode(hasSetTextAction()).performTextInput("mi música local")
        tab("Biblioteca").performClick()
        compose.onNodeWithText("Ajustes").performClick()
        restoration.emulateSavedInstanceStateRestore()
        back()
        tab("Biblioteca").assertIsSelected()
        tab("Buscar").performClick()
        compose.onNode(hasSetTextAction()).assertTextEquals("mi música local")
        restoration.emulateSavedInstanceStateRestore()
        tab("Buscar").assertIsSelected()
        compose.onNode(hasSetTextAction()).assertTextEquals("mi música local")
    }

    @Test fun localMusicSearchOwnsBackBeforeItsParentPage() {
        shadowOf(RuntimeEnvironment.getApplication()).denyPermissions(Manifest.permission.READ_MEDIA_AUDIO)
        val restoration = startApp()
        tab("Biblioteca").performClick()
        compose.onNodeWithText("Canciones").performClick()
        compose.runOnIdle { assertEquals(AppRoute.LocalMusic, navigator.visibleRoute()) }
        compose.onNodeWithText("Permiso de acceso a música no concedido. " +
            "Concédelo en Ajustes para escanear el dispositivo.").assertIsDisplayed()
        compose.onNodeWithContentDescription("Buscar en esta lista").performClick()
        compose.onNode(hasSetTextAction()).performTextInput("audio local")
        restoration.emulateSavedInstanceStateRestore()
        compose.onNode(hasSetTextAction()).assertTextEquals("audio local")
        back()
        compose.onNode(hasSetTextAction()).assertDoesNotExist()
        compose.runOnIdle { assertEquals(AppRoute.LocalMusic, navigator.visibleRoute()) }
        back()
        tab("Biblioteca").assertIsSelected()
    }

    @Test fun restoredPlayerWithoutMediaCanReturnFromQueueWithoutATrappedBlankScreen() {
        val restoration = startApp()
        tab("Buscar").performClick()
        compose.runOnIdle { navigator.pushAll(AppRoute.NowPlaying, AppRoute.Queue) }
        restoration.emulateSavedInstanceStateRestore()
        compose.onNodeWithText("Cola de reproducción").assertIsDisplayed()
        back()
        compose.onNodeWithText("No hay ninguna canción seleccionada.").assertIsDisplayed()
        compose.onNodeWithText("Atrás").performClick()
        tab("Buscar").assertIsSelected()
    }
}
