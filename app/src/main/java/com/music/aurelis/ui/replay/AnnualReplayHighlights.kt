// SPDX-FileCopyrightText: 2026 Aurelis
// SPDX-License-Identifier: LGPL-3.0-or-later

package com.music.aurelis.ui.replay

import com.music.aurelis.data.stats.ReplaySummary

/** B09 — Snapshot of the actual local listening year, with no invented statistics. */
data class AnnualReplayHighlights(
    val year: Int,
    val minutes: Long,
    val plays: Int,
    val topSong: String?,
    val topArtist: String?,
)

fun ReplaySummary.annualHighlights(year: Int): AnnualReplayHighlights =
    AnnualReplayHighlights(
        year = year,
        minutes = minutes,
        plays = totalPlays,
        topSong = songs.firstOrNull()?.song?.title?.takeIf { it.isNotBlank() },
        topArtist = artists.firstOrNull()?.title?.takeIf { it.isNotBlank() },
    )
