// Aurelis © 2026. GPL-3.0-or-later
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

    fun play(haptic: Haptic) {
        val v = vibrator ?: return
        if (!feedbackEnabled() || !v.hasVibrator()) return
        val effect = when (haptic) {
            Haptic.Tick -> VibrationEffect.createOneShot(8, 40)
            Haptic.Tap -> VibrationEffect.createOneShot(12, 80)
            Haptic.Select -> VibrationEffect.createOneShot(15, 120)
            Haptic.ToggleOn -> VibrationEffect.createWaveform(longArrayOf(0, 10, 30, 18), intArrayOf(0, 60, 0, 140), -1)
            Haptic.ToggleOff -> VibrationEffect.createWaveform(longArrayOf(0, 18, 30, 10), intArrayOf(0, 140, 0, 60), -1)
            Haptic.SkipNext -> VibrationEffect.createWaveform(longArrayOf(0, 8, 20, 8, 20, 12), intArrayOf(0, 60, 0, 90, 0, 130), -1)
            Haptic.SkipPrevious -> VibrationEffect.createWaveform(longArrayOf(0, 12, 20, 8, 20, 8), intArrayOf(0, 130, 0, 90, 0, 60), -1)
            Haptic.Resume -> VibrationEffect.createWaveform(longArrayOf(0, 25, 15), intArrayOf(0, 80, 160), -1)
            Haptic.Pause -> VibrationEffect.createWaveform(longArrayOf(0, 15, 25), intArrayOf(0, 160, 80), -1)
            Haptic.Expand -> VibrationEffect.createOneShot(20, 100)
        }
        v.vibrate(effect, touchAttributes)
    }
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
