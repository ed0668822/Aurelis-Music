package com.music.aurelis.data

import com.music.aurelis.data.model.LocalSong
import com.music.aurelis.data.model.SortOption
import java.text.Normalizer
import java.util.Locale

/** Same case/accent rules in Home search and all local library views. */
object LocalSearch {
    private val marks = Regex("\\p{M}+")
    fun normalize(text: String): String = Normalizer.normalize(text, Normalizer.Form.NFD)
        .replace(marks, "").lowercase(Locale.ROOT).trim()

    fun matches(song: LocalSong, query: String): Boolean {
        return matchesNormalized(song, normalize(query))
    }
    fun matchesNormalized(song: LocalSong, key: String): Boolean = key.isEmpty() ||
        normalize(song.title).contains(key) || normalize(song.artist).contains(key) || normalize(song.album.orEmpty()).contains(key)

    fun sort(songs: List<LocalSong>, option: SortOption): List<LocalSong> = when (option) {
        SortOption.TITLE_ASC -> songs.sortedBy { normalize(it.title) }
        SortOption.TITLE_DESC -> songs.sortedByDescending { normalize(it.title) }
        SortOption.ARTIST_ASC -> songs.sortedBy { normalize(it.artist) }
        SortOption.ARTIST_DESC -> songs.sortedByDescending { normalize(it.artist) }
        SortOption.ALBUM_ASC -> songs.sortedBy { normalize(it.album.orEmpty()) }
        SortOption.ALBUM_DESC -> songs.sortedByDescending { normalize(it.album.orEmpty()) }
        SortOption.DURATION_ASC -> songs.sortedBy { it.durationMs }
        SortOption.DURATION_DESC -> songs.sortedByDescending { it.durationMs }
    }
}
