// Aurelis © 2026. GPL-3.0-or-later
package com.music.aurelis.playback

import android.util.Log
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import com.music.aurelis.playback.smart.TransitionPolicy
import com.music.aurelis.playback.smart.TransitionPlan
import com.music.aurelis.playback.smart.TransitionStyle
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.math.cos
import kotlin.math.sin

private const val TAG = "CrossfadeController"

/**
 * Crossfade real entre dos pistas locales: dos pistas audibles a la vez, la saliente
 * cayendo mientras la entrante sube, con curva equal-power (sin²+cos² = 1) para no
 * tener caída de potencia en el medio del solapamiento.
 *
 * ## Por qué dos jugadores
 *
 * Un solo ExoPlayer renderiza un elemento de cola a la vez, así que en el límite de
 * pista hay exactamente una fuente y su ganancia es 1 (sin desvanecer) o 0 (silencio).
 * Un solo jugador nunca puede solapar dos pistas; el resultado es un agujero en la
 * unión. Se necesita un segundo decodador.
 *
 * ## Qué jugador hace qué
 *
 * Dos pares, no un jugador y un ayudante. Ambos son ExoPlayer completos construidos
 * igual y ambos pueden poseer la cola; en cualquier instante uno de ellos *es* la
 * sesión (apunta el MediaSession, tiene el foco de audio y lleva la notificación) y
 * el otro está inactivo. Se intercambian roles en cada transición.
 *
 * - **[activo]** — el jugador al que apunta la sesión ahora. El resto de la app solo
 *   ve este.
 * - **[en espera]** — el jugador inactivo. Entre transiciones no tiene nada. Para
 *   armar una transición se carga con la cola, posicionado en la pista entrante en
 *   el punto de inicio del plan, y se inicia silenciosamente.
 *
 * La palabra clave es **entrante**. Una versión anterior de esta clase puso la pista
 * *saliente* en el segundo jugador: el jugador de sesión saltaba a la siguiente
 * canción y el segundo llevaba la cola de la antigua. Eso funciona, pero obliga a un
 * momento donde ambos jugadores renderizan *el mismo audio*, y dos ExoPlayers no
 * pueden iniciarse con precisión de muestra entre sí. Cualquier desalineación —
 * medida en transiciones reales de 9 a 41 ms — se oía como el último instante de
 * la pista saliente sonando dos veces, al inicio de cada crossfade. Ningún ajuste
 * lo elimina; la duplicación es estructural.
 *
 * Cargar la pista *entrante* en el jugador de espera la elimina por completo. Los
 * dos jugadores nunca tienen el mismo audio, así que no hay nada que alinear, nada
 * que entregar y ninguna costura que ocultar. La pista entrante ya está sonando,
 * desde exactamente la posición correcta, cuando su fader empieza a moverse.
 *
 * ## La entrega
 *
 * Como ambos jugadores poseen la cola, terminar una transición es un **intercambio
 * de roles** en lugar de un seek: nada se vuelve a cargar, nada se vuelve a buscar,
 * y ningún audio se renderiza dos veces. [onHandoff] es lo que lo hace: el servicio
 * mueve el MediaSession, el foco de audio, sus listeners y todo lo que reserva
 * contra un cambio de pista al jugador entrante.
 *
 * Se dispara a mitad del blend — [HANDOFF_AT] — donde la curva equal-power hace que
 * las dos pistas suenen igual de fuertes y la entrante toma el control. El índice
 * de cola, los metadatos, la notificación y la UI cambian a la canción entrante ahí:
 * no en su primera nota apenas audible, ni rezagando la canción en su salida. Antes
 * el jugador entrante es una sombra silenciosa-then-ascendente de la sesión; después
 * [saliente] es el jugador inactivo, aún audible, desvaneciéndose.
 *
 * ## Curva
 *
 * `sin`/`cos` en lugar del viejo `sqrt`: `sin²+cos²=1` exactamente, así que dos
 * pistas desvaneciéndose entre sí mantienen potencia *constante* durante todo el
 * camino y la transición no tiene una caída en el medio. Esa es la ley de crossfade
 * estándar, y es lo que hace que un crossfade largo suene como una mezcla en lugar
 * de una caída.
 */
