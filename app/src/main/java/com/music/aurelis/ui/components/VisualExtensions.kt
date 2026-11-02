// Aurelis © 2026. GPL-3.0-or-later
package com.music.aurelis.ui.components

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.text.TextStyle
import dev.chrisbanes.haze.ExperimentalHazeApi
import dev.chrisbanes.haze.HazeEffectScope
import dev.chrisbanes.haze.HazeInputScale
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.HazeStyle
import dev.chrisbanes.haze.hazeEffect

@OptIn(ExperimentalHazeApi::class)
fun Modifier.optimizedHazeEffect(
    state: HazeState,
    style: HazeStyle = HazeStyle.Unspecified,
    block: (HazeEffectScope.() -> Unit)? = null,
): Modifier = hazeEffect(state, style) {
    inputScale = HazeInputScale.Fixed(0.33f)
    block?.invoke(this)
}

fun Modifier.thumbnailBorder(
    shape: Shape = RectangleShape
): Modifier = this

@Composable
fun ExplicitSongTitle(
    title: String,
    isExplicit: Boolean = false,
    modifier: Modifier = Modifier,
    style: TextStyle = MaterialTheme.typography.bodyLarge,
    color: Color = Color.Unspecified
) {
    Text(
        text = if (isExplicit) "$title 🅴" else title,
        modifier = modifier,
        style = style,
        color = color
    )
}

@Composable
fun ExplicitBadge(
    modifier: Modifier = Modifier
) {
    Text(
        text = "E",
        modifier = modifier,
        style = MaterialTheme.typography.labelSmall
    )
}
