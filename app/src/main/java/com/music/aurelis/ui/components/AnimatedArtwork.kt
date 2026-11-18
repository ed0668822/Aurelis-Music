// Aurelis © 2026. GPL-3.0-or-later
package com.music.aurelis.ui.components

import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import com.music.aurelis.ui.player.ArtworkTransition
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.graphics.ImageBitmap
import coil3.SingletonImageLoader
import coil3.compose.LocalPlatformContext
import coil3.request.ImageRequest
import coil3.request.SuccessResult
import com.music.aurelis.ui.graphics.forPixelAccess
import com.music.aurelis.ui.graphics.toImageBitmap
import com.music.aurelis.ui.theme.LocalReduceMotion
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.ensureActive
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.layout.ContentScale
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
    corner: androidx.compose.ui.unit.Dp = 10.dp,
    miniPlayerStyle: Boolean = false,
) {
    val context = LocalPlatformContext.current
    val reduceMotion = LocalReduceMotion.current
    // Retain the last decoded frame only while its successor is loading. A failed
    // or missing successor resolves to the neutral placeholder, never a wrong sleeve.
    val transition = remember(context) { ArtworkTransition<ImageBitmap?>(null) }
    LaunchedEffect(artworkUri) {
        val request = transition.begin()
        if (artworkUri.isNullOrBlank()) { transition.complete(request, null); return@LaunchedEffect }
        val result = try {
            SingletonImageLoader.get(context).execute(ImageRequest.Builder(context)
                .data(artworkUri).size(if (size <= 64.dp) 128 else 1024).forPixelAccess().build())
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (_: Exception) {
            null
        }
        val next = (result as? SuccessResult)?.image?.toImageBitmap()
        kotlinx.coroutines.currentCoroutineContext().ensureActive()
        transition.complete(request, next)
    }
    val resolvedSize = size
    val miniBorder = if (isSystemInDarkTheme()) Color.White.copy(alpha = 0.15f)
                     else Color.Black.copy(alpha = 0.15f)
    val gradientColors = listOf(MaterialTheme.colorScheme.primary, MaterialTheme.colorScheme.secondary)
    val displayText = if (title.isNotEmpty()) {
        title.first().uppercaseChar().toString()
    } else {
        "♫"
    }

    Box(
        modifier = modifier
            .size(resolvedSize)
            .clip(RoundedCornerShape(corner))
            .then(if (miniPlayerStyle) Modifier.background(MaterialTheme.colorScheme.surfaceVariant)
                  else Modifier.background(Brush.verticalGradient(colors = gradientColors)))
            .then(if (miniPlayerStyle) Modifier.border(1.dp, miniBorder, RoundedCornerShape(corner))
                  else Modifier),
        contentAlignment = Alignment.Center,
    ) {
        Crossfade(targetState = transition.value, animationSpec = tween(if (reduceMotion || miniPlayerStyle) 0 else 220),
            modifier = Modifier.fillMaxSize(), label = "player-artwork-crossfade") { bitmap ->
            if (bitmap != null) Image(bitmap, null, Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
            else if (!miniPlayerStyle) Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text(displayText, style = MaterialTheme.typography.headlineMedium, color = MaterialTheme.colorScheme.onPrimary)
            }
        }
    }
}
