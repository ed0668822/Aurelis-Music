// SPDX-FileCopyrightText: 2026 Aurelis
// SPDX-License-Identifier: LGPL-3.0-or-later
package com.music.aurelis.playback

import com.music.aurelis.data.settings.AutoMixArtworkTuning
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class AutoMixArtworkTuningTest {
    @Test fun revealNeverLeavesValidProgressBounds() {
        assertEquals(0f, AutoMixArtworkTuning.revealProgress(-1f), 0f)
        assertEquals(0f, AutoMixArtworkTuning.revealProgress(Float.NaN), 0f)
        assertEquals(1f, AutoMixArtworkTuning.revealProgress(2f), 0f)
        assertEquals(0.5f, AutoMixArtworkTuning.revealProgress(0.5f), 0.0001f)
    }

    @Test fun revealRemainsMonotonic() {
        var previous = 0f
        for (step in 1..100) {
            val current = AutoMixArtworkTuning.revealProgress(step / 100f)
            assertTrue(current >= previous)
            previous = current
        }
    }

    @Test fun artworkWindowStaysInsideActualOverlap() {
        val window = AutoMixArtworkTuning.windowAfter(5.0, 9.0, 0.0)!!
        assertTrue(window.first >= 5.0)
        assertTrue(window.second <= 9.0)
        assertTrue(window.second > window.first)
    }

    @Test fun shortOrInvalidOverlapNeverProducesWindow() {
        assertNull(AutoMixArtworkTuning.windowAfter(2.0, 2.5, 3.0))
        assertNull(AutoMixArtworkTuning.windowAfter(Double.NaN, 7.0, 1.0))
    }
}
