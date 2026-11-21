// SPDX-FileCopyrightText: 2026 Aurelis
// SPDX-License-Identifier: LGPL-3.0-or-later
package com.music.aurelis.ui.replay

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer

/**
 * Only the visible Replay story card receives its reveal. Offscreen pager
 * cards stay measured and ready but never animate a stale presentation.
 */
@Composable
internal fun ReplayYearMotion(
    visible: Boolean,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    val opacity by animateFloatAsState(
        targetValue = if (visible) 1f else 0.55f,
        animationSpec = tween(310, easing = FastOutSlowInEasing),
        label = "replayYearOpacity",
    )
    val vertical by animateFloatAsState(
        targetValue = if (visible) 0f else 0.035f,
        animationSpec = tween(360, easing = FastOutSlowInEasing),
        label = "replayYearPosition",
    )
    Box(modifier.graphicsLayer {
        alpha = opacity
        translationY = size.height * vertical
    }) { content() }
}
