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
    /** Album identity lets the UI avoid repainting the same cover for sibling tracks. */
    val outgoingAlbum: String = "",
    val incomingAlbum: String = "",
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

/**
 * Timing and easing for organic artwork reveals.
 * Kept independent of the audio engine so both crossfade and native Loop Mix
 * can publish their existing real-time progress without introducing a player.
 */
object AutoMixArtworkTuning {
    const val MIN_WINDOW_SECONDS = 0.8
    const val WINDOW_SECONDS = 2.6
    const val PREPARE_LEAD_SECONDS = 1.5

    fun revealProgress(progress: Float): Float {
        val p = if (progress.isFinite()) progress.coerceIn(0f, 1f) else 0f
        val smooth = p * p * (3f - 2f * p)
        return smooth * 0.75f + p * 0.25f
    }

    /**
     * The window cannot start before the observed media handoff and cannot
     * extend past the actual end of the overlap.
     */
    fun windowAfter(
        swapSeconds: Double,
        endSeconds: Double,
        preparedSeconds: Double,
    ): Pair<Double, Double>? {
        if (!swapSeconds.isFinite() || !endSeconds.isFinite() ||
            !preparedSeconds.isFinite() || endSeconds - swapSeconds < MIN_WINDOW_SECONDS
        ) return null
        val remaining = endSeconds - swapSeconds
        val lead = (PREPARE_LEAD_SECONDS - preparedSeconds)
            .coerceIn(0.0, remaining - MIN_WINDOW_SECONDS)
        val duration = minOf(WINDOW_SECONDS, remaining - lead)
        val start = swapSeconds + lead
        return start to minOf(endSeconds, start + duration)
    }
}
