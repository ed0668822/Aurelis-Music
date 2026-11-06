// Aurelis © 2026. GPL-3.0-or-later
package com.music.aurelis.data.lyrics

import com.music.aurelis.data.model.Song
import org.json.JSONArray
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import okhttp3.OkHttpClient
import okhttp3.Request
import java.util.concurrent.TimeUnit

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
    private val client by lazy {
        OkHttpClient.Builder().followRedirects(false).followSslRedirects(false).retryOnConnectionFailure(false)
            .connectTimeout(3, TimeUnit.SECONDS).callTimeout(8, TimeUnit.SECONDS).build()
    }

    /** Explicit, bounded and cancellable lookup; 404/empty/mismatched recordings are not network errors. */
    internal suspend fun lookup(
        title: String, artist: String, durationMs: Long, httpClient: OkHttpClient = client,
    ): LyricsRepository.Result? {
        currentCoroutineContext().ensureActive()
        if (title.isBlank()) return null
        val request = Request.Builder().url(searchUrl(title, artist))
            .header("User-Agent", LYRICS_AGENT).header("Accept", "application/json").build()
        val raw = try { requestLyricsBody(request, httpClient) }
        catch (error: LyricsRequestException) {
            if (error.failure.httpCode == 404) return null
            throw error
        }
        currentCoroutineContext().ensureActive()
        try {
            val rows = JSONArray(raw)
            if (rows.length() > 500) throw LyricsRequestException(LyricsFailure(LyricsFailureKind.TOO_LARGE))
            val duration = durationMs.takeIf { it > 0 }?.div(1000)?.coerceAtMost(Int.MAX_VALUE.toLong())?.toInt()
            val best = selectTrack(rows, title, artist, duration) ?: return null
            val synced = best.optString("syncedLyrics").takeIf { it.isNotBlank() && it != "null" }
                ?.let(Lyrics::parse)?.takeIf { it.isSynced }
            val plain = best.optString("plainLyrics").takeIf { it.isNotBlank() && it != "null" }
                ?.let(Lyrics::parse)
            val parsed = synced ?: plain ?: return null
            return LyricsRepository.Result(LyricsSource.LRCLIB, parsed.lines, parsed.isSynced)
        } catch (cancelled: CancellationException) { throw cancelled }
        catch (error: LyricsRequestException) { throw error }
        catch (_: Exception) { throw LyricsRequestException(LyricsFailure(LyricsFailureKind.INVALID_RESPONSE)) }
    }

    suspend fun searchLyrics(title: String, artist: String, duration: Int?): LrcLibTrack? {
        val request = Request.Builder().url(searchUrl(title, artist))
            .header("User-Agent", LYRICS_AGENT).header("Accept", "application/json").build()
        val body = requestLyricsBody(request, client)
        val best = selectTrack(JSONArray(body), title, artist, duration) ?: return null
        return LrcLibTrack(best.optLong("id"), best.optString("name"), best.optString("trackName"),
            best.optString("artistName"), best.optString("albumName"),
            if (best.isNull("duration")) null else best.optDouble("duration"),
            best.optString("plainLyrics").takeIf { it.isNotBlank() && it != "null" },
            best.optString("syncedLyrics").takeIf { it.isNotBlank() && it != "null" })
    }

    internal fun searchUrl(title: String, artist: String): okhttp3.HttpUrl {
        return okhttp3.HttpUrl.Builder().scheme("https").host("lrclib.net")
            .addPathSegment("api").addPathSegment("search")
            .addQueryParameter("track_name", title).apply {
                if (artist.isNotBlank()) addQueryParameter("artist_name", artist)
            }.build()
    }
    internal fun selectTrack(arr: JSONArray, title: String, artist: String, duration: Int?): org.json.JSONObject? {
        val candidates = (0 until arr.length()).mapNotNull { arr.optJSONObject(it) }.filter {
                com.music.aurelis.data.LocalSearch.normalize(it.optString("trackName")) == com.music.aurelis.data.LocalSearch.normalize(title) &&
                (artist.isBlank() || com.music.aurelis.data.LocalSearch.normalize(it.optString("artistName")) == com.music.aurelis.data.LocalSearch.normalize(artist)) &&
                (duration == null || duration <= 0 || (!it.isNull("duration") && it.optDouble("duration").isFinite() && kotlin.math.abs(it.optDouble("duration") - duration) <= 5))
            }
        return candidates.minByOrNull { if (duration != null) kotlin.math.abs(it.optDouble("duration") - duration) else 0.0 }
    }

    suspend fun getLyrics(song: Song): LyricsResult {
        val found = lookup(song.title, song.artist, song.duration?.toLong() ?: 0L)
        return LyricsResult(found?.lines.orEmpty(), "lrclib", found?.isSynced == true)
    }

    suspend fun lyrics(title: String, artist: String, durationMs: Long): List<LyricLine>? =
        lookup(title, artist, durationMs)?.lines

    fun parseLrc(lrc: String): List<LyricLine> = Lyrics.parse(lrc)?.takeIf { it.isSynced }?.lines.orEmpty()
}
