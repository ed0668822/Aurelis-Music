// F044: BitChord PlayerActionRow/PillSegment port. GPL-3.0-or-later.
// Copyright belongs to original BitChord contributors; see NOTICE.
package com.music.aurelis.ui.components

import android.os.SystemClock
import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Speaker
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.*
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.media3.common.Player
import com.music.aurelis.R
import com.music.aurelis.ui.haptics.Haptic
import com.music.aurelis.ui.haptics.rememberHaptics
import com.music.aurelis.ui.icons.AurelisPlayerIcons

/**
 * Keep the real controls in the main player: Aurelis portrait uses a separate
 * queue sheet until F046, unlike BitChord's queue pane. Three 64dp pill segments
 * match BitChord's style, but shrink to fit 320dp portrait viewports.
 */
@Composable
internal fun PlayerPlaybackActions(
    lyricsOpen: Boolean, queueOpen: Boolean, shuffleEnabled: Boolean,
    repeatMode: Int, autoplayEnabled: Boolean, connected: Boolean,
    canSetAutoplay: Boolean, reduceMotion: Boolean,
    onToggleLyrics: () -> Unit, onToggleQueue: () -> Unit,
    onToggleShuffle: () -> Unit, onCycleRepeat: () -> Unit, onToggleAutoplay: () -> Unit,
    onOpenOutput: () -> Unit,
) {
    val on = stringResource(R.string.player_enabled)
    val off = stringResource(R.string.player_disabled)
    val repeatState = stringResource(when (repeatMode) {
        Player.REPEAT_MODE_ALL -> R.string.player_repeat_all
        Player.REPEAT_MODE_ONE -> R.string.player_repeat_one
        else -> R.string.player_repeat_off
    })
    BoxWithConstraints(Modifier.fillMaxWidth()) {
        val width = ((maxWidth - 94.dp) / 3f).coerceIn(38.dp, 64.dp)
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween) {
            PlayerBottomGlyph(AurelisPlayerIcons.LyricsQuote, stringResource(R.string.player_lyrics),
                onToggleLyrics, highlighted = lyricsOpen)
            // BitChord: the three playback modes belong to the queue pane.
            // In the main player the center slot opens local audio output instead.
            Crossfade(targetState = queueOpen,
                animationSpec = tween(if (reduceMotion) 0 else 220),
                label = "player-center-mode") { showQueueModes ->
            if (showQueueModes) Row(Modifier.height(44.dp).clip(CircleShape)
                .background(Color.White.copy(alpha = 0.12f)).testTag("player-mode-pill"),
                verticalAlignment = Alignment.CenterVertically) {
                ModeSegment(AurelisPlayerIcons.Shuffle, stringResource(R.string.player_shuffle),
                    if (shuffleEnabled) on else off, shuffleEnabled, connected, width, reduceMotion,
                    400L, if (shuffleEnabled) Haptic.ToggleOff else Haptic.ToggleOn, onToggleShuffle)
                Divider()
                ModeSegment(if (repeatMode == Player.REPEAT_MODE_ONE) null else AurelisPlayerIcons.Repeat,
                    stringResource(R.string.player_repeat), repeatState,
                    repeatMode != Player.REPEAT_MODE_OFF, connected, width, reduceMotion,
                    0L, when (repeatMode) {
                        Player.REPEAT_MODE_OFF -> Haptic.ToggleOn
                        Player.REPEAT_MODE_ONE -> Haptic.ToggleOff
                        else -> Haptic.Select
                    }, onCycleRepeat, if (repeatMode == Player.REPEAT_MODE_ONE) "1" else null)
                Divider()
                ModeSegment(AurelisPlayerIcons.Infinity, stringResource(R.string.player_autoplay),
                    if (autoplayEnabled) on else off, autoplayEnabled, canSetAutoplay, width,
                    reduceMotion, 700L, if (autoplayEnabled) Haptic.ToggleOff else Haptic.ToggleOn,
                    onToggleAutoplay)
            } else {
                PlayerBottomGlyph(
                    Icons.Rounded.Speaker,
                    stringResource(R.string.audio_output),
                    onOpenOutput,
                )
            }
            }
            PlayerBottomGlyph(AurelisPlayerIcons.Queue, stringResource(R.string.player_queue),
                onToggleQueue, highlighted = queueOpen)
        }
    }
}

@Composable
private fun Divider() {
    Box(Modifier.width(1.dp).fillMaxHeight().background(Color.White.copy(alpha = 0.20f)))
}

@Composable
private fun ModeSegment(
    glyph: ImageVector?, title: String, status: String, active: Boolean,
    enabled: Boolean, width: Dp, reduceMotion: Boolean, cooldown: Long,
    haptic: Haptic, onClick: () -> Unit, mark: String? = null,
) {
    val haptics = rememberHaptics()
    var lastTap by remember { mutableLongStateOf(Long.MIN_VALUE) }
    Box(Modifier.width(width).height(44.dp)
        .background(if (active) Color.White.copy(alpha = 0.14f) else Color.Transparent)
        .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null,
            enabled = enabled, role = Role.Button) {
            val now = SystemClock.uptimeMillis()
            if (lastTap == Long.MIN_VALUE || now - lastTap >= cooldown) {
                lastTap = now
                haptics.play(haptic)
                onClick()
            }
        }
        .semantics {
            contentDescription = title
            stateDescription = status
            selected = active
            if (!enabled) disabled()
        }, contentAlignment = Alignment.Center) {
        val ink = Color.White.copy(alpha = if (!enabled) 0.3f else if (active) 1f else 0.75f)
        Crossfade(glyph to mark, animationSpec = tween(if (reduceMotion) 0 else 200),
            label = "playerModeGlyph") { (vector, label) ->
            if (label != null) Text(label, fontSize = 17.sp, fontWeight = FontWeight.Bold, color = ink)
            else if (vector != null) Icon(vector, null, tint = ink, modifier = Modifier.size(24.dp))
        }
    }
}
