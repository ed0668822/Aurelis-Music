// Aurelis © 2026. GPL-3.0-or-later
package com.music.aurelis.playback

class AudioOutputPolicy {
    // Manages audio output routing preferences (Speaker, Bluetooth, USB DAC) for local playback
    fun getPreferredOutput(): String {
        return "AUTO"
    }
}
