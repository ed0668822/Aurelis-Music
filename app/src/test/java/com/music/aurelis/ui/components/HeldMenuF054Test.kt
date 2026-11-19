// SPDX-FileCopyrightText: 2026 Aurelis
// SPDX-License-Identifier: LGPL-3.0-or-later
package com.music.aurelis.ui.components

import androidx.compose.ui.unit.IntRect
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.LayoutDirection
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class HeldMenuF054Test {
    @Test fun ltrTrailingEdgeAndTopSpaceMatchSourceBehavior() {
        val anchor = IntRect(300, 600, 340, 640)
        val popup = heldMenuPosition(anchor, IntSize(360, 720), IntSize(256, 300),
            LayoutDirection.Ltr, margin = 16, gap = 8)
        assertEquals(84, popup.x)
        assertEquals(292, popup.y)
    }

    @Test fun rtlAlignsTheOppositeEdgeAndNeverLeavesTheViewport() {
        val anchor = IntRect(5, 90, 60, 130)
        val popup = heldMenuPosition(anchor, IntSize(360, 720), IntSize(256, 300),
            LayoutDirection.Rtl, margin = 16, gap = 8)
        assertEquals(16, popup.x)
        assertEquals(138, popup.y)
    }

    @Test fun missingAnchorCentersAndTransformsFromCentre() {
        val bounds = IntSize(400, 700)
        val popup = heldMenuPosition(null, bounds, IntSize(200, 240),
            LayoutDirection.Ltr, 16, 8)
        assertEquals(100, popup.x)
        assertEquals(230, popup.y)
        val origin = heldMenuTransformOrigin(null, IntRect(100, 230, 300, 470))
        assertEquals(0.5f, origin.pivotFractionX, 0f)
        assertEquals(0.5f, origin.pivotFractionY, 0f)
    }

    @Test fun partiallyOffscreenAnchorStillProducesClampedOrigin() {
        val anchor = IntRect(345, 680, 390, 730)
        val panel = IntRect(88, 376, 344, 676)
        val origin = heldMenuTransformOrigin(anchor, panel)
        assertTrue(origin.pivotFractionX in 0f..1f)
        assertTrue(origin.pivotFractionY in 0f..1f)
    }
}
