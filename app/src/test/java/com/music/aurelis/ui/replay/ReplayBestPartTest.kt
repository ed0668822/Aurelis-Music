// SPDX-FileCopyrightText: 2026 Aurelis
// SPDX-License-Identifier: LGPL-3.0-or-later
package com.music.aurelis.ui.replay

import com.music.aurelis.data.lyrics.LyricLine
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class ReplayBestPartTest {
    @Test fun repeatsIdentifyFirstChorus() {
        val lyrics = listOf(
            LyricLine(8_000L, "This is the chorus"),
            LyricLine(72_000L, "This is the chorus"),
            LyricLine(90_000L, "A different passage")
        )
        assertEquals(8_000L, ReplayBestPart.chorusStart(lyrics, 160_000L))
    }

    @Test fun noConfidentChorusProducesNoCandidate() {
        assertNull(ReplayBestPart.chorusStart(listOf(LyricLine(30_000L, "A unique lyric here")), 180_000L))
        assertNull(ReplayBestPart.chorusStart(emptyList(), 180_000L))
        assertNull(ReplayBestPart.chorusStart(listOf(LyricLine(8_000L, "This is the chorus"), LyricLine(36_000L, "This is the chorus")), 45_000L))
    }
}
