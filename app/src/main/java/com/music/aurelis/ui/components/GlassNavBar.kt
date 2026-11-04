// Aurelis © 2026. GPL-3.0-or-later
package com.music.aurelis.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.SkipPrevious
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.music.aurelis.data.model.LocalSong
import com.music.aurelis.ui.haptics.Haptic
import com.music.aurelis.ui.haptics.rememberHaptics
import com.music.aurelis.ui.player.PlayerDock

val NAV_BAR_GUTTER = 16.dp
val NAV_BAR_LIFT = 8.dp
val PILL_INSET = 12.dp
val TAB_VERTICAL_PADDING = 8.dp
val INLINE_SPACING = 12.dp
val INLINE_GUTTER = 28.dp
val INLINE_LIFT = 4.dp
val TAB_ICON_SIZE = 24.dp

@Composable
fun GlassNavBar(
    tabs: List<BottomTab>,
    selectedIndex: Int,
    onTabSelected: (Int) -> Unit,
    song: LocalSong?,
    isPlaying: Boolean,
    isLoading: Boolean,
    onPlayPause: () -> Unit,
    onNext: () -> Unit,
    onPrevious: () -> Unit,
    onExpand: () -> Unit,
    modifier: Modifier = Modifier,
    controlsLocked: Boolean = false,
    onBlockedControl: () -> Unit = {},
    dock: PlayerDock? = null,
) {
    val contentColor = glassContentColor()
    val selectedColor = contentColor
    val unselectedColor = contentColor.copy(alpha = 0.65f)
    val haptics = rememberHaptics()

    val currentOnTabSelected by rememberUpdatedState(onTabSelected)

    Column(
        modifier = modifier
            .padding(horizontal = NAV_BAR_GUTTER)
            .padding(bottom = NAV_BAR_LIFT),
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp)
                .clip(CircleShape)
                .lightweightLiquidGlass(
                    shape = RoundedCornerShape(percent = 50),
                    fallbackColor = MaterialTheme.colorScheme.surface,
                )
                .border(
                    width = 0.5.dp,
                    color = Color.White.copy(alpha = 0.10f),
                    shape = RoundedCornerShape(percent = 50),
                ),
        ) {
            Row(
                modifier = Modifier
                    .padding(horizontal = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                tabs.forEachIndexed { index, tab ->
                    val isSelected = index == selectedIndex
                    val tint = if (isSelected) selectedColor else unselectedColor
                    val onClick = {
                        if (!isSelected) haptics.play(Haptic.Select)
                        currentOnTabSelected(index)
                    }
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .clickable(onClick = onClick)
                            .padding(vertical = 4.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) {
                        Icon(
                            imageVector = tab.icon,
                            contentDescription = tab.label,
                            tint = tint,
                            modifier = Modifier.size(TAB_ICON_SIZE),
                        )
                        Text(
                            text = tab.label,
                            style = MaterialTheme.typography.labelMedium,
                            color = tint,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                    if (index < tabs.lastIndex) Spacer(modifier = Modifier.width(4.dp))
                }
            }
        }

        song?.let { current ->
            GlassNowPlaying(
                song = current,
                isPlaying = isPlaying,
                isLoading = isLoading,
                contentColor = contentColor,
                onPlayPause = onPlayPause,
                onNext = onNext,
                onPrevious = onPrevious,
                onExpand = onExpand,
                controlsLocked = controlsLocked,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp),
            )
        }
    }
}

@Composable
private fun GlassNowPlaying(
    song: LocalSong,
    isPlaying: Boolean,
    isLoading: Boolean,
    contentColor: Color,
    onPlayPause: () -> Unit,
    onNext: () -> Unit,
    onPrevious: () -> Unit,
    onExpand: () -> Unit,
    controlsLocked: Boolean,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        IconButton(onClick = onPrevious, enabled = !controlsLocked) {
            Icon(imageVector = Icons.Default.SkipPrevious, contentDescription = null, tint = contentColor)
        }
        IconButton(onClick = onPlayPause, enabled = !controlsLocked) {
            if (isLoading) {
                CircularProgressIndicator(modifier = Modifier.size(20.dp))
            } else {
                Icon(
                    imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                    contentDescription = null,
                    tint = contentColor,
                )
            }
        }
        IconButton(onClick = onNext, enabled = !controlsLocked) {
            Icon(imageVector = Icons.Default.SkipNext, contentDescription = null, tint = contentColor)
        }
        Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp)) {
            Text(
                text = song.title,
                style = MaterialTheme.typography.titleMedium,
                color = contentColor,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = song.artist,
                style = MaterialTheme.typography.bodyMedium,
                color = contentColor.copy(alpha = 0.7f),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
        IconButton(onClick = onExpand) {
            Icon(imageVector = Icons.Default.ExpandLess, contentDescription = null, tint = contentColor)
        }
    }
}
