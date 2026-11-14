// Aurelis © 2026. GPL-3.0-or-later
// Adapted from BitChord b690dc14 MiniPlayer.miniPlayerGestures (GPLv3); see NOTICE.
package com.music.aurelis.ui.components

import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.drag
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.PointerInputChange
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.positionChange
import androidx.compose.ui.input.pointer.util.VelocityTracker
import androidx.compose.ui.unit.dp
import com.music.aurelis.ui.player.MiniPlayerPull
import kotlin.math.abs

/** Decide one axis once. Queue changes happen only after a completed drag. */
@Composable
internal fun Modifier.miniPlayerGestures(
    pull: MiniPlayerPull?,
    enabled: Boolean,
    canNext: Boolean,
    canPrevious: Boolean,
    onNext: () -> Unit,
    onPrevious: () -> Unit,
    onExpand: () -> Unit,
): Modifier {
    val currentPull by rememberUpdatedState(pull)
    val currentEnabled by rememberUpdatedState(enabled)
    val currentCanNext by rememberUpdatedState(canNext)
    val currentCanPrevious by rememberUpdatedState(canPrevious)
    val next by rememberUpdatedState(onNext)
    val previous by rememberUpdatedState(onPrevious)
    val expand by rememberUpdatedState(onExpand)
    return pointerInput(Unit) {
        val trackThreshold = 72.dp.toPx()
        val expandThreshold = 48.dp.toPx()
        awaitEachGesture {
            val down = awaitFirstDown(requireUnconsumed = false)
            val pointer = down.id
            var start: PointerInputChange? = null
            var vertical = false
            while (start == null) {
                val change = awaitPointerEvent().changes.firstOrNull { it.id == pointer }
                    ?: return@awaitEachGesture
                if (!change.pressed || change.isConsumed) return@awaitEachGesture
                val moved = change.position - down.position
                if (moved.getDistance() < viewConfiguration.touchSlop) continue
                vertical = moved.y < 0f && abs(moved.x) < -moved.y * 1.5f
                if (!vertical && abs(moved.x) < abs(moved.y)) {
                    // A downward drag is not an expand tap or a queue command.
                    change.consume()
                    drag(pointer) { it.consume() }
                    return@awaitEachGesture
                }
                change.consume()
                start = change
            }
            val slopped = start ?: return@awaitEachGesture
            if (!vertical) {
                var distance = slopped.position.x - down.position.x
                val finished = drag(pointer) { change ->
                    distance += change.positionChange().x
                    change.consume()
                }
                if (finished && currentEnabled) {
                    if (distance <= -trackThreshold && currentCanNext) next()
                    else if (distance >= trackThreshold && currentCanPrevious) previous()
                }
            } else {
                // Keep Aurelis's existing vertical distance measured after touch slop.
                var distance = (slopped.position.y - down.position.y + viewConfiguration.touchSlop).coerceAtMost(0f)
                val target = currentPull
                val tracker = VelocityTracker()
                tracker.addPosition(slopped.uptimeMillis, slopped.position)
                var finished = false
                try {
                    target?.beginDrag()
                    target?.drag(distance)
                    finished = drag(pointer) { change ->
                        val delta = change.positionChange().y
                        distance += delta
                        tracker.addPosition(change.uptimeMillis, change.position)
                        change.consume()
                        target?.drag(delta)
                    }
                    if (finished) {
                        if (target != null) target.release(tracker.calculateVelocity().y)
                        else if (-distance >= expandThreshold) expand()
                    }
                } finally {
                    if (!finished) target?.cancel()
                }
            }
        }
    }
}
