// SPDX-FileCopyrightText: 2026 Aurelis
// SPDX-License-Identifier: LGPL-3.0-or-later
package com.music.aurelis.playback.smart

import org.junit.Assert.assertEquals
import org.junit.Test

class AutomixThreadBudgetTest {
    @Test fun balancedOnEightCoresUsesTwoThreads() {
        assertEquals(2, AutomixThreadBudget.choose(2, 8, false, false, false))
    }

    @Test fun maximumDoesNotOversubscribeSmallDevices() {
        assertEquals(2, AutomixThreadBudget.choose(4, 4, false, false, false))
        assertEquals(1, AutomixThreadBudget.choose(4, 1, false, false, false))
    }

    @Test fun lowRamAndSevereHeatTakePriorityOverUserPreset() {
        assertEquals(1, AutomixThreadBudget.choose(4, 8, true, false, false))
        assertEquals(1, AutomixThreadBudget.choose(4, 8, false, true, true))
    }

    @Test fun moderateHeatLimitsMaximumButRetainsBalanced() {
        assertEquals(2, AutomixThreadBudget.choose(4, 12, false, true, false))
        assertEquals(2, AutomixThreadBudget.choose(2, 8, false, true, false))
    }

    @Test fun efficientAndBalancedKeepOneAnalysisWhenBothTracksAreReady() {
        assertEquals(listOf(1), AutomixThreadBudget.allocate(1, 2, true))
        assertEquals(listOf(2), AutomixThreadBudget.allocate(2, 2, true))
    }

    @Test fun fullBudgetIsReservedForOneTrackWhenTheOtherCannotStart() {
        assertEquals(emptyList<Int>(), AutomixThreadBudget.allocate(4, 0, true))
        assertEquals(listOf(4), AutomixThreadBudget.allocate(4, 1, true))
    }

    @Test fun maximumSplitsOnlyWhenBothTracksAreReadyAndThereIsTime() {
        assertEquals(listOf(2, 2), AutomixThreadBudget.allocate(4, 2, true))
        assertEquals(listOf(4), AutomixThreadBudget.allocate(4, 2, false))
    }

    @Test fun thermalBudgetPreventsParallelAnalysisInMaximumMode() {
        val warmBudget = AutomixThreadBudget.choose(4, 8, false, true, false)
        assertEquals(listOf(2), AutomixThreadBudget.allocate(warmBudget, 2, true))
    }
}
