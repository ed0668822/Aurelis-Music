// SPDX-FileCopyrightText: 2026 Aurelis
// SPDX-License-Identifier: LGPL-3.0-or-later

package com.music.aurelis.playback

import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.PI
import kotlin.math.sin

class R128MeterTest {
    private fun sine(amplitude: Double, rate: Int = 48_000): FloatArray =
        FloatArray(rate) { index ->
            (amplitude * sin(2.0 * PI * 1000.0 * index / rate)).toFloat()
        }

    @Test fun albumGateCombinesQuietAndLoudTracksWithoutMeanOfDecibels() {
        val loud = R128Meter(48_000, 1).apply { add(sine(0.5)) }
        val quiet = R128Meter(48_000, 1).apply { add(sine(0.1)) }
        val integrated = R128Meter.integratedLufs(loud.blockEnergies() + quiet.blockEnergies())
        assertNotNull(integrated)
        assertTrue(integrated!! > quiet.finish()!!.integratedLufs)
        assertTrue(integrated < loud.finish()!!.integratedLufs)
    }

    @Test fun shortAudioIsNotInventedAsValidMeasurement() {
        val meter = R128Meter(48_000, 1)
        meter.add(FloatArray(240))
        assertNull(meter.finish())
    }

    @Test fun silenceFailsAbsoluteLoudnessGate() {
        val meter = R128Meter(48_000, 2)
        meter.add(FloatArray(48_000), FloatArray(48_000))
        assertNull(meter.finish())
    }

    @Test fun quieterSineProducesLowerLufs() {
        val loud = R128Meter(48_000, 1).apply { add(sine(0.5)) }.finish()
        val quiet = R128Meter(48_000, 1).apply { add(sine(0.1)) }.finish()
        assertNotNull(loud)
        assertNotNull(quiet)
        val difference = loud!!.integratedLufs - quiet!!.integratedLufs
        assertTrue("Expected about 14 dB, found $difference", difference in 13.0..15.0)
    }

    @Test fun peakIsAtLeastSamplePeak() {
        val meter = R128Meter(48_000, 1)
        meter.add(sine(0.75))
        val measured = meter.finish()
        assertNotNull(measured)
        assertTrue(measured!!.truePeakLinear!! >= 0.749)
    }
}
