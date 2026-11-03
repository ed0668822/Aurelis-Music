// Aurelis © 2026. GPL-3.0-or-later
package com.music.aurelis.data.lyrics

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/** Public Apple lyrics plus optional authenticated Spotify and Musixmatch routes.
 *
 * Stub: the full BitChord implementation depends on internal helpers
 * (Candidate, wordsFromRuns, TimedRun, withInstrumentalGaps, scrapeAppleToken)
 * that are not yet ported to Aurelis. Until then this source returns null,
 * which is correct — the lookup simply skips it and tries the next source.
 */
object PaxSenix {

    @Volatile
    private var apiKey: String = ""

    fun setApiKey(value: String) {
        apiKey = value.trim()
    }

    suspend fun lyrics(
        title: String,
        artist: String,
        durationMs: Long,
        album: String? = null,
    ): List<LyricLine>? = withContext(Dispatchers.IO) {
        null
    }

    suspend fun spotifyLyrics(
        title: String,
        artist: String,
        durationMs: Long,
    ): List<LyricLine>? = withContext(Dispatchers.IO) {
        null
    }

    suspend fun musixmatchLyrics(
        title: String,
        artist: String,
        durationMs: Long,
    ): List<LyricLine>? = withContext(Dispatchers.IO) {
        null
    }
}
