// SPDX-FileCopyrightText: 2026 Aurelis
// SPDX-License-Identifier: LGPL-3.0-or-later

package com.music.aurelis.data.discord

import com.music.aurelis.data.model.Song
import java.net.URI
import java.text.Normalizer
import java.util.Locale
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.HttpUrl
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject

/**
 * Public album art for official Discord Social SDK activity assets.
 *
 * No content://, file://, SMB, WebDAV or authenticated/private image is sent
 * to Discord. If the source has no public CDN thumbnail, a metadata-only
 * iTunes Search API lookup finds the same track/artist (not a fuzzy guess).
 * Never uploads images from local storage or silently makes them public.
 *
 * This resolver runs only when the listener opted into Discord Rich Presence.
 */
internal object DiscordArtwork {
    private val imageDomains = listOf(
        "ytimg.com",
        "googleusercontent.com",
        "ggpht.com",
        "mzstatic.com",
        "scdn.co",
        "spotifycdn.com",
        "dzcdn.net",
        "coverartarchive.org",
        "qobuz.com",
        "tidal.com",
    )

    private data class CacheValue(val imageUrl: String?, val untilMs: Long)
    private val cache = ConcurrentHashMap<String, CacheValue>()
    private val http = OkHttpClient.Builder()
        .connectTimeout(3, TimeUnit.SECONDS)
        .readTimeout(4, TimeUnit.SECONDS)
        .callTimeout(6, TimeUnit.SECONDS)
        .build()

    /**
     * Discord receives an HTTPS URL that its image proxy can reach.
     * Limit to known public image hosts, avoiding private album artwork
     * servers and signed stream links accidentally stored in Song.thumbnailUrl.
     * The Discord Social SDK caps this field at 300 characters.
     */
    internal fun publicUrl(source: String?): String? {
        val url = source?.trim()?.takeIf { it.isNotEmpty() && it.length <= 300 } ?: return null
        val parsed = runCatching { URI(url) }.getOrNull() ?: return null
        if (!parsed.scheme.equals("https", ignoreCase = true) ||
            parsed.userInfo != null || parsed.port !in listOf(-1, 443) ||
            parsed.fragment != null) return null
        val host = parsed.host?.lowercase(Locale.ROOT) ?: return null
        if (imageDomains.none { host == it || host.endsWith(".$it") }) return null
        return url
    }

    /** No external query for a publicly resolvable image already on the song. */
    suspend fun resolve(song: Song): String? {
        publicUrl(song.thumbnailUrl)?.let { return it }
        if (song.title.isBlank() || song.artist.isBlank() ||
            song.artist.equals("Unknown Artist", ignoreCase = true)) return null
        val key = "${song.title}\u0000${song.artist}\u0000${song.albumName.orEmpty()}"
        val now = System.currentTimeMillis()
        cache[key]?.takeIf { it.untilMs > now }?.let { return it.imageUrl }
        val result = withContext(Dispatchers.IO) {
            runCatching { lookupPublicCover(song) }.getOrNull()
        }
        // Reuse successful art for 12 h, and avoid retry loops when no match exists.
        cache[key] = CacheValue(result, now + if (result == null) 30 * 60_000L else 12 * 60 * 60_000L)
        return result
    }

    private fun lookupPublicCover(song: Song): String? {
        val url = HttpUrl.Builder().scheme("https")
            .host("itunes.apple.com").addPathSegment("search")
            .addQueryParameter("media", "music")
            .addQueryParameter("entity", "song")
            .addQueryParameter("country", "US")
            .addQueryParameter("limit", "15")
            .addQueryParameter("term", "${song.title} ${song.artist}")
            .build()
        val request = Request.Builder().url(url)
            .header("Accept", "application/json")
            .header("User-Agent", "AurelisMusic/2.7 (public album artwork)")
            .build()
        http.newCall(request).execute().use { response ->
            if (!response.isSuccessful) return null
            val results = JSONObject(response.body?.string() ?: return null)
                .optJSONArray("results") ?: return null
            var bestArt: String? = null
            var bestScore = -1
            for (index in 0 until results.length()) {
                val candidate = results.optJSONObject(index) ?: continue
                if (candidate.optString("wrapperType") != "track" ||
                    !sameName(song.title, candidate.optString("trackName")) ||
                    !sameName(song.artist, candidate.optString("artistName"))) continue

                val art100 = candidate.optString("artworkUrl100").replace(
                    "http://", "https://",
                )
                val better = art100.replace("/100x100bb.jpg", "/512x512bb.jpg")
                val art = publicUrl(better) ?: publicUrl(art100) ?: continue
                // Song is declared in :shared, so Kotlin cannot smart-cast
                // its nullable public property across module boundaries.
                val albumName = song.albumName
                val albumMatch = !albumName.isNullOrBlank() &&
                    sameName(albumName, candidate.optString("collectionName"))
                val score = if (albumMatch) 2 else 1
                if (score > bestScore) {
                    bestScore = score
                    bestArt = art
                }
                if (score == 2) break
            }
            return bestArt
        }
    }

    /** Strict name matching: never substitute another artist or similarly named song. */
    internal fun sameName(left: String, right: String): Boolean = normalize(left) == normalize(right)

    private fun normalize(name: String): String =
        Normalizer.normalize(name, Normalizer.Form.NFKD)
            .replace(Regex("\\p{M}+"), "")
            .lowercase(Locale.ROOT)
            .replace(Regex("[^\\p{L}\\p{N}]+"), " ")
            .trim()
            .replace(Regex("\\s+"), " ")
}
