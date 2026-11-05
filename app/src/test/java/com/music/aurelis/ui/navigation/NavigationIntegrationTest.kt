package com.music.aurelis.ui.navigation

import androidx.activity.ComponentActivity
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
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33], qualifiers = "w360dp-h800dp-mdpi", application = AurelisApplication::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class NavigationIntegrationTest {
    @get:Rule val compose = createAndroidComposeRule<ComponentActivity>()
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

    private fun tab(label: String) = compose.onNode(isSelectable() and hasText(label))
    private fun back() = compose.runOnIdle { compose.activity.onBackPressedDispatcher.onBackPressed() }

    @Test fun realLibraryLinksAndNestedSettingsRespectAndroidBackAndRestore() {
        val restoration = startApp()
        tab("Biblioteca").performClick().assertIsSelected()
        compose.onNodeWithText("Ajustes").performClick()
        tab("Biblioteca").assertDoesNotExist()
        compose.onNodeWithText("Equalizer").performScrollTo().performClick()
        compose.onNodeWithText("Audio Equalizer & DSP").assertIsDisplayed()
        restoration.emulateSavedInstanceStateRestore()
        compose.onNodeWithText("Audio Equalizer & DSP").assertIsDisplayed()
        back()
        // Settings scroll state survives both the child page and saved-state recreation.
        compose.onNodeWithText("Equalizer").assertIsDisplayed()
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
        val restoration = startApp()
        tab("Biblioteca").performClick()
        compose.onNodeWithText("Canciones").performClick()
        compose.runOnIdle { assertEquals(AppRoute.LocalMusic, navigator.visibleRoute()) }
        compose.onNodeWithContentDescription("Buscar").performClick()
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
