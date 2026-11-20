// SPDX-FileCopyrightText: 2026 Aurelis
// SPDX-License-Identifier: LGPL-3.0-or-later
package com.music.aurelis.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntRect
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.music.aurelis.data.LocalUserLibrary
import com.music.aurelis.data.model.LocalSong
import com.music.aurelis.ui.icons.AurelisMenuIcons

/** BitChord-style grouping, limited to approved Aurelis local-music actions. */
internal enum class SongMenuPage { Actions, Playlists, Info, Timer, Mix }

@Composable
fun SongActionsDialog(
    song: LocalSong,
    store: LocalUserLibrary,
    onEnqueue: () -> Unit,
    onDismiss: () -> Unit,
    onPlayNext: () -> Unit,
    queueActionsEnabled: Boolean = true,
    playNextEnabled: Boolean = true,
    onOpenAlbum: (() -> Unit)? = null,
    onOpenArtist: (() -> Unit)? = null,
    anchorBounds: IntRect? = null,
    artworkFrost: androidx.compose.ui.graphics.ImageBitmap? = null,
    onLyricsOffset: (() -> Unit)? = null,
    onLyricsSources: (() -> Unit)? = null,
    onPlaybackTools: (() -> Unit)? = null,
    onStartLocalRadio: (() -> Unit)? = null,
    onCopyDiagnostics: (() -> Unit)? = null,
    mixQueueSize: Int = 0,
    mixInProgress: Boolean = false,
    mixError: String? = null,
    onSetSleepTimer: ((Long, Boolean) -> Unit)? = null,
    timerRemainingMs: Long = 0L,
    sleepAfterTrack: Boolean = false,
) {
    val user by store.state.collectAsState()
    var page by remember(song.id) { mutableStateOf(SongMenuPage.Actions) }
    var newPlaylistName by remember(song.id) { mutableStateOf("") }
    val favorite = song.id in user.favorites

    HeldContextMenu(
        onDismiss = onDismiss,
        compactSongStyle = true,
        anchorBounds = anchorBounds,
        artworkFrost = artworkFrost,
    ) {
        if (page == SongMenuPage.Actions) {
            SongFavoriteQuickAction(
                icon = if (favorite) AurelisMenuIcons.StarFilled else AurelisMenuIcons.Star,
                label = if (favorite) "Quitar de favoritos" else "Añadir a favoritos",
                enabled = !user.readOnly,
                selected = favorite,
                onClick = { store.toggleFavorite(song.id) },
            )
            SongMenuSeparator()
            SongMenuAction(
                icon = AurelisMenuIcons.PlaylistAdd,
                label = "Añadir a playlist",
                enabled = !user.readOnly,
                onClick = { page = SongMenuPage.Playlists },
            )

            SongMenuSeparator()
            onStartLocalRadio?.let { start ->
                SongMenuAction(AurelisMenuIcons.Radio, "Iniciar radio local", !mixInProgress) {
                    onDismiss(); start()
                }
                SongMenuAction(AurelisMenuIcons.MixOverview, "Resumen de AutoMix", true) {
                    page = SongMenuPage.Mix
                }
            }
            SongMenuAction(
                icon = AurelisMenuIcons.PlayNext,
                label = "Reproducir después",
                enabled = queueActionsEnabled && playNextEnabled,
                onClick = { onPlayNext(); onDismiss() },
            )
            if (queueActionsEnabled && !playNextEnabled) {
                Text(
                    "Desactiva el modo aleatorio para elegir la siguiente canción.",
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp),
                    style = MaterialTheme.typography.bodySmall,
                    color = neutralMenuInk().copy(alpha = 0.62f),
                )
            }
            SongMenuAction(
                icon = AurelisMenuIcons.PlayLast,
                label = "Añadir a la cola",
                enabled = queueActionsEnabled,
                onClick = { onEnqueue(); onDismiss() },
            )

            SongMenuSeparator()
            onOpenAlbum?.let { action ->
                SongMenuAction(AurelisMenuIcons.Album, "Abrir álbum", true) {
                    onDismiss(); action()
                }
            }
            onOpenArtist?.let { action ->
                SongMenuAction(AurelisMenuIcons.Artist, "Abrir artista", true) {
                    onDismiss(); action()
                }
            }
            SongMenuAction(
                icon = AurelisMenuIcons.Info,
                label = "Información de la canción",
                enabled = true,
                onClick = { page = SongMenuPage.Info },
            )

            if (onSetSleepTimer != null || onLyricsOffset != null ||
                onLyricsSources != null || onPlaybackTools != null) {
                SongMenuSeparator()
                if (onSetSleepTimer != null) {
                    SongMenuAction(AurelisMenuIcons.SleepTimer, "Temporizador", true) {
                        page = SongMenuPage.Timer
                    }
                }
                onLyricsOffset?.let { action ->
                    SongMenuAction(AurelisMenuIcons.LyricsOffset, "Desplazamiento de letras", true) {
                        onDismiss(); action()
                    }
                }
                onLyricsSources?.let { action ->
                    SongMenuAction(AurelisMenuIcons.LyricsSources, "Fuentes de letras", true) {
                        onDismiss(); action()
                    }
                }
                onPlaybackTools?.let { action ->
                    SongMenuAction(AurelisMenuIcons.PlaybackTools, "Herramientas de reproducción", true) {
                        onDismiss(); action()
                    }
                }
            }
            onCopyDiagnostics?.let { copy ->
                SongMenuSeparator()
                SongMenuAction(AurelisMenuIcons.CopyDiagnostic, "Copiar diagnóstico", true) {
                    onDismiss(); copy()
                }
            }
        } else {
            Row(
                Modifier.fillMaxWidth().heightIn(min = 44.dp).padding(horizontal = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                IconButton(onClick = { page = SongMenuPage.Actions }) {
                    Icon(Icons.Rounded.ArrowBack, contentDescription = "Volver")
                }
                Text(
                    when (page) {
                        SongMenuPage.Playlists -> "Añadir a playlist"
                        SongMenuPage.Info -> "Información de la canción"
                        SongMenuPage.Timer -> "Temporizador"
                        SongMenuPage.Mix -> "Resumen de AutoMix"
                        SongMenuPage.Actions -> ""
                    },
                    style = MaterialTheme.typography.titleSmall,
                    color = neutralMenuInk(),
                )
            }
            SongMenuSeparator()
            when (page) {
                SongMenuPage.Playlists -> {
                    user.playlists.forEach { playlist ->
                        SongMenuAction(
                            icon = AurelisMenuIcons.PlaylistAdd,
                            label = playlist.name,
                            enabled = !user.readOnly,
                            onClick = {
                                store.setPlaylistSongs(playlist.id, playlist.songIds + song.id)
                                onDismiss()
                            },
                        )
                    }
                    OutlinedTextField(
                        value = newPlaylistName,
                        onValueChange = { newPlaylistName = it.take(100) },
                        label = { Text("Nueva playlist") },
                        singleLine = true,
                        enabled = !user.readOnly,
                        modifier = Modifier.fillMaxWidth()
                            .padding(horizontal = 8.dp, vertical = 5.dp),
                    )
                    SongMenuAction(
                        icon = AurelisMenuIcons.PlaylistAdd,
                        label = "Crear playlist con esta canción",
                        enabled = !user.readOnly && newPlaylistName.isNotBlank(),
                        onClick = {
                            store.createPlaylist(newPlaylistName.trim(), listOf(song.id))
                            onDismiss()
                        },
                    )
                }
                SongMenuPage.Info -> {
                    SongInfoValue("Título", song.title)
                    SongInfoValue("Artista", song.artist)
                    song.album?.takeIf { it.isNotBlank() }?.let { SongInfoValue("Álbum", it) }
                    SongInfoValue("Duración", song.durationText)
                    song.sourceQuality?.takeIf { it.isNotBlank() }?.let { SongInfoValue("Formato", it) }
                }
                SongMenuPage.Timer -> {
                    val status = when {
                        sleepAfterTrack -> "Al terminar la canción"
                        timerRemainingMs > 0 ->
                            ((timerRemainingMs + 59_999L) / 60_000L).toString() + " min restantes"
                        else -> "Desactivado"
                    }
                    SongInfoValue("Estado", status)
                    for (minutes in listOf(15, 30, 45, 60)) {
                        SongMenuAction(
                            icon = AurelisMenuIcons.SleepTimer,
                            label = minutes.toString() + " min",
                            enabled = onSetSleepTimer != null,
                            onClick = {
                                onSetSleepTimer?.invoke(minutes * 60_000L, false)
                                onDismiss()
                            },
                        )
                    }
                    SongMenuAction(AurelisMenuIcons.SleepTimer, "Al terminar esta canción",
                        onSetSleepTimer != null) {
                        onSetSleepTimer?.invoke(0L, true)
                        onDismiss()
                    }
                    SongMenuAction(AurelisMenuIcons.SleepTimer, "Cancelar temporizador",
                        onSetSleepTimer != null) {
                        onSetSleepTimer?.invoke(0L, false)
                        onDismiss()
                    }
                }
                SongMenuPage.Mix -> {
                    SongInfoValue("Modo", "Recomendaciones de música local")
                    SongInfoValue("Estado", when {
                        mixInProgress -> "Preparando la mezcla"
                        mixError != null -> mixError
                        else -> "Sin análisis de mezcla en curso"
                    })
                    SongInfoValue("Canciones en cola", mixQueueSize.toString())
                    Text("Este resumen utiliza datos reales de la cola. " +
                        "No certifica BPM ni transiciones medidas.",
                        Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                        style = MaterialTheme.typography.bodySmall,
                        color = neutralMenuInk().copy(alpha = 0.72f))
                }
                SongMenuPage.Actions -> Unit
            }
        }
    }
}

