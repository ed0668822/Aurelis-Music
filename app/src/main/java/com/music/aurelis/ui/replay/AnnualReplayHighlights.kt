// SPDX-FileCopyrightText: 2026 Aurelis
// SPDX-License-Identifier: LGPL-3.0-or-later

package com.music.aurelis.ui.replay

import com.music.aurelis.data.stats.ReplaySummary
import com.music.aurelis.data.model.Song

/** B09 — Snapshot of the actual local listening year, with no invented statistics. */
data class AnnualReplayHighlights(
    val year: Int,
    val minutes: Long,
    val plays: Int,
    val topSong: String?,
    val topArtist: String?,
    val topAlbum: String?,
    val topGenre: String?,
    val uniqueSongs: Int,
    val busiestDay: String?,
    val personality: AnnualListeningPersonality?,
    /** A locally stored song that exists in this year's rankings. */
    val localHighlight: Song?,
)

/** Based solely on the local listening histogram. Never infer a personality without data. */
enum class AnnualListeningPersonality {
    NIGHT_OWL, EARLY_BIRD, GOLDEN_HOUR, DAYTIME, ALL_DAY
}

/** The year story thresholds from BitChord v1.10, adapted to Aurelis local statistics. */
internal fun annualListeningPersonality(hours: List<Long>): AnnualListeningPersonality? {
    val total = hours.sumOf { it.coerceAtLeast(0L) }.toDouble()
    if (total <= 0.0) return null
    fun share(range: IntRange) = range.sumOf { hours.getOrElse(it) { 0L }.coerceAtLeast(0L) }.toDouble() / total
    val night = share(22..23) + share(0..3)
    return when {
        night >= 0.30 -> AnnualListeningPersonality.NIGHT_OWL
        share(5..9) >= 0.30 -> AnnualListeningPersonality.EARLY_BIRD
        share(17..21) >= 0.40 -> AnnualListeningPersonality.GOLDEN_HOUR
        share(10..16) >= 0.50 -> AnnualListeningPersonality.DAYTIME
        else -> AnnualListeningPersonality.ALL_DAY
    }
}

fun ReplaySummary.annualHighlights(year: Int): AnnualReplayHighlights =
    AnnualReplayHighlights(
        year = year,
        minutes = minutes,
        plays = totalPlays,
        topSong = songs.firstOrNull()?.song?.title?.takeIf { it.isNotBlank() },
        topArtist = artists.firstOrNull()?.title?.takeIf { it.isNotBlank() },
        topAlbum = albums.firstOrNull()?.title?.takeIf { it.isNotBlank() },
        topGenre = genres.firstOrNull()?.title?.takeIf { it.isNotBlank() },
        uniqueSongs = distinctSongs,
        busiestDay = busiestDay?.takeIf { it.isNotBlank() },
        personality = annualListeningPersonality(hourOfDay),
        localHighlight = songs.firstOrNull { it.song.localUri != null }?.song,
    )
