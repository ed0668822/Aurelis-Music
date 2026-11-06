package com.music.aurelis.ui.player

import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp

/** Navigation owns visibility. Cancelled gestures never open or close the player. */
@Composable
internal fun Modifier.playerSwipeTrigger(direction: Int, onTriggered: () -> Unit): Modifier {
    val threshold = with(LocalDensity.current) { 48.dp.toPx() }
    val current = rememberUpdatedState(onTriggered)
    return pointerInput(threshold, direction) {
        var distance = 0f
        detectVerticalDragGestures(
            onDragStart = { distance = 0f },
            onDragCancel = { distance = 0f },
            onDragEnd = {
                if (distance * direction >= threshold) current.value()
                distance = 0f
            },
            onVerticalDrag = { change, amount -> change.consume(); distance += amount },
        )
    }
}
