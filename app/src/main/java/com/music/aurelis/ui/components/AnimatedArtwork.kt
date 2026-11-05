// Aurelis © 2026. GPL-3.0-or-later
package com.music.aurelis.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.layout.ContentScale
import coil3.compose.AsyncImage
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

@Composable
fun AnimatedArtwork(
    songKey: String = "",
    artworkUri: String? = null,
    title: String = "",
    size: androidx.compose.ui.unit.Dp = 180.dp,
    progressFraction: Float = 0f,
    modifier: Modifier = Modifier,
) {
    val resolvedSize = size
    val gradientColors = if (progressFraction > 0.5f) {
        listOf(MaterialTheme.colorScheme.secondary, MaterialTheme.colorScheme.primary)
    } else {
        listOf(MaterialTheme.colorScheme.primary, MaterialTheme.colorScheme.secondary)
    }
    val displayText = if (title.isNotEmpty()) {
        title.first().uppercaseChar().toString()
    } else {
        "♫"
    }

    Box(
        modifier = modifier
            .size(resolvedSize)
            .clip(RoundedCornerShape(24.dp))
            .background(Brush.verticalGradient(colors = gradientColors)),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = displayText,
            style = MaterialTheme.typography.headlineMedium,
            color = MaterialTheme.colorScheme.onPrimary,
            modifier = Modifier
                .clip(CircleShape)
                .background(Color.Black.copy(alpha = 0.12f)),
        )
        AsyncImage(model = artworkUri, contentDescription = null,
            contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize())
    }
}

