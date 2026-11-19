// SPDX-FileCopyrightText: 2026 Aurelis
// SPDX-License-Identifier: LGPL-3.0-or-later
package com.music.aurelis.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.background
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.IntRect
import androidx.compose.material.icons.rounded.Timer
import androidx.compose.material.icons.rounded.Tune
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import com.music.aurelis.data.LocalUserLibrary
import com.music.aurelis.data.model.LocalSong
import com.music.aurelis.ui.icons.AurelisIcons
import com.music.aurelis.ui.icons.AurelisPlayerIcons

/**
 * F039: local-only song actions in the existing anchored, scrollable IosMenu.
 * Do not expose account, stream, download, radio or video operations here.
 */
@Composable
fun SongActionsDialog(
    song: LocalSong, store: LocalUserLibrary, onEnqueue: () -> Unit, onDismiss: () -> Unit,
    onPlayNext: () -> Unit, queueActionsEnabled: Boolean = true, playNextEnabled: Boolean = true,
    onOpenAlbum: (() -> Unit)? = null, onOpenArtist: (() -> Unit)? = null,
    anchorBounds: IntRect? = null,
    onLyricsOffset: (() -> Unit)? = null,
    onPlaybackTools: (() -> Unit)? = null,
) {
    val user by store.state.collectAsState()
    var name by remember(song.id) { mutableStateOf("") }
    val favorite = song.id in user.favorites

    HeldContextMenu(onDismiss, compactSongStyle = true, anchorBounds = anchorBounds) {
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

        // Local favorite mirrors BitChord's leading quick-action tile.
        // It stays mounted after a tap so the state updates immediately.
        SongFavoriteQuickAction(
            icon = if (favorite) AurelisPlayerIcons.StarFilled else AurelisPlayerIcons.Star,
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
        if (onOpenAlbum != null || onOpenArtist != null) {
            HorizontalDivider(Modifier.padding(vertical = 6.dp))
            onOpenAlbum?.let { open ->
                SongMenuAction(
                    icon = AurelisIcons.Album, label = "Abrir álbum", enabled = true,
                    onClick = { onDismiss(); open() },
                )
            }
            onOpenArtist?.let { open ->
                SongMenuAction(
                    icon = AurelisIcons.Artist, label = "Abrir artista", enabled = true,
                    onClick = { onDismiss(); open() },
                )
            }
        }
        // BitChord's compact three-dot menu also groups current-player tools.
        // These actions remain connected to Aurelis' existing local settings.
        if (onLyricsOffset != null || onPlaybackTools != null) {
            HorizontalDivider(Modifier.padding(vertical = 6.dp))
            onLyricsOffset?.let { open ->
                SongMenuAction(
                    icon = Icons.Rounded.Tune, label = "Desplazamiento de letras",
                    enabled = true, onClick = { onDismiss(); open() },
                )
            }
            onPlaybackTools?.let { open ->
                SongMenuAction(
                    icon = Icons.Rounded.Timer, label = "Herramientas de reproducción",
                    enabled = true, onClick = { onDismiss(); open() },
                )
            }
        }
        HorizontalDivider(Modifier.padding(vertical = 6.dp))
        TextButton(onClick = onDismiss, modifier = Modifier.fillMaxWidth()) {
            Text("Cerrar")
        }
    }
}

/** The reference quick action occupies 66dp, with a 23dp glyph over a 12sp label. */
@Composable
private fun SongFavoriteQuickAction(
    icon: ImageVector, label: String, enabled: Boolean, selected: Boolean, onClick: () -> Unit,
) {
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val accent = MaterialTheme.colorScheme.primary
    val ink = if (selected) accent else MaterialTheme.colorScheme.onSurface
    Row(Modifier.fillMaxWidth().height(66.dp).padding(horizontal = 10.dp)) {
        Column(
            Modifier.weight(1f).fillMaxHeight().clip(RoundedCornerShape(16.dp))
                .background(if (pressed) MaterialTheme.colorScheme.onSurface.copy(alpha = 0.10f)
                            else Color.Transparent)
                .clickable(interactionSource = interaction, indication = null,
                    enabled = enabled, onClick = onClick)
                .padding(horizontal = 4.dp, vertical = 8.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            Icon(icon, contentDescription = null, tint = ink.copy(alpha = if (enabled) 1f else 0.4f),
                modifier = Modifier.size(23.dp))
            Spacer(Modifier.height(6.dp))
            Text(label, color = ink.copy(alpha = if (enabled) 1f else 0.4f),
                fontSize = 12.sp, lineHeight = 15.sp, fontWeight = FontWeight.Medium,
                maxLines = 1, overflow = TextOverflow.Ellipsis)
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
