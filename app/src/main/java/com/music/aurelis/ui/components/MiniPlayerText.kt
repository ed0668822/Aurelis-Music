// SPDX-FileCopyrightText: 2026 Aurelis
// SPDX-License-Identifier: LGPL-3.0-or-later
// Badge geometry adapted with permission from BitChord SongText; preserve NOTICE.
package com.music.aurelis.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.music.aurelis.ui.graphics.fadingEdges

/** Same 36dp fade used by BitChord for one-line metadata. */
@Composable
internal fun MiniPlayerFadingText(value: String, style: TextStyle, tint: Color, modifier: Modifier = Modifier) {
    var overflowing by remember(value) { mutableStateOf(false) }
    Text(value, style = style, color = tint, maxLines = 1, softWrap = false,
        overflow = TextOverflow.Clip,
        onTextLayout = { overflowing = it.didOverflowWidth },
        modifier = modifier.then(if (overflowing) Modifier.fadingEdges(36.dp, end = { 1f }) else Modifier))
}

@Composable
internal fun MiniPlayerSongTitle(
    title: String, explicit: Boolean?, style: TextStyle, color: Color, badgeColor: Color,
    modifier: Modifier = Modifier,
) {
    Row(modifier, verticalAlignment = Alignment.CenterVertically) {
        if (explicit == true) {
            MiniPlayerExplicitBadge(badgeColor)
            Spacer(Modifier.width(6.dp))
        }
        MiniPlayerFadingText(title, style, color, Modifier.weight(1f, fill = false))
    }
}

/** BitChord's square E cutout: the letter reveals the actual blurred backdrop. */
@Composable
internal fun MiniPlayerExplicitBadge(color: Color) {
    val measurer = rememberTextMeasurer()
    val style = MaterialTheme.typography.labelSmall.copy(
        fontSize = 9.5.sp, fontWeight = FontWeight.W800, letterSpacing = 0.sp,
    )
    val letter = remember(measurer, style) { measurer.measure("E", style) }
    val density = LocalDensity.current
    val capPx = with(density) { style.fontSize.toPx() } * 0.705f
    val side = with(density) { (capPx / 0.52f).toDp() }
    Canvas(Modifier.size(side).graphicsLayer { compositingStrategy = CompositingStrategy.Offscreen }) {
        drawRoundRect(color = color, cornerRadius = CornerRadius(2.dp.toPx()))
        drawText(
            textLayoutResult = letter,
            topLeft = Offset(
                (size.width - letter.size.width) / 2f,
                (size.height + capPx) / 2f - letter.firstBaseline,
            ),
            blendMode = BlendMode.DstOut,
        )
    }
}
