// SPDX-FileCopyrightText: 2026 Aurelis
// SPDX-License-Identifier: LGPL-3.0-or-later
package com.music.aurelis.ui.player

/** Audio-clock mapping for a future MP4 export, independent of rendering or player. */
internal data class LyricsVideoLineWindow(val lineIndex: Int, val fromUs: Long, val toUs: Long)
internal data class LyricsShareTimeline(
    val segments: List<Pair<Long, Long>>,
    val windows: List<LyricsVideoLineWindow>,
    val durationMs: Long,
) {
    fun trackMsAt(outputUs: Long): Long {
        var offsetMs = (outputUs / 1000L).coerceAtLeast(0L)
        for ((start, end) in segments) {
            val length = (end - start).coerceAtLeast(0L)
            if (offsetMs < length) return start + offsetMs
            offsetMs -= length
        }
        return segments.lastOrNull()?.second ?: 0L
    }
}

/**
 * Builds contiguous excerpts for lyrics video sharing. Keeps gaps explicit,
 * rejects unknown/invalid timestamps and limits output to a minute.
 * Never invents timing for lyrics whose provider has no synchronized data.
 */
internal fun lyricsShareTimeline(lines: List<LyricsShareLine>): LyricsShareTimeline? {
    val segments = mutableListOf<Pair<Long, Long>>()
    val windows = mutableListOf<LyricsVideoLineWindow>()
    var outputMs = 0L
    var lastGap = false
    for ((index, line) in lines.withIndex()) {
        if (line.isGap) {
            lastGap = true
            continue
        }
        val start = line.startMs ?: continue
        val end = line.endMs ?: continue
        if (start < 0L || end - start < 300L || outputMs >= 60_000L) continue
        val duration = minOf(end - start, 60_000L - outputMs)
        if (duration < 300L) break
        // Adjacent lines in the same passage share one source segment.
        val previous = segments.lastOrNull()
        if (!lastGap && previous != null && start in previous.second..(previous.second + 500L)) {
            segments[segments.lastIndex] = previous.first to (start + duration)
        } else {
            segments += start to (start + duration)
        }
        windows += LyricsVideoLineWindow(index, outputMs * 1000L, (outputMs + duration) * 1000L)
        outputMs += duration
        lastGap = false
    }
    return if (windows.isEmpty()) null else LyricsShareTimeline(segments, windows, outputMs)
}
