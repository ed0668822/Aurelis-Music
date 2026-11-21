// SPDX-FileCopyrightText: 2026 Aurelis
// SPDX-License-Identifier: LGPL-3.0-or-later

package com.music.aurelis.playback

import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.log10
import kotlin.math.max
import kotlin.math.pow
import kotlin.math.roundToInt
import kotlin.math.sin
import kotlin.math.tan

/**
 * Streaming stereo/mono ITU-R BS.1770 K-weighted integrated loudness meter.
 *
 * The EBU R128 integrated measurement uses 400 ms blocks at 100 ms hops,
 * a -70 LUFS absolute gate, then a gate 10 LU below the preliminary
 * integrated loudness. Four-times oversampled FIR peak detection is used
 * to guard positive normalization gains. No PCM is kept after a block.
 *
 * Channels beyond stereo must not be handed here without their correct
 * BS.1770 layout weights; refusing them is safer than silently mismeasuring.
 */
internal class R128Meter(private val sampleRate: Int, private val channels: Int) {
    init {
        require(sampleRate in 8000..384000)
        require(channels in 1..2)
    }

    private val shelf = arrayOf(Biquad(shelfCoefficients(sampleRate)), Biquad(shelfCoefficients(sampleRate)))
    private val highPass = arrayOf(Biquad(highPassCoefficients(sampleRate)), Biquad(highPassCoefficients(sampleRate)))
    private val hopFrames = max(1, (sampleRate / 10.0).roundToInt())
    private val hopEnergy = DoubleArray(4)
    private val energies = ArrayList<Double>()
    private val truePeak = OversampledPeak()
    private var hopIndex = 0
    private var hops = 0
    private var framesInHop = 0
    private var energyInHop = 0.0
    private var framesTotal = 0L

    /** Samples are interleaved by time as two planar chunks. */
    fun add(left: FloatArray, right: FloatArray = left) {
        require(left.size == right.size)
        for (i in left.indices) {
            val a = left[i].toDouble()
            val b = right[i].toDouble()
            if (!a.isFinite() || !b.isFinite()) continue
            val la = highPass[0].apply(shelf[0].apply(a))
            val lb = if (channels == 2) highPass[1].apply(shelf[1].apply(b)) else 0.0
            energyInHop += la * la + lb * lb
            truePeak.add(a, b, channels)
            framesTotal++
            framesInHop++
            if (framesInHop == hopFrames) {
                hopEnergy[hopIndex] = energyInHop
                hopIndex = (hopIndex + 1) % 4
                hops++
                if (hops >= 4) energies += hopEnergy.sum() / (4.0 * hopFrames)
                framesInHop = 0
                energyInHop = 0.0
            }
        }
    }

    /** Raw K-weighted window energies for a true multi-track gated album analysis. */
    fun blockEnergies(): List<Double> = energies.toList()

    fun finish(): ReplayGainMeasurement? {
        if (framesTotal < sampleRate / 2) return null
        val integrated = integratedLufs(energies) ?: return null
        return ReplayGainMeasurement(
            integratedLufs = integrated,
            truePeakLinear = truePeak.peak.takeIf { it.isFinite() && it > 0.0 },
        )
    }

    companion object {
        /** EBU Tech 3341: absolute gate followed by a relative -10 LU gate. */
        fun integratedLufs(blocks: List<Double>): Double? {
            if (blocks.isEmpty()) return null
            val absolute = 10.0.pow((-70.0 + 0.691) / 10.0)
            val accepted = blocks.filter { it.isFinite() && it >= absolute }
            if (accepted.isEmpty()) return null
            val relative = accepted.average() / 10.0
            val included = accepted.filter { it >= relative }
            if (included.isEmpty()) return null
            return (-0.691 + 10.0 * log10(included.average()))
                .takeIf { it.isFinite() && it in -100.0..10.0 }
        }
    }

    private class Biquad(private val c: Coeff) {
        private var z1 = 0.0
        private var z2 = 0.0
        fun apply(sample: Double): Double {
            val y = c.b0 * sample + z1
            z1 = c.b1 * sample - c.a1 * y + z2
            z2 = c.b2 * sample - c.a2 * y
            return y
        }
    }

    private data class Coeff(val b0: Double, val b1: Double, val b2: Double, val a1: Double, val a2: Double)

    private fun shelfCoefficients(rate: Int): Coeff {
        val k = tan(PI * 1681.974450955533 / rate)
        val vh = 10.0.pow(3.99984385397 / 20.0)
        val vb = vh.pow(0.4996667741545416)
        val q = 0.7071752369554196
        val norm = 1.0 + k / q + k * k
        return Coeff(
            (vh + vb * k / q + k * k) / norm,
            2 * (k * k - vh) / norm,
            (vh - vb * k / q + k * k) / norm,
            2 * (k * k - 1.0) / norm,
            (1.0 - k / q + k * k) / norm,
        )
    }

    private fun highPassCoefficients(rate: Int): Coeff {
        val k = tan(PI * 38.13547087602444 / rate)
        val q = 0.5003270373238773
        val norm = 1.0 + k / q + k * k
        // The BS.1770 RLB prefilter intentionally uses un-normalised numerator.
        return Coeff(
            1.0, -2.0, 1.0,
            2.0 * (k * k - 1.0) / norm,
            (1.0 - k / q + k * k) / norm,
        )
    }

    /**
     * 4x windowed-sinc reconstruction (33 taps) for intersample peak detection.
     * FIR coefficients are DC-normalized per fractional phase. The limiter
     * remains the final clipping guard for peaks outside FIR precision.
     */
    private class OversampledPeak {
        private val taps = Array(3) { phase ->
            val fraction = (phase + 1) / 4.0
            DoubleArray(33) { i ->
                val x = 16.0 + fraction - i
                val sinc = if (abs(x) < 1e-12) 1.0 else sin(PI * x) / (PI * x)
                val window = 0.42 - 0.5 * cos(2.0 * PI * i / 32.0) +
                    0.08 * cos(4.0 * PI * i / 32.0)
                sinc * window
            }.also { weights ->
                val sum = weights.sum()
                for (i in weights.indices) weights[i] /= sum
            }
        }
        private val a = DoubleArray(33)
        private val b = DoubleArray(33)
        private var next = 0
        private var filled = 0
        var peak: Double = 0.0
            private set

        fun add(left: Double, right: Double, channels: Int) {
            peak = max(peak, max(abs(left), if (channels == 2) abs(right) else 0.0))
            a[next] = left
            b[next] = right
            next = (next + 1) % a.size
            filled++
            if (filled < a.size) return
            for (weights in taps) {
                var l = 0.0
                var r = 0.0
                for (i in weights.indices) {
                    val index = (next + i) % a.size
                    l += a[index] * weights[i]
                    if (channels == 2) r += b[index] * weights[i]
                }
                peak = max(peak, max(abs(l), abs(r)))
            }
        }
    }
}
