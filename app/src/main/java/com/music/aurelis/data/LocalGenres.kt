// SPDX-FileCopyrightText: 2026 Aurelis
// SPDX-License-Identifier: LGPL-3.0-or-later
package com.music.aurelis.data

import com.music.aurelis.data.model.LocalSong

data class LocalGenreGroup(val key: String, val name: String, val songs: List<LocalSong>)

/** A category must be backed by actual MediaStore or embedded-file metadata. */
object LocalGenres {
    private fun knownTag(raw: String?): String? = raw?.trim()?.takeIf { label ->
        label.isNotBlank() && !label.equals("<unknown>", ignoreCase = true) &&
            LocalSearch.normalize(label).isNotEmpty()
    }

    fun groups(songs: List<LocalSong>): List<LocalGenreGroup> = songs.groupBy { song ->
        knownTag(song.genre)?.let { "tag:" + LocalSearch.normalize(it) } ?: "unknown"
    }.map { (key, tracks) ->
        // The missing-tag bucket is never turned into a speculative music style.
        val name = if (key == "unknown") "Sin clasificar"
            else tracks.firstNotNullOf { knownTag(it.genre) }
        LocalGenreGroup(key, name, tracks)
    }.sortedBy { LocalSearch.normalize(it.name) }
}
