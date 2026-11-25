// SPDX-FileCopyrightText: © 2026 Aurelis

package com.music.aurelis

import com.music.aurelis.ui.player.LyricsShareLine
import com.music.aurelis.ui.player.lyricsShareText
import org.junit.Assert.assertEquals
import org.junit.Test

class LyricsShareTextTest {
    private val lines = listOf(
        LyricsShareLine("First verse", "Primer verso"),
        LyricsShareLine("", null, isGap = true),
        LyricsShareLine("Last verse", "Último verso"),
    )

    @Test fun preservesSkippedVersesAndIncludesSongCredit() {
        assertEquals(
            "First verse\n…\nLast verse\n\nSong — Artist",
            lyricsShareText("Song", "Artist", lines, includeSubText = false),
        )
    }

    @Test fun canCopyTranslationsWithoutRequiringSongMetadata() {
        assertEquals(
            "First verse\nPrimer verso\n…\nLast verse\nÚltimo verso",
            lyricsShareText("", "", lines, includeSubText = true),
        )
    }
}
