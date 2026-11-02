// Aurelis © 2026. GPL-3.0-or-later
package com.music.aurelis.data.lyrics

import com.music.aurelis.data.model.Song

class LrcLibProvider : LyricsProvider {
    override val name: String = "lrclib"

    override suspend fun getLyrics(song: Song): LyricsResult {
        return LrcLib.getLyrics(song)
    }

    override suspend fun translateLyrics(song: Song, targetLanguage: String): LyricsResult? {
        return null
    }
}
