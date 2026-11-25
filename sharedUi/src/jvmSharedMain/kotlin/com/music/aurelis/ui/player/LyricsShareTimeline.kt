// SPDX-FileCopyrightText: 2026 Aurelis
// SPDX-License-Identifier: LGPL-3.0-or-later
package com.music.aurelis.ui.player

const val LYRICS_VIDEO_MAX_MINUTES = 10

data class LyricsVideoLineWindow(val lineIndex: Int, val fromUs: Long, val toUs: Long)

data class LyricsShareTimeline(
    val windows: List<LyricsVideoLineWindow>,
    val durationMs: Long,
) {
    val exceedsVideoLimit: Boolean
        get() = durationMs > LYRICS_VIDEO_MAX_MINUTES * 60_000L
}

/** Places selected lines on the original track clock, leaving unselected passages empty. */
fun lyricsShareTimeline(lines: List<LyricsShareLine>, trackDurationMs: Long? = null): LyricsShareTimeline? {
    val knownDuration = trackDurationMs?.takeIf { it > 0L }
    val timed = lines.mapIndexedNotNull { index, line ->
        if (line.isGap || line.text.isBlank()) return@mapIndexedNotNull null
        val start = line.startMs ?: return@mapIndexedNotNull null
        val end = line.endMs ?: return@mapIndexedNotNull null
        val boundedEnd = minOf(end, knownDuration ?: end)
        if (start < 0L || boundedEnd <= start || boundedEnd > Long.MAX_VALUE / 1000L) return@mapIndexedNotNull null
        LyricsVideoLineWindow(index, start * 1000L, boundedEnd * 1000L)
    }.sortedBy { it.fromUs }.distinctBy { it.fromUs }
    if (timed.isEmpty()) return null
    val windows = timed.mapIndexed { index, window ->
        val nextStart = timed.getOrNull(index + 1)?.fromUs ?: window.toUs
        window.copy(toUs = minOf(window.toUs, nextStart))
    }
    return LyricsShareTimeline(windows, knownDuration ?: windows.last().toUs / 1000L)
}
