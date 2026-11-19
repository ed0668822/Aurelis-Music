// SPDX-FileCopyrightText: 2026 Aurelis
// SPDX-License-Identifier: LGPL-3.0-or-later
package com.music.aurelis.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.snap
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.paneTitle
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntRect
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.window.Popup
import androidx.compose.ui.window.PopupPositionProvider
import androidx.compose.ui.window.PopupProperties
import com.music.aurelis.ui.theme.LocalReduceMotion

/** Window coordinates, measured popup size and logical trailing edge, including RTL. */
internal fun heldMenuPosition(
    anchor: IntRect?, window: IntSize, menu: IntSize, direction: LayoutDirection,
    margin: Int, gap: Int,
): IntOffset {
    val insetX = margin.coerceIn(0, ((window.width - menu.width) / 2).coerceAtLeast(0))
    val insetY = margin.coerceIn(0, ((window.height - menu.height) / 2).coerceAtLeast(0))
    val maxX = (window.width - menu.width - insetX).coerceAtLeast(insetX)
    val maxY = (window.height - menu.height - insetY).coerceAtLeast(insetY)
    val visibleAnchor = anchor?.takeIf {
        it.width > 0 && it.height > 0 && it.right > 0 && it.bottom > 0 &&
            it.left < window.width && it.top < window.height
    }
    if (visibleAnchor == null) return IntOffset(
        ((window.width - menu.width) / 2).coerceIn(insetX, maxX),
        ((window.height - menu.height) / 2).coerceIn(insetY, maxY),
    )
    val x = if (direction == LayoutDirection.Ltr) visibleAnchor.right - menu.width else visibleAnchor.left
    val below = visibleAnchor.bottom + gap
    val above = visibleAnchor.top - gap - menu.height
    val y = when {
        below <= maxY -> below
        above >= insetY -> above
        window.height - visibleAnchor.bottom >= visibleAnchor.top -> below
        else -> above
    }
    return IntOffset(x.coerceIn(insetX, maxX), y.coerceIn(insetY, maxY))
}

/**
 * The popover grows from the selected row, not the centre of the screen.
 * A button-opened menu has no anchor, so it grows from its own centre.
 */
internal fun heldMenuTransformOrigin(anchor: IntRect?, panel: IntRect): TransformOrigin {
    if (anchor == null || panel.width <= 0 || panel.height <= 0) return TransformOrigin.Center
    val centerX = (anchor.left + anchor.right) / 2f
    val centerY = (anchor.top + anchor.bottom) / 2f
    return TransformOrigin(
        ((centerX - panel.left) / panel.width).coerceIn(0f, 1f),
        ((centerY - panel.top) / panel.height).coerceIn(0f, 1f),
    )
}

/** Focusable bounded action panel. Back and outside taps dismiss the real owner. */
@Composable
fun IosMenu(onDismiss: () -> Unit, compactSongStyle: Boolean = false,
    content: @Composable ColumnScope.() -> Unit) {
    val menuState = LocalHeldMenuState.current
    // Consume only the opener of this menu; later button-opened menus cannot reuse it.
    val anchor = remember(menuState) { menuState?.takeAnchor() }
    DisposableEffect(menuState, anchor) {
        menuState?.restoreAnchor = null
        onDispose {
            // A lazy row may have disappeared while its menu was open.
            menuState?.restoreAnchor = anchor?.takeIf { it.attached }
        }
    }
    val bounds = anchor?.bounds
    val density = LocalDensity.current
    val margin = with(density) { 16.dp.roundToPx() }
    val gap = with(density) { 8.dp.roundToPx() }
    val positionProvider = remember(bounds, margin, gap) {
        object : PopupPositionProvider {
            override fun calculatePosition(anchorBounds: IntRect, windowSize: IntSize,
                layoutDirection: LayoutDirection, popupContentSize: IntSize): IntOffset =
                heldMenuPosition(bounds, windowSize, popupContentSize, layoutDirection, margin, gap)
        }
    }
    val reduced = LocalReduceMotion.current
    val progress = remember { Animatable(if (reduced) 1f else 0f) }
    LaunchedEffect(reduced) {
        if (reduced) progress.snapTo(1f)
        else progress.animateTo(1f, tween(180, easing = FastOutSlowInEasing))
    }
    val configuration = LocalConfiguration.current
    // The BitChord song menu is a 256dp slab with 30dp corners.
    // Other Aurelis menus keep their original dimensions.
    val width = (configuration.screenWidthDp - 32).coerceIn(1, if (compactSongStyle) 256 else 320).dp
    val height = (configuration.screenHeightDp - 64).coerceIn(1, 560).dp
    val shape = RoundedCornerShape(if (compactSongStyle) 30.dp else 20.dp)
    val panelBounds = remember(bounds, width, height, density) {
        // The exact window bounds are resolved by the PopupPositionProvider.
        // Position remains independently tested; this value is used only as a
        // stable point of origin for its visual expansion.
        val screen = androidx.compose.ui.unit.IntSize(
            with(density) { configuration.screenWidthDp.dp.roundToPx() },
            with(density) { configuration.screenHeightDp.dp.roundToPx() },
        )
        val panelSize = androidx.compose.ui.unit.IntSize(
            with(density) { width.roundToPx() },
            with(density) { height.roundToPx() },
        )
        val at = heldMenuPosition(bounds, screen, panelSize, LayoutDirection.Ltr, margin, gap)
        IntRect(at.x, at.y, at.x + panelSize.width, at.y + panelSize.height)
    }
    val origin = heldMenuTransformOrigin(bounds, panelBounds)
    val dark = MaterialTheme.colorScheme.background.luminance() < 0.5f
    val fallbackFill = if (dark) Color(0xFF232326) else Color(0xFFF4F4F6)
    Popup(popupPositionProvider = positionProvider, onDismissRequest = onDismiss,
        properties = PopupProperties(focusable = true, dismissOnBackPress = true,
            dismissOnClickOutside = true, usePlatformDefaultWidth = false)) {
        Surface(
            modifier = Modifier
                .width(width)
                .heightIn(max = height)
                .semantics { paneTitle = "Acciones" }
                .graphicsLayer {
                    alpha = progress.value
                    transformOrigin = origin
                    // F054: a natural iOS sheet growth without shrinking its
                    // 48dp touch targets throughout the entire open animation.
                    val scale = if (reduced) 1f else 0.92f + 0.08f * progress.value
                    scaleX = scale
                    scaleY = scale
                }
                .shadow(if (reduced) 0.dp else 20.dp, shape, clip = false)
                .liquidGlass(shape, fallbackColor = fallbackFill),
            shape = shape,
            color = Color.Transparent,
        ) {
            Column(Modifier.verticalScroll(rememberScrollState()).padding(12.dp), content = content)
        }
    }
}
