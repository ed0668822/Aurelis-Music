package com.music.aurelis.data

import com.music.aurelis.data.model.LocalSong

data class LocalGenreGroup(val key: String, val name: String, val songs: List<LocalSong>)

/** Groups only reported metadata. Colour never determines a musical genre. */
object LocalGenres {
    fun groups(songs: List<LocalSong>): List<LocalGenreGroup> = songs.groupBy {
        it.genre?.trim()?.takeIf(String::isNotBlank)?.let { tag -> "tag:${LocalSearch.normalize(tag)}" } ?: "unknown"
    }.map { (key, tracks) ->
        LocalGenreGroup(key, if (key == "unknown") "Sin clasificar" else tracks.first().genre!!.trim(), tracks)
    }.sortedBy { LocalSearch.normalize(it.name) }
}
