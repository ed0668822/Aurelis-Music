// SPDX-FileCopyrightText: 2026 Aurelis
// SPDX-License-Identifier: LGPL-3.0-or-later

package com.music.aurelis.playback

import com.music.aurelis.data.settings.AutoMixEffect
import com.music.aurelis.data.settings.AutoMixOptions
import com.music.aurelis.playback.smart.FILTER_SWEEP
import com.music.aurelis.playback.smart.FilterTransitionVariant
import com.music.aurelis.playback.smart.MAX_BPM
import com.music.aurelis.playback.smart.MIN_BEATMATCH_CONFIDENCE
import com.music.aurelis.playback.smart.MIN_BPM
import com.music.aurelis.playback.smart.TrackAnalysis
import com.music.aurelis.playback.smart.TransitionPlan
import com.music.aurelis.playback.smart.TransitionStyle
import com.music.aurelis.playback.smart.audibleStartOf
import com.music.aurelis.playback.smart.incomingRateBetween
import com.music.aurelis.playback.smart.stretchReachable
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
 *
 * Tempo limits, the stretch window and the beat-confidence floor are the
 * planner's own ([MIN_BPM], [MAX_BPM], [stretchReachable],
 * [MIN_BEATMATCH_CONFIDENCE]), so this stage never refuses a pair the planner
 * already judged beat-matchable. The result then goes through
 * [arbitrateAutoMix], which withdraws DJ gestures from pairs of music they do
 * not suit.
 */
internal fun alignAutoMixBeats(
    original: TransitionPlan,
    options: AutoMixOptions,
    outgoing: TrackAnalysis,
    incoming: TrackAnalysis,
    outgoingLength: Double,
    incomingLength: Double,
    playbackTime: Double,
): TransitionPlan = arbitrateAutoMix(
    plan = alignBeats(original, options, outgoing, incoming, outgoingLength, incomingLength, playbackTime),
    options = options,
    outgoing = outgoing,
    incoming = incoming,
)

