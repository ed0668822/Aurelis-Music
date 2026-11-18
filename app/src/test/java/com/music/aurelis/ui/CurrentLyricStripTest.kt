// SPDX-FileCopyrightText: 2026 Aurelis
// SPDX-License-Identifier: LGPL-3.0-or-later
package com.music.aurelis.ui

import com.music.aurelis.data.lyrics.LyricLine
import com.music.aurelis.ui.components.currentLyricStripIndex
import org.junit.Assert.assertEquals
import org.junit.Test

class CurrentLyricStripTest {
    @Test fun returnsIntroGapAndCorrectlyAdvancesThroughRepeatedStamps() {
        val lines = listOf(
            LyricLine(0, ""),
            LyricLine(1_000, "first"),
            LyricLine(1_000, "second"),
            LyricLine(2_000, ""),
            LyricLine(4_000, "third"),
        )
        assertEquals(-1, currentLyricStripIndex(lines, -1))
        assertEquals(0, currentLyricStripIndex(lines, 500))
        assertEquals(2, currentLyricStripIndex(lines, 1_000))
        assertEquals(3, currentLyricStripIndex(lines, 2_500))
        assertEquals(4, currentLyricStripIndex(lines, 9_000))
    }

    @Test fun emptyLyricsAreHandledWithoutIndexErrors() {
        assertEquals(-1, currentLyricStripIndex(emptyList(), 4_000))
    }
}
