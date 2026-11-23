package com.music.aurelis.playback

import com.music.aurelis.playback.AutoMixStartProbe.Status
import org.junit.Assert.assertEquals
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
}
