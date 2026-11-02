// Aurelis © 2026. GPL-3.0-or-later
package com.music.aurelis.playback.audio

interface FloatAudioProcessor {
    fun configure(sampleRate: Int, channels: Int)
    fun process(block: AudioBlock)
    fun flush()
    fun reset()
}
