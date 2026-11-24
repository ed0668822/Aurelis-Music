// SPDX-FileCopyrightText: © 2026 Aurelis

package com.music.aurelis.playback.cast

/** TV08 recovery thresholds. Revoke stale remote sessions; never auto-play the phone. */
internal object RemoteRecoveryPolicy {
    const val DLNA_CONSECUTIVE_MISSES = 5
    const val BROWSER_HEARTBEAT_TIMEOUT_MS = 15_000L

    fun dlnaUnreachable(consecutiveMisses: Int): Boolean =
        consecutiveMisses >= DLNA_CONSECUTIVE_MISSES

    fun browserUnreachable(active: Boolean, nowElapsedMs: Long, lastHeartbeatElapsedMs: Long): Boolean =
        active && lastHeartbeatElapsedMs > 0 &&
            nowElapsedMs >= lastHeartbeatElapsedMs &&
            nowElapsedMs - lastHeartbeatElapsedMs >= BROWSER_HEARTBEAT_TIMEOUT_MS
}
