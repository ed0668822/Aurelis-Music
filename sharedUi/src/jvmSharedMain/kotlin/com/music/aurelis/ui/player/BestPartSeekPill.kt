// SPDX-FileCopyrightText: 2026 Aurelis
// SPDX-License-Identifier: LGPL-3.0-or-later
package com.music.aurelis.ui.player

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.music.aurelis.data.lyrics.LyricLine
import com.music.aurelis.playback.PlaybackPosition
import com.music.aurelis.sharedui.resources.Res
import com.music.aurelis.sharedui.resources.best_part
import org.jetbrains.compose.resources.stringResource
import java.util.Locale

/**
 * Real, offline chorus candidate. Repetition of a full lyric line over at
 * least twelve seconds is evidence; one word, untimed lyrics or a guessed
 * waveform is not. Returns null rather than fabricating a heatmap.
 */
fun bestPartChorusStart(lines: List<LyricLine>, durationMs: Long): Long? {
    if (durationMs < 60_000L) return null
    val usable = lines.mapNotNull { line ->
        val text = line.text.trim().lowercase(Locale.ROOT)
            .replace(Regex("[^\\p{L}\\p{N}\\s]"), "")
            .replace(Regex("\\s+"), " ").trim()
        if (text.length < 8 || line.timeMs < 5_000L ||
            line.timeMs > durationMs - 30_000L
        ) null else line.timeMs to text
    }
    return usable.groupBy { it.second }.values
        .filter { it.size >= 2 }
        .mapNotNull { copies ->
            val earliest = copies.minOf { it.first }
            val latest = copies.maxOf { it.first }
            earliest.takeIf { latest - earliest >= 12_000L }
        }.minOrNull()
}

@Composable
internal fun BestPartSeekPill(
    lines: List<LyricLine>?,
    durationMs: Long,
    position: PlaybackPosition,
    mixing: Boolean,
    onSeek: (Long) -> Unit,
) {
    val target = remember(lines, durationMs) {
        bestPartChorusStart(lines.orEmpty(), durationMs)
    } ?: return
    // A candidate only remains visible while ahead. A seek during AutoMix
    // would interrupt the active mix, so the action is explicitly hidden.
    if (mixing || position.positionMs >= target - 400L) return
    Row(
        modifier = Modifier
            .padding(bottom = 4.dp)
            .clip(CircleShape)
            .background(Color.White.copy(alpha = 0.18f))
            .clickable { onSeek(target) }
            .padding(horizontal = 13.dp, vertical = 6.dp),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            imageVector = Icons.Rounded.AutoAwesome,
            contentDescription = null,
            tint = Color.White,
            modifier = Modifier.size(14.dp),
        )
        Text(
            text = stringResource(Res.string.best_part),
            color = Color.White,
            style = MaterialTheme.typography.labelMedium,
            modifier = Modifier.padding(start = 6.dp),
        )
    }
}
