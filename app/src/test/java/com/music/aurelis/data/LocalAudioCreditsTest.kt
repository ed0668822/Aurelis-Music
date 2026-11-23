// SPDX-FileCopyrightText: 2026 Aurelis
// SPDX-License-Identifier: LGPL-3.0-or-later

package com.music.aurelis.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class LocalAudioCreditsTest {
    @Test fun `preserves roles without conflating album artist and composer`() {
        val parsed = parseLocalTrackCredits(mapOf(
            LocalCreditRole.COMPOSER to "A. Writer; B. Writer; a. writer",
            LocalCreditRole.LYRICIST to "Lyricist, Jr.",
            LocalCreditRole.ALBUM_ARTIST to "",
        ))
        assertEquals(listOf(LocalCreditRole.COMPOSER, LocalCreditRole.LYRICIST), parsed.map { it.role })
        assertEquals(listOf("A. Writer", "B. Writer"), parsed[0].names)
        assertEquals(listOf("Lyricist, Jr."), parsed[1].names)
    }

    @Test fun `missing credit metadata does not invent any names`() {
        assertTrue(parseLocalTrackCredits(emptyMap()).isEmpty())
        assertTrue(parseLocalTrackCredits(mapOf(LocalCreditRole.COMPOSER to " ; ; ")).isEmpty())
    }
}
