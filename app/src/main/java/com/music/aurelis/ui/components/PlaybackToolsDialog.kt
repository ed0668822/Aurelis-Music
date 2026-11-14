// Aurelis © 2026. GPL-3.0-or-later
package com.music.aurelis.ui.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.music.aurelis.playback.PlaybackState

@Composable
internal fun PlaybackToolsDialog(state: PlaybackState, onTimer: (Long, Boolean) -> Unit, onSpeed: (Float) -> Unit, onSilence: (Boolean) -> Unit, onAutoplay: (Boolean) -> Unit, onDismiss: () -> Unit) {
    AlertDialog(onDismissRequest = onDismiss, title = { Text("Controles de reproducción") }, text = {
        Column(Modifier.verticalScroll(rememberScrollState())) {
            TextButton(onClick = { onAutoplay(!state.autoplayEnabled) },
                enabled = state.canUsePlaybackTools && state.canSetAutoplay) {
                Text(if (state.autoplayEnabled) "Desactivar continuación local" else "Activar continuación local")
            }
            state.autoplayStatus?.let { Text(it) }
            Text("Velocidad: ${state.playbackSpeed}×")
            for (speed in listOf(0.5f, 0.75f, 1f, 1.25f, 1.5f, 2f)) TextButton(
                onClick = { onSpeed(speed) }, enabled = state.canUsePlaybackTools && state.canSetPlaybackSpeed,
            ) { Text("${speed}×") }
            TextButton(onClick = { onSilence(!state.skipSilence) },
                enabled = state.canUsePlaybackTools && state.canSkipSilence) {
                Text(if (state.skipSilence) "Desactivar salto de silencio" else "Activar salto de silencio")
            }
            state.controlRestriction?.let { Text(it) }
            Text("Temporizador")
            Text(when {
                state.sleepAfterTrack -> "Se pausará al terminar esta canción"
                state.sleepRemainingMs > 0 -> "Tiempo restante: ${(state.sleepRemainingMs + 59_999) / 60_000} min"
                else -> "Desactivado"
            })
            if (!state.canUsePlaybackTools) Text("El servicio conectado no ofrece este control.")
            for (minutes in listOf(15, 30, 45, 60)) TextButton(
                onClick = { onTimer(minutes * 60_000L, false) }, enabled = state.canUsePlaybackTools,
            ) { Text("$minutes min") }
            TextButton(onClick = { onTimer(0L, true) }, enabled = state.canUsePlaybackTools) { Text("Al terminar esta canción") }
            TextButton(onClick = { onTimer(0L, false) }, enabled = state.canUsePlaybackTools) { Text("Cancelar temporizador") }
        }
    }, confirmButton = { TextButton(onClick = onDismiss) { Text("Cerrar") } })
}
