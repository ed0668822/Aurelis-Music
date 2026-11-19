// SPDX-FileCopyrightText: 2026 Aurelis
// SPDX-License-Identifier: LGPL-3.0-or-later
package com.music.aurelis.ui.utils

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/** F065: the existing iOS rubber-band maths must never emit invalid layers. */
class ReducedMotionF065Test {
    @Test fun overscrollCurveIsFiniteForInvalidInputAndUnboundedContainer() {
        assertEquals(0f, rubberBand(Float.NaN, 360f), 0f)
        assertEquals(0f, rubberBand(Float.POSITIVE_INFINITY, 360f), 0f)
        assertEquals(0f, rubberBand(Float.NEGATIVE_INFINITY, 360f), 0f)
        assertTrue(rubberBand(160f, Float.NaN).isFinite())
        assertTrue(rubberBand(160f, 0f).isFinite())
    }

    @Test fun reboundDirectionAndStrengthRemainBounded() {
        assertEquals(0f, rubberBand(0f, 360f), 0f)
        val positive = rubberBand(60f, 360f)
        val negative = rubberBand(-60f, 360f)
        assertTrue(positive > 0f && positive < 60f)
        assertEquals(-positive, negative, 0.0001f)
    }
}
