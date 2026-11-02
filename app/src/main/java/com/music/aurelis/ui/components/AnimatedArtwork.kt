// Aurelis © 2026. GPL-3.0-or-later
package com.music.aurelis.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FadeIn
import androidx.compose.animation.core.FadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalRetainedComposition
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.IntOffset
import kotlin.random.Random

@Composable
fun AnimatedArtwork(
    songKey: String = "",
    artworkUri: String? = null,
    title: String = "",
    size: dp = 180.dp,
    progressFraction: Float = 0f,
    modifier: Modifier = Modifier
) {
    var expanded by remember { mutableStateOf(false) }

    // Animated size based on progress
    val animatedSize = animateFloatAsState(
        targetValue = if (progressFraction > 0.5f) 200.dp else size,
        animationSpec = tween(durationMillis = 200)
    )

    // Generate colorful gradient based on song key hash
    val colorHash = songKey.hashCode()
    val primaryColor = MaterialTheme.colorScheme.primary
    val secondaryColor = MaterialTheme.colorScheme.secondary
    val accentColor = when {
        colorHash % 3 == 0 -> Color(0xFFFF0000)
        colorHash % 3 == 1 -> Color(0xFF00FF00)
        else -> Color(0xFF0000FF)
    }

    // Animated gradient based on progress
    val progressGradient by animateColorAsState(
        targetValue = if (progressFraction > 0.5f) secondaryColor else primaryColor,
        animationSpec = tween(durationMillis = 300)
    )

    Box(
        modifier = modifier
            .size(animatedSize)
            .clip(CircleShape)
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        progressGradient,
                        primaryColor
                    )
                )
            ),
        contentAlignment = Alignment.Center
    ) {
        // Artwork placeholder based on available URI
        if (artworkUri != null) {
            painterResource(artworkUri) { image ->
                Image(
                    painter = image,
                    contentDescription = title,
                    modifier = Modifier.size(animatedSize).clip(CircleShape)
                )
            } ?: run {
                // Fallback: show initials or icon
                Text(
                    text = title.isNotEmpty() ? title.substring(0, 1).uppercase() : "🎵",
                    style = MaterialTheme.typography.displayMedium.copy(
                        color = MaterialTheme.colorScheme.onSurface
                    ),
                    modifier = Modifier.size(animatedSize).clip(CircleShape)
                )
            }
        } else {
            Text(
                text = title.isNotEmpty() ? title.substring(0, 1).uppercase() : "🎵",
                style = MaterialTheme.typography.displayMedium.copy(
                    color = MaterialTheme.colorScheme.onSurface
                ),
                modifier = Modifier.size(animatedSize).clip(CircleShape)
            )
        }
    }
}
