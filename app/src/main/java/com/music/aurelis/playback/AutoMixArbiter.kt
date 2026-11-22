// SPDX-FileCopyrightText: 2026 Aurelis
// SPDX-License-Identifier: LGPL-3.0-or-later

package com.music.aurelis.playback

import com.music.aurelis.data.settings.AutoMixEffect
import com.music.aurelis.data.settings.AutoMixOptions
import com.music.aurelis.playback.smart.MusicKind
import com.music.aurelis.playback.smart.TrackAnalysis
import com.music.aurelis.playback.smart.TransitionPlan
import com.music.aurelis.playback.smart.TransitionStyle
import com.music.aurelis.playback.smart.audibleStartOf
import com.music.aurelis.playback.smart.musicKindOf

/**
 * The last word on a blend-engine plan, once the planner, the duration adapter
 * and the beat aligner have each had theirs.
 *
 * It reads what kind of music the pair is (see [musicKindOf]) and withdraws
 * the DJ gestures from pairs they do not suit: a beat-matched blend or filter
 * ride between two calm tracks, or between a driven one and a calm one, only
 * stretches and filters music that has no beat to match. Those pairs get the
 * plain equal-power fade at the same timing instead. Everything else is left
 * as the earlier stages decided, with the pair's kinds noted in the plan's
 * policy reasons so the log says why.
 *
 * Explicit effect presets are never touched: they are the listener's choice.
 * The loop mix is decided before any of this and does not pass through here.
 */
internal fun arbitrateAutoMix(
    plan: TransitionPlan,
    options: AutoMixOptions,
    outgoing: TrackAnalysis,
    incoming: TrackAnalysis,
): TransitionPlan {
    if (plan.blocked || options.effect != AutoMixEffect.AUTO) return plan
    val style = plan.transitionStyle
    if (style != TransitionStyle.DJ_BLEND && style != TransitionStyle.DJ_FILTER) return plan

    val out = musicKindOf(outgoing)
    val into = musicKindOf(incoming)
    val tag = "automix:kind=${out.name.lowercase()}>${into.name.lowercase()}"

    val unsuited = (out == MusicKind.CALM && into == MusicKind.CALM) ||
        (out == MusicKind.DANCE && into == MusicKind.CALM) ||
        (out == MusicKind.CALM && into == MusicKind.DANCE)
    if (!unsuited) return plan.copy(policyReasons = plan.policyReasons + tag)

    val cue = audibleStartOf(incoming)
    return plan.copy(
        transitionStyle = TransitionStyle.EQUAL_POWER,
        incomingPlaybackRate = 1.0,
        outgoingPlaybackRate = 1.0,
        phaseLock = false,
        bassSwap = false,
        filterSweep = 0.0,
        incomingCueTime = cue,
        incomingHandoffTime = cue,
        policyReasons = plan.policyReasons + tag + "automix:calmPair",
    )
}
