// SPDX-FileCopyrightText: 2026 Aurelis
// SPDX-License-Identifier: LGPL-3.0-or-later

package com.music.aurelis.playback

import com.music.aurelis.data.settings.AutoMixEffect
import com.music.aurelis.data.settings.AutoMixOptions
import com.music.aurelis.playback.smart.TrackAnalysis
import com.music.aurelis.playback.smart.TransitionPlan
import com.music.aurelis.playback.smart.TransitionStyle
import kotlin.math.abs
import kotlin.math.min
import kotlin.math.roundToInt

/**
 * Mornye-inspired, local beat matching reimplemented for Aurelis's existing
 * two-deck playback planner. No live seeks or playback-speed pulses are
 * triggered here: both actions have caused audible glitches on Android routes.
 *
 * A model beat is a scheduling hint, not proof that two AudioTracks rendered
 * their buffers at the same instant. For that reason [TransitionPlan.phaseLock]
 * remains off until a device-level verification path is available.
 *
 * The existing Loop Mix check in CrossfadeController still takes precedence.
 */
internal fun alignAutoMixBeats(
    original: TransitionPlan,
    options: AutoMixOptions,
    outgoing: TrackAnalysis,
    incoming: TrackAnalysis,
    outgoingLength: Double,
    incomingLength: Double,
    playbackTime: Double,
): TransitionPlan {
    if (original.blocked || original.transitionStyle == TransitionStyle.GAPLESS) return original
    if (original.fadeSeconds <= 0.0 || !original.fadeSeconds.isFinite()) return original
    val safe = options.normalized()
    if (safe.effect == AutoMixEffect.CROSSFADE) {
        return noBeatmatch(original, "crossfadePreset", plain = true, startFromBeginning = true)
    }
    if (safe.effectiveSpeed != 1f) {
        return noBeatmatch(original, "effectTempo", plain = true)
    }
    if (!incomingLength.isFinite() || incomingLength < 20.0 ||
        !outgoingLength.isFinite() || outgoingLength < 20.0
    ) {
        return noBeatmatch(original, "analysisUnavailable", plain = true)
    }
    if (!outgoing.isUsable || !incoming.isUsable) {
        return noBeatmatch(original, "analysisUnavailable", plain = true)
    }

    val tail = outgoing.tailBeats
    val head = incoming.headBeats
    if (tail == null || head == null ||
        tail.beats.isEmpty() || head.beats.isEmpty()
    ) {
        return noBeatmatch(original, "analysisUnavailable", plain = true)
    }
    if (!tail.confidence.isFinite() || !head.confidence.isFinite() ||
        tail.confidence < TRUSTED_BEAT_CONFIDENCE ||
        head.confidence < TRUSTED_BEAT_CONFIDENCE ||
        !tail.bpm.isFinite() || !head.bpm.isFinite() ||
        tail.bpm !in VALID_BPM || head.bpm !in VALID_BPM
    ) {
        return noBeatmatch(original, "unreliableBeats", plain = true)
    }
    if (original.policyReasons.any { it == "unmixable-pair" }) {
        return noBeatmatch(original, "incompatibleTempo", plain = true)
    }

    val best = TEMPO_OCTAVES
        .map { factor -> tail.bpm / (head.bpm * factor) }
        .minByOrNull { abs(it - 1.0) }
        ?: return noBeatmatch(original, "analysisUnavailable", plain = true)
    if (!best.isFinite() || abs(best - 1.0) >= MAX_STRETCH_DIFFERENCE) {
        return noBeatmatch(original, "incompatibleTempo", plain = true)
    }

    // A late first musical beat often means an intro that should play
    // uninterrupted. Silence before the first beat is excluded by audibleStart.
    val audibleStart = (incoming.audibleStartTime ?: incoming.pickupTime ?: 0.0)
        .takeIf { it.isFinite() && it >= 0.0 } ?: 0.0
    val firstBeat = head.beats.asSequence()
        .filter { it.isFinite() && it >= audibleStart - BEAT_TIME_TOLERANCE }
        .minOrNull()
        ?: return noBeatmatch(original, "alignmentUnverified", plain = true)
    if (firstBeat > min(MAX_INTRO_SECONDS, incomingLength * MAX_INTRO_FRACTION)) {
        return noBeatmatch(original, "longIntro", plain = true)
    }

    // Align at or *after* the planned start so snapping does not silently
    // extend a user-selected manual overlap past its 60-second upper bound.
    // The fade still ends at the source planner's musical mix-out anchor.
    val end = original.transitionEnd
    val target = original.transitionStart
    if (!end.isFinite() || !target.isFinite() || end > outgoingLength ||
        end <= target || playbackTime > end
    ) {
        return noBeatmatch(original, "alignmentUnverified", plain = true)
    }
    val outPeriod = 60.0 / tail.bpm
    val alignedStart = tail.beats.asSequence()
        .filter {
            it.isFinite() && it >= target - BEAT_TIME_TOLERANCE &&
                it <= end - MIN_OVERLAP_SECONDS &&
                it <= target + outPeriod + BEAT_TIME_TOLERANCE
        }
        .minOrNull()
        ?: return noBeatmatch(original, "alignmentUnverified", plain = true)
    val overlap = end - alignedStart
    if (overlap < MIN_OVERLAP_SECONDS || overlap > MAX_OVERLAP_SECONDS + BEAT_TIME_TOLERANCE) {
        return noBeatmatch(original, "alignmentUnverified", plain = true)
    }

    // The incoming cue is fixed *before* prepare(), avoiding audible seeks.
    // Only nearby measured beats can replace the plan's existing cue point;
    // no beat evidence means the current safe crossfade is retained.
    val desiredCue = original.incomingCueTime
    val maxCue = incomingLength - 2.0 * overlap
    if (!desiredCue.isFinite() || maxCue < 0.0) {
        return noBeatmatch(original, "alignmentUnverified", plain = true)
    }
    val inPeriod = 60.0 / head.bpm
    val alignedCue = head.beats.asSequence()
        .filter {
            it.isFinite() && it >= audibleStart - BEAT_TIME_TOLERANCE &&
                it >= 0.0 && it <= maxCue &&
                abs(it - desiredCue) <= inPeriod + BEAT_TIME_TOLERANCE
        }
        .minByOrNull { abs(it - desiredCue) }
        ?: return noBeatmatch(original, "alignmentUnverified", plain = true)

    // If a side's beat model cannot explain three consecutive beat intervals
    // consistently, its timestamps cannot safely authorize tempo stretching.
    if (!gridIsStable(tail.beats, alignedStart, outPeriod) ||
        !gridIsStable(head.beats, alignedCue, inPeriod)
    ) {
        return noBeatmatch(original, "alignmentUnverified", plain = true)
    }

    return original.copy(
        transitionStart = alignedStart,
        fadeSeconds = overlap,
        shouldStart = playbackTime >= alignedStart,
        incomingCueTime = alignedCue,
        incomingPlaybackRate = best,
        outgoingPlaybackRate = 1.0,
        beatSeconds = outPeriod,
        transitionBeats = (overlap / outPeriod).roundToInt(),
        transitionStyle = TransitionStyle.DJ_BLEND,
        bassSwap = true,
        filterSweep = 0.0,
        // Real-time AudioTrack nudge pulses are not proven click-free.
        phaseLock = false,
        policyReasons = original.policyReasons + "automix:beatAligned",
        reason = if (playbackTime >= alignedStart) "automix-beatmatched"
            else "before-automix-beatmatched",
    )
}

