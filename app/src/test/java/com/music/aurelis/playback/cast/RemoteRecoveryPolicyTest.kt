// SPDX-FileCopyrightText: © 2026 Aurelis

package com.music.aurelis.playback.cast

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class RemoteRecoveryPolicyTest {
    @Test fun dlnaDisconnectsOnlyAfterRepeatedMisses() {
        for (misses in 0 until 5) {
            assertFalse(RemoteRecoveryPolicy.dlnaUnreachable(misses))
        }
        assertTrue(RemoteRecoveryPolicy.dlnaUnreachable(5))
    }

    @Test fun browserNeverTimesOutBeforeActualPlaybackBegan() {
        assertFalse(RemoteRecoveryPolicy.browserUnreachable(false, 60_000, 1))
        assertFalse(RemoteRecoveryPolicy.browserUnreachable(true, 60_000, 0))
        assertFalse(RemoteRecoveryPolicy.browserUnreachable(true, 12_000, 20_000))
    }

    @Test fun browserHeartbeatTimeoutIsBoundedAndDeterministic() {
        assertFalse(RemoteRecoveryPolicy.browserUnreachable(true, 19_999, 5_000))
        assertTrue(RemoteRecoveryPolicy.browserUnreachable(true, 20_000, 5_000))
        assertTrue(RemoteRecoveryPolicy.browserUnreachable(true, 40_000, 5_000))
    }
}
