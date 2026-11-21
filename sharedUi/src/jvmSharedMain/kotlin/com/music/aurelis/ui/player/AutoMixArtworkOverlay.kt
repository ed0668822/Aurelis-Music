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

    // Use the same embedded/local cover lookup as the normal player. The active
    // song can have a precise per-file image where MediaSession metadata has an
    // outdated album thumbnail. Hold that lookup across the handoff.
    val currentArt = PlayerPlatform.host.rememberRemoteArtworkUrl(song)
    var outgoingResolved by remember(mix.generation) { mutableStateOf<String?>(null) }
    var incomingResolved by remember(mix.generation) { mutableStateOf<String?>(null) }
    LaunchedEffect(mix.generation, song.videoId, currentArt) {
        if (!currentArt.isNullOrBlank()) {
            if (song.videoId == mix.outgoingMediaId) outgoingResolved = currentArt
            if (song.videoId == mix.incomingMediaId) incomingResolved = currentArt
        }
    }
    val outgoingSource = outgoingResolved ?: mix.outgoingArtworkUrl
    val incomingSource = incomingResolved ?: mix.incomingArtworkUrl

    var outgoingFallback by remember(mix.generation) { mutableStateOf<String?>(null) }
    var incomingFallback by remember(mix.generation) { mutableStateOf<String?>(null) }
    var outgoingFailed by remember(mix.generation) { mutableStateOf(emptySet<String>()) }
    var incomingFailed by remember(mix.generation) { mutableStateOf(emptySet<String>()) }

    // A non-null URI is not evidence that it decodes. Search by verified
    // title/artist when Coil reports an error, once per actual image failure.
    LaunchedEffect(mix.generation, outgoingSource, outgoingFailed) {
        if ((outgoingSource.isNullOrBlank() || (outgoingSource != null && outgoingSource in outgoingFailed)) &&
            outgoingFallback == null && mix.outgoingTitle.isNotBlank()
        ) {
            outgoingFallback = PlayerPlatform.host.resolveMixArtwork(mix.outgoingTitle, mix.outgoingArtist)
        }
    }
    LaunchedEffect(mix.generation, incomingSource, incomingFailed) {
        if ((incomingSource.isNullOrBlank() || (incomingSource != null && incomingSource in incomingFailed)) &&
            incomingFallback == null && mix.incomingTitle.isNotBlank()
        ) {
            incomingFallback = PlayerPlatform.host.resolveMixArtwork(mix.incomingTitle, mix.incomingArtist)
        }
    }
    // Respect the existing 1200px player / 160px row artwork cache ladder.
    val requestedPx = if (mini) ROW_ART_PX else PLAYER_ART_PX
    val outgoingCandidate = listOfNotNull(
        outgoingResolved, outgoingFallback, mix.outgoingArtworkUrl,
    ).firstOrNull { it.isNotBlank() && it !in outgoingFailed }
    val incomingCandidate = listOfNotNull(
        incomingResolved, incomingFallback, mix.incomingArtworkUrl,
    ).firstOrNull { it.isNotBlank() && it !in incomingFailed }
    val outgoing = outgoingCandidate.artworkAt(requestedPx)
    val incoming = incomingCandidate.artworkAt(requestedPx)
    if (outgoing.isNullOrBlank() && incoming.isNullOrBlank()) return

    val progress = mix.progress.takeIf { it.isFinite() }?.coerceIn(0f, 1f) ?: 0f
    val eased = progress * progress * (3f - 2f * progress)
    Box(modifier) {
        if (!outgoing.isNullOrBlank()) {
            AsyncImage(
                model = outgoing,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                onError = {
                    outgoingCandidate?.let { outgoingFailed = outgoingFailed + it }
                },
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
                onError = {
                    incomingCandidate?.let { incomingFailed = incomingFailed + it }
                },
                modifier = Modifier.fillMaxSize().then(transitionModifier),
            )
        }
    }
}
