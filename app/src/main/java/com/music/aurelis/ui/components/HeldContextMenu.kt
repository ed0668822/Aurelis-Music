// Aurelis © 2026. GPL-3.0-or-later
package com.music.aurelis.ui.components

import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.layout.boundsInWindow
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.unit.IntRect
import kotlin.math.roundToInt

/** One UI host owns pending anchors; no Activity, song or bitmap is retained globally. */
internal class HeldMenuState {
    var pendingAnchor: HeldMenuAnchor? = null
    var restoreAnchor by mutableStateOf<HeldMenuAnchor?>(null)

    fun takeAnchor(): HeldMenuAnchor? {
        val anchor = pendingAnchor
        pendingAnchor = null
        return anchor?.takeIf { it.attached }
    }
}

internal class HeldMenuAnchor {
    val focusRequester = FocusRequester()
    var bounds by mutableStateOf<IntRect?>(null)
    var attached = false
}

internal val LocalHeldMenuState = staticCompositionLocalOf<HeldMenuState?> { null }

@Composable
internal fun rememberHeldMenuState(): HeldMenuState {
    val state = remember { HeldMenuState() }
    val restore = state.restoreAnchor
    LaunchedEffect(restore) {
        if (restore != null) {
            // Wait until the focusable popup is removed. Host disposal cancels this job.
            withFrameNanos { }
            if (restore.attached) restore.focusRequester.requestFocus()
            state.restoreAnchor = null
        }
    }
    return state
}

/** Shared accessible gesture: a hold opens actions without invoking playback. */
@OptIn(androidx.compose.foundation.ExperimentalFoundationApi::class)
fun Modifier.heldContextMenuInteractions(onClick: () -> Unit, onLongClick: () -> Unit): Modifier =
    composed {
        val state = LocalHeldMenuState.current
        val anchor = remember { HeldMenuAnchor() }
        DisposableEffect(state, anchor) {
            onDispose {
                anchor.attached = false
                anchor.bounds = null
                if (state != null && state.pendingAnchor === anchor) state.pendingAnchor = null
            }
        }
        this.focusRequester(anchor.focusRequester)
            .onGloballyPositioned { coordinates ->
                anchor.attached = coordinates.isAttached
                val bounds = coordinates.boundsInWindow()
                anchor.bounds = IntRect(bounds.left.roundToInt(), bounds.top.roundToInt(),
                    bounds.right.roundToInt(), bounds.bottom.roundToInt())
            }
            .combinedClickable(onClick = onClick, onLongClick = {
                state?.restoreAnchor = null
                state?.pendingAnchor = anchor
                onLongClick()
            }, onLongClickLabel = "Mostrar acciones de la canción")
    }

/** Uses the same focus, dismissal, scrolling and motion policy as other menus. */
@Composable
fun HeldContextMenu(onDismiss: () -> Unit, actions: @Composable ColumnScope.() -> Unit) {
    IosMenu(onDismiss, actions)
}
