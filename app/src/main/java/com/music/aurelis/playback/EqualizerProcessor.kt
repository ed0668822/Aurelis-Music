// Aurelis © 2026. GPL-3.0-or-later
package com.music.aurelis.playback

import com.music.aurelis.playback.audio.AudioBlock
import kotlin.math.*

/** Tuning is published atomically; filter history belongs exclusively to the audio thread. */
class EqualizerProcessor {
    private data class Tuning(val enabled: Boolean, val gains: List<Float>, val qs: List<Float>, val preamp: Float, val balance: Float)
    @Volatile private var tuning = Tuning(false, List(EqLayout.SLOTS) { 0f }, List(EqLayout.SLOTS) { 0.707f }, 0f, 0f)
    val isEnabled get() = tuning.enabled
    val isShaping get() = tuning.let { it.enabled && (it.gains.any { gain -> abs(gain) > 0.01f } || abs(it.preamp) > 0.01f) }
    private var sampleRate = 0
    private var channelCount = 0
    private var applied: Tuning? = null
    private var stages = emptyArray<Stage>()
    private var preampGain = 1.0
    private class Stage(val b0: Double, val b1: Double, val b2: Double, val a1: Double, val a2: Double, channels: Int) {
        val z1 = DoubleArray(channels); val z2 = DoubleArray(channels)
        fun sample(input: Double, channel: Int): Double {
            val output = b0 * input + z1[channel]
            z1[channel] = b1 * input - a1 * output + z2[channel]
            z2[channel] = b2 * input - a2 * output
            return output
        }
    }
    fun configure(sampleRate: Int, channelCount: Int) {
        this.sampleRate = sampleRate; this.channelCount = channelCount; flush()
    }
    fun setTuning(enabled: Boolean, curve: EqCurve, balance: Float): EqCurve {
        val gains = List(EqLayout.SLOTS) { curve.gainsDb.getOrElse(it) { 0f }.let { gain -> if (gain.isFinite()) gain.coerceIn(-12f, 12f) else 0f } }
        val qs = List(EqLayout.SLOTS) { curve.qs.getOrElse(it) { 0.707f }.let { q -> if (q.isFinite()) q.coerceIn(0.1f, 10f) else 0.707f } }
        val preamp = if (curve.preampDb.isFinite()) curve.preampDb.coerceIn(-24f, 0f) else 0f
        val pan = if (balance.isFinite()) balance.coerceIn(-1f, 1f) else 0f
        tuning = Tuning(enabled, gains, qs, preamp, pan)
        return EqCurve(gains.toFloatArray(), qs.toFloatArray(), preamp)
    }
    fun process(block: AudioBlock) {
        val snapshot = tuning
        if (!snapshot.enabled) { applied = null; return }
        if (sampleRate <= 0 || channelCount <= 0 || block.channelCount != channelCount || block.frameCount == 0) return
        if (applied !== snapshot) {
            if (applied?.gains != snapshot.gains || applied?.qs != snapshot.qs) {
                stages = EqLayout.slots.mapIndexedNotNull { index, slot ->
                    if (abs(snapshot.gains[index]) < 0.01f || slot.frequencyHz >= sampleRate * 0.48f) null
                    else stage(slot, snapshot.gains[index], snapshot.qs[index])
                }.toTypedArray()
            }
            val active = FloatArray(EqLayout.SLOTS) { index -> if (EqLayout.slots[index].frequencyHz < sampleRate * 0.48f) snapshot.gains[index] else 0f }
            val preamp = if (active.indices.any { active[it] != snapshot.gains[it] }) EqCurve.of(active, snapshot.qs.toFloatArray()).preampDb else snapshot.preamp
            preampGain = 10.0.pow(preamp / 20.0)
            applied = snapshot
        }
        for (frame in 0 until block.frameCount) for (channel in 0 until channelCount) {
            val index = frame * channelCount + channel
            var sample = block.samples[index].toDouble() * preampGain
            for (filter in stages) sample = filter.sample(sample, channel)
            if (channelCount == 2) sample *= when (channel) {
                0 -> if (snapshot.balance > 0) 1.0 - snapshot.balance else 1.0
                else -> if (snapshot.balance < 0) 1.0 + snapshot.balance else 1.0
            }
            val output = sample.toFloat()
            block.samples[index] = if (output.isFinite()) output else 0f
        }
    }
    private fun stage(slot: FilterSlot, gain: Float, q: Float): Stage {
        val a = 10.0.pow(gain / 40.0)
        val omega = 2 * PI * slot.frequencyHz / sampleRate
        val c = cos(omega); val alpha = sin(omega) / (2 * q); val beta = 2 * sqrt(a) * alpha
        val coefficients = when (slot.kind) {
            FilterKind.BELL -> doubleArrayOf(1 + alpha * a, -2 * c, 1 - alpha * a, 1 + alpha / a, -2 * c, 1 - alpha / a)
            FilterKind.LOW_SHELF -> doubleArrayOf(a * ((a + 1) - (a - 1) * c + beta), 2 * a * ((a - 1) - (a + 1) * c),
                a * ((a + 1) - (a - 1) * c - beta), (a + 1) + (a - 1) * c + beta,
                -2 * ((a - 1) + (a + 1) * c), (a + 1) + (a - 1) * c - beta)
            FilterKind.HIGH_SHELF -> doubleArrayOf(a * ((a + 1) + (a - 1) * c + beta), -2 * a * ((a - 1) + (a + 1) * c),
                a * ((a + 1) + (a - 1) * c - beta), (a + 1) - (a - 1) * c + beta,
                2 * ((a - 1) - (a + 1) * c), (a + 1) - (a - 1) * c - beta)
        }
        val a0 = coefficients[3]
        return Stage(coefficients[0] / a0, coefficients[1] / a0, coefficients[2] / a0, coefficients[4] / a0, coefficients[5] / a0, channelCount)
    }
    fun flush() { applied = null; stages = emptyArray() }
    fun reset() { setTuning(false, EqCurve.FLAT, 0f); flush() }
}
