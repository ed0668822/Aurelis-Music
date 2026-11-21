// SPDX-FileCopyrightText: 2026 Aurelis
// SPDX-License-Identifier: LGPL-3.0-or-later
package com.music.aurelis.ui.replay

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ReplayYearPlanTest {
    @Test fun annualRevealsAreOrderedFromOpenerToSummary() {
        val moments = ReplayYearMoment.entries
        assertEquals(ReplayYearMoment.OPENER, moments.first())
        assertEquals(ReplayYearMoment.SUMMARY, moments.last())
        assertEquals(moments.size, moments.distinct().size)
    }

    @Test fun musicalRevealsHoldLongerThanHeadlineSlides() {
        assertTrue(ReplayYearMoment.ARTIST_REVEAL.revealAtMs > ReplayYearMoment.MINUTES.revealAtMs)
        assertTrue(ReplayYearMoment.SONG_REVEAL.revealAtMs > ReplayYearMoment.GENRES.revealAtMs)
    }
}
