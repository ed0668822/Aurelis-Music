// SPDX-FileCopyrightText: 2026 Aurelis
// SPDX-License-Identifier: GPL-3.0-or-later
// F040: PlayerScrubber/ThinSlider geometry adapted from BitChord (GPLv3); see NOTICE.
package com.music.aurelis.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.ProgressBarRangeInfo
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.disabled
import androidx.compose.ui.semantics.progressBarRangeInfo
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.setProgress
import androidx.compose.ui.unit.dp
import com.music.aurelis.R

/** F040: BitChord's thin scrubber, keeping Aurelis's MediaController seek callback. */
@Composable
fun PlayerSlider(
    positionMs: Long,
    durationMs: Long,
    onSeek: (Long) -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    showRemaining: Boolean = false,
    centerLabel: @Composable BoxScope.() -> Unit = {},
) {
    val seekLabel = stringResource(R.string.player_seek)
    val duration = durationMs.coerceAtLeast(0L)
    val canSeek = enabled && duration > 0L
    // Drag fraction is local presentation state; do not seek until the finger is released.
    var dragging by remember { mutableStateOf(false) }
    var draggedFraction by remember { mutableFloatStateOf(0f) }
    val position = positionMs.coerceIn(0L, duration)
    val fraction = if (duration > 0L) (position.toDouble() / duration).toFloat().coerceIn(0f, 1f) else 0f
    val shown = if (dragging) draggedFraction else fraction
    val shownTime = fractionToPosition(shown, duration)
    val ink = Color.White.copy(alpha = 0.55f)

    Column(modifier.fillMaxWidth()) {
        ThinSlider(
            value = shown,
            onValueChange = { value ->
                if (canSeek) {
                    draggedFraction = safeSeekFraction(value)
                    dragging = true
                }
            },
            onValueChangeFinished = {
                if (dragging && canSeek) onSeek(fractionToPosition(draggedFraction, duration))
                dragging = false
            },
            enabled = canSeek,
            modifier = Modifier.testTag("player-seek-track").semantics {
                contentDescription = seekLabel
                if (!canSeek) disabled()
                progressBarRangeInfo = ProgressBarRangeInfo(shown, 0f..1f)
                if (canSeek) setProgress { value ->
                    onSeek(fractionToPosition(value, duration))
                    true
                }
            },
        )
        // BitChord lifts the time labels 9dp into the bar's touch-target space.
        Box(Modifier.fillMaxWidth().offset(y = (-9).dp)) {
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(formatPlayerTime(shownTime), style = MaterialTheme.typography.labelMedium, color = ink)
                Text(
                    if (showRemaining) "-" + formatPlayerTime((duration - shownTime).coerceAtLeast(0L))
                    else formatPlayerTime(duration),
                    style = MaterialTheme.typography.labelMedium,
                    color = ink,
                )
            }
            Box(Modifier.matchParentSize()) { centerLabel() }
        }
    }
}

/** The range is always finite even when fed an unavailable playback position. */
internal fun safeSeekFraction(value: Float): Float = if (value.isFinite()) value.coerceIn(0f, 1f) else 0f

internal fun fractionToPosition(value: Float, durationMs: Long): Long =
    (safeSeekFraction(value).toDouble() * durationMs.coerceAtLeast(0L).toDouble())
        .toLong().coerceIn(0L, durationMs.coerceAtLeast(0L))

private fun formatPlayerTime(ms: Long): String {
    val seconds = ms.coerceAtLeast(0L) / 1_000L
    return "${seconds / 60}:${(seconds % 60).toString().padStart(2, '0')}"
}
