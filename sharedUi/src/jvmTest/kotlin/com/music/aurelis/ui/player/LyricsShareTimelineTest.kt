// SPDX-FileCopyrightText: 2026 Aurelis
// SPDX-License-Identifier: LGPL-3.0-or-later
package com.music.aurelis.ui.player

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class LyricsShareTimelineTest {
    @Test fun unsynchronizedLyricsDoNotPretendToHaveVideoTiming() {
        assertNull(lyricsShareTimeline(listOf(LyricsShareLine("Hello", null))))
    }

    @Test fun sparseSelectionPreservesIntroGapAndOutro() {
        val timeline = lyricsShareTimeline(listOf(
            LyricsShareLine("First verse", null, startMs = 10_000L, endMs = 12_000L),
            LyricsShareLine("Second verse", null, startMs = 25_000L, endMs = 28_000L),
        ), trackDurationMs = 180_000L)!!
        assertEquals(180_000L, timeline.durationMs)
        assertEquals(
            listOf(
                LyricsVideoLineWindow(0, 10_000_000L, 12_000_000L),
                LyricsVideoLineWindow(1, 25_000_000L, 28_000_000L),
            ),
            timeline.windows,
        )
    }

    @Test fun gapsAndInvalidSamplesAreNotInferred() {
        val timeline = lyricsShareTimeline(listOf(
            LyricsShareLine("Start", null, startMs = 5_000L, endMs = 7_000L),
            LyricsShareLine("", null, isGap = true),
            LyricsShareLine("Bad", null, startMs = 9_000L, endMs = 8_000L),
            LyricsShareLine("End", null, startMs = 20_000L, endMs = 23_000L),
        ))!!
        assertEquals(listOf(0, 3), timeline.windows.map { it.lineIndex })
        assertEquals(23_000L, timeline.durationMs)
    }

    @Test fun trackEndAndOverlapsBoundTheLyricWindows() {
        val timeline = lyricsShareTimeline(listOf(
            LyricsShareLine("First verse", null, startMs = 8_000L, endMs = 12_000L),
            LyricsShareLine("Second verse", null, startMs = 10_000L, endMs = 16_000L),
            LyricsShareLine("After the end", null, startMs = 14_000L, endMs = 17_000L),
        ), trackDurationMs = 13_000L)!!
        assertEquals(listOf(10_000_000L, 13_000_000L), timeline.windows.map { it.toUs })
    }

    @Test fun durationLimitRejectsTheWholeTrackWithoutTrimmingIt() {
        val lines = listOf(LyricsShareLine("Verse", null, startMs = 1_000L, endMs = 2_000L))
        val limit = LYRICS_VIDEO_MAX_MINUTES * 60_000L
        assertFalse(lyricsShareTimeline(lines, limit)!!.exceedsVideoLimit)
        val overLimit = lyricsShareTimeline(lines, limit + 1)!!
        assertTrue(overLimit.exceedsVideoLimit)
        assertEquals(limit + 1, overLimit.durationMs)
    }
}
