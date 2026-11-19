// SPDX-FileCopyrightText: 2026 Aurelis
// SPDX-License-Identifier: LGPL-3.0-or-later
package com.music.aurelis.ui.player

/** The dots follow the actual instrumental interval, never the anticipatory scroll clock. */
internal fun lyricGapProgress(positionMs: Long, startMs: Long, endMs: Long): Float {
    if (positionMs <= startMs) return if (endMs <= startMs && positionMs == startMs) 1f else 0f
    if (positionMs >= endMs) return 1f
    if (endMs <= startMs) return 0f
    val span = endMs.toDouble() - startMs.toDouble()
    return ((positionMs.toDouble() - startMs.toDouble()) / span).toFloat().coerceIn(0f, 1f)
}

/** Each of the three dots progressively fills a third of the instrumental time. */
internal fun lyricGapDotFill(progress: Float, dot: Int): Float =
    (progress.coerceIn(0f, 1f) * 3f - dot.coerceIn(0, 2)).coerceIn(0f, 1f)
