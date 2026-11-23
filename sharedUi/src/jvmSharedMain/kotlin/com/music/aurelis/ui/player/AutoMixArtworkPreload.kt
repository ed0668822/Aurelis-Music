package com.music.aurelis.ui.player

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import coil3.SingletonImageLoader
import coil3.compose.LocalPlatformContext
import coil3.request.ImageRequest
import coil3.request.SuccessResult
import com.music.aurelis.data.model.PLAYER_ART_PX
import com.music.aurelis.data.model.ROW_ART_PX
import com.music.aurelis.data.model.artworkAt
import com.music.aurelis.playback.AutoMixVisualPhase
import kotlinx.coroutines.launch

/**
 * Adapts BitChord 1.10's prepared-cover phase to Aurelis's single MediaSession.
 * Only artwork is decoded; this never opens a second audio player or changes a
 * transition decision. Preload is retained across ARMED -> MIXING and scoped to
 * the exact generation so late YouTube/local results cannot cross songs.
 */
@Composable
fun PrepareAutoMixArtwork(mini: Boolean = false) {
    val feed = PlayerPlatform.host.autoMixVisualState ?: return
    val gate = autoMixArtworkGate(mini)
    val context = LocalPlatformContext.current
    LaunchedEffect(feed, gate.enabled, gate.skipThrough, context) {
        val launched = mutableSetOf<Long>()
        feed.collect { state ->
            if (state == null || state.phase != AutoMixVisualPhase.ARMED ||
                !gate.allows(state.generation) || !launched.add(state.generation)
            ) return@collect
            val raw = state.incoming.artworkUrl?.takeIf { it.isNotBlank() } ?: return@collect
            val generation = state.generation
            val loader = SingletonImageLoader.get(context)
            fun valid() = feed.value?.let {
                it.generation == generation && it.phase != AutoMixVisualPhase.CANCELLED
            } == true
            if (!mini) launch {
                val result = runCatching {
                    loader.execute(
                        ImageRequest.Builder(context)
                            .data(raw.artworkAt(PLAYER_ART_PX))
                            .size(PLAYER_ART_PX)
                            .build(),
                    )
                }.getOrNull()
                if (result is SuccessResult && valid()) {
                    AutoMixPreparedCover.retain(generation, result)
                }
            }
            launch {
                val result = runCatching {
                    loader.execute(
                        ImageRequest.Builder(context)
                            .data(raw.artworkAt(ROW_ART_PX))
                            .size(ROW_ART_PX)
                            .build(),
                    )
                }.getOrNull()
                if (result is SuccessResult && valid()) {
                    AutoMixPreparedCover.retainMini(generation, result)
                }
            }
        }
    }
}
