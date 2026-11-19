// SPDX-FileCopyrightText: 2026 Aurelis
// SPDX-License-Identifier: LGPL-3.0-or-later
package com.music.aurelis.ui.components

import android.database.ContentObserver
import android.media.AudioManager
import android.os.Handler
import android.os.Looper
import android.provider.Settings
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.VolumeDown
import androidx.compose.material.icons.automirrored.rounded.VolumeUp
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.ProgressBarRangeInfo
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.disabled
import androidx.compose.ui.semantics.progressBarRangeInfo
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.setProgress
import androidx.compose.ui.unit.dp
import com.music.aurelis.R
import kotlin.math.roundToInt

/** F042: BitChord's 6/10dp volume geometry, controlling Android's real music stream. */
@Composable
fun PlayerVolumeRow(modifier: Modifier = Modifier, onOpenOutput: (() -> Unit)? = null) {
    val context = LocalContext.current.applicationContext
    val audio = remember(context) { context.getSystemService(AudioManager::class.java) }
    var maximum by remember { mutableIntStateOf(0) }
    var level by remember { mutableIntStateOf(0) }
    var available by remember { mutableStateOf(false) }

    fun refresh() {
        try {
            maximum = audio?.getStreamMaxVolume(AudioManager.STREAM_MUSIC) ?: 0
            level = audio?.getStreamVolume(AudioManager.STREAM_MUSIC)?.coerceIn(0, maximum) ?: 0
            available = audio != null && !audio.isVolumeFixed && maximum > 0
        } catch (_: RuntimeException) {
            available = false
        }
    }

    fun setLevel(fraction: Float) {
        if (!available || !fraction.isFinite() || maximum <= 0) return
        try {
            audio?.setStreamVolume(
                AudioManager.STREAM_MUSIC,
                (fraction.coerceIn(0f, 1f) * maximum).roundToInt(),
                0,
            )
        } catch (_: RuntimeException) {
            available = false
        }
        refresh()
    }

    DisposableEffect(context, audio) {
        val observer = object : ContentObserver(Handler(Looper.getMainLooper())) {
            override fun onChange(selfChange: Boolean) = refresh()
        }
        var registered = false
        refresh()
        try {
            context.contentResolver.registerContentObserver(Settings.System.CONTENT_URI, true, observer)
            registered = true
        } catch (_: SecurityException) {
            // The slider still reads back changes after user interaction.
        }
        onDispose {
            if (registered) context.contentResolver.unregisterContentObserver(observer)
        }
    }

    val label = stringResource(R.string.player_volume)
    val fraction = if (maximum > 0) level.toFloat() / maximum else 0f
    Row(modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Icon(Icons.AutoMirrored.Rounded.VolumeDown, contentDescription = null,
            modifier = Modifier.size(20.dp), tint = Color.White.copy(alpha = 0.5f))
        Spacer(Modifier.width(10.dp))
        ThinSlider(
            value = fraction,
            onValueChange = ::setLevel,
            onValueChangeFinished = ::refresh,
            enabled = available,
            idleHeight = 6.dp,
            activeHeight = 10.dp,
            modifier = Modifier.weight(1f).semantics {
                contentDescription = label
                progressBarRangeInfo = ProgressBarRangeInfo(fraction, 0f..1f)
                if (available) setProgress { value ->
                    setLevel(value)
                    true
                } else disabled()
            },
        )
        Spacer(Modifier.width(10.dp))
        Icon(Icons.AutoMirrored.Rounded.VolumeUp,
            contentDescription = if (onOpenOutput != null) stringResource(R.string.audio_output) else null,
            modifier = Modifier.size(20.dp).then(if (onOpenOutput != null)
                Modifier.clickable(onClick = onOpenOutput) else Modifier),
            tint = Color.White.copy(alpha = 0.5f))
    }
}
