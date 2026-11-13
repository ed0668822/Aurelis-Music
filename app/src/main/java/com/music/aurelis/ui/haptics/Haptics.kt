// Aurelis © 2026. GPL-3.0-or-later
// Aurelis header covers local contributions. Rhythms and motor fallback adapted
// from BitChord b690dc146009d4815825fa4a20815ef2e4ee8898, GPLv3.
// Original BitChord contributors; source, hash and changes recorded in NOTICE.
package com.music.aurelis.ui.haptics

import android.content.Context
import android.os.VibrationEffect
import android.os.VibrationAttributes
import android.os.Vibrator
import android.os.VibratorManager
import android.provider.Settings
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.hapticfeedback.HapticFeedback
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import com.music.aurelis.data.SettingsRepository

enum class Haptic {
    Tick,
    Tap,
    Select,
    ToggleOn,
    ToggleOff,
    SkipNext,
    SkipPrevious,
    Resume,
    Pause,
    Expand,
}

private fun feedbackAllowed(context: Context): Boolean =
    SettingsRepository.hapticFeedback.value &&
        Settings.System.getInt(context.contentResolver, Settings.System.HAPTIC_FEEDBACK_ENABLED, 1) != 0

/** Filters Compose's existing feedback without generating a second vibration. */
@Composable
internal fun rememberAurelisHapticFeedback(): HapticFeedback {
    val platform = LocalHapticFeedback.current
    val context = LocalContext.current.applicationContext
    return remember(platform, context) {
        object : HapticFeedback {
            override fun performHapticFeedback(hapticFeedbackType: HapticFeedbackType) {
                if (feedbackAllowed(context)) platform.performHapticFeedback(hapticFeedbackType)
            }
        }
    }
}

class Haptics(
    private val vibrator: Vibrator?,
    private val feedbackEnabled: () -> Boolean = { true },
) {
    private val touchAttributes = VibrationAttributes.Builder()
        .setUsage(VibrationAttributes.USAGE_TOUCH).build()
    private val compiled = mutableMapOf<Haptic, VibrationEffect>()

    fun play(haptic: Haptic) {
        val v = vibrator ?: return
        if (!feedbackEnabled() || !v.hasVibrator()) return
        val effect = synchronized(compiled) { compiled.getOrPut(haptic) { compile(v, rhythmOf(haptic)) } }
        v.vibrate(effect, touchAttributes)
    }

    private fun compile(vibrator: Vibrator, beats: List<Beat>): VibrationEffect {
        // Check every primitive used by this pattern, including LOW_TICK. API
        // level alone does not imply support, nor does support for another rhythm.
        val primitives = beats.map { it.kind.primitive }.distinct().toIntArray()
        if (vibrator.areAllPrimitivesSupported(*primitives)) {
            val composition = VibrationEffect.startComposition()
            beats.forEach { beat ->
                // The SDK's @IntDef requires visible primitive constants here.
                val delay = beat.gapMs.toInt()
                when (beat.kind) {
                    BeatKind.Tick -> composition.addPrimitive(VibrationEffect.Composition.PRIMITIVE_TICK, beat.scale, delay)
                    BeatKind.LowTick -> composition.addPrimitive(VibrationEffect.Composition.PRIMITIVE_LOW_TICK, beat.scale, delay)
                    BeatKind.Click -> composition.addPrimitive(VibrationEffect.Composition.PRIMITIVE_CLICK, beat.scale, delay)
                }
            }
            return composition.compose()
        }
        val coarse = !vibrator.hasAmplitudeControl()
        val selected = if (coarse && beats.size > 2) listOf(beats.first(), beats.last()) else beats
        val timings = LongArray(selected.size * 2)
        val amplitudes = IntArray(selected.size * 2)
        selected.forEachIndexed { index, beat ->
            timings[index * 2] = if (coarse) beat.gapMs.coerceAtLeast(if (index == 0) 0L else 20L) else beat.gapMs
            timings[index * 2 + 1] = if (coarse) {
                (beat.kind.pulseMs * 2.5f * (0.6f + 0.4f * beat.scale)).toLong().coerceIn(18, 40)
            } else beat.kind.pulseMs
            amplitudes[index * 2 + 1] = (beat.kind.amplitude * beat.scale).toInt().coerceIn(1, 255)
        }
        return if (coarse) VibrationEffect.createWaveform(timings, -1)
            else VibrationEffect.createWaveform(timings, amplitudes, -1)
    }
}

private enum class BeatKind(val primitive: Int, val pulseMs: Long, val amplitude: Int) {
    Tick(VibrationEffect.Composition.PRIMITIVE_TICK, 8, 110),
    LowTick(VibrationEffect.Composition.PRIMITIVE_LOW_TICK, 10, 95),
    Click(VibrationEffect.Composition.PRIMITIVE_CLICK, 14, 210),
}

private class Beat(val kind: BeatKind, val scale: Float, val gapMs: Long)

private fun rhythmOf(haptic: Haptic): List<Beat> = when (haptic) {
    Haptic.Tick -> listOf(Beat(BeatKind.Tick, 0.35f, 0))
    Haptic.Tap -> listOf(Beat(BeatKind.Click, 0.5f, 0))
    Haptic.Select -> listOf(Beat(BeatKind.Tick, 0.4f, 0), Beat(BeatKind.Click, 0.75f, 18))
    Haptic.ToggleOn -> listOf(Beat(BeatKind.Tick, 0.4f, 0), Beat(BeatKind.Click, 0.9f, 14))
    Haptic.ToggleOff -> listOf(Beat(BeatKind.Click, 0.75f, 0), Beat(BeatKind.Tick, 0.3f, 14))
    Haptic.SkipNext -> listOf(Beat(BeatKind.Tick, 0.4f, 0), Beat(BeatKind.Tick, 0.55f, 16), Beat(BeatKind.Click, 0.7f, 16))
    Haptic.SkipPrevious -> listOf(Beat(BeatKind.Click, 0.7f, 0), Beat(BeatKind.Tick, 0.55f, 16), Beat(BeatKind.Tick, 0.4f, 16))
    Haptic.Resume -> listOf(Beat(BeatKind.LowTick, 0.5f, 0), Beat(BeatKind.Click, 0.85f, 22))
    Haptic.Pause -> listOf(Beat(BeatKind.Click, 0.85f, 0), Beat(BeatKind.LowTick, 0.4f, 22))
    Haptic.Expand -> listOf(Beat(BeatKind.Tick, 0.3f, 0), Beat(BeatKind.Tick, 0.45f, 12), Beat(BeatKind.Click, 0.6f, 12))
}

@Composable
fun rememberHaptics(): Haptics {
    val context = LocalContext.current.applicationContext
    return remember(context) {
        val manager = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager
        val vibrator = manager?.defaultVibrator
        Haptics(vibrator) {
            // Read on each user action so changing Android's setting takes effect
            // immediately without retaining a ContentObserver or an Activity.
            feedbackAllowed(context)
        }
    }
}
