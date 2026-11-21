// SPDX-FileCopyrightText: 2026 Aurelis
// SPDX-License-Identifier: LGPL-3.0-or-later
package com.music.aurelis.ui.replay

import com.music.aurelis.data.lyrics.LyricLine
import java.util.Locale

/**
 * Pure, offline chorus-based Best Part candidate for Replay Year.
 * No network heatmap or fabricated audio analysis. Returns null when synced
 * lyrics cannot establish a repeated passage with reasonable confidence.
 */
internal object ReplayBestPart {
    private const val MIN_DURATION_MS = 60_000L
    private const val SKIP_HEAD_MS = 5_000L
    private const val SKIP_TAIL_MS = 30_000L

    fun chorusStart(lines: List<LyricLine>, durationMs: Long): Long? {
        if (durationMs < MIN_DURATION_MS) return null
        val eligible = lines.mapNotNull { line ->
            val text = line.text.trim().lowercase(Locale.ROOT)
                .replace(Regex("[^\\p{L}\\p{N}\\s]"), "")
                .replace(Regex("\\s+"), " ")
                .trim()
            if (text.length < 8 || line.timeMs < SKIP_HEAD_MS ||
                line.timeMs > durationMs - SKIP_TAIL_MS
            ) null else line.timeMs to text
        }
        // A chorus normally repeats a recognizable lyric. One duplicated word
        // or a single uncorroborated line is not evidence of a chorus.
        val repeated = eligible.groupBy { it.second }.filterValues { it.size >= 2 }
        return repeated.values.mapNotNull { copies ->
            val first = copies.minOf { it.first }
            val last = copies.maxOf { it.first }
            first.takeIf { last - first >= 12_000L }
        }.minOrNull()
    }
}
