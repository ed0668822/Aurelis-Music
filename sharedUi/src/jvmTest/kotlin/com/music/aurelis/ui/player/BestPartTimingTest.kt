// SPDX-FileCopyrightText: 2026 Aurelis
// SPDX-License-Identifier: LGPL-3.0-or-later
package com.music.aurelis.ui.player

import com.music.aurelis.data.lyrics.LyricLine
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class BestPartTimingTest {
    @Test fun repeatedSynchronizedChorusIsFound() {
        assertEquals(
            8_000L,
            bestPartChorusStart(
                listOf(
                    LyricLine(8_000L, "This is a chorus"),
                    LyricLine(75_000L, "This is a chorus"),
                ),
                180_000L,
            ),
        )
    }

    @Test fun missingOrUniqueLyricsDoNotProduceBestPart() {
        assertNull(bestPartChorusStart(emptyList(), 180_000L))
        assertNull(bestPartChorusStart(listOf(LyricLine(17_000L, "Only one verse")), 180_000L))
        assertNull(bestPartChorusStart(listOf(
            LyricLine(5_000L, "Repeated lines"),
            LyricLine(20_000L, "Repeated lines"),
        ), 40_000L))
    }
}
