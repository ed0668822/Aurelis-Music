// SPDX-FileCopyrightText: 2026 Aurelis
// SPDX-License-Identifier: LGPL-3.0-or-later

package com.music.aurelis.playback

import java.util.concurrent.ConcurrentHashMap
import kotlin.math.pow

/**
 * B03: one normalization decision for each track; callers must never multiply
 * ReplayGain and source normalization together.
 *
 * Measurements are accepted only from a complete EBU R128 scan. The scanner
 * will publish them to [ReplayGainMeasurements] off the audio thread.
 */
internal data class ReplayGainMeasurement(
    val integratedLufs: Double,
    val truePeakLinear: Double?,
    val albumIntegratedLufs: Double? = null,
    val albumTruePeakLinear: Double? = null,
)

enum class ReplayGainMode { TRACK, ALBUM }

internal object ReplayGainPolicy {
    const val TARGET_LUFS = -18.0
    const val PEAK_CEILING = 0.985

    /** No measurement, invalid result, or bit-perfect mode: zero gain change. */
    fun gain(
        measurement: ReplayGainMeasurement?,
        mode: ReplayGainMode = ReplayGainMode.TRACK,
        bitPerfect: Boolean = false,
    ): Float {
        if (bitPerfect || measurement == null) return 1f
        val album = mode == ReplayGainMode.ALBUM && measurement.albumIntegratedLufs != null
        val lufs = if (album) measurement.albumIntegratedLufs!! else measurement.integratedLufs
        val peak = if (album) measurement.albumTruePeakLinear else measurement.truePeakLinear
        if (!lufs.isFinite() || lufs !in -100.0..10.0) return 1f
        val wanted = 10.0.pow(((TARGET_LUFS - lufs).coerceIn(-24.0, 12.0)) / 20.0)
        val safe = if (peak != null && peak.isFinite() && peak > 0.0) {
            minOf(wanted, PEAK_CEILING / peak)
        } else wanted
        return safe.coerceIn(0.01, 4.0).toFloat()
    }
}

/**
 * B04: non-blocking handoff from a background analyzer to the existing
 * per-player LoudnessProcessor. Entries never write back to audio files.
 */
internal object ReplayGainMeasurements {
    private val byMediaId = ConcurrentHashMap<String, ReplayGainMeasurement>()

    fun publish(mediaId: String, measurement: ReplayGainMeasurement) {
        if (mediaId.isBlank() || !measurement.integratedLufs.isFinite()) return
        byMediaId[mediaId] = measurement
    }

    fun get(mediaId: String): ReplayGainMeasurement? = byMediaId[mediaId]
    fun remove(mediaId: String) { byMediaId.remove(mediaId) }
    fun clear() { byMediaId.clear() }
}
