package com.music.aurelis.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.music.aurelis.data.LocalUserLibrary
import com.music.aurelis.data.model.LocalSong

@Composable
fun SongActionsDialog(
    song: LocalSong, store: LocalUserLibrary, onEnqueue: () -> Unit, onDismiss: () -> Unit,
    onPlayNext: () -> Unit, queueActionsEnabled: Boolean = true, playNextEnabled: Boolean = true,
) {
    val user by store.state.collectAsState()
    var name by remember(song.id) { mutableStateOf("") }
    HeldContextMenu(onDismiss) {
        Text(song.title, style = MaterialTheme.typography.titleLarge, modifier = Modifier.padding(8.dp))
        Text(song.artist, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.padding(horizontal = 8.dp))
        HorizontalDivider(Modifier.padding(vertical = 8.dp))
        TextButton(onClick = { store.toggleFavorite(song.id) }, enabled = !user.readOnly) {
            Text(if (song.id in user.favorites) "Quitar de favoritos" else "Añadir a favoritos")
        }
        TextButton(onClick = { onPlayNext(); onDismiss() }, enabled = queueActionsEnabled && playNextEnabled) {
            Text("Reproducir después")
        }
        if (queueActionsEnabled && !playNextEnabled) {
            Text("Desactiva el modo aleatorio para elegir la siguiente canción.", modifier = Modifier.padding(horizontal = 8.dp))
        }
        TextButton(onClick = { onEnqueue(); onDismiss() }, enabled = queueActionsEnabled) { Text("Añadir a la cola") }
        user.playlists.forEach { playlist ->
            TextButton(onClick = { store.setPlaylistSongs(playlist.id, playlist.songIds + song.id); onDismiss() }, enabled = !user.readOnly) {
                Text("Añadir a ${playlist.name}")
            }
        }
        OutlinedTextField(name, { name = it.take(100) }, label = { Text("Nueva playlist") }, singleLine = true,
            enabled = !user.readOnly, modifier = Modifier.fillMaxWidth())
        TextButton(onClick = { store.createPlaylist(name, listOf(song.id)); onDismiss() }, enabled = !user.readOnly && name.isNotBlank()) {
            Text("Crear playlist con esta canción")
        }
        TextButton(onClick = onDismiss) { Text("Cerrar") }
    }
}
