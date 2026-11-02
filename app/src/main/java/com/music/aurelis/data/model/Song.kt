// Aurelis © 2026. GPL-3.0-or-later
package com.music.aurelis.data.model

data class Song(
    val id: String = "",
    val videoId: String = id,
    val title: String,
    val artist: String,
    val albumName: String? = null,
    /** Duración en milisegundos. */
    val duration: Int? = null,
    val thumbnailUrl: String? = null,
)
