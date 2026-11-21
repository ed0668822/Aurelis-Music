// SPDX-FileCopyrightText: 2026 Aurelis
// SPDX-License-Identifier: LGPL-3.0-or-later
package com.music.aurelis.playback.smart

/**
 * Adaptive upper bound for ONNX intra-op parallelism. The user still picks
 * Efficient / Balanced / Maximum; that is a ceiling, not permission to
 * oversubscribe a thermally limited or low-memory phone.
 *
 * No model topology, DSP audio quality or transition timing is changed.
 */
object AutomixThreadBudget {
    fun choose(
        requested: Int,
        availableCpus: Int,
        lowRam: Boolean,
        thermalModerate: Boolean,
        thermalSevere: Boolean,
    ): Int {
        val cpuBudget = (availableCpus.coerceAtLeast(1) / 2).coerceAtLeast(1)
        val capped = requested.coerceIn(1, 4).coerceAtMost(cpuBudget)
        return when {
            thermalSevere || lowRam -> 1
            thermalModerate -> capped.coerceAtMost(2)
            else -> capped
        }
    }
}
