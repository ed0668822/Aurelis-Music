// Aurelis © 2026. GPL-3.0-or-later
package com.music.aurelis.playback

class SleepTimer {
    // Sleep timer for stopping local music playback automatically
    private var remainingMs: Long = 0L

    fun start(durationMs: Long) {
        remainingMs = durationMs
    }

    fun cancel() {
        remainingMs = 0L
    }
}
