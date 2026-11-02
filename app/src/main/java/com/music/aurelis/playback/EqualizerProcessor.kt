// Aurelis © 2026. GPL-3.0-or-later
package com.music.aurelis.playback

import com.music.aurelis.playback.audio.AudioBlock
import kotlin.math.abs

class EqualizerProcessor {
    private var sampleRate: Int = 0
    private var channelCount: Int = 0

    @Volatile
    var isEnabled: Boolean = false
        private set

    @Volatile
    var isShaping: Boolean = false
        private set

    private var curve: EqCurve = EqCurve.FLAT
    private var balance: Float = 0f

    fun configure(sampleRate: Int, channelCount: Int) {
        this.sampleRate = sampleRate
        this.channelCount = channelCount
    }

    fun setTuning(enabled: Boolean, curve: EqCurve, balance: Float): EqCurve? {
        isEnabled = enabled
        this.curve = curve
        this.balance = balance.coerceIn(-1f, 1f)
        isShaping = enabled && (
            curve.gainsDb.any { abs(it) > 0.01f } ||
                abs(curve.preampDb) > 0.01f
            )
        return curve
    }

    fun process(block: AudioBlock) {
        if (!isEnabled || block.frameCount == 0) return
        if (channelCount <= 0 || sampleRate <= 0) return

        val stereoBalance = balance.coerceIn(-1f, 1f)
        if (abs(stereoBalance) < 0.01f) return

        val gainOffset = 1.0f + stereoBalance * 0.18f
        val frames = block.frameCount
        val channels = block.channelCount.coerceAtLeast(1)

        for (frame in 0 until frames) {
            val base = frame * channels
            for (channel in 0 until channels) {
                val idx = base + channel
                val sample = block.samples[idx]
                val direction = if (channel == 0) -1f else 1f
                block.samples[idx] = sample * (gainOffset + (stereoBalance * direction * 0.08f))
            }
        }
    }

    fun flush() {
        // No persistent state to flush in this lightweight implementation.
    }

    fun reset() {
        isEnabled = false
        isShaping = false
        curve = EqCurve.FLAT
        balance = 0f
    }
}
