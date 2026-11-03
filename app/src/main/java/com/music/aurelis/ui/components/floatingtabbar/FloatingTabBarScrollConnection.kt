// Aurelis © 2026. GPL-3.0-or-later
package com.music.aurelis.ui.components.floatingtabbar

import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.NestedScrollValue
import androidx.compose.ui.input.nestedscroll.nestedScroll

/**
 * Connection between a page's scroll and the floating tab bar's inline/expanded
 * state. The page dispatches its scroll into [nestedScroll] with this connection;
 * the bar reacts by collapsing inline as the page scrolls down and expanding
 * back as it scrolls up.
 */
class FloatingTabBarScrollConnection {
    private var onScroll: (Float) -> Unit = {}

    /** Called by the page on every scroll delta (positive = scrolling down). */
    fun onScroll(delta: Float) {
        onScroll(delta)
    }

    /** Sets the scroll listener. Called by [FloatingTabBar]. */
    internal fun setOnScroll(listener: (Float) -> Unit) {
        onScroll = listener
    }

    /** Modifier to attach to a scrolling container so its deltas reach this connection. */
    fun Modifier.nestedScrollConnection(): Modifier = this.nestedScroll(
        connection = object : NestedScrollConnection {
            override fun onPreScroll(available: androidx.compose.ui.unit.Offset, source: NestedScrollSource): androidx.compose.ui.unit.Offset {
                if (source == NestedScrollSource.Active) {
                    onScroll(-available.y)
                }
                return androidx.compose.ui.unit.Offset.Zero
            }
        }
    )
}
