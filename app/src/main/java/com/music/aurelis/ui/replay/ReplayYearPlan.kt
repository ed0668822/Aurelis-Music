// SPDX-FileCopyrightText: 2026 Aurelis
// SPDX-License-Identifier: LGPL-3.0-or-later
package com.music.aurelis.ui.replay

import com.music.aurelis.data.stats.ReplaySummary

/**
 * Annual story pacing from BitChord v1.10, adapted to local Aurelis data.
 * Optional cards appear only when the listener has actual matching statistics.
 */
internal enum class ReplayYearMoment(val revealAtMs: Long) {
    OPENER(1_200L),
    MINUTES(1_600L),
    BUSIEST_DAY(1_600L),
    LISTENING_CLOCK(1_600L),
    GENRES(1_600L),
    ARTIST_REVEAL(3_400L),
    TOP_ARTISTS(1_600L),
    SONG_REVEAL(3_000L),
    TOP_SONGS(1_600L),
    ALBUMS(1_600L),
    MONTHS(1_600L),
    SUMMARY(1_600L),
}

/** Same user data drives the yearly card, the story and any future share media. */
internal fun ReplaySummary.yearMoments(): List<ReplayYearMoment> {
    if (isEmpty) return emptyList()
    return ReplayYearMoment.entries.filter { moment ->
        when (moment) {
            ReplayYearMoment.BUSIEST_DAY -> !busiestDay.isNullOrBlank()
            ReplayYearMoment.LISTENING_CLOCK -> annualListeningPersonality(hourOfDay) != null
            ReplayYearMoment.GENRES -> genres.isNotEmpty()
            ReplayYearMoment.ARTIST_REVEAL -> artists.isNotEmpty()
            ReplayYearMoment.TOP_ARTISTS -> artists.size >= 3
            ReplayYearMoment.SONG_REVEAL -> songs.isNotEmpty()
            ReplayYearMoment.TOP_SONGS -> songs.size >= 3
            ReplayYearMoment.ALBUMS -> albums.size >= 3
            ReplayYearMoment.MONTHS -> false // Only an annual snapshot knows monthly top tracks
            else -> true
        }
    }
}
