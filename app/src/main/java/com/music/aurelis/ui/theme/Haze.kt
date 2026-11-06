// Aurelis © 2026. GPL-3.0-or-later
package com.music.aurelis.ui.theme

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.music.aurelis.data.SettingsRepository
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.layout.BoxScope
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.hazeEffect
import dev.chrisbanes.haze.hazeSource
import dev.chrisbanes.haze.materials.ExperimentalHazeMaterialsApi
import dev.chrisbanes.haze.materials.HazeMaterials

/**
 * Haze es una biblioteca de Chris Banes para efectos de desenfoque (blur) en
 * Compose, disponible en Android 12+ (API 31+). Se usa para crear efectos de
 * cristal (glassmorphism) y desenfoque de fondo en la UI.
 *
 * Esta función crea un HazeState y lo aplica a un Box que cubre toda la pantalla,
 * permitiendo que cualquier elemento con `.hazeEffect()` pueda samplear el fondo.
 */
@Composable
fun rememberHazeState(): HazeState {
    return remember { HazeState() }
}

/**
 * Estilos de haze disponibles (nombres propios para evitar conflicto con dev.chrisbanes.haze.HazeStyle).
 */
enum class AurelisHazeStyle {
    Thin,
    Regular,
    Thick,
}

/**
 * Aplica un efecto de haze a un elemento, usando el material de haze (blur + tint).
 *
 * @receiver El modifier del elemento.
 * @param hazeState El HazeState creado con [rememberHazeState].
 * @param style El estilo de haze (thin, regular, thick).
 * @param tint El color de tint para el haze (se adapta automáticamente al tema).
 */
@OptIn(ExperimentalHazeMaterialsApi::class)
@Composable
fun Modifier.aurelisHazeEffect(
    hazeState: HazeState,
    style: AurelisHazeStyle = AurelisHazeStyle.Regular,
    tint: Color? = null,
): Modifier {
    val reduceBlur by SettingsRepository.reduceDynamicBlur.collectAsStateWithLifecycle()
    if (reduceBlur || !com.music.aurelis.ui.components.LocalGlassHardware.current) return this.background(tint ?: MaterialTheme.colorScheme.surface)
    val themeTint = if (MaterialTheme.colorScheme.surface.luminance() > 0.5f) {
        Color(0xFFFAFAFA)
    } else {
        Color(0xFF121212)
    }
    val finalTint = tint ?: themeTint

    return when (style) {
        AurelisHazeStyle.Thin -> this
            .then(Modifier.hazeEffect(state = hazeState, style = HazeMaterials.thin(finalTint)))
        AurelisHazeStyle.Regular -> this
            .then(Modifier.hazeEffect(state = hazeState, style = HazeMaterials.regular(finalTint)))
        AurelisHazeStyle.Thick -> this
            .then(Modifier.hazeEffect(state = hazeState, style = HazeMaterials.thick(finalTint)))
    }
}

/**
 * Aplica un efecto de haze a un Box que cubre toda la pantalla, permitiendo que
 * cualquier elemento con `.aurelisHazeEffect()` pueda samplear el fondo.
 *
 * @param modifier El modifier del Box.
 * @param hazeState El HazeState creado con [rememberHazeState].
 * @param content El contenido del Box.
 */
@Composable
fun HazeBox(
    modifier: Modifier = Modifier,
    hazeState: HazeState = rememberHazeState(),
    content: @Composable BoxScope.() -> Unit,
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .hazeSource(hazeState),
        content = content,
    )
}

/**
 * Verifica si el dispositivo soporta haze (Android 12+).
 * Since minSdk is 33, this is always supported.
 */
fun isHazeSupported(): Boolean = true