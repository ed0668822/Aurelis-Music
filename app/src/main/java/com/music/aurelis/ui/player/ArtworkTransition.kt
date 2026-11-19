// SPDX-FileCopyrightText: 2026 Aurelis
// SPDX-License-Identifier: LGPL-3.0-or-later
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

    /**
     * F052: an explicit no-artwork/failed state still belongs to the most
     * recent request. Retain the old frame while a valid request is pending.
     */
    fun isCurrent(request: Long): Boolean = request == generation
}
