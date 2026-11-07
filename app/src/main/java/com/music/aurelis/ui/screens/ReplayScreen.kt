// Aurelis © 2026. GPL-3.0-or-later
package com.music.aurelis.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.window.DialogProperties
import com.music.aurelis.R
import com.music.aurelis.data.LocalUserLibrary
import com.music.aurelis.data.model.LocalSong
import java.text.DateFormat
import java.util.Date

@Composable
fun ReplayScreen(onBack: () -> Unit = {}, store: LocalUserLibrary, songs: List<LocalSong>, onPlay: (LocalSong) -> Unit) {
    val user by store.state.collectAsState()
    var confirm by remember { mutableStateOf(false) }
    val byId = remember(songs) { songs.associateBy { it.id } }
    Column(Modifier.fillMaxSize().statusBarsPadding().padding(16.dp)) {
        TextButton(onClick = onBack) { Text("Atrás") }
        Text("Replay & Statistics", style = MaterialTheme.typography.titleLarge)
        if (user.readOnly) Text("No se pudo leer el historial guardado.")
        if (user.listening.isEmpty()) Text(stringResource(R.string.navigation_replay_empty))
        else {
            Text("${user.listening.values.sumOf { it.listenedMs } / 60_000} minutos escuchados")
            TextButton(onClick = { confirm = true }, enabled = !user.readOnly) { Text("Borrar historial") }
            LazyColumn(Modifier.weight(1f)) {
                items(user.listening.entries.sortedByDescending { it.value.lastPlayedAt }, key = { it.key }) { (id, stats) ->
                    val song = byId[id]
                    TextButton(onClick = { song?.let(onPlay) }, enabled = song != null) {
                        Column(Modifier.fillMaxWidth()) {
                            Text(song?.title ?: "Canción no disponible")
                            Text("${stats.playCount} reproducciones · ${stats.listenedMs / 1000} s")
                            Text(DateFormat.getDateTimeInstance(DateFormat.SHORT, DateFormat.SHORT).format(Date(stats.lastPlayedAt)))
                        }
                    }
                }
            }
        }
    }
    if (confirm) AlertDialog(modifier = Modifier.width(320.dp), properties = DialogProperties(usePlatformDefaultWidth = false),
        onDismissRequest = { confirm = false }, title = { Text("¿Borrar historial?") },
        text = { Text("Se borrarán las estadísticas de escucha de este perfil. Tus playlists y favoritos se conservarán.") },
        confirmButton = { TextButton(onClick = { store.clearHistory(); confirm = false }) { Text("Borrar historial") } },
        dismissButton = { TextButton(onClick = { confirm = false }) { Text("Cancelar") } })
}
