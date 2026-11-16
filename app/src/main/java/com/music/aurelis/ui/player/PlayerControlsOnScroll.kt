// Adapted from BitChord b690dc146009d4815825fa4a20815ef2e4ee8898 PlayerLyrics.kt.
// Original BitChord contributors, GPLv3; see NOTICE and LICENSE_AUDIT.
package com.music.aurelis.ui.player

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp

private val CONTROLS_SCROLL_SLOP = 20.dp

@Composable
internal fun rememberPlayerControlsOnScroll(
    enabled: Boolean = true,
    onReveal: () -> Unit,
    onHide: () -> Unit,
): NestedScrollConnection {
    val controlsSlopPx = with(LocalDensity.current) { CONTROLS_SCROLL_SLOP.toPx() }
    val revealControls by rememberUpdatedState(onReveal)
    val hideControls by rememberUpdatedState(onHide)
    return remember(enabled, controlsSlopPx) {
        object : NestedScrollConnection {
            private var travel = 0f

            override fun onPreScroll(available: Offset, source: NestedScrollSource): Offset {
                if (enabled && source == NestedScrollSource.UserInput && available.y != 0f) {
                    if (travel != 0f && (travel > 0f) != (available.y > 0f)) travel = 0f
                    travel += available.y
                    // Finger travelling up is the list going forward.
                    if (travel <= -controlsSlopPx) {
                        travel = 0f
                        hideControls()
                    } else if (travel >= controlsSlopPx) {
                        travel = 0f
                        revealControls()
                    }
                }
                return Offset.Zero
            }
        }
    }
}
