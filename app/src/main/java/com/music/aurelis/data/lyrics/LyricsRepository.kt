// Aurelis © 2026. GPL-3.0-or-later
package com.music.aurelis.data.lyrics

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

object LyricsRepository {

    data class Result(val source: LyricsSource, val lines: List<LyricLine>,
        val isSynced: Boolean = lines.any { it.timeMs > 0 || it.isWordSynced })

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

    suspend fun lyrics(
        title: String,
        artist: String,
        durationMs: Long,
        album: String? = null,
        sources: Set<LyricsSource> = LyricsSource.offered.toSet(),
        order: List<LyricsSource> = LyricsSource.offered,
        prioritizeSyllableSync: Boolean = false,
        isrc: String? = null,
        onSourceStarted: ((LyricsSource) -> Unit)? = null,
        onSourceResult: ((LyricsSource, Result?) -> Unit)? = null,
        onSourceCancelled: ((LyricsSource) -> Unit)? = null,
    ): Result? {
        val sequence = (order + LyricsSource.offered).distinct()
            .filter { it in sources && LyricsProviderRegistry.isAvailable(it) }
        for (source in sequence) {
            onSourceStarted?.invoke(source)
            try {
                val found = when (source) {
                    LyricsSource.LRCLIB -> LrcLib.lookup(title, artist, durationMs)
                    else -> null
                }
                onSourceResult?.invoke(source, found)
                if (found != null) return found
            } catch (cancelled: CancellationException) {
                onSourceCancelled?.invoke(source)
                throw cancelled
            } catch (error: LyricsRequestException) {
                onSourceResult?.invoke(source, null)
                throw error
            }
        }
        return null
    }
}
