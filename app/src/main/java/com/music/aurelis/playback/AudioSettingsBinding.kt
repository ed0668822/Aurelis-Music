package com.music.aurelis.playback

import com.music.aurelis.data.EqualizerMode
import com.music.aurelis.data.SettingsRepository as Settings
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.*

/** Service-owned subscriptions. Cancelling its scope releases all observers. */
@OptIn(kotlinx.coroutines.FlowPreview::class)
internal fun bindAudioSettings(scope: CoroutineScope, equalizer: EqualizerProcessor, spatial: (Boolean) -> Unit) {
    fun apply() {
        equalizer.setTuning(Settings.equalizerEnabled.value,
            if (Settings.equalizerMode.value == EqualizerMode.MANUAL) manualCurve(Settings.equalizerBands.value)
            else toneCurve(Settings.equalizerToneX.value, Settings.equalizerToneY.value, Settings.equalizerFocused.value),
            Settings.equalizerBalance.value)
    }
    apply()
    scope.launch {
        merge(Settings.equalizerEnabled.map { Unit }, Settings.equalizerMode.map { Unit },
            Settings.equalizerToneX.map { Unit }, Settings.equalizerToneY.map { Unit },
            Settings.equalizerFocused.map { Unit }, Settings.equalizerBands.map { Unit },
            Settings.equalizerBalance.map { Unit }).debounce(40).collect { apply() }
    }
    scope.launch { Settings.spatialAudio.collect { spatial(it) } }
}
