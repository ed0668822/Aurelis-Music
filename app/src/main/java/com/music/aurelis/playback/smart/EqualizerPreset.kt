// Aurelis © 2026. GPL-3.0-or-later
package com.music.aurelis.playback.smart

data class EqualizerPreset(
    val name: String,
    val gains: FloatArray
) {
    companion object {
        val Flat = EqualizerPreset("Flat", floatArrayOf(0f, 0f, 0f, 0f, 0f))
        val BassBoost = EqualizerPreset("Bass Boost", floatArrayOf(4f, 3f, 0f, -1f, -2f))
    }
}
