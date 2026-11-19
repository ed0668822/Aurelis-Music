// SPDX-FileCopyrightText: 2026 Aurelis
// SPDX-License-Identifier: LGPL-3.0-or-later
package com.music.aurelis.ui.player

import com.music.aurelis.data.lyrics.LyricLine
import com.music.aurelis.data.lyrics.LyricSyllable
import com.music.aurelis.data.lyrics.LyricWord
import org.junit.Assert.*
import org.junit.Test

class LyricTimingTest {
    @Test fun positiveOffsetDelaysLyricsAndSeekingIsItsInverse() {
        assertEquals(1000L, adjustedLyricsPosition(2000, 1000))
        assertEquals(2000L, adjustedLyricsSeekTarget(1000, 1000))
    }
    @Test fun negativeOffsetAdvancesLyricsWithoutOverflowAtTrackEdges() {
        assertEquals(2000L, adjustedLyricsPosition(1000, -1000))
        assertEquals(1000L, adjustedLyricsSeekTarget(2000, -1000))
        assertEquals(0L, adjustedLyricsPosition(500, 1000))
        assertEquals(0L, adjustedLyricsSeekTarget(500, -1000))
        assertEquals(Long.MAX_VALUE, adjustedLyricsSeekTarget(Long.MAX_VALUE - 100, 1000))
        assertEquals(Long.MAX_VALUE, adjustedLyricsPosition(Long.MAX_VALUE - 100, -1000))
    }
    @Test fun frameClockInterpolatesBetweenPlayerSamples() {
        val clock = LyricClock(1000)
        assertEquals(1000L, clock.frame(0.0, 1000, 0.0, 0))
        assertEquals(1100L, clock.frame(100.0, 1000, 0.0, 0))
        assertEquals(1200L, clock.frame(200.0, 1000, 0.0, 0))
    }
    @Test fun restartingAfterPauseDoesNotExtrapolateAStaleSample() {
        val clock = LyricClock(1000)
        clock.frame(0.0, 1000, 0.0, 0)
        clock.hold(2000, 1000.0)
        assertEquals(2000L, clock.frame(5000.0, 2000, 900.0, 0))
        assertEquals(2100L, clock.frame(5100.0, 2000, 900.0, 0))
    }
    @Test fun explicitForwardAndBackwardSeeksCanReanchorTheClock() {
        val clock = LyricClock(1000)
        clock.frame(0.0, 1000, 0.0, 0)
        assertEquals(10_000L, clock.frame(100.0, 10_000, 100.0, 1))
        assertEquals(500L, clock.frame(200.0, 500, 200.0, 2))
    }
    @Test fun multipleRapidSeeksReanchorWithoutAccumulatingDrift() {
        val clock = LyricClock(0)
        assertEquals(0L, clock.frame(0.0, 0, 0.0, 0))
        assertEquals(40_000L, clock.frame(100.0, 40_000, 100.0, 1))
        assertEquals(2000L, clock.frame(200.0, 2000, 200.0, 2))
        assertEquals(17_000L, clock.frame(300.0, 17_000, 300.0, 3))
        assertEquals(17_100L, clock.frame(400.0, 17_000, 300.0, 3))
    }
    @Test fun fasterPlaybackLearnsItsRateWithoutAnExtraPlaybackClock() {
        val clock = LyricClock(1000)
        clock.frame(0.0, 1000, 0.0, 0)
        clock.frame(500.0, 1750, 500.0, 0)
        clock.frame(1000.0, 2500, 1000.0, 0)
        clock.frame(1500.0, 3250, 1500.0, 0)
        val observed = clock.frame(1600.0, 3250, 1500.0, 0)
        assertTrue("1.5x lyric clock did not follow the player: $observed", observed in 3300L..3500L)
    }
    @Test fun pauseAndResumeAtSpeedDoNotExtrapolateAcrossStoppedTime() {
        val clock = LyricClock(0)
        clock.frame(0.0, 1000, 0.0, 0)
        clock.hold(2000, 1000.0)
        assertEquals(2000L, clock.frame(60_000.0, 2000, 900.0, 0))
        assertTrue(clock.frame(60_100.0, 2000, 900.0, 0) in 2050L..2400L)
        clock.hold(3000, 60_200.0)
        assertEquals(3000L, clock.displayedMs.toLong())
    }
    @Test fun anUnannouncedOldReadingDoesNotMoveWordsBackwards() {
        val clock = LyricClock(3000)
        clock.frame(0.0, 3000, 0.0, 0)
        val before = clock.frame(100.0, 3000, 0.0, 0)
        assertTrue(clock.frame(200.0, 1000, 200.0, 0) >= before)
    }
    @Test fun revealDoesNotSplitSurrogatePairsOrCombiningClusters() {
        val breaks = lyricCharacterBreaks("🌙a\u0301b")
        assertArrayEquals(intArrayOf(0, 2, 4, 5), breaks)
        assertEquals(LyricReveal(0, 2, 0.5f), lyricReveal(breaks, 1f))
        assertEquals(LyricReveal(2, 4, 0.5f), lyricReveal(breaks, 3f))
        assertEquals(LyricReveal(5, 5, 0f), lyricReveal(breaks, 99f))
        assertEquals(LyricReveal(0, 2, 0f), lyricReveal(breaks, Float.NaN))
    }
    @Test fun timedSyllablesDriveTheSameRevealUsedByThePainter() {
        val word = LyricWord(1000, 3000, "Luna", listOf(LyricSyllable(1000, 2000, 0, 2), LyricSyllable(2000, 3000, 2, 4)))
        val line = LyricLine(1000, "Luna", listOf(word))
        assertEquals(1f, line.revealedChars(1500), 0f)
        assertEquals(3f, line.revealedChars(2500), 0f)
        assertEquals(LyricReveal(3, 4, 0f), lyricReveal(lyricCharacterBreaks(line.text), line.revealedChars(2500)))
    }
    @Test fun rtlScriptUsesRightPhysicalAlignmentWhenSolo() {
        val base = androidx.compose.ui.text.TextStyle.Default
        val hebrew = lyricVoiceStyle(base, "שלום עולם", false, false)
        assertTrue(lyricTextIsRtl("مرحبا بالعالم"))
        assertTrue(lyricTextIsRtl("שלום עולם"))
        assertEquals(androidx.compose.ui.text.style.TextAlign.Right, hebrew.textAlign)
        assertEquals(androidx.compose.ui.text.style.TextDirection.Rtl, hebrew.textDirection)
        assertEquals(androidx.compose.ui.text.style.TextAlign.Left,
            lyricVoiceStyle(base, "Hello world", false, false).textAlign)
    }
    @Test fun duetLaneIsPhysicalButBackgroundVoiceShapesItsOwnScript() {
        val base = androidx.compose.ui.text.TextStyle.Default
        val lead = lyricVoiceStyle(base, "السلام", false, true)
        val answering = lyricVoiceStyle(base, "Hello", true, true)
        assertEquals(androidx.compose.ui.text.style.TextAlign.Left, lead.textAlign)
        assertEquals(androidx.compose.ui.text.style.TextDirection.Rtl, lead.textDirection)
        assertEquals(androidx.compose.ui.text.style.TextAlign.Right, answering.textAlign)
        assertEquals(androidx.compose.ui.text.style.TextDirection.Ltr, answering.textDirection)
    }
    @Test fun overlappingVoicesRemainFocusedUntilTheirActualEnds() {
        val first = LyricLine(0, "Uno", listOf(LyricWord(0, 3000, "Uno")))
        val second = LyricLine(1000, "Dos", listOf(LyricWord(1000, 2000, "Dos")))
        assertEquals(listOf(0, 1), activeLyricRows(listOf(first, second), 1500))
        assertEquals(listOf(1), activeLyricRows(listOf(first, second), 3500))
    }
}
