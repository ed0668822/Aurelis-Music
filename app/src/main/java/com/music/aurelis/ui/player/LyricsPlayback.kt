// Adapted from BitChord PlayerLyrics.kt (GPLv3); original contributors and changes in NOTICE.
package com.music.aurelis.ui.player

import androidx.compose.runtime.*
import androidx.compose.runtime.snapshots.Snapshot
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.music.aurelis.playback.PlaybackPosition
import kotlinx.coroutines.flow.collect
import kotlin.math.abs

/** Same offset convention as BitChord: positive delays lyrics, seeking is its inverse. */
internal fun adjustedLyricsPosition(positionMs: Long, offsetMs: Int): Long =
    shiftLyricsTime(positionMs, -offsetMs.toLong())

internal fun adjustedLyricsSeekTarget(lineTimeMs: Long, offsetMs: Int): Long =
    shiftLyricsTime(lineTimeMs, offsetMs.toLong())

private fun shiftLyricsTime(timeMs: Long, delta: Long): Long {
    val time = timeMs.coerceAtLeast(0)
    return if (delta > 0 && time > Long.MAX_VALUE - delta) Long.MAX_VALUE else (time + delta).coerceAtLeast(0)
}

/** One frame clock per visible panel; stopped/hidden/reduced-motion panels use real reports directly. */
@Composable
internal fun rememberLyricsPlayhead(
    trackKey: String,
    position: PlaybackPosition,
    isPlaying: Boolean,
    interpolate: Boolean,
    offsetMs: Int,
): State<Long> {
    val lifecycle = LocalLifecycleOwner.current.lifecycle
    var foreground by remember(lifecycle) { mutableStateOf(lifecycle.currentState.isAtLeast(Lifecycle.State.STARTED)) }
    DisposableEffect(lifecycle) {
        val observer = LifecycleEventObserver { _, _ -> foreground = lifecycle.currentState.isAtLeast(Lifecycle.State.STARTED) }
        lifecycle.addObserver(observer)
        onDispose { lifecycle.removeObserver(observer) }
    }
    val clock = remember(trackKey, position) { mutableLongStateOf(Snapshot.withoutReadObservation { position.positionMs }) }
    val engine = remember(trackKey, position) { LyricClock(clock.longValue) }
    val running = isPlaying && position.advancing && interpolate && foreground
    LaunchedEffect(engine, position, running) {
        if (!running) {
            snapshotFlow { position.positionMs }.collect {
                engine.hold(it, System.nanoTime() / 1_000_000.0)
                clock.longValue = it
            }
        } else {
            engine.restart()
            while (true) withFrameNanos { frame ->
                val system = System.nanoTime()
                val now = if (abs(system - frame) < 250_000_000L) frame else system
                val sampled = position.sampledAtNanos
                clock.longValue = engine.frame(now / 1_000_000.0, position.positionMs,
                    if (sampled > 0) sampled / 1_000_000.0 else Double.NaN, position.seeks.toLong())
            }
        }
    }
    return remember(clock, position, running, offsetMs) {
        derivedStateOf { adjustedLyricsPosition(if (running) clock.longValue else position.positionMs, offsetMs) }
    }
}
