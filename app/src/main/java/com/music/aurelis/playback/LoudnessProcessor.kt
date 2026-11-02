// Aurelis © 2026. GPL-3.0-or-later
package com.music.aurelis.playback

import com.music.aurelis.playback.audio.AudioBlock

class LoudnessProcessor {
    var holdLimiter: Boolean = false

    private var sampleRate: Int = 0
    private var channelCount: Int = 0

    fun configure(sampleRate: Int, channels: Int) {
        this.sampleRate = sampleRate
        this.channelCount = channels
    }

    fun onStreamBoundary() {
        // Loudness smoothing reset for track change boundaries.
    }

    fun process(block: AudioBlock) {
        if (block.frameCount == 0) return
        val limitGain = if (holdLimiter) 0.95f else 1.0f
        for (index in 0 until block.sampleCount) {
            block.samples[index] *= limitGain
        }
    }

    fun flush() {
        holdLimiter = false
    }

    fun reset() {
        holdLimiter = false
        sampleRate = 0
        channelCount = 0
    }
}
