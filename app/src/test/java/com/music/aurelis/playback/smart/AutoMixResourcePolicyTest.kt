// SPDX-FileCopyrightText: 2026 Aurelis
// SPDX-License-Identifier: LGPL-3.0-or-later

package com.music.aurelis.playback.smart

import org.junit.Assert.assertEquals
import org.junit.Test

class AutoMixResourcePolicyTest {
    private val gib = 1024L * 1024L * 1024L

    @Test fun usesFourWorkersOnAnIdleWellProvisionedDevice() {
        assertEquals(4, AutoMixResourcePolicy.inferenceThreads(8, 4 * gib))
    }

    @Test fun yieldsCoresOnSmallerProcessors() {
        assertEquals(1, AutoMixResourcePolicy.inferenceThreads(2, 4 * gib))
        assertEquals(2, AutoMixResourcePolicy.inferenceThreads(4, 4 * gib))
        assertEquals(3, AutoMixResourcePolicy.inferenceThreads(6, 4 * gib))
    }

    @Test fun scalesDownWhenAvailableRamFalls() {
        assertEquals(3, AutoMixResourcePolicy.inferenceThreads(8, 2 * gib))
        assertEquals(2, AutoMixResourcePolicy.inferenceThreads(8, 1 * gib))
        assertEquals(1, AutoMixResourcePolicy.inferenceThreads(8, 512L * 1024 * 1024))
    }

    @Test fun lowRamOrMemoryPressureAlwaysUsesOneWorker() {
        assertEquals(1, AutoMixResourcePolicy.inferenceThreads(8, 4 * gib, lowMemory = true))
        assertEquals(1, AutoMixResourcePolicy.inferenceThreads(8, 4 * gib, lowRamDevice = true))
        assertEquals(1, AutoMixResourcePolicy.inferenceThreads(
            8, 1 * gib, lowMemoryThresholdBytes = 700L * 1024 * 1024,
        ))
    }

    @Test fun respectsPowerSaverAndThermalWarnings() {
        assertEquals(2, AutoMixResourcePolicy.inferenceThreads(8, 4 * gib, powerSave = true))
        assertEquals(2, AutoMixResourcePolicy.inferenceThreads(8, 4 * gib, warm = true))
        assertEquals(1, AutoMixResourcePolicy.inferenceThreads(8, 4 * gib, hot = true))
    }

    @Test fun unknownMemoryDoesNotMaxOutTheCpu() {
        assertEquals(2, AutoMixResourcePolicy.inferenceThreads(8, 0L))
    }
}
