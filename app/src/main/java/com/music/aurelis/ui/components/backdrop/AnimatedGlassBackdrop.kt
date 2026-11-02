// Aurelis © 2026. GPL-3.0-or-later
package com.music.aurelis.ui.components.backdrop

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color

@Composable
fun AnimatedGlassBackdrop(
    modifier: Modifier = Modifier,
    isDarkTheme: Boolean = true
) {
    val gradientColors = if (isDarkTheme) {
        listOf(Color(0xFF121212), Color(0xFF1E1E2C), Color(0xFF0F0F13))
    } else {
        listOf(Color(0xFFF5F5F7), Color(0xFFE8E8ED), Color(0xFFFFFFFF))
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Brush.verticalGradient(colors = gradientColors))
    )
}
