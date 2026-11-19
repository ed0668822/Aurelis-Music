// SPDX-FileCopyrightText: 2026 Aurelis
// SPDX-License-Identifier: LGPL-3.0-or-later
package com.music.aurelis.ui.components

import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.FastOutSlowInEasing
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
    // F052: hold the previous decoded cover until Coil supplies the next one.
    // This state belongs to the mounted player, not to each song id: a queue
    // skip or a duplicate queue entry cannot momentarily blank the artwork.
    val transition = remember(context) { ArtworkTransition<ImageBitmap?>(null) }
    val decodeSize = if (size <= 64.dp) 128 else 1024
    LaunchedEffect(artworkUri, decodeSize, context) {
        val request = transition.begin()
        if (artworkUri.isNullOrBlank()) {
            // A missing local file must eventually remove the previous cover.
            transition.complete(request, null)
            return@LaunchedEffect
        }

        val next = try {
            val result = SingletonImageLoader.get(context).execute(
                ImageRequest.Builder(context)
                    .data(artworkUri)
                    // Request expanded size, never the dock's temporarily
                    // collapsed size: opening the player stays sharp.
                    .size(decodeSize)
                    .forPixelAccess()
                    .build(),
            )
            (result as? SuccessResult)?.image?.toImageBitmap()
        } catch (cancelled: CancellationException) {
            // Replaced tracks may finish their Coil decode later. The old
            // coroutine must not publish a stale cover or swallow cancellation.
            throw cancelled
        } catch (_: Exception) {
            null
        }
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
        Crossfade(
            targetState = transition.value,
            animationSpec = tween(
                durationMillis = if (reduceMotion || miniPlayerStyle) 0 else 320,
                easing = FastOutSlowInEasing,
            ),
            modifier = Modifier.fillMaxSize(),
            label = "player-artwork-crossfade",
        ) { bitmap ->
            if (bitmap != null) Image(bitmap, null, Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
            else if (!miniPlayerStyle) Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text(displayText, style = MaterialTheme.typography.headlineMedium, color = MaterialTheme.colorScheme.onPrimary)
            }
        }
    }
}
