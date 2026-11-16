// Aurelis © 2026. GPL-3.0-or-later
package com.music.aurelis.ui.player

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue

/** Keep the visible frame during a load; a superseded request cannot publish. */
internal class ArtworkTransition<T>(initial: T) {
    var value by mutableStateOf(initial)
        private set
    private var generation = 0L
    fun begin(): Long = ++generation
    fun complete(request: Long, next: T) {
        if (request == generation) value = next
    }
}
