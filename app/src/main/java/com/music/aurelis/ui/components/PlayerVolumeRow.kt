// Aurelis © 2026. GPL-3.0-or-later
package com.music.aurelis.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.geometry.Offset
import android.database.ContentObserver
import android.media.AudioManager
import android.os.Handler
import android.os.Looper
import android.provider.Settings
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.VolumeDown
import androidx.compose.material.icons.automirrored.rounded.VolumeUp
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.music.aurelis.R
import kotlin.math.roundToInt

/** Real STREAM_MUSIC volume. This does not install another player or audio pipeline. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlayerVolumeRow(modifier: Modifier = Modifier) {
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
        } catch (_: RuntimeException) { available = false }
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
        } catch (_: SecurityException) { /* Slider can still read back its own changes. */ }
        onDispose { if (registered) context.contentResolver.unregisterContentObserver(observer) }
    }
    val label = stringResource(R.string.player_volume)
    Row(modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Icon(Icons.AutoMirrored.Rounded.VolumeDown, null, Modifier.size(20.dp), tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f))
        Spacer(Modifier.width(10.dp))
        Slider(value = if (maximum > 0) level.toFloat() / maximum else 0f,
            onValueChange = { value ->
                if (available && value.isFinite()) {
                    try { audio?.setStreamVolume(AudioManager.STREAM_MUSIC, (value.coerceIn(0f, 1f) * maximum).roundToInt(), 0) }
                    catch (_: RuntimeException) { available = false }
                    refresh()
                }
            }, enabled = available, modifier = Modifier.weight(1f).height(32.dp).semantics { contentDescription = label },
            thumb = { Box(Modifier.size(6.dp)) },
            track = { slider ->
                val ink = MaterialTheme.colorScheme.onSurface
                Canvas(Modifier.fillMaxWidth().height(6.dp)) {
                    drawLine(ink.copy(alpha = 0.2f), Offset(0f, size.height / 2), Offset(size.width, size.height / 2), size.height, StrokeCap.Round)
                    drawLine(ink, Offset(0f, size.height / 2), Offset(size.width * slider.value, size.height / 2), size.height, StrokeCap.Round)
                }
            },
            colors = SliderDefaults.colors(thumbColor = MaterialTheme.colorScheme.onSurface,
                activeTrackColor = MaterialTheme.colorScheme.onSurface,
                inactiveTrackColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.2f)))
        Spacer(Modifier.width(10.dp))
        Icon(Icons.AutoMirrored.Rounded.VolumeUp, null, Modifier.size(20.dp), tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f))
    }
}
