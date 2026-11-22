// SPDX-FileCopyrightText: 2026 Aurelis
// SPDX-License-Identifier: LGPL-3.0-or-later
package com.music.aurelis.ui.player

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil3.compose.AsyncImage
import com.music.aurelis.data.settings.AutoMixArtworkTuning
import com.music.aurelis.data.model.Song
import com.music.aurelis.data.model.artworkAt
import com.music.aurelis.data.model.PLAYER_ART_PX
import com.music.aurelis.data.model.ROW_ART_PX

/**
 * Never rewind a cover reveal when controller samples arrive out of order.
 * Invalid samples cannot force the mask to become blank.
 */
internal fun nextAutoMixRevealTarget(previous: Float, incoming: Float): Float {
    val last = previous.takeIf { it.isFinite() }?.coerceIn(0f, 1f) ?: 0f
    val observed = incoming.takeIf { it.isFinite() }?.coerceIn(0f, 1f) ?: last
    return maxOf(last, observed)
}

/**
 * Don't paint a second full-size cover if both songs have the same artwork.
 * Song-specific local artwork URIs can differ even when the album art is
 * identical, so a known album + artist match also counts as the same cover.
 * Missing covers keep the normal player image instead of a black rectangle.
 */
internal fun needsAutoMixArtworkReveal(
    outgoingUrl: String?,
    incomingUrl: String?,
    outgoingAlbum: String,
    incomingAlbum: String,
    outgoingArtist: String,
    incomingArtist: String,
): Boolean {
    if (outgoingUrl.isNullOrBlank() || incomingUrl.isNullOrBlank()) return false
    if (outgoingUrl == incomingUrl) return false
    val sameAlbum = outgoingAlbum.isNotBlank() && incomingAlbum.isNotBlank() &&
        outgoingAlbum.trim().equals(incomingAlbum.trim(), ignoreCase = true)
    val sameArtist = outgoingArtist.isNotBlank() && incomingArtist.isNotBlank() &&
        outgoingArtist.trim().equals(incomingArtist.trim(), ignoreCase = true)
    return !(sameAlbum && sameArtist)
}

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
    if (!needsAutoMixArtworkReveal(
            outgoing, incoming, mix.outgoingAlbum, mix.incomingAlbum,
            mix.outgoingArtist, mix.incomingArtist,
        )
    ) return

    val progress = mix.progress.takeIf { it.isFinite() }?.coerceIn(0f, 1f) ?: 0f
    // Smooth only between audio-controller samples, with a 40ms cap.
    // This interpolation never advances ahead of the real audio controller.
    // Restart per generation so one song's fade never leaks into the next.
    val sampledProgress = remember(mix.generation) { Animatable(progress) }
    LaunchedEffect(mix.generation, progress, reducedMotion) {
        val target = nextAutoMixRevealTarget(sampledProgress.targetValue, progress)
        if (reducedMotion) sampledProgress.snapTo(target)
        else sampledProgress.animateTo(target, tween(40, easing = LinearEasing))
    }
    val eased = AutoMixArtworkTuning.revealProgress(sampledProgress.value)
    // Keep outgoing artwork (or the host's current cover) visible until Coil
    // has actually decoded the next image. A URL alone is not a ready frame.
    var incomingReady by remember(mix.generation, incoming) { mutableStateOf(false) }
    var outgoingReady by remember(mix.generation, outgoing) { mutableStateOf(false) }
    // Before MediaSession switches tracks the ordinary sleeve already shows
    // the outgoing cover. Repainting it here introduced a second rectangular
    // cover above full-bleed artwork. Only pin it once the incoming track owns
    // the normal sleeve, so the new image can be revealed without a hard cut.
    val onIncomingTrack = song.videoId == mix.incomingMediaId
    Box(modifier) {
        if (!outgoing.isNullOrBlank()) {
            AsyncImage(
                model = outgoing,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                onSuccess = { outgoingReady = true },
                onError = {
                    outgoingReady = false
                    outgoingCandidate?.let { outgoingFailed = outgoingFailed + it }
                },
                // Pre-decode while still on the outgoing song, but do not draw
                // a duplicate atop its full-bleed cover. A missing decode
                // never becomes an opaque placeholder after the handoff.
                modifier = Modifier.fillMaxSize().graphicsLayer {
                    alpha = if (onIncomingTrack && outgoingReady && incomingReady) 1f else 0f
                },
            )
        }
        if (!incoming.isNullOrBlank()) {
            // The ordinary cover remains underneath until Coil has decoded
            // the incoming image; do not cut to a black loading rectangle.
            val transitionModifier = if (!incomingReady) {
                Modifier.graphicsLayer { alpha = 0f }
            } else if (reducedMotion) {
                Modifier.graphicsLayer { alpha = eased }
            } else {
                Modifier
                    .graphicsLayer { compositingStrategy = CompositingStrategy.Offscreen }
                    .drawWithContent {
                        drawContent()
                        // Audio-driven organic reveal; reduced-motion still uses alpha.
                        // No independent UI timer or extra Canvas decoder is created.
                        drawOrganicArtworkMask(eased, mix.generation)
                    }
            }
            AsyncImage(
                model = incoming,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                onSuccess = { incomingReady = true },
                onError = {
                    incomingReady = false
                    incomingCandidate?.let { incomingFailed = incomingFailed + it }
                },
                modifier = Modifier.fillMaxSize().then(transitionModifier),
            )
        }
    }
}