/** A bounded consistency check on three local intervals, not a live phase lock. */
private fun gridIsStable(beats: List<Double>, reference: Double, period: Double): Boolean {
    if (period <= 0.0 || !period.isFinite()) return false
    val nearby = beats.asSequence()
        .filter { it.isFinite() && it >= reference - BEAT_TIME_TOLERANCE }
        .take(4)
        .toList()
    if (nearby.size < 4) return false
    val errors = nearby.zipWithNext { a, b -> abs((b - a) - period) }
    return errors.maxOrNull()?.let { it <= GRID_SPREAD_SECONDS } == true
}

private fun noBeatmatch(
    plan: TransitionPlan,
    why: String,
    plain: Boolean,
    startFromBeginning: Boolean = false,
): TransitionPlan {
    val unfiltered = plain && plan.transitionStyle != TransitionStyle.GAPLESS
    return plan.copy(
        incomingPlaybackRate = 1.0,
        outgoingPlaybackRate = 1.0,
        phaseLock = false,
        transitionStyle = if (unfiltered) TransitionStyle.EQUAL_POWER else plan.transitionStyle,
        bassSwap = if (unfiltered) false else plan.bassSwap,
        filterSweep = if (unfiltered) 0.0 else plan.filterSweep,
        incomingCueTime = if (startFromBeginning) 0.0 else plan.incomingCueTime,
        policyReasons = plan.policyReasons + "automix:$why",
    )
}

private val VALID_BPM = 60.0..180.0
private val TEMPO_OCTAVES = listOf(0.5, 1.0, 2.0)
private const val TRUSTED_BEAT_CONFIDENCE = 0.55
private const val MAX_STRETCH_DIFFERENCE = 0.08
private const val BEAT_TIME_TOLERANCE = 0.001
private const val GRID_SPREAD_SECONDS = 0.04
private const val MAX_INTRO_SECONDS = 12.0
private const val MAX_INTRO_FRACTION = 0.1
private const val MIN_OVERLAP_SECONDS = 3.0
private const val MAX_OVERLAP_SECONDS = 60.0
