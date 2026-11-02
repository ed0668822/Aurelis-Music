// Aurelis © 2026. GPL-3.0-or-later
package com.music.aurelis.data.lyrics

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class LyricsRepository {
    data class Result(val source: LyricsSource, val lines: List<LyricLine>)

    private val _lyrics = MutableStateFlow<Result?>(null)
    val lyrics: StateFlow<Result?> = _lyrics.asStateFlow()

    private val _translation = MutableStateFlow<Result?>(null)
    val translation: StateFlow<Result?> = _translation.asStateFlow()

    private val _lines = MutableStateFlow<List<LyricLine>>(emptyList())
    val lines: StateFlow<List<LyricLine>> = _lines.asStateFlow()

    private val _source = MutableStateFlow<LyricsSource?>(null)
    val source: StateFlow<LyricsSource?> = _source.asStateFlow()

    fun flush() {
        _lyrics.value = null
        _translation.value = null
        _lines.value = emptyList()
        _source.value = null
    }

    fun record(lines: List<LyricLine>, source: LyricsSource) {
        _lines.value = lines
        _source.value = source
        _lyrics.value = Result(source, lines)
    }

    companion object {
        suspend fun lyrics(
            videoId: String,
            title: String,
            artist: String,
            durationMs: Long,
            album: String? = null,
            sources: Set<LyricsSource> = LyricsSource.entries.toSet(),
            order: List<LyricsSource> = LyricsSource.entries,
            prioritizeSyllableSync: Boolean = false,
            isrc: String? = null,
            onSourceStarted: ((LyricsSource) -> Unit)? = null,
            onSourceResult: ((LyricsSource, Result?) -> Unit)? = null,
            onSourceCancelled: ((LyricsSource) -> Unit)? = null,
        ): Result? {
            val sequence = (order + LyricsSource.entries)
                .distinct()
                .filter { it in sources && !it.hidden }
            for (source in sequence) {
                onSourceStarted?.invoke(source)
                val lines = try {
                    when (source) {
                        LyricsSource.LRCLIB -> LrcLib.lyrics(title, artist, durationMs)
                        LyricsSource.MEGALOBIZ -> Megalobiz.lyrics(title, artist)
                        LyricsSource.MUSIXMATCH -> Musixmatch.lyrics(title, artist, durationMs)
                        LyricsSource.UNISON -> Unison.lyrics(title, artist, durationMs, album)
                        else -> null
                    }
                } catch (e: Exception) {
                    onSourceResult?.invoke(source, null)
                    continue
                }
                if (lines != null && lines.isNotEmpty()) {
                    val result = Result(source, lines)
                    onSourceResult?.invoke(source, result)
                    return result
                }
                onSourceResult?.invoke(source, null)
            }
            return null
        }
    }
}