private fun alignBeats(
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

    // A measured 0.25+ confidence can guide a bass-only handoff,
    // but never authorize a speed change or claim phase lock.
    val outGrid = outgoing.tailBeats
    val inGrid = incoming.headBeats
    val outBpm = outGrid?.bpm ?: outgoing.bpm
    val inBpm = inGrid?.bpm ?: incoming.bpm
    val outConfidence = outGrid?.confidence ?: outgoing.beatConfidence
    val inConfidence = inGrid?.confidence ?: incoming.beatConfidence
    val assistedCandidate = safe.effect == AutoMixEffect.AUTO &&
        outgoing.isUsable && incoming.isUsable &&
        outBpm.isFinite() && inBpm.isFinite() &&
        outBpm in VALID_BPM && inBpm in VALID_BPM &&
        outConfidence.isFinite() && inConfidence.isFinite() &&
        outConfidence >= ASSISTED_BEAT_CONFIDENCE &&
        inConfidence >= ASSISTED_BEAT_CONFIDENCE &&
        original.policyReasons.none { it == "unmixable-pair" }

    fun fallback(why: String, startFromBeginning: Boolean = false): TransitionPlan =
        noBeatmatch(
            plan = original,
            why = why,
            incoming = incoming,
            startFromBeginning = startFromBeginning,
            assisted = assistedCandidate && why in ASSISTED_FALLBACK_REASONS,
        )

    if (safe.effect == AutoMixEffect.CROSSFADE) {
        return fallback("crossfadePreset", startFromBeginning = true)
    }
    if (safe.effectiveSpeed != 1f) {
        return fallback("effectTempo")
    }
    if (!incomingLength.isFinite() || incomingLength < 20.0 ||
        !outgoingLength.isFinite() || outgoingLength < 20.0
    ) {
        return fallback("analysisUnavailable")
    }
    if (!outgoing.isUsable || !incoming.isUsable) {
        return fallback("analysisUnavailable")
    }

    val tail = outGrid
    val head = inGrid
    if (tail == null || head == null ||
        tail.beats.isEmpty() || head.beats.isEmpty()
    ) {
        return fallback("analysisUnavailable")
    }
    if (!tail.confidence.isFinite() || !head.confidence.isFinite() ||
        tail.confidence < TRUSTED_BEAT_CONFIDENCE ||
        head.confidence < TRUSTED_BEAT_CONFIDENCE ||
        !tail.bpm.isFinite() || !head.bpm.isFinite() ||
        tail.bpm !in VALID_BPM || head.bpm !in VALID_BPM
    ) {
        return fallback("unreliableBeats")
    }
    if (original.policyReasons.any { it == "unmixable-pair" }) {
        return fallback("incompatibleTempo")
    }

    // The planner's own stretch window (incoming slowed at most 10%, sped up
    // at most 25%), with the incoming tempo folded into the outgoing octave.
    if (!stretchReachable(tail.bpm, head.bpm)) {
        return fallback("incompatibleTempo")
    }
    val best = (incomingRateBetween(tail.bpm, head.bpm) * 10000).roundToInt() / 10000.0
    if (!best.isFinite()) return fallback("incompatibleTempo")

    // A late first musical beat often means an intro that should play
    // uninterrupted. Silence before the first beat is excluded by audibleStart.
    val audibleStart = (incoming.audibleStartTime ?: incoming.pickupTime ?: 0.0)
        .takeIf { it.isFinite() && it >= 0.0 } ?: 0.0
    val beatTolerance = if (safe.hasEffects) EFFECT_BEAT_TIME_TOLERANCE else BEAT_TIME_TOLERANCE
    val firstBeat = head.beats.asSequence()
        .filter { it.isFinite() && it >= audibleStart - beatTolerance }
        .minOrNull()
        ?: return fallback("alignmentUnverified")
    if (firstBeat > min(MAX_INTRO_SECONDS, incomingLength * MAX_INTRO_FRACTION)) {
        return fallback("longIntro")
    }

    // Align at or *after* the planned start so snapping does not silently
    // extend a user-selected manual overlap past its 60-second upper bound.
    // The fade still ends at the source planner's musical mix-out anchor.
    val end = original.transitionEnd
    val target = original.transitionStart
    if (!end.isFinite() || !target.isFinite() || end > outgoingLength ||
        end <= target || playbackTime > end
    ) {
        return fallback("alignmentUnverified")
    }
    val outPeriod = 60.0 / tail.bpm
    val alignedStart = tail.beats.asSequence()
        .filter {
            it.isFinite() && it >= target - beatTolerance &&
                it <= end - MIN_OVERLAP_SECONDS &&
                it <= target + outPeriod + beatTolerance
        }
        .minOrNull()
        ?: return fallback("alignmentUnverified")
    val overlap = end - alignedStart
    if (overlap < MIN_OVERLAP_SECONDS || overlap > MAX_OVERLAP_SECONDS + beatTolerance) {
        return fallback("alignmentUnverified")
    }

    // The incoming cue is fixed *before* prepare(), avoiding audible seeks.
    // Only nearby measured beats can replace the plan's existing cue point;
    // no beat evidence means the current safe crossfade is retained.
    val desiredCue = original.incomingCueTime
    val maxCue = incomingLength - 2.0 * overlap
    if (!desiredCue.isFinite() || maxCue < 0.0) {
        return fallback("alignmentUnverified")
    }
    val inPeriod = 60.0 / head.bpm
    val alignedCue = head.beats.asSequence()
        .filter {
            it.isFinite() && it >= audibleStart - beatTolerance &&
                it >= 0.0 && it <= maxCue &&
                abs(it - desiredCue) <= inPeriod + beatTolerance
        }
        .minByOrNull { abs(it - desiredCue) }
        ?: return fallback("alignmentUnverified")

    // If a side's beat model cannot explain three consecutive beat intervals
    // consistently, its timestamps cannot safely authorize tempo stretching.
    if (!gridIsStable(tail.beats, alignedStart, outPeriod) ||
        !gridIsStable(head.beats, alignedCue, inPeriod)
    ) {
        return fallback("alignmentUnverified")
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
        .sorted()
        .take(4)
        .toList()
    if (nearby.size < 4) return false
    val errors = nearby.zipWithNext { a, b -> abs((b - a) - period) }
    return errors.maxOrNull()?.let { it <= GRID_SPREAD_SECONDS } == true
}

/**
 * The beat alignment could not be verified, so no stretch and no phase claim.
 *
 * Only the listener's explicit presets force a plain fade. Otherwise the
 * planner's gesture is kept as far as it does not lean on the unverified
 * grid: a filter ride (which needs none) stays as planned, and a beat-matched
 * blend becomes a filter ride rather than a flat crossfade, so a pair the
 * planner worked out vocal handling for does not lose it here.
 */
private fun noBeatmatch(
    plan: TransitionPlan,
    why: String,
    incoming: TrackAnalysis,
    startFromBeginning: Boolean = false,
    assisted: Boolean = false,
): TransitionPlan {
    val keepsPlannerGesture = why !in FORCED_CROSSFADE_REASONS &&
        (plan.transitionStyle == TransitionStyle.DJ_BLEND || plan.transitionStyle == TransitionStyle.DJ_FILTER)
    val reasons = plan.policyReasons + "automix:$why" +
        (if (assisted && !keepsPlannerGesture) listOf("automix:assisted") else emptyList())
    val unstretched = plan.copy(
        incomingPlaybackRate = 1.0,
        outgoingPlaybackRate = 1.0,
        phaseLock = false,
        policyReasons = reasons,
    )
    if (!keepsPlannerGesture) {
        return unstretched.copy(
            transitionStyle = TransitionStyle.EQUAL_POWER,
            bassSwap = false,
            filterSweep = 0.0,
            incomingCueTime = if (startFromBeginning) 0.0 else plan.incomingCueTime,
        )
    }
    if (plan.transitionStyle == TransitionStyle.DJ_FILTER) return unstretched
    val cue = audibleStartOf(incoming)
    return unstretched.copy(
        transitionStyle = TransitionStyle.DJ_FILTER,
        filterSweep = FILTER_SWEEP,
        filterVariant = FilterTransitionVariant.SWEEP,
        incomingCueTime = cue,
        incomingHandoffTime = cue,
    )
}

private val FORCED_CROSSFADE_REASONS = setOf("crossfadePreset", "effectTempo")
private val VALID_BPM = MIN_BPM..MAX_BPM
private const val TRUSTED_BEAT_CONFIDENCE = MIN_BEATMATCH_CONFIDENCE
private const val BEAT_TIME_TOLERANCE = 0.02
private const val EFFECT_BEAT_TIME_TOLERANCE = 0.05
private const val ASSISTED_BEAT_CONFIDENCE = 0.25
private val ASSISTED_FALLBACK_REASONS = setOf(
    "unreliableBeats", "incompatibleTempo", "alignmentUnverified", "longIntro",
)
private const val GRID_SPREAD_SECONDS = 0.04
private const val MAX_INTRO_SECONDS = 12.0
private const val MAX_INTRO_FRACTION = 0.1
private const val MIN_OVERLAP_SECONDS = 3.0
private const val MAX_OVERLAP_SECONDS = 60.0
