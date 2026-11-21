// SPDX-FileCopyrightText: 2026 Aurelis
// SPDX-License-Identifier: LGPL-3.0-or-later
package com.music.aurelis.data.settings

/** The visual timeline for a real AutoMix handoff, independent of either UI surface. */
data class AutoMixArtworkState(
    val generation: Long,
    val outgoingMediaId: String,
    val incomingMediaId: String,
    val outgoingTitle: String,
    val outgoingArtist: String,
    val incomingTitle: String,
    val incomingArtist: String,
    val outgoingArtworkUrl: String?,
    val incomingArtworkUrl: String?,
    val progress: Float = 0f,
) {
    fun contains(mediaId: String): Boolean =
        mediaId == outgoingMediaId || mediaId == incomingMediaId

    companion object {
        /** Make the visual midpoint coincide with the actual native LoopMix handoff. */
        fun loopProgress(seconds: Double, handoff: Double, end: Double): Float {
            if (!seconds.isFinite() || !handoff.isFinite() || !end.isFinite() ||
                handoff <= 0.0 || end <= handoff
            ) return 0f
            val fraction = if (seconds <= handoff) {
                0.5 * seconds / handoff
            } else {
                0.5 + 0.5 * (seconds - handoff) / (end - handoff)
            }
            return fraction.coerceIn(0.0, 1.0).toFloat()
        }
    }
}
