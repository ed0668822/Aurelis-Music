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
    @Test fun overlappingVoicesRemainFocusedUntilTheirActualEnds() {
        val first = LyricLine(0, "Uno", listOf(LyricWord(0, 3000, "Uno")))
        val second = LyricLine(1000, "Dos", listOf(LyricWord(1000, 2000, "Dos")))
        assertEquals(listOf(0, 1), activeLyricRows(listOf(first, second), 1500))
        assertEquals(listOf(1), activeLyricRows(listOf(first, second), 3500))
    }
}
