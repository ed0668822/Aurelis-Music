package com.music.aurelis.playback

import com.music.aurelis.playback.AutoMixStartProbe.Status
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class AutoMixStartProbeTest {
    @Test fun readyWithoutPositionProgressKeepsWaiting() {
        val probe = AutoMixStartProbe(positionMs = 5_000, startedAtMs = 100)
        assertEquals(Status.WAITING, probe.status(5_000, playing = true, nowMs = 500))
        assertEquals(Status.WAITING, probe.status(5_040, playing = false, nowMs = 500))
        assertEquals(Status.ADVANCING, probe.status(5_020, playing = true, nowMs = 500))
    }

    @Test fun pastDeadlineTheStandbyCannotTakeOver() {
        val probe = AutoMixStartProbe(positionMs = 5_000, startedAtMs = 100)
        assertEquals(Status.EXPIRED, probe.status(5_000, playing = false, nowMs = 1_100))
        assertEquals(Status.EXPIRED, probe.status(5_040, playing = true, nowMs = 1_100))
    }

    @Test fun alignmentRequiresThreeConsistentPositionReads() {
        val probe = AutoMixStartProbe(positionMs = 5_000, startedAtMs = 100)
        assertNull(probe.aligned(12.0, 65.0))
        assertNull(probe.aligned(-9.0, 65.0))
        assertEquals(true, probe.aligned(20.0, 65.0))
    }

    @Test fun positionJitterOrLateStartRejectsAlignment() {
        val jitter = AutoMixStartProbe(positionMs = 5_000, startedAtMs = 100)
        jitter.aligned(-45.0, 65.0)
        jitter.aligned(50.0, 65.0)
        assertEquals(false, jitter.aligned(0.0, 65.0))

        val late = AutoMixStartProbe(positionMs = 5_000, startedAtMs = 100)
        late.aligned(-108.0, 65.0)
        late.aligned(-104.0, 65.0)
        assertEquals(false, late.aligned(-106.0, 65.0))
    }
}
