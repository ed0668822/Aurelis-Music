// SPDX-FileCopyrightText: 2026 Aurelis
// SPDX-License-Identifier: LGPL-3.0-or-later

package com.music.aurelis.playback.smart

/**
 * A coarse read of what kind of music a track is, from analysis fields that
 * already exist. There is no genre tag involved: the same song reads the same
 * whatever its metadata says.
 */
enum class MusicKind {
    /** A steady, kick-driven track at a danceable tempo with a trusted grid. */
    DANCE,

    /** Sung through, without the drive to count as [DANCE]. */
    VOCAL,

    /** No kick to speak of, or no beat evidence at all: acoustic, ambient, rubato. */
    CALM,

    /** Anything the evidence does not place. Never acted on by the arbiter. */
    GENERAL,
}

private const val KICK_LEVEL = 0.5
private const val DANCE_KICK_SHARE = 0.5
private const val CALM_KICK_SHARE = 0.2
private const val DANCE_MIN_BPM = 100.0
private const val DANCE_MAX_BPM = 180.0

/** With no low-band curve to read a kick from, only a very sure grid can call a track dance. */
private const val DANCE_CONFIDENCE_WITHOUT_KICK = 0.8
private const val VOCAL_KIND_PROBABILITY = 0.62

/**
 * Fraction of the track whose low band sits at or above half its own loud
 * reference: a steady kick keeps it there, a plucked or sparse arrangement
 * does not. Null without a usable low-band curve.
 */
private fun kickShare(analysis: TrackAnalysis): Double? {
    val energies = analysis.lowEnergyCurve.map { it.energy }.filter { it.isFinite() && it >= 0 }
    if (energies.size < 8) return null
    val sorted = energies.sorted()
    val reference = sorted[((sorted.size - 1) * 0.9).toInt()]
    if (reference <= 1e-9) return null
    return energies.count { it >= reference * KICK_LEVEL }.toDouble() / energies.size
}

/** Classifies [analysis]; an unusable analysis is [MusicKind.GENERAL]. */
fun musicKindOf(analysis: TrackAnalysis): MusicKind {
    if (!analysis.isUsable) return MusicKind.GENERAL
    val confidence = analysis.beatConfidence.orZero()
    val bpm = analysis.bpm.orZero()
    val kick = kickShare(analysis)
    val sung = analysis.vocalProbability.orZero() >= VOCAL_KIND_PROBABILITY
    val driven = confidence >= MIN_BEATMATCH_CONFIDENCE &&
        (if (kick != null) kick >= DANCE_KICK_SHARE else confidence >= DANCE_CONFIDENCE_WITHOUT_KICK)
    return when {
        driven && bpm in DANCE_MIN_BPM..DANCE_MAX_BPM && !sung -> MusicKind.DANCE
        confidence < MIN_DJ_CONFIDENCE || (kick != null && kick < CALM_KICK_SHARE) -> MusicKind.CALM
        sung -> MusicKind.VOCAL
        else -> MusicKind.GENERAL
    }
}
