package com.music.aurelis.playback

import com.music.aurelis.data.LocalListening
import com.music.aurelis.data.LocalSearch
import com.music.aurelis.data.LocalUserLibraryState
import com.music.aurelis.data.model.LocalSong
import kotlin.math.abs
import kotlin.math.ln

/** Metadata-only local recommendation. No network, decoding, inferred BPM or crossfade. */
object LocalAutoMix {
    private data class Candidate(val song: LocalSong, val artists: Set<String>, val title: String, val genres: Set<String>, val tie: Int)
    fun build(
        songs: List<LocalSong>, user: LocalUserLibraryState, seedId: String? = null,
        limit: Int = 20, nowMs: Long = System.currentTimeMillis(), variation: Long = 0,
    ): List<LocalSong> {
        if (limit <= 0) return emptyList()
        val pool = songs.asSequence().filter { it.id.isNotBlank() && it.durationMs >= 500 &&
            (it.contentUri.startsWith("content://") || it.contentUri.startsWith("file://")) }
            .distinctBy { it.id }.map { song ->
                val artistName = LocalSearch.normalize(song.artist)
                val artists = if (song.artistId == null && artistName in UNKNOWN_ARTISTS) emptySet()
                    else QueueBuilder.artistSet(song.artist)
                Candidate(song, artists, LocalSearch.normalize(QueueBuilder.normalisedTitle(song.title)), genres(song.genre), tie(song.id, variation))
            }.toList()
        if (pool.isEmpty()) return emptyList()
        val byId = pool.associateBy { it.song.id }
        val seed = byId[seedId] ?: user.listening.entries.sortedByDescending { it.value.lastPlayedAt }
            .firstNotNullOfOrNull { byId[it.key] } ?: pool.filter { it.song.id in user.favorites }.minByOrNull { it.tie } ?: pool.minBy { it.tie }
        val recent = user.listening.entries.asSequence().filter { it.value.lastPlayedAt > 0 &&
            it.value.lastPlayedAt >= nowMs.coerceAtLeast(0) - RECENT_WINDOW_MS }
            .sortedByDescending { it.value.lastPlayedAt }.take(5).map { it.key }.toSet()
        val preferredArtists = mutableMapOf<String, Double>(); val preferredGenres = mutableMapOf<String, Double>()
        for (candidate in pool) {
            val stats = user.listening[candidate.song.id]
            val affinity = (if (candidate.song.id in user.favorites) 3.0 else 0.0) +
                ln(1.0 + (stats?.playCount ?: 0).coerceAtLeast(0)).coerceAtMost(3.0)
            candidate.artists.forEach { preferredArtists[it] = (preferredArtists[it] ?: 0.0) + affinity }
            candidate.genres.forEach { preferredGenres[it] = (preferredGenres[it] ?: 0.0) + affinity }
        }
        val score = pool.associateWith { candidate ->
            val stats = user.listening[candidate.song.id] ?: LocalListening()
            (if (candidate.song.id in user.favorites) 7.0 else 0.0) +
                (if (candidate.artists.any { it in seed.artists }) 6.0 else 0.0) +
                (if (candidate.genres.any { it in seed.genres }) 6.0 else 0.0) +
                (candidate.artists.maxOfOrNull { preferredArtists[it] ?: 0.0 } ?: 0.0).coerceAtMost(4.0) +
                (candidate.genres.maxOfOrNull { preferredGenres[it] ?: 0.0 } ?: 0.0).coerceAtMost(4.0) +
                ln(1.0 + stats.playCount.coerceAtLeast(0)).coerceAtMost(3.0) +
                (if (stats.playCount == 0) 2.0 else 0.0) +
                (1.0 - abs(candidate.song.durationMs.toDouble() - seed.song.durationMs) / seed.song.durationMs.coerceAtLeast(1)).coerceIn(0.0, 1.0) +
                (if (candidate.song.year != null && seed.song.year != null && abs(candidate.song.year - seed.song.year) <= 3) 0.5 else 0.0)
        }
        val chosen = mutableListOf(seed)
        val counts = mutableMapOf<String, Int>()
        fun count(candidate: Candidate) { candidate.artists.forEach { counts[it] = (counts[it] ?: 0) + 1 } }
        count(seed)
        val remaining = pool.filterNot { it.song.id == seed.song.id }.toMutableList()
        while (chosen.size < limit.coerceAtMost(50)) {
            val available = remaining.filter { candidate ->
                candidate.artists.all { artist -> (counts[artist] ?: 0) <
                    if (artist in seed.artists) QueueBuilder.SEED_ARTIST_LIMIT else QueueBuilder.PER_ARTIST_LIMIT } &&
                chosen.none { previous -> candidate.title.isNotEmpty() && candidate.title == previous.title &&
                    (candidate.artists.isEmpty() || previous.artists.isEmpty() || candidate.artists.any { it in previous.artists }) }
            }
            if (available.isEmpty()) break
            val fresh = available.filterNot { it.song.id in recent }.ifEmpty { available }
            val lastArtists = chosen.takeLast(2).flatMap { it.artists }.toSet()
            val diverse = fresh.filter { candidate -> candidate.artists.none { it in lastArtists } }.ifEmpty { fresh }
            val next = diverse.minWith(compareByDescending<Candidate> { score.getValue(it) }.thenBy { it.tie }.thenBy { it.song.id })
            chosen.add(next); count(next); remaining.remove(next)
        }
        return chosen.map { it.song }
    }
    private fun genres(value: String?): Set<String> = value.orEmpty().split(',', ';', '/')
        .map(LocalSearch::normalize).filter { it.any(Char::isLetter) && it !in setOf("unknown", "desconocido", "<unknown>") }.toSet()
    private fun tie(id: String, variation: Long): Int {
        var value = id.hashCode().toLong() xor variation
        value = (value xor (value ushr 33)) * -49064778989728563L
        value = (value xor (value ushr 33)) * -4265267296055464877L
        return (value xor (value ushr 33)).toInt()
    }
    private const val RECENT_WINDOW_MS = 2 * 60 * 60 * 1000L
    private val UNKNOWN_ARTISTS = setOf("", "<unknown>", "unknown", "unknown artist", "artista desconocido")
}
