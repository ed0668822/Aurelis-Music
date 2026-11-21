// SPDX-FileCopyrightText: 2026 Aurelis
// SPDX-License-Identifier: LGPL-3.0-or-later

package com.music.aurelis.playback

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.pow

class ReplayGainPolicyTest {
    @Test fun unknownMeasurementLeavesPcmUntouched() {
        assertEquals(1f, ReplayGainPolicy.gain(null), 0f)
    }

    @Test fun integratedLoudnessTargetsMinusEighteenLufs() {
        val measured = ReplayGainMeasurement(-24.0, 0.1)
        assertEquals(1.9952623f, ReplayGainPolicy.gain(measured), 0.0001f)
    }

    @Test fun ceilingPreventsClippingForKnownTruePeak() {
        val measured = ReplayGainMeasurement(-30.0, 0.9)
        assertTrue(ReplayGainPolicy.gain(measured) * 0.9 <= 0.98501)
    }

    @Test fun albumModeUsesAlbumGainWithoutCompoundingTrackGain() {
        val measured = ReplayGainMeasurement(-30.0, 0.1, -20.0, 0.1)
        assertEquals(10.0.pow(2.0 / 20.0).toFloat(), ReplayGainPolicy.gain(measured, ReplayGainMode.ALBUM), 0.0001f)
    }

    @Test fun albumWithoutCompleteMeasurementFallsBackToTrack() {
        val measured = ReplayGainMeasurement(-24.0, 0.1)
        assertEquals(ReplayGainPolicy.gain(measured), ReplayGainPolicy.gain(measured, ReplayGainMode.ALBUM), 0f)
    }

    @Test fun bitPerfectBypassesAnyGainAndBadMeasurementIsIgnored() {
        assertEquals(1f, ReplayGainPolicy.gain(ReplayGainMeasurement(-35.0, 0.2), bitPerfect = true), 0f)
        assertEquals(1f, ReplayGainPolicy.gain(ReplayGainMeasurement(Double.NaN, 0.2)), 0f)
    }

    @Test fun concurrentRegistryIsKeyedByMediaId() {
        val item = ReplayGainMeasurement(-22.0, 0.75)
        ReplayGainMeasurements.publish("content://media/audio/123", item)
        assertEquals(item, ReplayGainMeasurements.get("content://media/audio/123"))
        ReplayGainMeasurements.remove("content://media/audio/123")
        assertEquals(null, ReplayGainMeasurements.get("content://media/audio/123"))
    }
}
