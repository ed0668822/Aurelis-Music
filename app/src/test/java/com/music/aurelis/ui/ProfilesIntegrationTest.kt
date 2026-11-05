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
    @Test fun settingsOnlyEditsOneLocalProfileAndPreservesItsIdentity() {
        val id = ProfileRepository.profileId
        compose.setContent { AurelisTheme { SettingsScreen(true, {}) } }
        compose.onNodeWithText("Crear perfil").assertDoesNotExist()
        compose.onNodeWithText("Perfiles locales").assertDoesNotExist()
        compose.onNodeWithText("Nombre").performScrollTo().performClick()
        compose.onNode(hasSetTextAction()).performTextReplacement("Mi nombre")
        compose.onNodeWithText("Guardar").performClick()
        compose.onAllNodesWithText("Mi nombre").onFirst().assertIsDisplayed()
        compose.runOnIdle { assertEquals(id, ProfileRepository.profileId); assertEquals("Mi nombre", ProfileRepository.userName) }
        compose.onNodeWithText("Eliminar perfil Mi nombre").assertDoesNotExist()
    }
    @Test fun removingPhotoDoesNotRemoveProfileOrItsName() {
        compose.runOnIdle { ProfileRepository.userPhotoUri = "content://legacy/photo"; ProfileRepository.rename("Mi perfil") }
        compose.setContent { AurelisTheme { SettingsScreen(true, {}) } }
        compose.onNodeWithText("Quitar foto").performClick()
        compose.runOnIdle { assertNull(ProfileRepository.userPhotoUri); assertEquals("Mi perfil", ProfileRepository.userName) }
        compose.onAllNodesWithText("Mi perfil").onFirst().assertIsDisplayed()
        compose.onNodeWithText("Quitar foto").assertDoesNotExist()
    }
}
