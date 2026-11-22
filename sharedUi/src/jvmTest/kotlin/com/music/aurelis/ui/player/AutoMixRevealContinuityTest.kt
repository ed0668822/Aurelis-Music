// SPDX-FileCopyrightText: 2026 Aurelis
// SPDX-License-Identifier: LGPL-3.0-or-later
package com.music.aurelis.ui.player

import org.junit.Assert.assertEquals
import org.junit.Test

class AutoMixRevealContinuityTest {
    @Test fun fullBleedGeometryDoesNotCollapseDuringTheMediaSessionSwap() {
        assertEquals(1f, autoMixBannerShare(0f, revealingFullBleed = true), 0.0001f)
        assertEquals(1f, autoMixBannerShare(0.8f, revealingFullBleed = true), 0.0001f)
        assertEquals(0.8f, autoMixBannerShare(0.8f, revealingFullBleed = false), 0.0001f)
    }

    @Test fun sameAlbumCoverNeverCreatesASecondBlackArtworkRectangle() {
        assertEquals(
            false, needsAutoMixArtworkReveal(
                "content://media/first", "content://media/second",
                "Meteora", "Meteora", "Linkin Park", "linkin park",
            ),
        )
        assertEquals(
            false, needsAutoMixArtworkReveal(
                "https://example.com/cover.jpg", "https://example.com/cover.jpg",
                "Album A", "Album B", "A", "B",
            ),
        )
        assertEquals(
            false, needsAutoMixArtworkReveal(
                null, "https://example.com/cover2.jpg", "A", "B", "A", "B",
            ),
        )
        assertEquals(
            true, needsAutoMixArtworkReveal(
                "https://example.com/first.jpg", "https://example.com/second.jpg",
                "Meteora", "Minutes to Midnight", "Linkin Park", "Linkin Park",
            ),
        )
    }

    @Test fun revealNeverRewindsOnLateAudioSamples() {
        assertEquals(0.65f, nextAutoMixRevealTarget(0.65f, 0.4f), 0.0001f)
        assertEquals(0.8f, nextAutoMixRevealTarget(0.65f, 0.8f), 0.0001f)
    }

    @Test fun invalidSamplesStayWithinVisualBounds() {
        assertEquals(0.4f, nextAutoMixRevealTarget(0.4f, Float.NaN), 0.0001f)
        assertEquals(0.4f, nextAutoMixRevealTarget(0.4f, Float.NEGATIVE_INFINITY), 0.0001f)
        assertEquals(1f, nextAutoMixRevealTarget(0.4f, 2f), 0.0001f)
        assertEquals(0f, nextAutoMixRevealTarget(Float.NaN, Float.NaN), 0.0001f)
    }

    @Test fun aNewGenerationMayStartFromZero() {
        assertEquals(0f, nextAutoMixRevealTarget(0f, 0f), 0.0001f)
        assertEquals(0.15f, nextAutoMixRevealTarget(0f, 0.15f), 0.0001f)
    }
}
