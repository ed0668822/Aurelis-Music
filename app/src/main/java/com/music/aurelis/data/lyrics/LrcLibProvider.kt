// Aurelis © 2026. GPL-3.0-or-later
package com.music.aurelis.data.lyrics

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject

class LrcLibProvider : LyricsProvider {
    override val name: String = "LrcLib"
    private val client = OkHttpClient()

    override suspend fun getLyrics(title: String, artist: String, durationMs: Long): LyricsResult? = withContext(Dispatchers.IO) {
        try {
            val url = "https://lrclib.net/api/get?track_name=${java.net.URLEncoder.encode(title, "UTF-8")}&artist_name=${java.net.URLEncoder.encode(artist, "UTF-8")}&duration=${durationMs / 1000}"
            val request = Request.Builder().url(url).header("User-Agent", "AurelisMusic/1.0").build()
            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) return@withContext null
                val body = response.body?.string() ?: return@withContext null
                val json = JSONObject(body)
                val syncedLyrics = json.optString("syncedLyrics")
                val plainLyrics = json.optString("plainLyrics")

                if (syncedLyrics.isNotBlank()) {
                    val lines = parseSyncedLyrics(syncedLyrics)
                    LyricsResult(lines, name, true)
                } else if (plainLyrics.isNotBlank()) {
                    val lines = plainLyrics.lines().map { LyricLine(0, it) }
                    LyricsResult(lines, name, false)
                } else null
            }
        } catch (e: Exception) {
            null
        }
    }

    override suspend fun translateLyrics(lines: List<LyricLine>, targetLanguage: String): List<LyricLine> = withContext(Dispatchers.IO) {
        // Simple mock translation or fallback
        lines.map { it.copy(translation = "(${targetLanguage}) ${it.text}") }
    }

    private fun parseSyncedLyrics(lrc: String): List<LyricLine> {
        val lines = mutableListOf<LyricLine>()
        val regex = Regex("""\[(\d{2}):(\d{2})\.(\d{2,3})](.+)""")
        lrc.lines().forEach { line ->
            val match = regex.find(line.trim())
            if (match != null) {
                val (min, sec, millis, text) = match.destructured
                val timeMs = min.toLong() * 60000 + sec.toLong() * 1000 + millis.padEnd(3, '0').take(3).toLong()
                lines.add(LyricLine(timeMs, text.trim()))
            }
        }
        return lines
    }
}
