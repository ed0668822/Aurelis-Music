package com.music.aurelis.ui

import android.graphics.Bitmap
import android.graphics.Canvas
import androidx.activity.ComponentActivity
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.semantics.SemanticsActions
import com.music.aurelis.AurelisApplication
import com.music.aurelis.data.SettingsRepository
import com.music.aurelis.data.EqualizerMode
import com.music.aurelis.playback.EqualizerPreset
import com.music.aurelis.ui.screens.SettingsScreen
import com.music.aurelis.ui.screens.EqualizerScreen
import com.music.aurelis.ui.theme.AurelisTheme
import com.music.aurelis.ui.theme.LocalReduceMotion
import java.io.File
import org.junit.*
import org.junit.Assert.*
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33], qualifiers = "w360dp-h800dp-mdpi", application = AurelisApplication::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class SettingsIntegrationTest {
    @get:org.junit.Rule(order = 0) val registerActivity = com.music.aurelis.ui.RobolectricActivityRule()
    @get:Rule(order = 1) val compose = createAndroidComposeRule<ComponentActivity>()
    @Test fun fontSelectorChangesTheActualThemeAndSurvivesReinitialization() {
        var family: androidx.compose.ui.text.font.FontFamily? = null
        compose.setContent { AurelisTheme {
            val selected = androidx.compose.material3.MaterialTheme.typography.bodyLarge.fontFamily
            SideEffect { family = selected }
            SettingsScreen(true, {})
        } }
        compose.onNode(isSelectable() and hasText("Vazirmatn")).performScrollTo().performClick()
        compose.runOnIdle {
            assertEquals(com.music.aurelis.ui.theme.VazirmatnFamily, family)
            SettingsRepository.init(RuntimeEnvironment.getApplication())
            assertEquals(com.music.aurelis.data.AppFont.VAZIRMATN, SettingsRepository.appFont.value)
        }
        compose.onAllNodes(isSelectable() and hasText("Sistema")).onFirst().performScrollTo().performClick()
        compose.runOnIdle { assertEquals(com.music.aurelis.ui.theme.AurelisFontFamily, family) }
    }

    @Test fun appearanceSettingsDriveThemeProvidersAndPersist() {
        var glass = false; var motion = false
        compose.setContent { AurelisTheme {
            val resolvedGlass = com.music.aurelis.ui.components.LocalLiquidGlassEnabled.current
            val resolvedMotion = LocalReduceMotion.current
            SideEffect { glass = resolvedGlass; motion = resolvedMotion }
            SettingsScreen(true, {})
        } }
        compose.runOnIdle { assertTrue(glass) }
        compose.onNodeWithContentDescription("Reducir desenfoque").performScrollTo().performClick()
        compose.onNodeWithContentDescription("Reducir animaciones").performScrollTo().performClick()
        compose.runOnIdle { assertFalse(glass); assertTrue(motion) }
        capture("settings-light")
        compose.onNode(isSelectable() and hasText("Oscuro")).performScrollTo().performClick()
        capture("settings-dark")
        compose.runOnIdle { SettingsRepository.init(RuntimeEnvironment.getApplication()); assertEquals(2, SettingsRepository.themeMode.value); assertTrue(SettingsRepository.reduceDynamicBlur.value) }
    }
    @Test fun equalizerControlsSaveActualBandsBalanceAndPresets() {
        compose.setContent { AurelisTheme { EqualizerScreen({}) } }
        compose.onNodeWithContentDescription("Activar ecualizador").performClick()
        compose.onNode(isSelectable() and hasText("7 bandas")).performClick()
        compose.onNodeWithContentDescription("60 Hz").performScrollTo().performSemanticsAction(SemanticsActions.SetProgress) { it(6f) }
        compose.runOnIdle { assertEquals(EqualizerMode.MANUAL, SettingsRepository.equalizerMode.value); assertEquals(6f, SettingsRepository.equalizerBands.value[0], 0f) }
        compose.onNodeWithText("Graves").performScrollTo().performClick()
        compose.onNodeWithContentDescription("Balance").performScrollTo().performSemanticsAction(SemanticsActions.SetProgress) { it(1f) }
        compose.runOnIdle {
            SettingsRepository.init(RuntimeEnvironment.getApplication())
            assertEquals(EqualizerPreset.BASS_BOOST.bands, SettingsRepository.equalizerBands.value)
            assertEquals(1f, SettingsRepository.equalizerBalance.value, 0f)
        }
        capture("equalizer")
    }
    @Test fun savingTranslationSettingsDoesNotRequestATranslation() {
        compose.setContent { AurelisTheme { SettingsScreen(true, {}) } }
        compose.onNodeWithText("Traducción").performScrollTo().performClick()
        compose.onNode(hasSetTextAction()).performTextInput("https://translation.example")
        compose.onNodeWithText("Guardar").performClick()
        compose.runOnIdle { assertEquals("https://translation.example", SettingsRepository.translationEndpoint.value) }
        compose.onNodeWithText("Traducción").assertIsDisplayed()
    }
    private fun capture(name: String) {
        val bitmap = compose.runOnIdle { val view = compose.activity.window.decorView
            Bitmap.createBitmap(view.width, view.height, Bitmap.Config.ARGB_8888).also { view.draw(Canvas(it)) } }
        val file = File("build/reports/b13-visual/$name.png"); file.parentFile!!.mkdirs()
        file.outputStream().use { assertTrue(bitmap.compress(Bitmap.CompressFormat.PNG, 100, it)) }
    }
}
