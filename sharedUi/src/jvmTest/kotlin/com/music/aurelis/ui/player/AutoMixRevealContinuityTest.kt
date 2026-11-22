// SPDX-FileCopyrightText: 2026 Aurelis
// SPDX-License-Identifier: LGPL-3.0-or-later
package com.music.aurelis.ui.player

import org.junit.Assert.assertEquals
import org.junit.Test

class AutoMixRevealContinuityTest {
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
