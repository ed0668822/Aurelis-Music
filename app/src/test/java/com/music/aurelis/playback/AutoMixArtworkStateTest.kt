// SPDX-FileCopyrightText: 2026 Aurelis
// SPDX-License-Identifier: LGPL-3.0-or-later
package com.music.aurelis.playback

import com.music.aurelis.data.settings.AutoMixArtworkState
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class AutoMixArtworkStateTest {
    @Test fun loopHandoffMatchesVisualMidpoint() {
        assertEquals(0f, AutoMixArtworkState.loopProgress(0.0, 4.0, 9.0), 0.0001f)
        assertEquals(0.5f, AutoMixArtworkState.loopProgress(4.0, 4.0, 9.0), 0.0001f)
        assertEquals(1f, AutoMixArtworkState.loopProgress(9.0, 4.0, 9.0), 0.0001f)
        assertEquals(1f, AutoMixArtworkState.loopProgress(12.0, 4.0, 9.0), 0.0001f)
    }

    @Test fun invalidClockDoesNotGenerateBrokenProgress() {
        assertEquals(0f, AutoMixArtworkState.loopProgress(Double.NaN, 3.0, 8.0), 0f)
        assertEquals(0f, AutoMixArtworkState.loopProgress(4.0, 6.0, 3.0), 0f)
    }

    @Test fun onlyActualMixPairCanDisplayOverlay() {
        val value = AutoMixArtworkState(1L, "song-1", "song-2", "A", "Artist", "B", "Artist", null, null)
        assertTrue(value.contains("song-1"))
        assertTrue(value.contains("song-2"))
        assertFalse(value.contains("unrelated-song"))
    }
}
