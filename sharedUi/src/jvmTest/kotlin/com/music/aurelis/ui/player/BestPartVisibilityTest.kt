// SPDX-FileCopyrightText: 2026 Aurelis
// SPDX-License-Identifier: LGPL-3.0-or-later
package com.music.aurelis.ui.player

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class BestPartVisibilityTest {
    @Test fun showsOnlyWhenConfirmedTargetIsStillAhead() {
        assertTrue(shouldOfferBestPart(45_000L, 5_000L, mixing = false, heard = false))
        assertFalse(shouldOfferBestPart(null, 5_000L, mixing = false, heard = false))
        assertFalse(shouldOfferBestPart(0L, 5_000L, mixing = false, heard = false))
    }

    @Test fun keepsAudioMixingAndAlreadyHeardMomentsSafe() {
        assertFalse(shouldOfferBestPart(45_000L, 5_000L, mixing = true, heard = false))
        assertFalse(shouldOfferBestPart(45_000L, 5_000L, mixing = false, heard = true))
        assertFalse(shouldOfferBestPart(45_000L, 44_700L, mixing = false, heard = false))
    }

    @Test fun disabledSettingHidesKnownBestPart() {
        assertFalse(shouldOfferBestPart(45_000L, 5_000L, mixing = false, heard = false, enabled = false))
        assertTrue(shouldOfferBestPart(45_000L, 5_000L, mixing = false, heard = false, enabled = true))
    }

    @Test fun allowsLateTargetAfterASeekBackwardWhenNotYetHeard() {
        assertTrue(shouldOfferBestPart(45_000L, 44_599L, mixing = false, heard = false))
    }
}
