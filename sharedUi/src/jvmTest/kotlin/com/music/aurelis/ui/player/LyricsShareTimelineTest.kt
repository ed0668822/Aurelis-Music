// SPDX-FileCopyrightText: 2026 Aurelis
// SPDX-License-Identifier: LGPL-3.0-or-later
package com.music.aurelis.ui.player

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class LyricsShareTimelineTest {
    @Test fun unsynchronizedLyricsDoNotPretendToHaveVideoTiming() {
        assertNull(lyricsShareTimeline(listOf(LyricsShareLine("Hello", null))))
    }

    @Test fun selectedTimedLinesFollowSourceClock() {
        val timeline = lyricsShareTimeline(listOf(
            LyricsShareLine("First verse", null, startMs = 10_000L, endMs = 12_000L),
            LyricsShareLine("Second verse", null, startMs = 25_000L, endMs = 28_000L),
        ))!!
        assertEquals(5_000L, timeline.durationMs)
        assertEquals(10_500L, timeline.trackMsAt(500_000L))
        assertEquals(26_000L, timeline.trackMsAt(3_000_000L))
        assertEquals(2, timeline.windows.size)
    }

    @Test fun gapsAndInvalidSamplesAreNotInferred() {
        val timeline = lyricsShareTimeline(listOf(
            LyricsShareLine("Start", null, startMs = 5_000L, endMs = 7_000L),
            LyricsShareLine("", null, isGap = true),
            LyricsShareLine("Bad", null, startMs = 9_000L, endMs = 9_100L),
            LyricsShareLine("End", null, startMs = 20_000L, endMs = 23_000L),
        ))!!
        assertEquals(2, timeline.segments.size)
        assertEquals(5_000L, timeline.durationMs)
    }
}
