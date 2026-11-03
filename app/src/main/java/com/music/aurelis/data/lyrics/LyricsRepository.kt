// Aurelis © 2026. GPL-3.0-or-later
package com.music.aurelis.data.lyrics

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.Collections
import java.util.concurrent.ConcurrentHashMap

object LyricsRepository {

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

    suspend fun lyrics(
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
    ): Result? = coroutineScope {
        val sequence = (order + LyricsSource.offered)
            .distinct()
            .filter { it in sources && !it.hidden }

        val searchTitle = title.forLyricsSearch()
        val searchArtist = artist.artistForLyricsSearch()

        val racing: List<Pair<LyricsSource, Deferred<Result?>>> = sequence.map { source ->
            val startMode = if (source == LyricsSource.GENIUS) {
                kotlinx.coroutines.CoroutineStart.LAZY
            } else {
                kotlinx.coroutines.CoroutineStart.DEFAULT
            }
            source to async(Dispatchers.IO, start = startMode) {
                onSourceStarted?.invoke(source)
                try {
                    val found = fetch(source, searchTitle, searchArtist, durationMs, album, isrc)
                        ?.let { result(source, it) }
                    onSourceResult?.invoke(source, found)
                    found
                } catch (cancelled: CancellationException) {
                    onSourceCancelled?.invoke(source)
                    throw cancelled
                } catch (_: Exception) {
                    onSourceResult?.invoke(source, null)
                    null
                }
            }
        }

        try {
            var lineSynced: Result? = null
            for ((source, job) in racing) {
                if (lineSynced != null && source == LyricsSource.GENIUS) continue

                val found = runCatching { job.await() }.getOrNull() ?: continue
                if (found.lines.any { it.isWordSynced }) return@coroutineScope found
                if (!prioritizeSyllableSync && found.lines.any { it.timeMs > 0 }) {
                    return@coroutineScope found
                }
                if (lineSynced == null) lineSynced = found
            }
            lineSynced
        } finally {
            racing.forEach { it.second.cancel() }
        }
    }

    private suspend fun fetch(
        source: LyricsSource,
        title: String,
        artist: String,
        durationMs: Long,
        album: String?,
        isrc: String?,
    ): List<LyricLine>? = when (source) {
        LyricsSource.LRC_RED -> LrcRed.lyrics(title, artist, durationMs, isrc)
        LyricsSource.BETTER_LYRICS -> BetterLyrics.lyrics(title, artist, durationMs, album)
        LyricsSource.BETTER_LYRICS_PORTATO -> BetterLyrics.portato(title, artist, durationMs, album)
        LyricsSource.LYRICS_PLUS -> LyricsPlus.lyrics(title, artist, durationMs, album, isrc)
        LyricsSource.BINI_LYRICS -> BiniLyrics.lyrics(title, artist, durationMs, album, isrc)?.lines
        LyricsSource.UNISON -> Unison.lyrics(title, artist, durationMs, album)
        LyricsSource.LRCLIB -> LrcLib.lyrics(title, artist, durationMs)
        LyricsSource.MUSIXMATCH -> Musixmatch.lyrics(title, artist, durationMs)
        LyricsSource.PAXSENIX -> PaxSenix.lyrics(title, artist, durationMs, album)
        LyricsSource.KUGOU -> KuGou.lyrics(title, artist, durationMs, album)
        LyricsSource.MEGALOBIZ -> Megalobiz.lyrics(title, artist)
        LyricsSource.GENIUS -> Genius.lyrics(title, artist)
        else -> null
    }

    private fun result(source: LyricsSource, lines: List<LyricLine>) =
        Result(source, lines.withBackgroundVocals())

    private class SharedDocuments(private val scope: CoroutineScope) {
        private val inFlight = ConcurrentHashMap<String, Deferred<String?>>()

        suspend fun get(url: String): String? =
            inFlight.computeIfAbsent(url) { scope.async(Dispatchers.IO) { lyricsGet(url) } }.await()

        fun cancel() = inFlight.values.forEach { it.cancel() }
    }
}
