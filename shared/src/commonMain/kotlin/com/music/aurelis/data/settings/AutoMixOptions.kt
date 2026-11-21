// SPDX-FileCopyrightText: 2026 Aurelis
// SPDX-License-Identifier: LGPL-3.0-or-later

package com.music.aurelis.data.settings

/**
 * User-facing experimental AutoMix effect selection.
 * AUTO retains Aurelis's existing transition behavior with guarded fallbacks.
 */
enum class AutoMixEffect {
    AUTO,
    CROSSFADE,
    MUFFLED,
    ECHO,
    PITCH,
    CUSTOM,
}

/** Volatile playback status for MiniPlayer and diagnostics, not a persisted preference. */
enum class AutoMixStage {
    IDLE,
    PREPARING,
    MIXING,
    CROSSFADING,
}

/** Validated settings for the opt-in Mornye-inspired AutoMix transition effects. */
data class AutoMixOptions(
    /** Zero selects automatic length; positive values are 3–60 seconds. */
    val durationSeconds: Int = 0,
    val effect: AutoMixEffect = AutoMixEffect.AUTO,
    val speed: Float = 1f,
    val pitchSemitones: Float = 0f,
    val echo: Boolean = false,
    val lowPass: Boolean = false,
) {
    fun normalized(): AutoMixOptions = copy(
        durationSeconds = if (durationSeconds <= 0) 0 else durationSeconds.coerceIn(3, 60),
        speed = if (speed.isFinite()) speed.coerceIn(0.8f, 1.2f) else 1f,
        pitchSemitones = if (pitchSemitones.isFinite()) pitchSemitones.coerceIn(-6f, 6f) else 0f,
    )

    val effectiveSpeed: Float get() = if (effect == AutoMixEffect.CUSTOM) speed else 1f
    val effectivePitchSemitones: Float get() =
        if (effect == AutoMixEffect.CUSTOM || effect == AutoMixEffect.PITCH) pitchSemitones else 0f
    val useEcho: Boolean get() =
        effect == AutoMixEffect.ECHO || (effect == AutoMixEffect.CUSTOM && echo)
    val useLowPass: Boolean get() =
        effect == AutoMixEffect.MUFFLED || (effect == AutoMixEffect.CUSTOM && lowPass)
    val hasEffects: Boolean get() =
        effectiveSpeed != 1f || effectivePitchSemitones != 0f || useEcho || useLowPass
}
