// SPDX-FileCopyrightText: 2026 Aurelis
// SPDX-License-Identifier: LGPL-3.0-or-later
// Adapted from BitChord b690dc14 MiniPlayer.miniPlayerGestures (GPLv3); see NOTICE.
package com.music.aurelis.ui.components

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animate
import androidx.compose.animation.core.spring
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.drag
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import com.music.aurelis.ui.theme.LocalReduceMotion
import androidx.compose.ui.input.pointer.PointerInputChange
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.positionChange
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.input.pointer.util.VelocityTracker
import androidx.compose.ui.unit.dp
import com.music.aurelis.ui.player.MiniPlayerPull
import kotlin.math.abs
import kotlin.math.sqrt
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

/** A rubber-band lift, bounded and non-linear; never changes player position. */
internal fun miniPlayerLiftDistance(pulledPx: Float, easePx: Float): Float =
    if (!pulledPx.isFinite() || !easePx.isFinite() || pulledPx <= 0f || easePx <= 0f) 0f
    else easePx * (sqrt(1f + pulledPx / easePx) - 1f)

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
    val reduceMotion by rememberUpdatedState(LocalReduceMotion.current)
    val lift = remember { mutableFloatStateOf(0f) }
    val settle = remember { arrayOfNulls<Job>(1) }
    val scope = rememberCoroutineScope()
    // Gesture thresholds are in physical pixels. Recreate the detector if
    // Android changes density while this mini-player stays composed.
    val density = LocalDensity.current.density
    return pointerInput(density) {
        val trackThreshold = 72.dp.toPx()
        val expandThreshold = 48.dp.toPx()
        val ease = 72.dp.toPx()
        val maxVelocity = viewConfiguration.maximumFlingVelocity
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
                    // As in BitChord, do not consume downward drags.
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
                settle[0]?.cancel()
                lift.floatValue = 0f
                // Give the sheet the displacement from initial contact, including slop.
                // The bar itself rises only for subsequent pull movement.
                var distance = slopped.position.y - down.position.y
                val target = currentPull
                val tracker = VelocityTracker()
                tracker.addPosition(down.uptimeMillis, down.position)
                tracker.addPosition(slopped.uptimeMillis, slopped.position)
                lift.floatValue = 0f
                var pulled = 0f
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
                        pulled -= delta
                        lift.floatValue = if (reduceMotion) 0f else miniPlayerLiftDistance(pulled, ease)
                    }
                    if (finished) {
                        if (target != null) target.release(tracker.calculateVelocity().y.coerceIn(-maxVelocity, maxVelocity))
                        else if (-distance >= expandThreshold) expand()
                    }
                } finally {
                    if (!finished) target?.cancel()
                    val start = lift.floatValue
                    if (reduceMotion || start <= 0f) lift.floatValue = 0f
                    else settle[0] = scope.launch {
                        animate(start, 0f, animationSpec = spring(
                            dampingRatio = 0.65f, stiffness = Spring.StiffnessMediumLow,
                        )) { value, _ -> lift.floatValue = value }
                    }
                }
            }
        }
    }.graphicsLayer { translationY = -lift.floatValue }
}
