// SPDX-FileCopyrightText: 2026 Aurelis
// SPDX-License-Identifier: LGPL-3.0-or-later
package com.music.aurelis.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.semantics.progressBarRangeInfo
import androidx.compose.ui.semantics.ProgressBarRangeInfo
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.graphics.graphicsLayer
import com.music.aurelis.R
import com.music.aurelis.ui.haptics.Haptic
import com.music.aurelis.ui.haptics.rememberHaptics
import com.music.aurelis.ui.graphics.fadingEdges
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import androidx.compose.ui.layout.ContentScale
import com.music.aurelis.ui.player.PlayerDock
import com.music.aurelis.ui.player.MiniPlayerPull
import com.music.aurelis.ui.player.playerDockArtwork

@Composable
fun MiniPlayer(
    title: String,
    artist: String,
    isPlaying: Boolean,
    onPlayPauseClick: () -> Unit,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    artworkUri: String? = null,
    isLoading: Boolean = false,
    enabled: Boolean = true,
    canNext: Boolean = true,
    onNext: () -> Unit = {},
    progress: Float = 0f,
    qualityLabel: String? = null,
    songId: String = "",
    dock: PlayerDock? = null,
    pull: MiniPlayerPull? = null,
    canPrevious: Boolean = false,
    onPrevious: () -> Unit = {},
    onQualityClick: (() -> Unit)? = null,
) {
    val glass = LocalLiquidGlassEnabled.current
    val openLabel = stringResource(R.string.player_open)
    val playLabel = stringResource(if (isPlaying) R.string.player_pause else R.string.player_play)
    val haptics = rememberHaptics()
    val shape = remember { RoundedCornerShape(percent = 50) }
    val surface = if (glass) Modifier.liquidGlass(shape,
        fallbackColor = MaterialTheme.colorScheme.surfaceContainerHigh) else Modifier
    Surface(modifier = modifier.fillMaxWidth().testTag("mini-player").miniPlayerGestures(pull, enabled, canNext, canPrevious,
        { haptics.play(Haptic.SkipNext); onNext() }, { haptics.play(Haptic.SkipPrevious); onPrevious() }, onClick)
        .semantics {
            contentDescription = openLabel
            stateDescription = qualityLabel.orEmpty()
            progressBarRangeInfo = ProgressBarRangeInfo(progress.takeIf { it.isFinite() }?.coerceIn(0f, 1f) ?: 0f, 0f..1f)
        }.then(surface).border(0.5.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.32f), shape),
        shape = shape, color = if (glass) Color.Transparent else MaterialTheme.colorScheme.surfaceContainerHigh,
        tonalElevation = if (glass) 0.dp else 4.dp, onClick = onClick) {
            Row(Modifier.fillMaxWidth().heightIn(min = 56.dp).padding(horizontal = 12.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically) {
                AnimatedArtwork(songKey = songId, artworkUri = artworkUri, size = 40.dp, corner = 8.dp,
                    modifier = Modifier.playerDockArtwork(dock, songId, mini = true))
                Spacer(Modifier.width(10.dp))
                Column(Modifier.weight(1f)) {
                    MiniPlayerFadingText(title, MaterialTheme.typography.titleMedium, MaterialTheme.colorScheme.onSurface)
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        MiniPlayerFadingText(artist, MaterialTheme.typography.bodyMedium,
                            MaterialTheme.colorScheme.onSurfaceVariant, Modifier.weight(1f))
                        if (!qualityLabel.isNullOrBlank()) {
                            Spacer(Modifier.width(4.dp))
                            SourceQualityBadge(qualityLabel, Modifier.testTag("mini-quality"), onQualityClick)
                        }
                    }
                }
                // The full screen exposes previous; keep the compact bar usable at 320dp.
                IconButton(onClick = { haptics.play(if (isPlaying) Haptic.Pause else Haptic.Resume); onPlayPauseClick() }, enabled = enabled,
                    modifier = Modifier.size(40.dp).semantics { contentDescription = playLabel }) {
                    if (isLoading) CircularProgressIndicator(Modifier.size(22.dp))
                    else Icon(if (isPlaying) Icons.Rounded.Pause else Icons.Rounded.PlayArrow,
                        null, modifier = Modifier.size(32.dp))
                }
                Spacer(Modifier.width(8.dp))
                IconButton(onClick = { haptics.play(Haptic.SkipNext); onNext() }, enabled = enabled && canNext, modifier = Modifier.size(40.dp)) {
                    Icon(painterResource(R.drawable.ic_player_next), stringResource(R.string.player_next),
                        modifier = Modifier.size(25.dp).graphicsLayer { scaleY = 0.85f })
                }
            }
    }
}

/** One-line fade like the reference player, with a stable compact-row layout. */
@Composable
private fun MiniPlayerFadingText(value: String, style: TextStyle, tint: Color, modifier: Modifier = Modifier) {
    var overflow by remember(value) { mutableStateOf(false) }
    Text(
        text = value, style = style, color = tint, maxLines = 1, softWrap = false,
        overflow = TextOverflow.Clip, onTextLayout = { overflow = it.didOverflowWidth },
        modifier = modifier.then(if (overflow) Modifier.fadingEdges(32.dp, end = { 1f }) else Modifier),
    )
}
