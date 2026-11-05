package com.music.aurelis.ui

import androidx.activity.ComponentActivity
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import com.music.aurelis.AurelisApplication
import com.music.aurelis.data.ProfileRepository
import com.music.aurelis.ui.screens.SettingsScreen
import com.music.aurelis.ui.theme.AurelisTheme
import org.junit.*
import org.junit.Assert.*
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33], qualifiers = "w360dp-h800dp-mdpi", application = AurelisApplication::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class ProfilesIntegrationTest {
    @get:Rule(order = 0) val registerActivity = RobolectricActivityRule()
    @get:Rule(order = 1) val compose = createAndroidComposeRule<ComponentActivity>()
    @Test fun createSwitchRenameAndConfirmedDeletionWorkThroughSettings() {
        compose.setContent { AurelisTheme { SettingsScreen(true, {}) } }
        compose.onNodeWithText("Crear perfil").performScrollTo().performClick()
        compose.onNode(hasSetTextAction()).performTextInput("Segundo")
        compose.onNodeWithText("Guardar").performClick()
        compose.runOnIdle { assertEquals(2, ProfileRepository.state.value.profiles.size) }
        compose.onNodeWithText("Segundo · Cambiar").performScrollTo().performClick()
        compose.runOnIdle { assertEquals("Segundo", ProfileRepository.userName) }
        compose.onNodeWithText("Nombre").performScrollTo().performClick()
        compose.onNode(hasSetTextAction()).performTextReplacement("Nuevo")
        compose.onNodeWithText("Guardar").performClick()
        compose.onNodeWithContentDescription("Eliminar perfil Nuevo").performScrollTo().performClick()
        compose.onNodeWithText("Cancelar").performClick()
        compose.runOnIdle { assertEquals(2, ProfileRepository.state.value.profiles.size) }
        compose.onNodeWithContentDescription("Eliminar perfil Nuevo").performScrollTo().performClick()
        compose.onNodeWithText("Eliminar", useUnmergedTree = true).performClick()
        compose.runOnIdle { assertEquals(1, ProfileRepository.state.value.profiles.size); assertEquals("Aurelis User", ProfileRepository.userName) }
    }
}
