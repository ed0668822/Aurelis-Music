// SPDX-FileCopyrightText: 2026 Aurelis
// SPDX-License-Identifier: LGPL-3.0-or-later
package com.music.aurelis.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Star
import androidx.compose.material.icons.rounded.StarBorder
import androidx.compose.material.icons.rounded.SkipNext
import androidx.compose.material.icons.rounded.PlaylistAdd
import androidx.compose.material.icons.rounded.MusicNote
import androidx.compose.material.icons.automirrored.rounded.QueueMusic
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import com.music.aurelis.data.LocalUserLibrary
import com.music.aurelis.data.model.LocalSong

/**
 * F039: local-only song actions in the existing anchored, scrollable IosMenu.
 * Do not expose account, stream, download, radio or video operations here.
 */
@Composable
fun SongActionsDialog(
    song: LocalSong, store: LocalUserLibrary, onEnqueue: () -> Unit, onDismiss: () -> Unit,
    onPlayNext: () -> Unit, queueActionsEnabled: Boolean = true, playNextEnabled: Boolean = true,
) {
    val user by store.state.collectAsState()
    var name by remember(song.id) { mutableStateOf("") }
    val favorite = song.id in user.favorites

    HeldContextMenu(onDismiss) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Surface(shape = RoundedCornerShape(9.dp),
                color = MaterialTheme.colorScheme.surfaceContainerHigh,
                modifier = Modifier.size(48.dp)) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(Icons.Rounded.MusicNote, contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant)
                    if (!song.artworkUri.isNullOrBlank()) {
                        AsyncImage(model = song.artworkUri, contentDescription = null,
                            modifier = Modifier.fillMaxSize().clip(RoundedCornerShape(9.dp)),
                            contentScale = ContentScale.Crop)
                    }
                }
            }
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(song.title, style = MaterialTheme.typography.titleMedium,
                    maxLines = 2, overflow = TextOverflow.Ellipsis)
                Text(song.artist, style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
        }
        HorizontalDivider(Modifier.padding(vertical = 6.dp))

        SongMenuAction(
            icon = if (favorite) Icons.Rounded.Star else Icons.Rounded.StarBorder,
            label = if (favorite) "Quitar de favoritos" else "Añadir a favoritos",
            enabled = !user.readOnly, selected = favorite,
            onClick = { store.toggleFavorite(song.id) },
        )
        HorizontalDivider(Modifier.padding(vertical = 6.dp))

        SongMenuAction(
            icon = Icons.Rounded.SkipNext, label = "Reproducir después",
            enabled = queueActionsEnabled && playNextEnabled,
            onClick = { onPlayNext(); onDismiss() },
        )
        if (queueActionsEnabled && !playNextEnabled) {
            Text("Desactiva el modo aleatorio para elegir la siguiente canción.",
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        SongMenuAction(
            icon = Icons.AutoMirrored.Rounded.QueueMusic, label = "Añadir a la cola",
            enabled = queueActionsEnabled,
            onClick = { onEnqueue(); onDismiss() },
        )
        HorizontalDivider(Modifier.padding(vertical = 6.dp))

        user.playlists.forEach { playlist ->
            SongMenuAction(
                icon = Icons.Rounded.PlaylistAdd,
                label = "Añadir a ${playlist.name}",
                enabled = !user.readOnly,
                onClick = {
                    store.setPlaylistSongs(playlist.id, playlist.songIds + song.id)
                    onDismiss()
                },
            )
        }

        OutlinedTextField(
            value = name, onValueChange = { name = it.take(100) },
            label = { Text("Nueva playlist") },
            singleLine = true, enabled = !user.readOnly,
            modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 5.dp),
        )
        SongMenuAction(
            icon = Icons.Rounded.PlaylistAdd,
            label = "Crear playlist con esta canción",
            enabled = !user.readOnly && name.isNotBlank(),
            onClick = { store.createPlaylist(name, listOf(song.id)); onDismiss() },
        )
        HorizontalDivider(Modifier.padding(vertical = 6.dp))
        TextButton(onClick = onDismiss, modifier = Modifier.fillMaxWidth()) {
            Text("Cerrar")
        }
    }
}

/** The same predictable row anatomy for favorites, queue and local playlists. */
@Composable
private fun SongMenuAction(
    icon: ImageVector, label: String, enabled: Boolean, onClick: () -> Unit,
    selected: Boolean = false,
) {
    TextButton(
        onClick = onClick, enabled = enabled,
        modifier = Modifier.fillMaxWidth().heightIn(min = 44.dp),
        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
    ) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Icon(icon, contentDescription = null, modifier = Modifier.size(20.dp),
                tint = if (selected) MaterialTheme.colorScheme.primary
                       else MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(Modifier.width(12.dp))
            Text(label, modifier = Modifier.weight(1f),
                style = MaterialTheme.typography.bodyMedium,
                color = if (selected) MaterialTheme.colorScheme.primary
                        else MaterialTheme.colorScheme.onSurface)
        }
    }
}
