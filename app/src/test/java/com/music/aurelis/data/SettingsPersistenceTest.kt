package com.music.aurelis.data

import com.music.aurelis.AurelisApplication
import com.music.aurelis.playback.*
import com.music.aurelis.playback.audio.AudioBlock
import kotlinx.coroutines.test.*
import kotlinx.coroutines.ExperimentalCoroutinesApi
import org.junit.*
import org.junit.Assert.*
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33], manifest = Config.NONE, application = AurelisApplication::class)
class SettingsPersistenceTest {
    private val app get() = RuntimeEnvironment.getApplication()
    @Test fun invalidAudioValuesAreBoundedWithoutOverwritingUnknownPreferences() {
        val prefs = app.getSharedPreferences("aurelis_settings", 0)
        prefs.edit().putString("unknown-setting", "keep").commit()
        SettingsRepository.setThemeMode(99)
        SettingsRepository.setEqualizerBalance(Float.NaN)
        SettingsRepository.setEqualizerBands(listOf(Float.NaN, 99f, -99f))
        SettingsRepository.init(app)
        assertEquals(0, SettingsRepository.themeMode.value)
        assertEquals(0f, SettingsRepository.equalizerBalance.value, 0f)
        assertEquals(listOf(0f, 12f, -12f, 0f, 0f, 0f, 0f), SettingsRepository.equalizerBands.value)
        assertEquals("keep", prefs.getString("unknown-setting", null))
    }
    @Test fun appearanceLyricsAndAudioPreferencesSurviveReinitialization() {
        SettingsRepository.setThemeMode(2); SettingsRepository.setReduceAnimation(true)
        SettingsRepository.setReduceDynamicBlur(true); SettingsRepository.setLiquidGlass(false)
        SettingsRepository.setSyncedLyrics(false); SettingsRepository.setEqualizerPreset(EqualizerPreset.BASS_BOOST)
        SettingsRepository.setEqualizerEnabled(true); SettingsRepository.init(app)
        assertEquals(2, SettingsRepository.themeMode.value)
        assertTrue(SettingsRepository.reduceAnimation.value); assertTrue(SettingsRepository.reduceDynamicBlur.value)
        assertFalse(SettingsRepository.liquidGlass.value); assertFalse(SettingsRepository.syncedLyrics.value)
        assertEquals(EqualizerPreset.BASS_BOOST.bands, SettingsRepository.equalizerBands.value)
        assertTrue(SettingsRepository.equalizerEnabled.value)
    }
    @OptIn(ExperimentalCoroutinesApi::class)
    @Test fun serviceBindingAppliesPreferencesToAudioWithoutRestartingAPlayer() = runTest {
        val eq = EqualizerProcessor().apply { configure(48_000, 2) }
        var spatial = false
        bindAudioSettings(backgroundScope, eq) { spatial = it }
        runCurrent(); advanceTimeBy(41); runCurrent()
        assertFalse(eq.isEnabled)
        SettingsRepository.setEqualizerEnabled(true); SettingsRepository.setEqualizerBalance(1f); SettingsRepository.setSpatialAudio(true)
        runCurrent(); advanceTimeBy(41); runCurrent()
        val block = AudioBlock(2, 10).apply { samples.fill(0.25f); reset(10) }
        eq.process(block)
        assertTrue(eq.isEnabled); assertTrue(spatial)
        assertEquals(0f, block.samples[0], 0f); assertEquals(0.25f, block.samples[1], 0f)
        SettingsRepository.setEqualizerEnabled(false); runCurrent(); advanceTimeBy(41); runCurrent()
        assertFalse(eq.isEnabled)
    }
}
