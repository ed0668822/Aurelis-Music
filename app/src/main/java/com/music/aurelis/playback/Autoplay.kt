// Aurelis © 2026. GPL-3.0-or-later
package com.music.aurelis.playback

import com.music.aurelis.data.model.LocalSong

/** Transparent local ordering, not an inferred similarity/BPM model. */
internal class AutoplayEngine {
    fun suggestNextTracks(current: LocalSong, library: List<LocalSong>, excluded: Set<String>, limit: Int = 10): List<LocalSong> =
        library.asSequence().filter { it.id !in excluded && android.net.Uri.parse(it.contentUri).scheme in listOf("content", "file") }
            .distinctBy { it.id }
            .sortedWith(compareByDescending<LocalSong> { it.artist.isNotBlank() && it.artist.equals(current.artist, true) }
                .thenByDescending { !it.album.isNullOrBlank() && it.album.equals(current.album, true) }
                .thenByDescending { !it.genre.isNullOrBlank() && it.genre.equals(current.genre, true) }
                .thenBy { it.id })
            .take(limit.coerceIn(0, 50)).toList()
}
