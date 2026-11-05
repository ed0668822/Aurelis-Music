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
    suspend fun searchLyrics(title: String, artist: String, duration: Int?): LrcLibTrack? = withContext(Dispatchers.IO) {
        try {
            val url = searchUrl(title, artist)
            val response = lyricsGet(url.toString()) ?: return@withContext null
            val arr = JSONArray(response)
            if (arr.length() == 0) return@withContext null
            val best = selectTrack(arr, title, artist, duration) ?: return@withContext null
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
        } catch (cancelled: kotlinx.coroutines.CancellationException) { throw cancelled
        } catch (e: Exception) {
            null
        }
    }

    internal fun searchUrl(title: String, artist: String): okhttp3.HttpUrl {
        return okhttp3.HttpUrl.Builder().scheme("https").host("lrclib.net")
            .addPathSegment("api").addPathSegment("search")
            .addQueryParameter("track_name", title).apply {
                if (artist.isNotBlank()) addQueryParameter("artist_name", artist)
            }.build()
    }
    internal fun selectTrack(arr: JSONArray, title: String, artist: String, duration: Int?): org.json.JSONObject? {
        val candidates = (0 until arr.length()).map { arr.getJSONObject(it) }.filter {
                com.music.aurelis.data.LocalSearch.normalize(it.optString("trackName")) == com.music.aurelis.data.LocalSearch.normalize(title) &&
                (artist.isBlank() || com.music.aurelis.data.LocalSearch.normalize(it.optString("artistName")) == com.music.aurelis.data.LocalSearch.normalize(artist)) &&
                (duration == null || duration <= 0 || (!it.isNull("duration") && kotlin.math.abs(it.optDouble("duration") - duration) <= 5))
            }
        return candidates.minByOrNull { if (duration != null) kotlin.math.abs(it.optDouble("duration") - duration) else 0.0 }
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

    fun parseLrc(lrc: String): List<LyricLine> = Lyrics.parse(lrc)?.takeIf { it.isSynced }?.lines.orEmpty()
}