@UnstableApi
class CrossfadeController(
    private val scope: CoroutineScope,
    /** El jugador que backing la sesión ahora. Se mueve en cada [onHandoff]. */
    private val active: () -> ExoPlayer,
    /** El jugador inactivo, que la próxima transición cargará con la pista entrante. */
    private val standby: () -> ExoPlayer,
    /**
     * Mueve la sesión al jugador que acaba de iniciar la pista entrante: el player
     * del MediaSession, el foco de audio, los listeners del servicio y todo lo que
     * reserva contra un cambio de pista.
     *
     * Se llama una vez por transición, a mitad del blend. Después de que retorne,
     * [active] debe responder `incoming` y [standby] debe responder `outgoing` —
     * esta clase no vuelve a leer ninguno durante una transición, pero todo lo demás
     * en el servicio sí.
     */
    private val onHandoff: (outgoing: ExoPlayer, incoming: ExoPlayer) -> Unit,
    /**
     * Política de transición local (duración de crossfade, estilo preferido).
     */
    private val policy: TransitionPolicy = TransitionPolicy(),
    /**
     * Invocado después de que [finish] complete la limpieza y retorne a IDLE.
     */
    private val onSettled: () -> Unit = {},
) {

    private var phase: Phase = Phase.IDLE
        private set

    private var transitionJob: Job? = null

    /** Punto del blend donde ocurre el intercambio de roles (mitad del crossfade). */
    private const val HANDOFF_AT: Float = 0.5f

    /** Longitud del paso de ganancia por frame para el desvanecimiento. */
    private const val GAIN_STEP_FRAMES: Int = 16

    /**
     * Inicia una transición de [outgoing] a [incoming]. No hace nada si ya hay una
     * transición en curso.
     */
    fun begin(outgoing: ExoPlayer, incoming: ExoPlayer, plan: TransitionPlan = policy.plan(0, 0)) {
        if (phase != Phase.IDLE) {
            Log.w(TAG, "Crossfade already in progress; ignoring begin()")
            return
        }
        transitionJob?.cancel()
        phase = Phase.ARMING
        transitionJob = scope.launch {
            try {
                arm(outgoing, incoming, plan)
                blend(outgoing, incoming, plan)
                finish(outgoing, incoming)
            } catch (e: CancellationException) {
                phase = Phase.IDLE
                throw e
            } catch (e: Exception) {
                Log.e(TAG, "Crossfade failed", e)
                phase = Phase.IDLE
            }
        }
    }

    /** Cancela cualquier transición en curso y vuelve a IDLE. */
    fun cancel() {
        transitionJob?.cancel()
        transitionJob = null
        phase = Phase.IDLE
    }

    /** Estado actual del controlador. */
    val currentPhase: Phase
        get() = phase

    private fun arm(outgoing: ExoPlayer, incoming: ExoPlayer, plan: TransitionPlan) {
        // Cargar la pista entrante en el jugador de espera, posicionada en el punto de inicio.
        incoming.seekTo(plan.cuePositionMs)
        incoming.setVolume(0f)
        incoming.prepare()
        phase = Phase.ARMED
    }

    private fun blend(outgoing: ExoPlayer, incoming: ExoPlayer, plan: TransitionPlan) {
        val durationMs = plan.crossfadeDurationMs.coerceIn(
            TransitionPolicy.MIN_CROSSFADE_MS,
            TransitionPolicy.MAX_CROSSFADE_MS,
        )
        val totalFrames = (durationMs / 10.0).toInt().coerceAtLeast(1)
        val framesPerStep = GAIN_STEP_FRAMES

        phase = Phase.BLENDING
        var frame = 0
        while (frame < totalFrames && isActive) {
            val t = frame / totalFrames.toFloat()
            val outGain = plan.outgoingGain(t)
            val inGain = plan.incomingGain(t)

            // Aplicar ganancias con headroom para evitar clipping en el solapamiento.
            val headroom = 1.0f / sqrt(outGain * outGain + inGain * inGain)
            outgoing.setVolume((outGain * headroom).coerceIn(0f, 1f))
            incoming.setVolume((inGain * headroom).coerceIn(0f, 1f))

            // Intercambio de roles a mitad del blend.
            if (t >= HANDOFF_AT && phase == Phase.BLENDING) {
                withContext(Dispatchers.Main) {
                    onHandoff(outgoing, incoming)
                }
                phase = Phase.HANDOFF_COMPLETE
            }

            frame += framesPerStep
            delay(GAIN_STEP_FRAMES)
        }

        // Asegurar ganancias finales: saliente a 0, entrante a 1.
        outgoing.setVolume(0f)
        incoming.setVolume(1f)
        phase = Phase.SETTLING
    }

    private fun finish(outgoing: ExoPlayer, incoming: ExoPlayer) {
        outgoing.stop()
        outgoing.clearMediaItems()
        phase = Phase.IDLE
        withContext(Dispatchers.Main) {
            onSettled()
        }
    }

    private fun sqrt(v: Float): Float = kotlin.math.sqrt(v.toDouble()).toFloat()

    /** Fase actual del controlador de crossfade. */
    enum class Phase {
        /** No hay transición en curso. */
        IDLE,
        /** Cargando la pista entrante en el jugador de espera. */
        ARMING,
        /** Pista entrante cargada y lista para empezar el blend. */
        ARMED,
        /** Dos pistas sonando a la vez, desvaneciendo. */
        BLENDING,
        /** Intercambio de roles completado. */
        HANDOFF_COMPLETE,
        /** Limpieza final, saliente detenido. */
        SETTLING,
    }
}
