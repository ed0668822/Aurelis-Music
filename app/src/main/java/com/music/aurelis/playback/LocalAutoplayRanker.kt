// SPDX-FileCopyrightText: 2026 Aurelis
// SPDX-License-Identifier: LGPL-3.0-or-later

package com.music.aurelis.playback

import com.music.aurelis.data.model.Song
import com.music.aurelis.data.model.QueueTier
import com.music.aurelis.playback.QueueCoordinator.asQueueEntry
import kotlin.math.abs
import kotlin.math.ln

/**
 * B07: deterministic, pure ranking of already-known local tracks.
 * No decoding, database queries, network access or model inference.
 */
internal object LocalAutoplayRanker {
    fun suggest(
        seed: Song,
        candidates: List<Song>,
        existing: List<Song>,
        likedUris: Set<String>,
        playCounts: Map<String, Int>,
        limit: Int,
        bpmFor: (String) -> Double? = { null },
    ): List<Song> {
        if (limit <= 0 || seed.localUri.isNullOrBlank()) return emptyList()
        val excluded = existing.mapNotNullTo(HashSet()) { it.localUri }
        excluded += seed.localUri
        val seen = HashSet<String>()
        val pool = candidates.asSequence().filter { song ->
            val uri = song.localUri
            !uri.isNullOrBlank() && uri !in excluded && seen.add(uri)
        }.take(850).toList()
        if (pool.isEmpty()) return emptyList()

        val recentArtists = existing.takeLast(8).map { it.artist.lowercase().trim() }
        val artistsSelected = ArrayList<String>()
        val chosen = ArrayList<Song>(minOf(limit, pool.size))
        val used = HashSet<String>()
        val seedBpm = bpmFor(seed.videoId)?.takeIf { it in 45.0..230.0 }
        val seedArtist = seed.artist.lowercase().trim()
        repeat(minOf(limit, pool.size)) { index ->
            val best = pool.asSequence().filter { it.localUri !in used }.maxByOrNull { song ->
                val artist = song.artist.lowercase().trim()
                val count = playCounts[song.videoId].orZero()
                val candidateBpm = bpmFor(song.videoId)?.takeIf { it in 45.0..230.0 }
                val bpmDistance = if (seedBpm != null && candidateBpm != null) {
                    minOf(
                        abs(seedBpm - candidateBpm),
                        abs(seedBpm - candidateBpm * 2.0),
                        abs(seedBpm * 2.0 - candidateBpm),
                    )
                } else null
                val bpmAffinity = if (bpmDistance != null) (25.0 - bpmDistance).coerceAtLeast(0.0) else 0.0
                val favorite = if (song.localUri in likedUris) 18.0 else 0.0
                val history = ln(1.0 + count.coerceAtLeast(0)) * 4.0
                val artistAffinity = if (artist == seedArtist) 13.0 else 0.0
                val varietyPenalty = artistsSelected.count { it == artist } * 20.0 +
                    recentArtists.count { it == artist } * 7.0
                // Stable tie-break avoids always choosing alphabetic first.
                val rotation = (song.localUri.orEmpty().hashCode() xor
                    seed.videoId.hashCode() xor index).ushr(1) % 1000 / 1000.0
                favorite + history + artistAffinity + bpmAffinity - varietyPenalty + rotation
            } ?: return chosen.map { it.asQueueEntry(QueueTier.AUTOPLAY) }
            chosen += best
            artistsSelected += best.artist.lowercase().trim()
            used += best.localUri.orEmpty()
        }
        return chosen.map { it.asQueueEntry(QueueTier.AUTOPLAY) }
    }

    private fun Int?.orZero(): Int = this ?: 0
}
