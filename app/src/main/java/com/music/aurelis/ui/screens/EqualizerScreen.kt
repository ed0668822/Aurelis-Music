// Aurelis © 2026. GPL-3.0-or-later
package com.music.aurelis.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.music.aurelis.data.SettingsRepository
import com.music.aurelis.data.EqualizerMode
import com.music.aurelis.playback.*
import com.music.aurelis.ui.components.SegmentedControl

@Composable
fun EqualizerScreen(onBack: () -> Unit) {
    val profiles by com.music.aurelis.data.ProfileRepository.state.collectAsState()
    if (profiles.readOnly) {
        Column(Modifier.fillMaxSize().statusBarsPadding().padding(24.dp)) {
            Text("Los ajustes están en modo de solo lectura.")
            TextButton(onClick = onBack) { Text("Atrás") }
        }
        return
    }
    val enabled by SettingsRepository.equalizerEnabled.collectAsState()
    val mode by SettingsRepository.equalizerMode.collectAsState()
    val bands by SettingsRepository.equalizerBands.collectAsState()
    val balance by SettingsRepository.equalizerBalance.collectAsState()
    val toneX by SettingsRepository.equalizerToneX.collectAsState()
    val toneY by SettingsRepository.equalizerToneY.collectAsState()
    val spatial by SettingsRepository.spatialAudio.collectAsState()
    Surface(Modifier.fillMaxSize()) {
        Column(Modifier.fillMaxSize().statusBarsPadding().navigationBarsPadding().verticalScroll(rememberScrollState()).padding(16.dp)) {
            TextButton(onClick = onBack) { Text("Atrás") }
            Text("Audio Equalizer & DSP", style = MaterialTheme.typography.titleLarge)
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("Ecualizador")
                Switch(enabled, SettingsRepository::setEqualizerEnabled, Modifier.semantics { contentDescription = "Activar ecualizador" })
            }
            SegmentedControl(listOf("Tono", "7 bandas"), mode.ordinal, { SettingsRepository.setEqualizerMode(EqualizerMode.entries[it]) })
            if (mode == EqualizerMode.MANUAL) {
                for (index in 0..6) EqSlider("${EqLayout.MANUAL_BANDS_HZ[index].toInt()} Hz", bands.getOrElse(index) { 0f }, -12f..12f, enabled) { value ->
                    SettingsRepository.setEqualizerBands(bands.toMutableList().apply { this[index] = value })
                }
                Row {
                    TextButton(onClick = { SettingsRepository.setEqualizerPreset(EqualizerPreset.FLAT) }) { Text("Plano") }
                    TextButton(onClick = { SettingsRepository.setEqualizerPreset(EqualizerPreset.BASS_BOOST) }) { Text("Graves") }
                    TextButton(onClick = { SettingsRepository.setEqualizerPreset(EqualizerPreset.VOCAL) }) { Text("Voces") }
                }
            } else {
                EqSlider("Brillo", toneX.toFloat(), -5f..5f, enabled) { SettingsRepository.setEqualizerTone(it.toInt(), toneY) }
                EqSlider("Contorno", toneY.toFloat(), -5f..5f, enabled) { SettingsRepository.setEqualizerTone(toneX, it.toInt()) }
            }
            EqSlider("Balance", balance, -1f..1f, enabled, SettingsRepository::setEqualizerBalance)
            TextButton(onClick = { SettingsRepository.setEqualizerBalance(0f); SettingsRepository.setEqualizerTone(0, 0); SettingsRepository.setEqualizerPreset(EqualizerPreset.FLAT) }) { Text("Restablecer ecualizador") }
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("Amplitud estéreo")
                Switch(spatial, SettingsRepository::setSpatialAudio, Modifier.semantics { contentDescription = "Amplitud estéreo" })
            }
            Text("El procesamiento modifica el audio local. La amplitud estéreo actúa en dos canales; no es Dolby Atmos.", style = MaterialTheme.typography.bodySmall)
        }
    }
}

@Composable
private fun EqSlider(label: String, value: Float, range: ClosedFloatingPointRange<Float>, enabled: Boolean, onCommit: (Float) -> Unit) {
    var drag by remember(value) { mutableFloatStateOf(value.coerceIn(range.start, range.endInclusive)) }
    Text(label, style = MaterialTheme.typography.bodyMedium)
    Slider(drag, { drag = it }, enabled = enabled, valueRange = range,
        onValueChangeFinished = { onCommit(drag) }, modifier = Modifier.semantics { contentDescription = label })
}
