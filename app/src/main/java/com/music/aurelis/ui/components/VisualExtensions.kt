// Aurelis © 2026. GPL-3.0-or-later
package com.music.aurelis.ui.components

import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color

fun Modifier.optimizedHazeEffect(
    enabled: Boolean = true,
    backgroundColor: Color = Color.Transparent,
    blurRadius: androidx.compose.ui.unit.Dp = 20.dp
): Modifier = this

fun Modifier.thumbnailBorder(
    shape: androidx.compose.ui.graphics.Shape = androidx.compose.ui.graphics.RectangleShape
): Modifier = this

@androidx.compose.runtime.Composable
fun ExplicitSongTitle(
    title: String,
    isExplicit: Boolean = false,
    modifier: Modifier = Modifier,
    style: androidx.compose.ui.text.TextStyle = androidx.compose.material3.MaterialTheme.typography.bodyLarge,
    color: Color = Color.Unspecified
) {
    androidx.compose.material3.Text(
        text = if (isExplicit) "$title 🅴" else title,
        modifier = modifier,
        style = style,
        color = color
    )
}

@androidx.compose.runtime.Composable
fun ExplicitBadge(
    modifier: Modifier = Modifier
) {
    androidx.compose.material3.Text(
        text = "E",
        modifier = modifier,
        style = androidx.compose.material3.MaterialTheme.typography.labelSmall
    )
}
