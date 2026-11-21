// SPDX-FileCopyrightText: 2026 Aurelis
// SPDX-License-Identifier: LGPL-3.0-or-later
package com.music.aurelis.ui.replay

import com.music.aurelis.data.model.Song
import com.music.aurelis.data.stats.ListeningStats
import com.music.aurelis.data.stats.ReplayYearData
import java.util.Locale

/**
 * B01: immutable preparation for the animated annual cards (B02).
 * A clip is selected once for the entire story, never per recomposition.
 */
internal class ReplayYearStoryModel(val data: ReplayYearData) {
    val moments: List<ReplayYearMoment> = data.summary.yearMoments()
    val personality: AnnualListeningPersonality? =
        annualListeningPersonality(data.summary.hourOfDay)
    val clips: Map<ReplayYearMoment, Song?> = chooseClips()

    private fun chooseClips(): Map<ReplayYearMoment, Song?> {
        val ranked = data.summary.songs.map { it.song }
        val numberOne = ranked.firstOrNull()
        val topFive = ranked.take(5).mapTo(HashSet()) { it.videoId }
        val used = HashSet<String>().apply { numberOne?.let { add(it.videoId) } }
        val results = LinkedHashMap<ReplayYearMoment, Song?>()

        fun take(candidates: Iterable<Song>, allowTop: Boolean = false): Song? {
            val next = candidates.firstOrNull {
                it.videoId !in used && (allowTop || it.videoId !in topFive)
            }
            if (next != null) used.add(next.videoId)
            return next
        }
        fun matchingArtist(name: String?): List<Song> {
            if (name.isNullOrBlank()) return emptyList()
            val wanted = name.trim().lowercase(Locale.ROOT)
            return ranked.filter {
                (ListeningStats.primaryArtist(it.artist) ?: it.artist)
                    .trim().lowercase(Locale.ROOT) == wanted
            }
        }

        for (moment in moments) {
            val clip = when (moment) {
                ReplayYearMoment.SONG_REVEAL, ReplayYearMoment.SUMMARY -> numberOne
                ReplayYearMoment.ARTIST_REVEAL ->
                    take(matchingArtist(data.summary.artists.firstOrNull()?.title), allowTop = true)
                ReplayYearMoment.TOP_ARTISTS ->
                    take(matchingArtist(data.summary.artists.getOrNull(1)?.title), allowTop = true)
                ReplayYearMoment.TOP_SONGS -> take(ranked.take(5), allowTop = true)
                ReplayYearMoment.BUSIEST_DAY ->
                    take(listOfNotNull(data.busiestDaySong)) ?: take(ranked.drop(5))
                ReplayYearMoment.ALBUMS -> {
                    val album = data.summary.albums.firstOrNull()?.title
                    take(ranked.filter { !album.isNullOrBlank() && it.albumName == album })
                        ?: take(ranked.drop(5))
                }
                else -> take(ranked.drop(5))
            }
            results[moment] = clip
        }
        return results
    }
}
