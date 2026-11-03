// Aurelis © 2026. GPL-3.0-or-later
package com.music.aurelis.playback

import android.util.Log
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import com.music.aurelis.playback.smart.TransitionPlan
import com.music.aurelis.playback.smart.TransitionPolicy
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

private const val TAG = "CrossfadeController"

@UnstableApi
class CrossfadeController(
    private val scope: CoroutineScope,
    private val active: () -> ExoPlayer,
    private val standby: () -> ExoPlayer,
    private val onHandoff: (outgoing: ExoPlayer, incoming: ExoPlayer) -> Unit,
    private val policy: TransitionPolicy = TransitionPolicy(),
    private val onSettled: () -> Unit = {},
) {

    private var phase: Phase = Phase.IDLE
        private set

    private var transitionJob: Job? = null

    companion object {
        @JvmField val HANDOFF_AT: Float = 0.5f
        @JvmField val GAIN_STEP_FRAMES: Int = 16
    }

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

    fun cancel() {
        transitionJob?.cancel()
        transitionJob = null
        phase = Phase.IDLE
    }

    val currentPhase: Phase get() = phase

    private fun arm(outgoing: ExoPlayer, incoming: ExoPlayer, plan: TransitionPlan) {
        incoming.seekTo(plan.cuePositionMs)
        incoming.setVolume(0f)
        incoming.prepare()
        phase = Phase.ARMED
    }

    private suspend fun blend(outgoing: ExoPlayer, incoming: ExoPlayer, plan: TransitionPlan) {
        val durationMs = plan.crossfadeDurationMs.coerceIn(
            TransitionPolicy.MIN_CROSSFADE_MS,
            TransitionPolicy.MAX_CROSSFADE_MS,
        )
        val totalFrames = (durationMs / 10.0).toInt().coerceAtLeast(1)
        val framesPerStep = GAIN_STEP_FRAMES

        phase = Phase.BLENDING
        var frame = 0
        // FIX: `isActive` requiere receiver; usa currentCoroutineContext().isActive
        while (frame < totalFrames && currentCoroutineContext().isActive) {
            val t = frame / totalFrames.toFloat()
            val outGain = plan.outgoingGain(t)
            val inGain = plan.incomingGain(t)

            val headroom = 1.0f / sqrt(outGain * outGain + inGain * inGain)
            outgoing.setVolume((outGain * headroom).coerceIn(0f, 1f))
            incoming.setVolume((inGain * headroom).coerceIn(0f, 1f))

            if (t >= HANDOFF_AT && phase == Phase.BLENDING) {
                onHandoff(outgoing, incoming)
                phase = Phase.HANDOFF_COMPLETE
            }

            frame += framesPerStep
            delay(GAIN_STEP_FRAMES.toLong())
        }

        outgoing.setVolume(0f)
        incoming.setVolume(1f)
        phase = Phase.SETTLING
    }

    private suspend fun finish(outgoing: ExoPlayer, incoming: ExoPlayer) {
        outgoing.stop()
        outgoing.clearMediaItems()
        phase = Phase.IDLE
        onSettled()
    }

    private fun sqrt(v: Float): Float = kotlin.math.sqrt(v.toDouble()).toFloat()

    enum class Phase {
        IDLE, ARMING, ARMED, BLENDING, HANDOFF_COMPLETE, SETTLING,
    }
}
