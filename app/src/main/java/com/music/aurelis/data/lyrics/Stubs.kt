// Aurelis © 2026. GPL-3.0-or-later.
package com.music.aurelis.data.lyrics

object SimpMusicLyrics {
    suspend fun lyrics(videoId: String, durationMs: Long): List<LyricLine>? = null
}

object YouTubeMusicLyrics {
    suspend fun lyrics(videoId: String): List<LyricLine>? = null
}

object YouTubeTranscriptLyrics {
    suspend fun lyrics(videoId: String): List<LyricLine>? = null
}

// PaxSenix se declara en PaxSenix.kt; este archivo solo mantiene los stubs de red prohibida.
