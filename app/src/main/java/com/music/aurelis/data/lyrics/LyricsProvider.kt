// Aurelis © 2026. GPL-3.0-or-later
package com.music.aurelis.data.lyrics

import com.music.aurelis.data.model.Song

interface LyricsProvider {
    val name: String
    suspend fun getLyrics(song: Song): LyricsResult
    suspend fun translateLyrics(song: Song, targetLanguage: String): LyricsResult?
}

data class LyricsResult(
    val lines: List<LyricLine>,
    val source: String = "",
    val isSynced: Boolean = false,
)