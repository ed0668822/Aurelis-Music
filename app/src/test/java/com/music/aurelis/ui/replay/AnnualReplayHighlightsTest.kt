// SPDX-FileCopyrightText: 2026 Aurelis
// SPDX-License-Identifier: LGPL-3.0-or-later

package com.music.aurelis.ui.replay

import com.music.aurelis.data.stats.ReplayPeriod
import com.music.aurelis.data.stats.ReplaySummary
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class AnnualReplayHighlightsTest {
    @Test fun `annual highlights use actual local totals and leave missing rankings empty`() {
        val data = ReplaySummary(
            period = ReplayPeriod.THIS_YEAR,
            label = "2026",
            totalMs = 3_600_000L,
            totalPlays = 12,
            songs = emptyList(),
            artists = emptyList(),
            albums = emptyList(),
            genres = emptyList(),
            hourOfDay = emptyList(),
            busiestDay = null,
            busiestDayMs = 0,
            distinctSongs = 0,
            distinctArtists = 0,
            distinctAlbums = 0,
            since = null,
        )
        val actual = data.annualHighlights(2026)
        assertEquals(2026, actual.year)
        assertEquals(60L, actual.minutes)
        assertEquals(12, actual.plays)
        assertNull(actual.topSong)
        assertNull(actual.topArtist)
    }
}
