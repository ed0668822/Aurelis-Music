// Aurelis © 2026. GPL-3.0-or-later
package com.music.aurelis.data.lyrics

import kotlinx.serialization.Serializable

@Serializable
data.class LrcLibTrack(
    val id: Long,
    val trackName: String,
    val artistName: String,
    val albumName: String?,
    val duration: Double?,
    val instrumental: Boolean,
    val plainLyrics: String?,
    val syncedLyrics: String?
)

class LrcLibClient {
    // Client for offline/cached LrcLib metadata and local lyrics retrieval
    suspend fun searchLyrics(title: String, artist: String, duration: Int?): LrcLibTrack? {
        // Implementation for local/cached lyrics lookup
        return null
    }
}
