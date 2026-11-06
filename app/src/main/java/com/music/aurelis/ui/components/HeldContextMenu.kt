// Aurelis © 2026. GPL-3.0-or-later
package com.music.aurelis.ui.components

import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

/** Shared accessible gesture: a hold opens actions without invoking playback. */
@OptIn(androidx.compose.foundation.ExperimentalFoundationApi::class)
fun Modifier.heldContextMenuInteractions(onClick: () -> Unit, onLongClick: () -> Unit): Modifier =
    combinedClickable(onClick = onClick, onLongClick = onLongClick,
        onLongClickLabel = "Mostrar acciones de la canción")

/** Uses the same focus, dismissal, scrolling and motion policy as other menus. */
@Composable
fun HeldContextMenu(onDismiss: () -> Unit, actions: @Composable ColumnScope.() -> Unit) {
    IosMenu(onDismiss, actions)
}
