// Aurelis © 2026. GPL-3.0-or-later
package com.music.aurelis.data.lyrics

import com.music.aurelis.data.model.Song
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray

data class LrcLibTrack(
    val id: Long,
    val name: String,
    val trackName: String?,
    val artistName: String?,
    val albumName: String?,
    val duration: Double?,
    val plainLyrics: String?,
    val syncedLyrics: String?,
)

object LrcLib {
    private const val BASE_URL = "https://lrclib.net/api"

    suspend fun searchLyrics(title: String, artist: String, duration: Int?): LrcLibTrack? = withContext(Dispatchers.IO) {
        try {
            val query = title.replace(" ", "%20")
            val artistQ = if (artist.isBlank()) "" else artist.replace(" ", "%20")
            val url = StringBuilder("$BASE_URL/search?track_name=$query")
            if (artist.isNotBlank()) url.append("&artist_name=$artistQ")
            if (duration != null && duration > 0) url.append("&duration=$duration")
            val response = Http.get(url.toString())
            val arr = JSONArray(response)
            if (arr.length() == 0) return@withContext null
            val best = arr.getJSONObject(0)
            LrcLibTrack(
                id = best.optLong("id"),
                name = best.optString("name"),
                trackName = best.optString("trackName").takeIf { it != "null" },
                artistName = best.optString("artistName").takeIf { it != "null" },
                albumName = best.optString("albumName").takeIf { it != "null" },
                duration = if (best.isNull("duration")) null else best.optDouble("duration"),
                plainLyrics = best.optString("plainLyrics").takeIf { it.isNotEmpty() && it != "null" },
                syncedLyrics = best.optString("syncedLyrics").takeIf { it.isNotEmpty() && it != "null" },
            )
        } catch (e: Exception) {
            null
        }
    }

    suspend fun getLyrics(song: Song): LyricsResult {
        val track = searchLyrics(song.title, song.artist, song.duration?.div(1000))
        val synced = track?.syncedLyrics
        val plain = track?.plainLyrics
        val lines = when {
            !synced.isNullOrBlank() -> parseLrc(synced)
            !plain.isNullOrBlank() -> plain.lineSequence().filter { it.isNotBlank() }.map { LyricLine(0L, it.trim()) }.toList()
            else -> emptyList()
        }
        return LyricsResult(lines = lines, source = "lrclib", isSynced = !synced.isNullOrBlank())
    }

    suspend fun lyrics(title: String, artist: String, durationMs: Long): List<LyricLine>? {
        val track = searchLyrics(title, artist, if (durationMs > 0) (durationMs / 1000).toInt() else null)
        val synced = track?.syncedLyrics
        val plain = track?.plainLyrics
        val lines = when {
            !synced.isNullOrBlank() -> parseLrc(synced)
            !plain.isNullOrBlank() -> plain.lineSequence().filter { it.isNotBlank() }.map { LyricLine(0L, it.trim()) }.toList()
            else -> emptyList()
        }
        return if (lines.isEmpty()) null else lines
    }

    fun parseLrc(lrc: String): List<LyricLine> {
        if (lrc.isBlank()) return emptyList()
        val result = mutableListOf<LyricLine>()
        val timeRegex = Regex("\\[(\\d{1,2}):(\\d{2})(?:\\.(\\d{2,3}))?]")
        lrc.lineSequence().forEach { line ->
            val matches = timeRegex.findAll(line)
            if (matches.count() == 0) return@forEach
            val text = line.replace(timeRegex, "").trim()
            if (text.isEmpty()) return@forEach
            matches.forEach { m ->
                val min = m.groupValues[1].toLongOrNull() ?: 0L
                val sec = m.groupValues[2].toLongOrNull() ?: 0L
                val msPart = m.groupValues[3]
                val ms = when (msPart.length) {
                    2 -> (msPart.toLongOrNull() ?: 0L) * 10
                    3 -> (msPart.toLongOrNull() ?: 0L)
                    else -> 0L
                }
                result.add(LyricLine(timeMs = min * 60_000L + sec * 1000L + ms, text = text))
            }
        }
        return result.sortedBy { it.timeMs }
    }
}
