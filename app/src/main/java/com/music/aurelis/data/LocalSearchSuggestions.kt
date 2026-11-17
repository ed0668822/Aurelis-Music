// Aurelis © 2026. GPL-3.0-or-later
package com.music.aurelis.data

import com.music.aurelis.data.model.LocalSong

/** BitChord's typed-query + completions contract, supplied entirely from local data. */
object LocalSearchSuggestions {
    fun build(query: String, songs: List<LocalSong>, collections: List<String>, history: List<String>): List<String> {
        val key = LocalSearch.normalize(query)
        if (key.isBlank()) return emptyList()
        val candidates = sequence {
            for (song in songs) {
                yield(song.title)
                yield(song.artist)
                val album = song.album
                if (album != null) yield(album)
            }
            yieldAll(collections)
            yieldAll(history)
        }
        val seen = hashSetOf(key)
        val completions = candidates.map(String::trim).filter { text ->
            val normalized = LocalSearch.normalize(text)
            normalized.isNotBlank() && normalized.contains(key) && seen.add(normalized)
        }.take(3).toList()
        // No alternative completion: keep Aurelis' instant local results/filter recovery.
        return if (completions.isEmpty()) emptyList() else listOf(query) + completions
    }
}
