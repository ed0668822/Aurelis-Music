// Aurelis © 2026. GPL-3.0-or-later
package com.music.aurelis.playback.smart

import kotlin.math.cos
import kotlin.math.sin

/**
 * Estilos de transición entre pistas para reproducción local.
 *
 * - [Gapless]: sin pausa, pista siguiente empieza en el instante exacto del corte.
 * - [Crossfade]: dos pistas audibles a la vez, la saliente cae mientras la entrante sube
 *   (curva equal-power sin²+cos² para mantener potencia constante).
 * - [Abrupt]: corte seco, la saliente se detiene y la entrante empieza desde cero.
 */
enum class TransitionStyle {
    GAPLESS,
    CROSSFADE,
    ABRUPT,
}

/**
 * Resultado de planificar una transición entre dos pistas locales.
 *
 * [style] es el estilo elegido. [crossfadeDurationMs] es la longitud del solapamiento
 * cuando el estilo es [CROSSFADE]. [cuePositionMs] es la posición de inicio de la pista
 * entrante dentro del solapamiento (0 = inicio de pista).
 *
 * Cuando no hay análisis de pista disponible, se devuelve una política por defecto:
 * crossfade de 3000 ms empezando en el inicio de la pista entrante.
 */
data class TransitionPlan(
    val style: TransitionStyle = TransitionStyle.CROSSFADE,
    val crossfadeDurationMs: Long = 3000L,
    val cuePositionMs: Long = 0L,
) {
    /** Potencia relativa de la pista saliente en el instante [t] (0 = inicio, 1 = fin del crossfade). */
    fun outgoingGain(t: Float): Float {
        val x = t.coerceIn(0f, 1f)
        return when (style) {
            TransitionStyle.CROSSFADE -> cos(x * kotlin.math.PI.toFloat() / 2f)
            TransitionStyle.GAPLESS -> 1f
            TransitionStyle.ABRUPT -> if (x < 1f) 1f else 0f
        }
    }

    /** Potencia relativa de la pista entrante en el instante [t] (0 = inicio, 1 = fin del crossfade). */
    fun incomingGain(t: Float): Float {
        val x = t.coerceIn(0f, 1f)
        return when (style) {
            TransitionStyle.CROSSFADE -> sin(x * kotlin.math.PI.toFloat() / 2f)
            TransitionStyle.GAPLESS -> 1f
            TransitionStyle.ABRUPT -> if (x < 1f) 0f else 1f
        }
    }
}

/**
 * Política de transición para reproducción local (100% archivos locales, sin red).
 *
 * Encapsula la duración de crossfade y el estilo preferido por el usuario. No depende
 * de análisis remoto ni de servicios de terceros.
 */
class TransitionPolicy {
    /** Duración predeterminada del solapamiento crossfade, en milisegundos. */
    var crossfadeDurationMs: Long = 3000L

    /** Estilo de transición predeterminado. */
    var defaultStyle: TransitionStyle = TransitionStyle.CROSSFADE

    /** Si se prefiere gapless sobre crossfade cuando el formato lo permite. */
    var preferGapless: Boolean = false

    /**
     * Planifica la transición entre [outgoing] y [incoming] usando únicamente metadatos
     * locales (duración, formato). Devuelve un [TransitionPlan] listo para usar.
     */
    fun plan(outgoingDurationMs: Long, incomingDurationMs: Long): TransitionPlan {
        val style = if (preferGapless) TransitionStyle.GAPLESS else defaultStyle
        return TransitionPlan(
            style = style,
            crossfadeDurationMs = crossfadeDurationMs.coerceAtLeast(0),
            cuePositionMs = 0L,
        )
    }

    companion object {
        /** Duración mínima de crossfade para evitar clics audibles. */
        const val MIN_CROSSFADE_MS: Long = 200

        /** Duración máxima razonable de crossfade. */
        const val MAX_CROSSFADE_MS: Long = 15_000
    }
}
