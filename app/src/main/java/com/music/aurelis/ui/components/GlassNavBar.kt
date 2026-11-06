// Aurelis © 2026. GPL-3.0-or-later
package com.music.aurelis.ui.components

import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.music.aurelis.data.SettingsRepository
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.semantics.Role
import com.music.aurelis.data.model.LocalSong
import com.music.aurelis.ui.haptics.Haptic
import com.music.aurelis.ui.haptics.rememberHaptics
import com.music.aurelis.ui.player.PlayerDock

import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import com.music.aurelis.ui.theme.LocalReduceMotion
import com.music.aurelis.ui.components.backdrop.backdrops.rememberLayerBackdrop
import com.music.aurelis.ui.components.backdrop.backdrops.layerBackdrop
import com.music.aurelis.ui.components.floatingtabbar.GlassPillMotion
import com.music.aurelis.ui.components.floatingtabbar.GlassSelectionPill
import com.music.aurelis.ui.components.floatingtabbar.pillPlacement

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
    canNext: Boolean = true,
    progress: Float = 0f,
    qualityLabel: String? = null,
) {
    val contentColor = glassContentColor()
    val selectedColor = contentColor
    val unselectedColor = contentColor.copy(alpha = 0.65f)
    val haptics = rememberHaptics()

    val currentOnTabSelected by rememberUpdatedState(onTabSelected)
    require(tabs.isNotEmpty() && selectedIndex in tabs.indices)
    val scope = rememberCoroutineScope()
    val motion = remember(scope) { GlassPillMotion(scope, selectedIndex) }
    val reduced = LocalReduceMotion.current
    LaunchedEffect(selectedIndex, reduced) {
        motion.reduceMotion = reduced
        if (reduced) motion.snapTo(selectedIndex) else motion.animateTo(selectedIndex)
    }
    val barGlass = rememberLayerBackdrop()
    val tabRow = rememberLayerBackdrop()
    val reduceBlur by SettingsRepository.reduceDynamicBlur.collectAsStateWithLifecycle()
    val capture = shouldSampleGlass(LocalLiquidGlassEnabled.current, reduceBlur, LocalGlassHardware.current)


    Column(
        modifier = modifier
            .padding(horizontal = NAV_BAR_GUTTER)
            .padding(bottom = NAV_BAR_LIFT),
    ) {
        CompositionLocalProvider(LocalGlassExport provides barGlass) {
            BoxWithConstraints(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp)
                    .liquidGlass(
                        shape = RoundedCornerShape(percent = 50),
                        fallbackColor = MaterialTheme.colorScheme.surface,
                    ),
            ) {
                val density = LocalDensity.current
                val rtl = LocalLayoutDirection.current == LayoutDirection.Rtl
                val tabWidth = (maxWidth - 16.dp - 4.dp * (tabs.size - 1)) / tabs.size
                val widthPx = with(density) { maxWidth.toPx() }
                val tabPx = with(density) { tabWidth.toPx() }
                val gapPx = with(density) { 4.dp.toPx() }
                val insetPx = with(density) { 8.dp.toPx() }
                val centerY = with(density) { 28.dp.toPx() }
                val heightPx = with(density) { 48.dp.toPx() }
                val growPx = with(density) { 8.dp.toPx() }
                SideEffect { motion.stepDp = tabWidth.value + 4f }
                GlassSelectionPill(motion, glassIndicatorColor(), barGlass, tabRow,
                    Modifier.matchParentSize().pillPlacement(
                        center = {
                            val x = insetPx + tabPx / 2f + (tabPx + gapPx) * motion.position
                            Offset(if (rtl) widthPx - x else x, centerY)
                        }, size = { motion.liveSize(Size(tabPx.coerceAtLeast(1f), heightPx),
                            Size(tabPx.coerceAtLeast(1f) + growPx, heightPx + growPx)) }))
                Row(
                    modifier = Modifier
                        .then(if (capture) Modifier.layerBackdrop(tabRow) else Modifier)
                        .selectableGroup()
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
                                .selectable(selected = isSelected, role = Role.Tab, onClick = onClick)
                                .heightIn(min = 48.dp)
                                .padding(vertical = 4.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                        ) {
                            Icon(
                                imageVector = tab.icon,
                                contentDescription = null,
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
        }

        song?.let { current ->
            MiniPlayer(
                title = current.title, artist = current.artist, artworkUri = current.artworkUri,
                isPlaying = isPlaying, isLoading = isLoading,
                onPlayPauseClick = onPlayPause, onNext = onNext,
                onClick = onExpand, enabled = !controlsLocked,
                canNext = canNext, progress = progress, qualityLabel = qualityLabel,
                modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
            )
        }
    }
}