/** Neutral ink like BitChord's floating menu, not the album tint. */
@Composable
private fun neutralMenuInk(): Color =
    if (MaterialTheme.colorScheme.background.luminance() < 0.5f)
        Color.White.copy(alpha = 0.84f)
    else Color(0xFF1A1A1A)

@Composable
private fun SongMenuSeparator() {
    Box(
        Modifier.fillMaxWidth().height(17.dp).padding(horizontal = 18.dp),
        contentAlignment = Alignment.Center,
    ) {
        Box(
            Modifier.fillMaxWidth().height(1.dp)
                .background(neutralMenuInk().copy(alpha = 0.10f)),
        )
    }
}

@Composable
private fun SongInfoValue(label: String, value: String) {
    Column(Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 6.dp)) {
        Text(label, style = MaterialTheme.typography.labelSmall,
            color = neutralMenuInk().copy(alpha = 0.65f))
        Text(value, style = MaterialTheme.typography.bodyMedium,
            color = neutralMenuInk())
    }
}

/** Quick tile: 66dp, a 23dp symbol over a one-line label, as in BitChord. */
@Composable
private fun SongFavoriteQuickAction(
    icon: ImageVector,
    label: String,
    enabled: Boolean,
    selected: Boolean,
    onClick: () -> Unit,
) {
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val ink = neutralMenuInk()
    Row(Modifier.fillMaxWidth().height(66.dp).padding(horizontal = 10.dp)) {
        Column(
            Modifier.weight(1f).fillMaxHeight().clip(RoundedCornerShape(16.dp))
                .background(if (pressed) ink.copy(alpha = 0.10f) else Color.Transparent)
                .clickable(
                    interactionSource = interaction, indication = null,
                    enabled = enabled, onClick = onClick,
                )
                .padding(horizontal = 4.dp, vertical = 8.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            Icon(icon, contentDescription = null,
                tint = ink.copy(alpha = if (enabled) 1f else 0.4f),
                modifier = Modifier.size(23.dp))
            Spacer(Modifier.height(6.dp))
            Text(label, color = ink.copy(alpha = if (enabled) 1f else 0.4f),
                fontSize = 12.sp, lineHeight = 15.sp,
                fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Medium,
                maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
    }
}

/** Matching grouped action row: no ripple, no album-coloured labels. */
@Composable
private fun SongMenuAction(
    icon: ImageVector,
    label: String,
    enabled: Boolean,
    onClick: () -> Unit,
) {
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val ink = neutralMenuInk()
    Row(
        Modifier.fillMaxWidth()
            .padding(horizontal = 8.dp)
            .heightIn(min = 44.dp)
            .clip(RoundedCornerShape(14.dp))
            .background(if (pressed) ink.copy(alpha = 0.10f) else Color.Transparent)
            .clickable(
                interactionSource = interaction,
                indication = null,
                enabled = enabled,
                onClick = onClick,
            )
            .padding(horizontal = 12.dp, vertical = 11.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(Modifier.width(28.dp), contentAlignment = Alignment.Center) {
            Icon(icon, contentDescription = null, modifier = Modifier.size(21.dp),
                tint = ink.copy(alpha = if (enabled) 1f else 0.4f))
        }
        Spacer(Modifier.width(10.dp))
        Text(label, modifier = Modifier.weight(1f),
            style = MaterialTheme.typography.bodyLarge.copy(
                fontSize = 17.sp, lineHeight = 21.sp, letterSpacing = (-0.3).sp,
            ),
            color = ink.copy(alpha = if (enabled) 1f else 0.4f))
    }
}
