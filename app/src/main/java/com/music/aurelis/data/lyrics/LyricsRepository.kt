// Aurelis © 2026. GPL-3.0-or-later
package com.music.aurelis.data.lyrics

import kotlinx.serialization.Serializable

@Serializable
data class LyricLine(
    val time: Long,
    val text: String,
    val translation: String? = null
)

@Serializable
data class LyricsResult(
    val lines: List<LyricLine>,
    val source: String,
    val isSynced: Boolean
)

interface LyricsProvider {
    val name: String
    suspend fun getLyrics(title: String, artist: String, durationMs: Long): LyricsResult?
    suspend fun translateLyrics(lines: List<LyricLine>, targetLanguage: String): List<LyricLine>
}
