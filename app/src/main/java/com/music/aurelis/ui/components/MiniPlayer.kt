// SPDX-FileCopyrightText: 2026 Aurelis
// SPDX-License-Identifier: LGPL-3.0-or-later
package com.music.aurelis.ui.components

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.ProgressBarRangeInfo
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.progressBarRangeInfo
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.music.aurelis.R
import com.music.aurelis.data.SettingsRepository
import com.music.aurelis.ui.haptics.Haptic
import com.music.aurelis.ui.haptics.rememberHaptics
import com.music.aurelis.ui.player.MiniPlayerPull
import com.music.aurelis.ui.player.PlayerDock
import com.music.aurelis.ui.player.playerDockArtwork
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.materials.ExperimentalHazeMaterialsApi
import dev.chrisbanes.haze.materials.HazeMaterials

/**
 * RV030: same 56dp capsule and 40dp controls as BitChord's frosted mini player.
 * F032 keeps input-quality details on an accessible long-press, with no extra
 * visible row or transport button. The actual playback callbacks stay local.
 */
@OptIn(ExperimentalFoundationApi::class, ExperimentalHazeMaterialsApi::class)
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
    isExplicit: Boolean? = null,
    hazeState: HazeState? = null,
) {
    val openLabel = stringResource(R.string.player_open)
    val playLabel = stringResource(if (isPlaying) R.string.player_pause else R.string.player_play)
    val haptics = rememberHaptics()
    val shape = remember { RoundedCornerShape(percent = 50) }
    val ink = MaterialTheme.colorScheme.onBackground
    val surfaceColor = MaterialTheme.colorScheme.surface
    val reduceDynamicBlur by SettingsRepository.reduceDynamicBlur.collectAsStateWithLifecycle()
    // Match the frosted tab bar, using its existing backdrop instead of a second source.
    val sharedHaze = hazeState ?: LocalAppHazeState.current ?: remember { HazeState() }
    val frost = if (reduceDynamicBlur) Modifier.background(surfaceColor)
                else Modifier.optimizedHazeEffect(sharedHaze, HazeMaterials.thin(surfaceColor))
    val qualityAvailable = onQualityClick != null && !qualityLabel.isNullOrBlank()
    val touch = if (qualityAvailable) Modifier.combinedClickable(
        onClick = onClick,
        onLongClick = { onQualityClick?.invoke() },
        onLongClickLabel = stringResource(R.string.mini_quality_action),
    ) else Modifier.clickable(onClick = onClick)

    Box(
        modifier.fillMaxWidth().testTag("mini-player")
            .miniPlayerGestures(pull, enabled, canNext, canPrevious,
                { haptics.play(Haptic.SkipNext); onNext() },
                { haptics.play(Haptic.SkipPrevious); onPrevious() }, onClick)
            .semantics {
                contentDescription = openLabel
                stateDescription = qualityLabel.orEmpty()
                progressBarRangeInfo = ProgressBarRangeInfo(
                    progress.takeIf { it.isFinite() }?.coerceIn(0f, 1f) ?: 0f, 0f..1f)
            }
            .clip(shape)
            .then(frost)
            .border(0.5.dp, Color.White.copy(alpha = 0.10f), shape)
            .then(touch),
    ) {
        Row(
            Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            AnimatedArtwork(
                songKey = songId, artworkUri = artworkUri, size = 40.dp, corner = 8.dp,
                miniPlayerStyle = true,
                modifier = Modifier.playerDockArtwork(dock, songId, mini = true, corner = 8.dp),
            )
            Spacer(Modifier.width(10.dp))
            Column(Modifier.weight(1f)) {
                MiniPlayerSongTitle(
                    title, isExplicit, MaterialTheme.typography.titleMedium,
                    ink, MaterialTheme.colorScheme.onSurfaceVariant,
                )
                MiniPlayerFadingText(
                    artist, MaterialTheme.typography.bodyMedium,
                    MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            if (isLoading) {
                // BitChord replaces the play action while the track is buffering.
                Box(Modifier.size(40.dp), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(Modifier.size(22.dp), color = ink, strokeWidth = 2.dp)
                }
            } else {
                IconButton(
                    onClick = {
                        haptics.play(if (isPlaying) Haptic.Pause else Haptic.Resume)
                        onPlayPauseClick()
                    },
                    enabled = enabled,
                    modifier = Modifier.size(40.dp).semantics { contentDescription = playLabel },
                ) {
                    Icon(
                        if (isPlaying) Icons.Rounded.Pause else Icons.Rounded.PlayArrow,
                        contentDescription = null, tint = ink, modifier = Modifier.size(32.dp),
                    )
                }
            }
            Spacer(Modifier.width(8.dp))
            IconButton(
                onClick = { haptics.play(Haptic.SkipNext); onNext() },
                enabled = enabled && canNext, modifier = Modifier.size(40.dp),
            ) {
                Icon(
                    painterResource(R.drawable.ic_player_next),
                    stringResource(R.string.player_next),
                    tint = ink.copy(alpha = if (enabled && canNext) 1f else 0.3f),
                    modifier = Modifier.size(25.dp).graphicsLayer { scaleY = 0.85f },
                )
            }
        }
    }
}
