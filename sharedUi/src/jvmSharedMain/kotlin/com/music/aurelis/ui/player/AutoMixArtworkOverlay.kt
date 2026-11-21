// SPDX-FileCopyrightText: 2026 Aurelis
// SPDX-License-Identifier: LGPL-3.0-or-later
package com.music.aurelis.ui.player

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil3.compose.AsyncImage
import com.music.aurelis.data.model.Song
import com.music.aurelis.data.model.artworkAt
import com.music.aurelis.data.model.PLAYER_ART_PX
import com.music.aurelis.data.model.ROW_ART_PX

/**
 * Both covers share the audio controller's actual progress, not an independent
 * timer. A MediaSession handoff never swaps the visible cover out from under us.
 * This composable neither owns nor creates any media player or audio output.
 */
@Composable
fun AutoMixArtworkOverlay(
    song: Song,
    mini: Boolean,
    modifier: Modifier = Modifier,
) {
    val enabled by PlayerSettings.autoMixArtworkEnabled.collectAsStateWithLifecycle()
    val miniEnabled by PlayerSettings.autoMixMiniArtworkEnabled.collectAsStateWithLifecycle()
    val reducedMotion by PlayerSettings.reduceAnimation.collectAsStateWithLifecycle()
    val transition by PlayerSettings.autoMixArtworkState.collectAsStateWithLifecycle()
    val mix = transition ?: return
    if (!enabled || (mini && !miniEnabled) || !mix.contains(song.videoId)) return

    var outgoingFallback by remember(mix.generation) { mutableStateOf<String?>(null) }
    var incomingFallback by remember(mix.generation) { mutableStateOf<String?>(null) }
    LaunchedEffect(mix.generation, mix.outgoingArtworkUrl) {
        if (mix.outgoingArtworkUrl.isNullOrBlank() && mix.outgoingTitle.isNotBlank()) {
            outgoingFallback = PlayerPlatform.host.resolveMixArtwork(mix.outgoingTitle, mix.outgoingArtist)
        }
    }
    LaunchedEffect(mix.generation, mix.incomingArtworkUrl) {
        if (mix.incomingArtworkUrl.isNullOrBlank() && mix.incomingTitle.isNotBlank()) {
            incomingFallback = PlayerPlatform.host.resolveMixArtwork(mix.incomingTitle, mix.incomingArtist)
        }
    }
    // Match the existing 1200px hero-art / 160px row-art cache ladder.
    // Avoid enlarging small thumbnails on the fullscreen player.
    val requestedPx = if (mini) ROW_ART_PX else PLAYER_ART_PX
    val outgoing = (outgoingFallback ?: mix.outgoingArtworkUrl).artworkAt(requestedPx)
    val incoming = (incomingFallback ?: mix.incomingArtworkUrl).artworkAt(requestedPx)
    if (outgoing.isNullOrBlank() && incoming.isNullOrBlank()) return

    val progress = mix.progress.takeIf { it.isFinite() }?.coerceIn(0f, 1f) ?: 0f
    val eased = progress * progress * (3f - 2f * progress)
    Box(modifier) {
        if (!outgoing.isNullOrBlank()) {
            AsyncImage(
                model = outgoing,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize(),
            )
        }
        if (!incoming.isNullOrBlank()) {
            val transitionModifier = if (reducedMotion) {
                Modifier.graphicsLayer { alpha = eased }
            } else {
                Modifier
                    .graphicsLayer { compositingStrategy = CompositingStrategy.Offscreen }
                    .drawWithContent {
                        drawContent()
                        // A soft diagonal mask reaches both ends precisely at p=0/1.
                        // GPU masking is considerably cheaper than another Canvas decoder.
                        val leading = (-0.35f + 1.70f * eased) * size.width
                        val feather = size.width * 0.28f
                        drawRect(
                            brush = Brush.linearGradient(
                                colors = listOf(Color.Black, Color.Transparent),
                                start = Offset(leading - feather, size.height * 0.20f),
                                end = Offset(leading + feather, size.height * 0.80f),
                            ),
                            blendMode = BlendMode.DstIn,
                        )
                    }
            }
            AsyncImage(
                model = incoming,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize().then(transitionModifier),
            )
        }
    }
}
