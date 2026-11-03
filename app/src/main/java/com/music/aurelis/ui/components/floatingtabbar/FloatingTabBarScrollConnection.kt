// Aurelis © 2026. GPL-3.0-or-later
package com.music.aurelis.ui.components.floatingtabbar

import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll

class FloatingTabBarScrollConnection {
    private var onScroll: (Float) -> Unit = {}

    fun onScroll(delta: Float) {
        onScroll(delta)
    }

    internal fun setOnScroll(listener: (Float) -> Unit) {
        onScroll = listener
    }

    fun Modifier.nestedScrollConnection(): Modifier = this.nestedScroll(
        connection = object : NestedScrollConnection {
            override fun onPreScroll(available: Offset, source: NestedScrollSource): Offset {
                if (source == NestedScrollSource.UserInput) {
                    onScroll(-available.y)
                }
                return Offset.Zero
            }
        },
    )
}
