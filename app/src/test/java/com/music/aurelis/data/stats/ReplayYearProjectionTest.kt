// SPDX-FileCopyrightText: © 2026 Aurelis

package com.music.aurelis.data.stats

import com.music.aurelis.data.model.Song
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ReplayYearProjectionTest {
    private fun summary() = ReplaySummary(
        period = ReplayPeriod.THIS_YEAR,
        label = "2026",
        totalMs = 2500L,
        totalPlays = 3,
        songs = listOf(
            RankedSong(Song(videoId = "a", title = "A", artist = "Artist", thumbnailUrl = null, fromAutoplay = false), 1500L, 2),
            RankedSong(Song(videoId = "b", title = "B", artist = "Artist", thumbnailUrl = null, fromAutoplay = false), 1000L, 1),
        ),
        artists = emptyList(),
        albums = emptyList(),
        genres = emptyList(),
        hourOfDay = List(24) { 0L },
        busiestDay = "2026-02-04",
        busiestDayMs = 1600L,
        distinctSongs = 2,
        distinctArtists = 1,
        distinctAlbums = 0,
        since = "2026-01",
    )

    @Test fun ignoresOtherYearsAndInvalidCalendarDays() {
        val january = StoredBucket(
            month = "2026-01",
            tracks = listOf(TrackEntry(id = "a", title = "A", ms = 1000L, plays = 1)),
            days = mapOf(3 to 1000L, 33 to 900L),
        )
        val lastYear = StoredBucket(
            month = "2025-12",
            tracks = listOf(TrackEntry(id = "foreign", ms = 8000L)),
            days = mapOf(20 to 8000L),
        )
        val year = ReplayYearProjection.fromBuckets(2026, summary(), listOf(lastYear, january))
        assertEquals(mapOf("2026-01-03" to 1000L), year.dayMs)
        assertEquals(1, year.monthTops.size)
        assertEquals("a", year.monthTops.single().song.song.videoId)
        assertEquals("2026", year.summary.label)
    }

    @Test fun annualProjectionUsesRecordedMonthTopsAndBusiestDaySong() {
        val january = StoredBucket(
            month = "2026-01",
            tracks = listOf(TrackEntry(id = "a", ms = 700L, plays = 1)),
            days = mapOf(3 to 700L),
        )
        val february = StoredBucket(
            month = "2026-02",
            tracks = listOf(
                TrackEntry(id = "a", ms = 300L, days = mutableMapOf("2026-02-04" to DayStats(ms = 200L))),
                TrackEntry(id = "b", ms = 900L, days = mutableMapOf("2026-02-04" to DayStats(ms = 800L))),
            ),
            days = mapOf(4 to 1600L),
        )
        val year = ReplayYearProjection.fromBuckets(2026, summary(), listOf(february, january))
        assertEquals(listOf("a", "b"), year.monthTops.map { it.song.song.videoId })
        assertEquals("2026-02-04", year.summary.busiestDay)
        assertEquals("b", year.busiestDaySong?.videoId)
        assertEquals(2, year.monthsWithListening)
        assertEquals(1600L, year.dayMs["2026-02-04"])
    }

    @Test fun noListeningDoesNotCreateSyntheticDaysOrSongs() {
        val emptySummary = summary().copy(songs = emptyList())
        val year = ReplayYearProjection.fromBuckets(
            2026,
            emptySummary,
            listOf(StoredBucket(month = "2026-05")),
        )
        assertTrue(year.dayMs.isEmpty())
        assertTrue(year.monthTops.isEmpty())
        assertNull(year.busiestDaySong)
        assertNull(year.summary.busiestDay)
    }
}
