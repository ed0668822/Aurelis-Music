// SPDX-FileCopyrightText: © 2026 Aurelis

package com.music.aurelis.playback.smart

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class AutoMixBeatConfirmationTest {
    private val primary = List(49) { it * 0.5 }

    @Test fun agreesAcrossShiftedWindowsOnTheSameTimeline() {
        val nearby = List(49) { 6.01 + it * 0.5 }
        assertTrue(agreesWithNearbyBeatGrid(primary, 120.0, nearby, 120.0))
    }

    @Test fun matchingTempoDoesNotConfirmAnOffsetPulse() {
        val nearby = List(49) { 6.12 + it * 0.5 }
        assertFalse(agreesWithNearbyBeatGrid(primary, 120.0, nearby, 120.0))
    }

    @Test fun rejectsSmallTempoDifferencesThatDriftAcrossTheWindow() {
        val nearby = List(49) { 6.0 + it * 0.505 }
        assertFalse(agreesWithNearbyBeatGrid(primary, 120.0, nearby, 60.0 / 0.505))
    }

    @Test fun rejectsWindowsWithoutEnoughMeasuredOverlap() {
        val nearby = List(49) { 23.0 + it * 0.5 }
        assertFalse(agreesWithNearbyBeatGrid(primary, 120.0, nearby, 120.0))
    }
}
