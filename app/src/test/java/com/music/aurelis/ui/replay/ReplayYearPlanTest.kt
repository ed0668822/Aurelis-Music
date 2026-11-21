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
    @Test fun annualMomentsLinkToExistingReplaySharePages() {
        assertEquals(ReplayStoryPage.INTRO, ReplayYearMoment.OPENER.storyPage())
        assertEquals(ReplayStoryPage.HABITS, ReplayYearMoment.BUSIEST_DAY.storyPage())
        assertEquals(ReplayStoryPage.ARTISTS, ReplayYearMoment.ARTIST_REVEAL.storyPage())
        assertEquals(ReplayStoryPage.SONGS, ReplayYearMoment.SONG_REVEAL.storyPage())
        assertEquals(ReplayStoryPage.SUMMARY, ReplayYearMoment.SUMMARY.storyPage())
    }

}
