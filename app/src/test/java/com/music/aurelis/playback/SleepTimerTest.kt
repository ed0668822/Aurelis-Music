package com.music.aurelis.playback

import org.junit.Assert.assertEquals
import org.junit.Test

class SleepTimerTest {

    @Test
    fun customSliderSpansOneStepToTwelveHours() {
        assertEquals(SleepTimer.STEP_MINUTES, SleepTimer.minutesAt(0f))
        assertEquals(SleepTimer.MAX_MINUTES, SleepTimer.minutesAt(1f))
        // Dragged past either end, it stays inside the range.
        assertEquals(SleepTimer.STEP_MINUTES, SleepTimer.minutesAt(-0.5f))
        assertEquals(SleepTimer.MAX_MINUTES, SleepTimer.minutesAt(1.5f))
    }

    @Test
    fun customSliderLandsOnWholeSteps() {
        for (i in 0..100) {
            assertEquals(0, SleepTimer.minutesAt(i / 100f) % SleepTimer.STEP_MINUTES)
        }
    }

    @Test
    fun fractionOfIsTheInverseOfMinutesAt() {
        for (minutes in SleepTimer.STEP_MINUTES..SleepTimer.MAX_MINUTES step SleepTimer.STEP_MINUTES) {
            assertEquals(minutes, SleepTimer.minutesAt(SleepTimer.fractionOf(minutes)))
        }
    }
    @Test
    fun fadeEnvelopeIsFullUntilLastThirtySeconds() {
        assertEquals(1f, SleepTimer.calculateFadeVolume(60_000L), 0.0001f)
        assertEquals(1f, SleepTimer.calculateFadeVolume(30_000L), 0.0001f)
        assertEquals(1f, SleepTimer.calculateFadeVolume(15_000L, fadeOutEnabled = false), 0.0001f)
        assertEquals(1f, SleepTimer.calculateFadeVolume(10_000L, fadeDurationMs = 0L), 0.0001f)
    }

    @Test
    fun fadesLinearlyAndClampsAtDeadline() {
        assertEquals(0.75f, SleepTimer.calculateFadeVolume(22_500L), 0.0001f)
        assertEquals(0.5f, SleepTimer.calculateFadeVolume(15_000L), 0.0001f)
        assertEquals(0.25f, SleepTimer.calculateFadeVolume(7_500L), 0.0001f)
        assertEquals(0f, SleepTimer.calculateFadeVolume(0L), 0.0001f)
        assertEquals(0f, SleepTimer.calculateFadeVolume(-99L), 0.0001f)
    }
}
