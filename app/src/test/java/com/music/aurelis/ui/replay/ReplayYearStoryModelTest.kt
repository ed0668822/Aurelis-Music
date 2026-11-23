package com.music.aurelis.ui.replay

import com.music.aurelis.data.model.Song
import com.music.aurelis.data.stats.RankedEntry
import com.music.aurelis.data.stats.RankedSong
import com.music.aurelis.data.stats.ReplayPeriod
import com.music.aurelis.data.stats.ReplaySummary
import com.music.aurelis.data.stats.ReplayYearData
import com.music.aurelis.data.stats.ReplayMonthTop
import java.time.YearMonth
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ReplayYearStoryModelTest {
    private fun year(count: Int): ReplayYearData {
        val tracks = List(count) { index ->
            RankedSong(
                Song(
                    videoId = "song" + index,
                    title = "Track " + index,
                    artist = if (index < 3) "Lead" else "Second",
                    thumbnailUrl = null,
                    fromAutoplay = false,
                ),
                (20 - index).toLong() * 1000L,
                1,
            )
        }
        val summary = ReplaySummary(
            period = ReplayPeriod.THIS_YEAR,
            label = "2026",
            totalMs = tracks.sumOf { it.ms },
            totalPlays = tracks.size,
            songs = tracks,
            artists = listOf(
                RankedEntry("Lead", null, null, null, 10000L, 3),
                RankedEntry("Second", null, null, null, 8000L, 3),
            ),
            albums = emptyList(),
            genres = emptyList(),
            hourOfDay = List(24) { 100L },
            busiestDay = null,
            busiestDayMs = 0L,
            distinctSongs = count,
            distinctArtists = 2,
            distinctAlbums = 0,
            since = "2026-01",
        )
        return ReplayYearData(2026, summary, emptyMap(), emptyList(), null)
    }

    @Test fun limitedHistorySkipsCardsWithNoEvidence() {
        val story = ReplayYearStoryModel(year(1))
        assertFalse(ReplayYearMoment.TOP_SONGS in story.moments)
        assertFalse(ReplayYearMoment.BUSIEST_DAY in story.moments)
        assertEquals(ReplayYearMoment.SUMMARY, story.moments.last())
        assertEquals("song0", story.clips[ReplayYearMoment.SONG_REVEAL]?.videoId)
    }

    @Test fun numberOneReservedForRevealAndSummary() {
        val story = ReplayYearStoryModel(year(11))
        assertEquals("song0", story.clips[ReplayYearMoment.SONG_REVEAL]?.videoId)
        assertEquals("song0", story.clips[ReplayYearMoment.SUMMARY]?.videoId)
        assertTrue(story.clips.filterKeys {
            it != ReplayYearMoment.SONG_REVEAL && it != ReplayYearMoment.SUMMARY
        }.values.none { it?.videoId == "song0" })
    }

    @Test fun monthPageUsesOnlyRecordedMonthTops() {
        val original = year(12)
        val withMonths = original.copy(monthTops = listOf(
            ReplayMonthTop(YearMonth.of(2026, 1), original.summary.songs[5]),
            ReplayMonthTop(YearMonth.of(2026, 2), original.summary.songs[6]),
        ))
        val story = ReplayYearStoryModel(withMonths)
        assertTrue(ReplayYearMoment.MONTHS in story.moments)
        assertEquals(ReplayYearMoment.SUMMARY, story.moments.last())
        assertEquals("song6", story.clips[ReplayYearMoment.MONTHS]?.videoId)
        assertTrue(ReplayYearMoment.MONTHS !in ReplayYearStoryModel(original).moments)
    }

    @Test fun nonHeadlineClipsDoNotReuseTopFive() {
        val story = ReplayYearStoryModel(year(12))
        val reserved = setOf(
            ReplayYearMoment.SONG_REVEAL,
            ReplayYearMoment.SUMMARY,
            ReplayYearMoment.ARTIST_REVEAL,
            ReplayYearMoment.TOP_ARTISTS,
            ReplayYearMoment.TOP_SONGS,
        )
        val rest = story.clips.filterKeys { it !in reserved }.values.filterNotNull()
        assertTrue(rest.none { it.videoId in setOf("song0", "song1", "song2", "song3", "song4") })
        assertEquals(rest.size, rest.map { it.videoId }.distinct().size)
    }
}
