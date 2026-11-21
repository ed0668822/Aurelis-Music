// SPDX-FileCopyrightText: 2026 Aurelis
// SPDX-License-Identifier: LGPL-3.0-or-later

package com.music.aurelis.ui.screens

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.music.aurelis.data.LocalPlaylistM3uFiles
import com.music.aurelis.data.OfflinePlaylist
import com.music.aurelis.data.OfflinePlaylists
import kotlinx.coroutines.launch

/**
 * B05/B06: SAF picker, conflict/missing-file preview and portable M3U8 export.
 * The user's music is never touched; confirmation creates an offline playlist.
 */
@Composable
internal fun PlaylistFileActions() {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val available by OfflinePlaylists.playlists.collectAsStateWithLifecycle()
    var preview by remember { mutableStateOf<LocalPlaylistM3uFiles.Preview?>(null) }
    var choosingExport by remember { mutableStateOf(false) }
    var toExport by remember { mutableStateOf<OfflinePlaylist?>(null) }
    var notice by remember { mutableStateOf<String?>(null) }
    var busy by remember { mutableStateOf(false) }

    val importPicker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) {
            scope.launch {
                busy = true
                runCatching { LocalPlaylistM3uFiles.read(context, uri) }
                    .onSuccess { preview = it }
                    .onFailure { notice = "Cannot import playlist: " + (it.message ?: "invalid file") }
                busy = false
            }
        }
    }
    val exportPicker = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("audio/x-mpegurl")
    ) { uri ->
        val selected = toExport
        toExport = null
        if (uri != null && selected != null) {
            scope.launch {
                busy = true
                runCatching { LocalPlaylistM3uFiles.write(context, uri, selected.songs) }
                    .onSuccess { notice = "Exported " + selected.songs.size + " tracks." }
                    .onFailure { notice = "Cannot export playlist: " + (it.message ?: "write failed") }
                busy = false
            }
        }
    }

    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 18.dp, vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        TextButton(enabled = !busy, onClick = {
            importPicker.launch(arrayOf("audio/x-mpegurl", "audio/mpegurl", "text/plain", "application/octet-stream"))
        }) { Text("Import M3U/M3U8") }
        TextButton(enabled = !busy, onClick = { choosingExport = true }) { Text("Export M3U8") }
    }
    if (choosingExport) {
        AlertDialog(
            onDismissRequest = { choosingExport = false },
            title = { Text("Export local playlist") },
            text = {
                Column {
                    available.forEach { playlist ->
                        TextButton(onClick = {
                            choosingExport = false
                            toExport = playlist
                            val safe = playlist.title.replace(Regex("[^a-zA-Z0-9 _.-]"), "_").take(60)
                                .ifBlank { "Aurelis playlist" }
                            exportPicker.launch(safe + ".m3u8")
                        }) { Text(playlist.title + " (" + playlist.songs.size + ")") }
                    }
                }
            },
            confirmButton = { TextButton(onClick = { choosingExport = false }) { Text("Close") } },
        )
    }
    preview?.let { pending ->
        val result = pending.result
        AlertDialog(
            onDismissRequest = { preview = null },
            title = { Text("Import " + pending.title + "?") },
            text = {
                Column {
                    Text("Found: " + result.songs.size + " local tracks")
                    Text("Missing: " + result.missing.size +
                        " · Ambiguous: " + result.ambiguous.size +
                        " · Repeated: " + result.repeated)
                    (result.missing + result.ambiguous).take(5).forEach { path ->
                        Text(path.take(120), style = MaterialTheme.typography.bodySmall)
                    }
                    if (result.missing.isNotEmpty() || result.ambiguous.isNotEmpty()) {
                        Text("Missing or ambiguous tracks are not added. No files will be deleted.")
                    }
                }
            },
            confirmButton = {
                TextButton(enabled = result.songs.isNotEmpty(), onClick = {
                    OfflinePlaylists.create(pending.title, result.songs)
                    notice = "Imported " + result.songs.size + " tracks into " + pending.title
                    preview = null
                }) { Text("Import " + result.songs.size + " tracks") }
            },
            dismissButton = { TextButton(onClick = { preview = null }) { Text("Cancel") } },
        )
    }
    notice?.let { message ->
        AlertDialog(
            onDismissRequest = { notice = null },
            title = { Text("Playlist") },
            text = { Text(message) },
            confirmButton = { TextButton(onClick = { notice = null }) { Text("OK") } },
        )
    }
}
