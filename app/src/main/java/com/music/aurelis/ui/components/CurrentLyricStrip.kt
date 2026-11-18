// SPDX-FileCopyrightText: 2026 Aurelis
// SPDX-License-Identifier: LGPL-3.0-or-later
package com.music.aurelis.ui.components

import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.tween
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.music.aurelis.R
import com.music.aurelis.data.lyrics.LyricLine
import com.music.aurelis.playback.LyricsState
import com.music.aurelis.ui.player.adjustedLyricsPosition

/** Returns the final occurrence at or before the playhead; includes instrumental gaps. */
internal fun currentLyricStripIndex(lines: List<LyricLine>, positionMs: Long): Int {
    var low = 0
    var high = lines.size
    while (low < high) {
        val middle = (low + high) ushr 1
        if (lines[middle].timeMs <= positionMs) low = middle + 1 else high = middle
    }
    return low - 1
}

/** F043: compact, time-aligned BitChord-style line above the scrubber. */
@Composable
fun CurrentLyricStrip(
    lyrics: LyricsState,
    positionMs: Long,
    offsetMs: Int,
    syncEnabled: Boolean,
    reduceMotion: Boolean,
    onOpenLyrics: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val loaded = lyrics as? LyricsState.Loaded
    val lines = loaded?.lines.orEmpty()
    val synced = syncEnabled && loaded?.isSynced == true
    val activeIndex = if (synced) currentLyricStripIndex(
        lines, adjustedLyricsPosition(positionMs, offsetMs)
    ) else -1
    val active = lines.getOrNull(activeIndex)
    val text = when {
        lyrics is LyricsState.Loading -> stringResource(R.string.lyrics_ui_loading)
        lyrics is LyricsState.Missing || lyrics is LyricsState.Failed ->
            stringResource(R.string.lyrics_ui_unavailable)
        !synced || lines.isEmpty() -> stringResource(R.string.player_no_lyrics_preview)
        active == null || active.isGap -> stringResource(R.string.lyrics_ui_instrumental)
        else -> active.text
    }
    val description = stringResource(R.string.player_lyrics_preview)
    Box(
        modifier = modifier.fillMaxWidth().height(32.dp).offset(y = 6.dp)
            .testTag("player-current-lyric")
            .clickable(onClick = onOpenLyrics)
            .semantics { contentDescription = description },
        contentAlignment = Alignment.CenterStart,
    ) {
        Crossfade(
            targetState = text,
            animationSpec = tween(durationMillis = if (reduceMotion) 0 else 180),
            label = "currentLyricStrip",
        ) { line ->
            Text(
                text = line,
                style = MaterialTheme.typography.labelMedium,
                color = Color.White.copy(alpha = 0.72f),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                textAlign = TextAlign.Start,
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}
