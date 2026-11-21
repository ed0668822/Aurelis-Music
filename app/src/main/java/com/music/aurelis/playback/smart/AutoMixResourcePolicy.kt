// SPDX-FileCopyrightText: 2026 Aurelis
// SPDX-License-Identifier: LGPL-3.0-or-later

package com.music.aurelis.playback.smart

/**
 * Limits AutoMix ONNX workers according to current system headroom.
 *
 * More workers only help offline analysis, not the real-time audio mixer.
 * Keep at least one core available for decoding, Media3 and the UI. When
 * memory, battery or thermal conditions deteriorate, prefer a slower
 * analysis (and its safe crossfade fallback) to starving playback.
 */
internal object AutoMixResourcePolicy {
    private const val MIB = 1024L * 1024L

    fun inferenceThreads(
        cores: Int,
        availableRamBytes: Long,
        lowMemoryThresholdBytes: Long = 0L,
        lowMemory: Boolean = false,
        lowRamDevice: Boolean = false,
        powerSave: Boolean = false,
        warm: Boolean = false,
        hot: Boolean = false,
    ): Int {
        val cpuBudget = when {
            cores <= 2 -> 1
            cores <= 4 -> 2
            cores <= 6 -> 3
            else -> 4
        }
        val nearMemoryThreshold = lowMemoryThresholdBytes > 0 &&
            availableRamBytes / 2 < lowMemoryThresholdBytes
        val ramBudget = when {
            lowMemory || lowRamDevice || nearMemoryThreshold -> 1
            availableRamBytes <= 0L -> 2 // Unknown: don't assume abundant RAM.
            availableRamBytes < 768 * MIB -> 1
            availableRamBytes < 1536 * MIB -> 2
            availableRamBytes < 2560 * MIB -> 3
            else -> 4
        }
        val heatBudget = when {
            hot -> 1
            warm -> 2
            else -> 4
        }
        return minOf(cpuBudget, ramBudget, heatBudget, if (powerSave) 2 else 4)
            .coerceAtLeast(1)
    }
}
