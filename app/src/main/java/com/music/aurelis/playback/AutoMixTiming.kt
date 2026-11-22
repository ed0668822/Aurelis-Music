// SPDX-FileCopyrightText: 2026 Aurelis
// SPDX-License-Identifier: LGPL-3.0-or-later

package com.music.aurelis.playback

import com.music.aurelis.data.settings.AutoMixOptions
import com.music.aurelis.playback.smart.MAX_BPM
import com.music.aurelis.playback.smart.MIN_BEATMATCH_CONFIDENCE
import com.music.aurelis.playback.smart.MIN_BPM
import com.music.aurelis.playback.smart.TransitionPlan
import com.music.aurelis.playback.smart.TransitionStyle
import kotlin.math.min
import kotlin.math.roundToInt

/**
 * Duration adaptation for experimental local AutoMix.
 *
 * The existing planner still chooses the outgoing mix-out anchor, incoming
 * cue, transition tier, and whether a pair can mix at all. This helper only
 * sizes the overlap within both files' real durations. Loop Mix remains
 * higher priority in CrossfadeController and is not modified here.
 *
 * Reimplemented in Kotlin from Mornye's AutoMix timing concepts; no Dart or
 * Rust code is copied. Keeping the timing isolated means returning to the
 * legacy planner takes no changes to its DSP or the two-deck handoff.
 */
internal fun adjustAutoMixTiming(
    original: TransitionPlan,
    options: AutoMixOptions,
    outgoingLength: Double,
    incomingLength: Double,
    outgoingBpm: Double,
    beatConfidence: Double,
    playbackTime: Double,
): TransitionPlan {
    val outgoingValid = outgoingLength.isFinite() && outgoingLength >= MIN_SONG_SECONDS
    val incomingKnown = incomingLength.isFinite() && incomingLength > 0.0
    if (!outgoingValid || (incomingKnown && incomingLength < MIN_SONG_SECONDS)) {
        return original.copy(
            shouldStart = false,
            markerVisible = false,
            blocked = true,
            reason = "automix-short-track",
        )
    }

    // Unknown incoming runtime cannot be safely capped to half its length.
    // Keep the existing planner's conservative fade until Media3 or the
    // offline analysis supplies the actual duration.
    if (!incomingKnown || original.blocked || original.transitionStyle == TransitionStyle.GAPLESS) {
        return original
    }

    val end = original.transitionEnd.takeIf {
        it.isFinite() && it > 0.0 && it <= outgoingLength
    } ?: outgoingLength
    val maxOverlap = min(
        MAX_FADE_SECONDS,
        min(outgoingLength / 2.0, min(incomingLength / 2.0, end / 2.0)),
    )
    if (maxOverlap < MIN_FADE_SECONDS) {
        return original.copy(
            shouldStart = false,
            markerVisible = false,
            blocked = true,
            reason = "automix-no-safe-overlap",
        )
    }

    val safe = options.normalized()
    val reliableTempo = outgoingBpm.isFinite() &&
        outgoingBpm in MIN_BPM..MAX_BPM &&
        beatConfidence.isFinite() && beatConfidence >= MIN_BEAT_CONFIDENCE
    // In automatic length the planner has already sized a DJ overlap from how
    // well this pair agrees (8 to 32 beats, longer for a calm join, shorter for
    // a clash); keep it rather than replacing it with a fixed eight beats. Only
    // the plain fades the planner did not size from the pair fall to the rule below.
    val plannerGesture = original.transitionStyle == TransitionStyle.DJ_BLEND ||
        original.transitionStyle == TransitionStyle.DJ_FILTER
    val plannerFade = original.fadeSeconds.takeIf {
        plannerGesture && it.isFinite() && it >= MIN_FADE_SECONDS
    }
    val keepPlannerFade = safe.durationSeconds == 0 && plannerFade != null
    val requested = if (safe.durationSeconds != 0) {
        safe.durationSeconds.toDouble()
    } else if (plannerFade != null) {
        plannerFade
    } else if (reliableTempo) {
        // Eight beats, but never so short that a 180 BPM track becomes a cut.
        (8.0 * 60.0 / outgoingBpm).coerceIn(MIN_FADE_SECONDS, MAX_AUTO_SECONDS)
    } else {
        DEFAULT_AUTO_SECONDS
    }

    // Incoming arrangements may be cued several seconds in. Cap the overlap
    // by the available audio *after* that cue, not by the whole file alone.
    val preliminary = requested.coerceIn(MIN_FADE_SECONDS, maxOverlap)
    val tentativeCue = if (
        original.transitionStyle == TransitionStyle.DJ_BLEND &&
        original.incomingHandoffTime.isFinite() && original.incomingHandoffTime > 0.0
    ) {
        (original.incomingHandoffTime -
            preliminary * original.incomingPlaybackRate.coerceAtLeast(0.01)).coerceAtLeast(0.0)
    } else {
        original.incomingCueTime.coerceAtLeast(0.0)
    }
    val maximumWithCue = min(maxOverlap, (incomingLength - tentativeCue) / 2.0)
    if (maximumWithCue < MIN_FADE_SECONDS) {
        return original.copy(
            shouldStart = false,
            markerVisible = false,
            blocked = true,
            reason = "automix-no-incoming-clearance",
        )
    }
    val fade = preliminary.coerceAtMost(maximumWithCue)
    val incomingCue = tentativeCue.coerceAtMost((incomingLength - 2.0 * fade).coerceAtLeast(0.0))
    val start = (end - fade).coerceAtLeast(0.0)
    val actualFade = end - start

    return original.copy(
        transitionStart = start,
        transitionEnd = end,
        fadeSeconds = actualFade,
        shouldStart = playbackTime >= start,
        incomingCueTime = incomingCue,
        handoffDuration = actualFade,
        transitionBeats = if (keepPlannerFade) original.transitionBeats
            else if (reliableTempo && safe.durationSeconds == 0) 8
            else if (reliableTempo) (actualFade * outgoingBpm / 60.0).roundToInt()
            else original.transitionBeats,
        reason = if (playbackTime >= start) "automix-duration-ready" else "before-automix-duration",
    )
}

private const val MIN_SONG_SECONDS = 20.0
private const val MIN_FADE_SECONDS = 3.0
private const val MAX_FADE_SECONDS = 60.0
private const val MAX_AUTO_SECONDS = 8.0
private const val DEFAULT_AUTO_SECONDS = 5.0
private const val MIN_BEAT_CONFIDENCE = MIN_BEATMATCH_CONFIDENCE
