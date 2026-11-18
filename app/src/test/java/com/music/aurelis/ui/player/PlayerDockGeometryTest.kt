package com.music.aurelis.ui.player

import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.unit.dp
import org.junit.Assert.*
import org.junit.Test

class PlayerDockGeometryTest {
    private val mini = Rect(12f, 720f, 52f, 760f)
    private val expanded = Rect(24f, 100f, 344f, 420f)
    @Test fun retiringAnOldCoverDoesNotClearItsReplacementForTheSameTrack() {
        val oldCover = Any()
        val newCover = Any()
        assertFalse(isCurrentDockOwner("track", newCover, "track", oldCover))
        assertTrue(isCurrentDockOwner("track", newCover, "track", newCover))
        assertFalse(isCurrentDockOwner("other", newCover, "track", newCover))
        assertFalse(isCurrentDockOwner("track", newCover, "track", null))
    }

    @Test fun interpolationUsesMeasuredEndpointsAndClampsTheFraction() {
        assertEquals(mini, artworkBoundsBetween(mini, expanded, -1f))
        assertEquals(expanded, artworkBoundsBetween(mini, expanded, 2f))
        assertEquals(Rect(18f, 410f, 198f, 590f), artworkBoundsBetween(mini, expanded, .5f))
    }
    @Test fun coversUseActualMeasuredCornerRadiiAcrossEveryQuarterFrame() {
        val frames = listOf(0f, .25f, .5f, .75f, 1f)
        val radii = listOf(8f, 6f, 4f, 2f, 0f)
        frames.forEachIndexed { index, fraction ->
            val pose = artworkDockPose(mini, expanded, 8.dp, 0.dp, fraction)
            assertNotNull(pose)
            assertEquals(radii[index], pose!!.corner.value, 0.0001f)
            assertEquals(artworkBoundsBetween(mini, expanded, fraction), pose.bounds)
        }
        assertEquals(5.7f, artworkDockPose(mini, expanded, 5.7.dp, 10.dp, 0f)!!.corner.value, 0.0001f)
        assertEquals(10f, artworkDockPose(mini, expanded, 5.7.dp, 10.dp, 1f)!!.corner.value, 0.0001f)
    }

    @Test fun corruptPoseOrUnmeasuredCornersDoNotReachTheDrawingLayer() {
        assertNull(artworkDockPose(mini, expanded, Float.NaN.dp, 0.dp, .5f))
        assertNull(artworkDockPose(mini, expanded, 8.dp, (-1).dp, .5f))
        assertNull(artworkDockPose(mini, expanded, 8.dp, 0.dp, Float.POSITIVE_INFINITY))
        assertEquals(mini, artworkDockPose(mini, expanded, 8.dp, 0.dp, -10f)?.bounds)
        assertEquals(expanded, artworkDockPose(mini, expanded, 8.dp, 0.dp, 10f)?.bounds)
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
