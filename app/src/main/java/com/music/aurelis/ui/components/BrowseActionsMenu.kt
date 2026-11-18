// SPDX-FileCopyrightText: 2026 Aurelis
// SPDX-License-Identifier: LGPL-3.0-or-later
package com.music.aurelis.ui.components

import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.music.aurelis.R

/** Only shows actions with a real local callback; no streaming/download placeholders. */
@Composable
fun BrowseActionsMenu(
    onDismiss: () -> Unit,
    expanded: Boolean = true,
    onPlay: (() -> Unit)? = null,
    onShuffle: (() -> Unit)? = null,
    onSelect: (() -> Unit)? = null,
    onRename: (() -> Unit)? = null,
    onDelete: (() -> Unit)? = null,
) {
    DropdownMenu(expanded = expanded, onDismissRequest = onDismiss) {
        onPlay?.let { action ->
            DropdownMenuItem(text = { Text(stringResource(R.string.collection_play)) },
                onClick = { onDismiss(); action() })
        }
        onShuffle?.let { action ->
            DropdownMenuItem(text = { Text(stringResource(R.string.shuffle)) },
                onClick = { onDismiss(); action() })
        }
        onSelect?.let { action ->
            DropdownMenuItem(text = { Text("Seleccionar canciones") },
                onClick = { onDismiss(); action() })
        }
        onRename?.let { action ->
            DropdownMenuItem(text = { Text(stringResource(R.string.playlist_rename)) },
                onClick = { onDismiss(); action() })
        }
        onDelete?.let { action ->
            DropdownMenuItem(text = { Text(stringResource(R.string.playlist_delete)) },
                onClick = { onDismiss(); action() })
        }
    }
}
