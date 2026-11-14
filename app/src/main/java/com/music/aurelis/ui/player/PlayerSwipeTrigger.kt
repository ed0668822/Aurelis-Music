// Aurelis © 2026. GPL-3.0-or-later
package com.music.aurelis.ui.player

import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import androidx.compose.ui.input.pointer.util.VelocityTracker

/** Navigation owns visibility. Cancelled gestures never open or close the player. */
@Composable
internal fun Modifier.playerSwipeTrigger(direction: Int, onTriggered: () -> Unit,
    pull: MiniPlayerPull? = null): Modifier {
    val threshold = with(LocalDensity.current) { 48.dp.toPx() }
    val current = rememberUpdatedState(onTriggered)
    return pointerInput(threshold, direction, pull) {
        var distance = 0f
        var accepted = false
        val velocity = VelocityTracker()
        try {
            detectVerticalDragGestures(
                onDragStart = { distance = 0f; accepted = false; velocity.resetTracking() },
                onDragCancel = { if (accepted) pull?.cancel(); accepted = false; distance = 0f },
                onDragEnd = {
                    if (pull != null) { if (accepted) pull.release(velocity.calculateVelocity().y) }
                    else if (distance * direction >= threshold) current.value()
                    accepted = false
                    distance = 0f
                },
                onVerticalDrag = { change, amount ->
                    change.consume(); distance += amount
                    velocity.addPosition(change.uptimeMillis, change.position)
                    if (!accepted && amount * direction > 0f) { accepted = true; pull?.beginDrag() }
                    if (accepted) pull?.drag(amount)
                },
            )
        } finally {
            // Pointer-input disposal also cancels an in-flight pull.
            if (accepted) pull?.cancel()
        }
    }
}
