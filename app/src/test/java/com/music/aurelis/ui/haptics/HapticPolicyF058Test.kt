// SPDX-FileCopyrightText: 2026 Aurelis
// SPDX-License-Identifier: LGPL-3.0-or-later
package com.music.aurelis.ui.haptics

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class HapticPolicyF058Test {
    @Test fun allActionsAreShortFiniteAndNonRepeatingByDesign() {
        assertEquals(10, Haptic.entries.size)
        Haptic.entries.forEach { action ->
            val duration = hapticRhythmDurationMs(action)
            assertTrue("Nonpositive duration for $action: $duration", duration > 0)
            assertTrue("Too long for $action: $duration ms", duration <= 65L)
        }
    }

    @Test fun transportMotionPatternsMirrorAsDistinctDirections() {
        assertEquals(hapticRhythmDurationMs(Haptic.SkipNext),
            hapticRhythmDurationMs(Haptic.SkipPrevious))
        assertEquals(hapticRhythmDurationMs(Haptic.Resume),
            hapticRhythmDurationMs(Haptic.Pause))
        assertTrue(hapticRhythmDurationMs(Haptic.Expand) > hapticRhythmDurationMs(Haptic.Tick))
    }

    @Test fun absentVibratorNeverThrowsWhenPlayerSendsAnyAction() {
        val noMotor = Haptics(null)
        Haptic.entries.forEach(noMotor::play)
    }
}
