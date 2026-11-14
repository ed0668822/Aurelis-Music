// Aurelis © 2026. GPL-3.0-or-later
package com.music.aurelis.ui

import android.graphics.Bitmap
import android.graphics.Canvas
import android.content.SharedPreferences
import android.os.VibrationAttributes
import android.os.VibratorManager
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.hapticfeedback.HapticFeedback
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.unit.dp
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.semantics.SemanticsActions
import com.music.aurelis.AurelisApplication
import com.music.aurelis.data.SettingsRepository
import com.music.aurelis.data.EqualizerMode
import com.music.aurelis.data.LocalProfileState
import com.music.aurelis.data.ProfileDataArea
import com.music.aurelis.data.ProfileRepository
import com.music.aurelis.data.ProfileStorage
import com.music.aurelis.playback.EqualizerPreset
import com.music.aurelis.ui.screens.SettingsScreen
import com.music.aurelis.ui.screens.EqualizerScreen
import com.music.aurelis.ui.theme.AurelisTheme
import com.music.aurelis.ui.theme.LocalReduceMotion
import com.music.aurelis.ui.haptics.Haptic
import com.music.aurelis.ui.haptics.Haptics
import com.music.aurelis.ui.haptics.rememberHaptics
import kotlinx.coroutines.flow.MutableStateFlow
import java.io.File
import org.junit.*
import org.junit.Assert.*
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.Shadows
import org.robolectric.shadows.ShadowVibrator
import org.robolectric.util.ReflectionHelpers
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33], qualifiers = "w360dp-h800dp-mdpi", application = AurelisApplication::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class SettingsIntegrationTest {
    @get:org.junit.Rule(order = 0) val registerActivity = com.music.aurelis.ui.RobolectricActivityRule()
    @get:Rule(order = 1) val compose = createAndroidComposeRule<ComponentActivity>()

    @Test fun hapticPreferenceControlsExistingHandlesAndPersistsFromTheActualScreen() {
        val app = RuntimeEnvironment.getApplication()
        val resolver = app.contentResolver
        val originalSystem = Settings.System.getString(resolver, Settings.System.HAPTIC_FEEDBACK_ENABLED)
        val originalApp = SettingsRepository.hapticFeedback.value
        val vibrator = checkNotNull(app.getSystemService(VibratorManager::class.java)).defaultVibrator
        val shadow = Shadows.shadowOf(vibrator)
        val originalHardware = vibrator.hasVibrator()
        val events = mutableListOf<HapticFeedbackType>()
        val platform = object : HapticFeedback {
            override fun performHapticFeedback(hapticFeedbackType: HapticFeedbackType) {
                events += hapticFeedbackType
            }
        }
        lateinit var nativeHandle: HapticFeedback
        lateinit var customHandle: Haptics
        ShadowVibrator.reset()
        Settings.System.putInt(resolver, Settings.System.HAPTIC_FEEDBACK_ENABLED, 1)
        SettingsRepository.setHapticFeedback(true)
        try {
            compose.setContent {
                CompositionLocalProvider(LocalHapticFeedback provides platform) {
                    AurelisTheme {
                        nativeHandle = LocalHapticFeedback.current
                        customHandle = rememberHaptics()
                        SettingsScreen(true, {})
                    }
                }
            }
            val originalHandles = compose.runOnIdle {
                nativeHandle.performHapticFeedback(HapticFeedbackType.LongPress)
                customHandle.play(Haptic.Tap)
                assertEquals(listOf(HapticFeedbackType.LongPress), events)
                assertNotNull(shadow.vibrationAttributesFromLastVibration)
                events.clear(); ShadowVibrator.reset()
                nativeHandle to customHandle
            }
            compose.onNodeWithContentDescription("Respuesta háptica").performScrollTo()
                .assertHeightIsAtLeast(48.dp).assertIsOn().performClick().assertIsOff()
            compose.runOnIdle {
                nativeHandle.performHapticFeedback(HapticFeedbackType.LongPress)
                customHandle.play(Haptic.Tap)
                assertTrue(events.isEmpty()); assertNull(shadow.vibrationAttributesFromLastVibration)
                SettingsRepository.init(app)
                assertFalse(SettingsRepository.hapticFeedback.value)
                Settings.System.putInt(resolver, Settings.System.HAPTIC_FEEDBACK_ENABLED, 0)
            }
            compose.onNodeWithContentDescription("Respuesta háptica").assertIsOff().performClick().assertIsOn()
            compose.runOnIdle {
                nativeHandle.performHapticFeedback(HapticFeedbackType.LongPress)
                customHandle.play(Haptic.Tap)
                assertTrue(events.isEmpty()); assertNull(shadow.vibrationAttributesFromLastVibration)
                assertSame(originalHandles.first, nativeHandle)
                assertSame(originalHandles.second, customHandle)
                Settings.System.putInt(resolver, Settings.System.HAPTIC_FEEDBACK_ENABLED, 1)
                nativeHandle.performHapticFeedback(HapticFeedbackType.LongPress)
                customHandle.play(Haptic.Tap)
                assertEquals(listOf(HapticFeedbackType.LongPress), events)
                assertEquals(VibrationAttributes.USAGE_TOUCH,
                    (shadow.vibrationAttributesFromLastVibration as VibrationAttributes).usage)
            }
        } finally {
            vibrator.cancel(); ShadowVibrator.reset(); shadow.setHasVibrator(originalHardware)
            Settings.System.putString(resolver, Settings.System.HAPTIC_FEEDBACK_ENABLED, originalSystem)
            SettingsRepository.setHapticFeedback(originalApp)
        }
    }

    @Test fun missingAndMalformedHapticPreferenceKeepCompatibilityWithoutOverwritingTheStoredValue() {
        val app = RuntimeEnvironment.getApplication()
        val prefs = ProfileStorage(app).open(ProfileRepository.profileId, ProfileDataArea.PREFERENCES)
        prefs.edit().remove("haptic_feedback").commit()
        SettingsRepository.init(app)
        assertTrue(SettingsRepository.hapticFeedback.value)
        assertFalse(prefs.contains("haptic_feedback"))
        SettingsRepository.setHapticFeedback(false)
        SettingsRepository.hapticFeedback.value = true
        SettingsRepository.init(app)
        assertFalse(SettingsRepository.hapticFeedback.value)
        assertFalse(prefs.getBoolean("haptic_feedback", true))
        prefs.edit().putString("haptic_feedback", "future-format").commit()
        SettingsRepository.init(app)
        assertTrue(SettingsRepository.hapticFeedback.value)
        assertEquals("future-format", prefs.getString("haptic_feedback", null))
    }

    @Test fun readOnlyProfileCannotChangeHapticPreferenceOrExposeItsToggle() {
        val profile = ReflectionHelpers.getField<MutableStateFlow<LocalProfileState>>(ProfileRepository, "mutableState")
        val original = profile.value
        val oldPreference = SettingsRepository.hapticFeedback.value
        val prefs = ProfileStorage(RuntimeEnvironment.getApplication()).open(ProfileRepository.profileId, ProfileDataArea.PREFERENCES)
        val storedBefore = prefs.all.toMap()
        try {
            profile.value = original.copy(readOnly = true)
            compose.setContent { AurelisTheme { SettingsScreen(true, {}) } }
            compose.onNodeWithText("Perfil en modo de solo lectura").assertIsDisplayed()
            compose.onNodeWithContentDescription("Respuesta háptica").assertDoesNotExist()
            compose.runOnIdle {
                assertThrows(IllegalStateException::class.java) { SettingsRepository.setHapticFeedback(!oldPreference) }
                assertEquals(oldPreference, SettingsRepository.hapticFeedback.value)
                assertEquals(storedBefore, prefs.all)
            }
        } finally {
            profile.value = original
        }
    }

    @Test fun failedHapticWriteKeepsThePreferenceAndShowsTheSaveError() {
        val originalPrefs = ReflectionHelpers.getField<SharedPreferences>(SettingsRepository, "prefs")
        val originalValue = SettingsRepository.hapticFeedback.value
        val storedBefore = originalPrefs.all.toMap()
        val failingPrefs = object : SharedPreferences by originalPrefs {
            override fun edit(): SharedPreferences.Editor = object : SharedPreferences.Editor by originalPrefs.edit() {
                override fun putBoolean(key: String?, value: Boolean): SharedPreferences.Editor = this
                override fun commit(): Boolean = false
            }
        }
        try {
            ReflectionHelpers.setField(SettingsRepository, "prefs", failingPrefs)
            compose.setContent { AurelisTheme { SettingsScreen(true, {}) } }
            compose.onNodeWithContentDescription("Respuesta háptica").performScrollTo().performClick()
            compose.runOnIdle {
                assertEquals(originalValue, SettingsRepository.hapticFeedback.value)
                assertEquals(storedBefore, originalPrefs.all)
            }
            compose.onNodeWithText("No se pudo guardar la respuesta háptica.").performScrollTo().assertIsDisplayed()
        } finally {
            ReflectionHelpers.setField(SettingsRepository, "prefs", originalPrefs)
        }
    }

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
