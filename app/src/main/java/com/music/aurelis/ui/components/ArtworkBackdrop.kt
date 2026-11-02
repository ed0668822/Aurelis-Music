// Aurelis © 2026. GPL-3.0-or-later
package com.music.aurelis.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

@Composable
fun ArtworkBackdrop(
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(
                BrushVertical(
                    listOf(
                        MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                        MaterialTheme.colorScheme.surface
                    )
                )
            )
    )
}

@Composable
fun BrushVertical(colors: List<Color>) = androidx.compose.ui.graphics.Brush.verticalGradient(colors)
