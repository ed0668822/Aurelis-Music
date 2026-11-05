package com.music.aurelis.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.DialogProperties
import com.music.aurelis.data.LocalUserLibrary
import com.music.aurelis.data.model.LocalSong

@Composable
fun SongActionsDialog(song: LocalSong, store: LocalUserLibrary, onEnqueue: () -> Unit, onDismiss: () -> Unit) {
    val user by store.state.collectAsState()
    var name by remember { mutableStateOf("") }
    AlertDialog(modifier = Modifier.width(320.dp), properties = DialogProperties(usePlatformDefaultWidth = false),
        onDismissRequest = onDismiss, title = { Text(song.title) }, text = {
            Column(Modifier.heightIn(max = 380.dp).verticalScroll(rememberScrollState())) {
                TextButton(onClick = { store.toggleFavorite(song.id) }, enabled = !user.readOnly) {
                    Text(if (song.id in user.favorites) "Quitar de favoritos" else "Añadir a favoritos")
                }
                TextButton(onClick = { onEnqueue(); onDismiss() }) { Text("Añadir a la cola") }
                user.playlists.forEach { playlist ->
                    TextButton(onClick = { store.setPlaylistSongs(playlist.id, playlist.songIds + song.id); onDismiss() }, enabled = !user.readOnly) {
                        Text("Añadir a ${playlist.name}")
                    }
                }
                OutlinedTextField(name, { name = it.take(100) }, label = { Text("Nueva playlist") }, singleLine = true)
                TextButton(onClick = { store.createPlaylist(name, listOf(song.id)); onDismiss() }, enabled = !user.readOnly && name.isNotBlank()) {
                    Text("Crear playlist con esta canción")
                }
            }
        }, confirmButton = { TextButton(onClick = onDismiss) { Text("Cerrar") } })
}
