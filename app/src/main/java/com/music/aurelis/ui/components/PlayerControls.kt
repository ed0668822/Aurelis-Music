// Aurelis © 2026. GPL-3.0-or-later
// Adapted from BitChord b690dc14 TransportRow (GPLv3); see NOTICE.
package com.music.aurelis.ui.components

import androidx.annotation.DrawableRes
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.snap
import androidx.compose.animation.core.tween
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import com.music.aurelis.R
import com.music.aurelis.ui.haptics.Haptic
import com.music.aurelis.ui.haptics.rememberHaptics
import com.music.aurelis.ui.player.playerAdditiveChrome
import com.music.aurelis.ui.player.playerControlTint

/** The same controller callbacks in both layouts; disabled skips keep their footprint. */
@Composable
fun PlayerControls(
    isPlaying: Boolean,
    isLoading: Boolean,
    onPlayPause: () -> Unit,
    onNext: () -> Unit,
    onPrevious: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    canNext: Boolean = true,
    canPrevious: Boolean = true,
    reduceAnimation: Boolean = false,
    blendChrome: Boolean = false,
    compact: Boolean = false,
) {
    val haptics = rememberHaptics()
    val playTouch = if (compact) 76.dp else 92.dp
    val playSize = if (compact) 58.dp else 74.dp
    val skipSize = if (compact) 44.dp else 50.dp
    Row(modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceAround,
        verticalAlignment = Alignment.CenterVertically) {
        TransportGlyph(R.drawable.ic_player_previous, stringResource(R.string.player_previous), skipSize, 56.dp,
            enabled && canPrevious, reduceAnimation, blendChrome, squash = 0.85f) {
            haptics.play(Haptic.SkipPrevious); onPrevious()
        }
        TransportGlyph(if (isPlaying) R.drawable.ic_player_pause else R.drawable.ic_player_play,
            stringResource(if (isPlaying) R.string.player_pause else R.string.player_play), playSize, playTouch,
            enabled, reduceAnimation, blendChrome, loading = isLoading) {
            haptics.play(if (isPlaying) Haptic.Pause else Haptic.Resume); onPlayPause()
        }
        TransportGlyph(R.drawable.ic_player_next, stringResource(R.string.player_next), skipSize, 56.dp,
            enabled && canNext, reduceAnimation, blendChrome, squash = 0.85f) {
            haptics.play(Haptic.SkipNext); onNext()
        }
    }
}

@Composable
private fun TransportGlyph(@DrawableRes resource: Int, label: String, size: Dp, target: Dp,
    enabled: Boolean, reduced: Boolean, blend: Boolean, squash: Float = 1f,
    loading: Boolean = false, onClick: () -> Unit) {
    val alpha by animateFloatAsState(if (enabled) 1f else 0.3f,
        animationSpec = if (reduced) snap() else tween(180), label = "transportAvailability")
    val ink = playerControlTint(MaterialTheme.colorScheme.onSurface, true, blend)
    Box(Modifier.size(target).clickable(interactionSource = remember { MutableInteractionSource() },
        indication = null, enabled = enabled, role = Role.Button, onClick = onClick)
        .semantics { contentDescription = label }, contentAlignment = Alignment.Center) {
        if (loading) CircularProgressIndicator(Modifier.size(30.dp), color = ink, strokeWidth = 2.dp)
        else Icon(painterResource(resource), null, tint = ink.copy(alpha = ink.alpha * alpha),
            modifier = Modifier.size(size).graphicsLayer { scaleY = squash }.playerAdditiveChrome(blend))
    }
}
