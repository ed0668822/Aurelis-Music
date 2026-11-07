package com.music.aurelis.ui.player

import androidx.compose.ui.geometry.Rect
import org.junit.Assert.*
import org.junit.Test

class PlayerDockGeometryTest {
    private val mini = Rect(12f, 720f, 52f, 760f)
    private val expanded = Rect(24f, 100f, 344f, 420f)
    @Test fun interpolationUsesMeasuredEndpointsAndClampsTheFraction() {
        assertEquals(mini, artworkBoundsBetween(mini, expanded, -1f))
        assertEquals(expanded, artworkBoundsBetween(mini, expanded, 2f))
        assertEquals(Rect(18f, 410f, 198f, 590f), artworkBoundsBetween(mini, expanded, .5f))
    }
    @Test fun absentInvalidOrOverflowingMeasurementsNeverCreateADrawSize() {
        for (bad in listOf(Rect.Zero, Rect(10f, 10f, 0f, 0f),
            Rect(Float.NaN, 0f, 40f, 40f), Rect(0f, 0f, Float.POSITIVE_INFINITY, 40f),
            Rect(-Float.MAX_VALUE, 0f, Float.MAX_VALUE, 40f))) {
            assertNull(artworkBoundsBetween(bad, expanded, .5f))
            assertNull(artworkBoundsBetween(mini, bad, .5f))
        }
        assertNull(artworkBoundsBetween(mini, expanded, Float.NaN))
    }
}
