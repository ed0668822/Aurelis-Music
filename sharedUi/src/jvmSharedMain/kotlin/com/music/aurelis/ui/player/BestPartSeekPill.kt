// SPDX-FileCopyrightText: 2026 Aurelis
// SPDX-License-Identifier: LGPL-3.0-or-later
package com.music.aurelis.ui.player

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import com.music.aurelis.ui.icons.AurelisIcons
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.produceState
import com.music.aurelis.data.innertube.MostReplayed
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import dev.chrisbanes.haze.HazeState
import com.music.aurelis.ui.components.liveMenuFrost
import com.music.aurelis.ui.components.MenuFrostDark
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.music.aurelis.data.lyrics.LyricLine
import com.music.aurelis.playback.PlaybackPosition
import com.music.aurelis.sharedui.resources.Res
import com.music.aurelis.sharedui.resources.best_part
import org.jetbrains.compose.resources.stringResource
import java.util.Locale

/**
 * Real, offline chorus candidate. Repetition of a full lyric line over at
 * least twelve seconds is evidence; one word, untimed lyrics or a guessed
 * waveform is not. Returns null rather than fabricating a heatmap.
 */
fun bestPartChorusStart(lines: List<LyricLine>, durationMs: Long): Long? {
    if (durationMs < 60_000L) return null
    val usable = lines.mapNotNull { line ->
        val text = line.text.trim().lowercase(Locale.ROOT)
            .replace(Regex("[^\\p{L}\\p{N}\\s]"), "")
            .replace(Regex("\\s+"), " ").trim()
        if (text.length < 8 || line.timeMs < 5_000L ||
            line.timeMs > durationMs - 30_000L
        ) null else line.timeMs to text
    }
    return usable.groupBy { it.second }.values
        .filter { it.size >= 2 }
        .mapNotNull { copies ->
            val earliest = copies.minOf { it.first }
            val latest = copies.maxOf { it.first }
            earliest.takeIf { latest - earliest >= 12_000L }
        }.minOrNull()
}

/**
 * A known replay peak is offered only before it is heard and while the
 * unified player can seek safely. Never fabricate a replay peak from audio data.
 */
internal fun shouldOfferBestPart(
    targetMs: Long?,
    positionMs: Long,
    mixing: Boolean,
    heard: Boolean,
    enabled: Boolean = true,
): Boolean = enabled && targetMs != null && targetMs > 0L && !mixing && !heard &&
    positionMs < targetMs - 400L

@Composable
internal fun BestPartSeekPill(
    lines: List<LyricLine>?,
    videoId: String,
    isLocal: Boolean,
    durationMs: Long,
    position: PlaybackPosition,
    mixing: Boolean,
    onSeek: (Long) -> Unit,
    hazeState: HazeState? = null,
    overlayOpen: () -> Boolean = { false },
 ) {
    val enabled by PlayerSettings.showBestPart.collectAsStateWithLifecycle()
    if (!enabled) return

    val publicPeak by produceState<Long?>(
        initialValue = if (isLocal) null else MostReplayed.knownPeak(videoId, durationMs),
        key1 = videoId,
        key2 = durationMs,
        key3 = isLocal,
    ) {
        value = if (!isLocal && durationMs >= 60_000L &&
            MostReplayed.isYouTubeId(videoId)
        ) MostReplayed.peakFor(videoId, durationMs) else null
    }
    val chorus = remember(lines, durationMs) {
        bestPartChorusStart(lines.orEmpty(), durationMs)
    }
    // YouTube's real most-replayed curve wins; synchronized lyrics can
    // only provide a repeatable candidate, never an invented heatmap.
    val target = publicPeak ?: chorus
    val reduceBlur by PlayerSettings.reduceDynamicBlur.collectAsStateWithLifecycle()
    var heard by remember(videoId) { mutableStateOf(false) }
    val reached = target != null &&
        position.positionMs in (target - 400L)..(target + 20_000L)
    LaunchedEffect(reached) {
        if (reached) heard = true
    }
    AnimatedVisibility(
        visible = shouldOfferBestPart(target, position.positionMs, mixing, heard, enabled),
        enter = expandVertically(tween(280)) + fadeIn(tween(280)),
        exit = fadeOut(tween(220)) + shrinkVertically(tween(220)),
    ) {
        Row(
            modifier = Modifier
                .padding(top = 8.dp)
                .clip(CircleShape)
                // Same sampled frost and glass rim as the original player.
                .then(
                    if (reduceBlur || mixing || overlayOpen() || hazeState == null) {
                        Modifier.background(MenuFrostDark.copy(alpha = 0.72f))
                    } else {
                        Modifier.liveMenuFrost(hazeState, MenuFrostDark)
                    },
                )
                .glassRim(CircleShape)
                .clickable(role = Role.Button) {
                    val seekTarget = target ?: return@clickable
                    heard = true
                    onSeek(seekTarget)
                }
                .padding(horizontal = 14.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                imageVector = AurelisIcons.MusicNote,
                contentDescription = null,
                tint = Color.White,
                modifier = Modifier.size(15.dp),
            )
            Text(
                text = stringResource(Res.string.best_part),
                color = Color.White,
                fontWeight = FontWeight.Bold,
                style = MaterialTheme.typography.labelLarge,
                modifier = Modifier.padding(start = 6.dp),
            )
        }
    }
}
