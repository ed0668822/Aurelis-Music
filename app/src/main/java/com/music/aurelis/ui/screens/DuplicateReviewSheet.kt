// SPDX-FileCopyrightText: 2026 Aurelis
// SPDX-License-Identifier: LGPL-3.0-or-later

package com.music.aurelis.ui.screens

import android.app.Activity
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.music.aurelis.R
import com.music.aurelis.data.DuplicateConfidence
import com.music.aurelis.data.DuplicateGroup
import com.music.aurelis.data.DuplicateTrack
import com.music.aurelis.data.LocalDuplicateScanner
import com.music.aurelis.data.model.Song
import kotlinx.coroutines.launch

/** The duplicate review is deliberately read-only unless Android grants a delete request. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DuplicateReviewSheet(
    songs: List<Song>,
    onDismiss: () -> Unit,
    onLibraryChanged: () -> Unit,
) {
    val context = LocalContext.current
    val language = LocalConfiguration.current.locales[0].language
    val spanish = language == "es"
    val scope = rememberCoroutineScope()
    var generation by remember { mutableIntStateOf(0) }
    var groups by remember { mutableStateOf<List<DuplicateGroup>>(emptyList()) }
    var scanning by remember { mutableStateOf(true) }
    var busy by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    var confirmation by remember { mutableStateOf<Pair<DuplicateTrack, DuplicateTrack>?>(null) }

    val deleteLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartIntentSenderForResult(),
    ) { result ->
        busy = false
        if (result.resultCode == Activity.RESULT_OK) {
            onLibraryChanged()
            generation++
        } else {
            error = if (spanish) "Android no eliminó el archivo." else "Android did not delete the file."
        }
    }

    LaunchedEffect(generation, songs) {
        scanning = true
        error = null
        runCatching { LocalDuplicateScanner.scan(context, songs) }
            .onSuccess { groups = it }
            .onFailure {
                error = if (spanish) "No se pudo revisar la música local." else "Could not scan local music."
            }
        scanning = false
    }

    confirmation?.let { pair ->
        val (keep, remove) = pair
        AlertDialog(
            onDismissRequest = { if (!busy) confirmation = null },
            title = {
                Text(if (spanish) "Confirmar eliminación" else "Confirm deletion")
            },
            text = {
                Text(
                    if (spanish) {
                        "Se conservará: ${keep.title} (${keep.format.uppercase()}).\n\n" +
                            "Se eliminará una copia idéntica: ${remove.path ?: remove.uri}.\n\n" +
                            "Las playlists que apuntan a la copia eliminada podrían dejar de encontrarla. " +
                            "Android volverá a solicitar tu permiso."
                    } else {
                        "Keep: ${keep.title} (${keep.format.uppercase()}).\n\n" +
                            "Delete this exact copy: ${remove.path ?: remove.uri}.\n\n" +
                            "Playlists pointing to the removed copy may lose their reference. " +
                            "Android will ask for your permission."
                    },
                )
            },
            confirmButton = {
                Button(
                    enabled = !busy,
                    onClick = {
                        confirmation = null
                        busy = true
                        scope.launch {
                            val verified = LocalDuplicateScanner.stillIdentical(context, keep, remove)
                            if (!verified) {
                                busy = false
                                error = if (spanish) {
                                    "Los archivos cambiaron o no pudieron verificarse. Vuelve a analizar."
                                } else {
                                    "Files changed or could not be verified. Scan again."
                                }
                                return@launch
                            }
                            runCatching {
                                if (Build.VERSION.SDK_INT < Build.VERSION_CODES.R) {
                                    throw IllegalStateException("Requires Android 11")
                                }
                                val request = MediaStore.createDeleteRequest(
                                    context.contentResolver,
                                    listOf(Uri.parse(remove.uri)),
                                )
                                deleteLauncher.launch(IntentSenderRequest.Builder(request.intentSender).build())
                            }.onFailure {
                                busy = false
                                error = if (spanish) {
                                    "Android no pudo solicitar la eliminación."
                                } else {
                                    "Could not request deletion from Android."
                                }
                            }
                        }
                    },
                ) { Text(if (spanish) "Continuar" else "Continue") }
            },
            dismissButton = {
                TextButton(onClick = { confirmation = null }, enabled = !busy) {
                    Text(if (spanish) "Cancelar" else "Cancel")
                }
            },
        )
    }

    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(Modifier.fillMaxWidth().padding(horizontal = 20.dp)) {
            Text(
                if (spanish) "Revisión de duplicados" else "Duplicate review",
                style = MaterialTheme.typography.headlineSmall,
            )
            Spacer(Modifier.height(6.dp))
            Text(
                if (spanish) "Las coincidencias aproximadas nunca se eliminan. " +
                    "Solo puedes borrar copias con hash SHA-256 idéntico, con tu permiso."
                else "Possible matches are never deleted. Only full SHA-256 verified copies " +
                    "can be removed with your permission.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.height(12.dp))
            OutlinedButton(
                onClick = { generation++ },
                enabled = !busy && !scanning,
            ) {
                Text(if (spanish) "Volver a analizar" else "Scan again")
            }
            error?.let {
                Text(it, color = MaterialTheme.colorScheme.error, modifier = Modifier.padding(vertical = 8.dp))
            }
            if (scanning || busy) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 24.dp),
                    horizontalArrangement = Arrangement.Center,
                ) {
                    CircularProgressIndicator()
                }
            }
            if (!scanning && groups.isEmpty() && error == null) {
                Text(
                    if (spanish) "No se encontraron duplicados entre los archivos disponibles."
                    else "No duplicates found among the available files.",
                    modifier = Modifier.padding(vertical = 16.dp),
                )
            }
            LazyColumn {
                items(
                    groups,
                    key = { group -> group.confidence.name + ":" + group.keep.uri },
                ) { group ->
                    DuplicateGroupRow(
                        group = group,
                        spanish = spanish,
                        enabled = !busy && !scanning,
                        onRequestDelete = { track -> confirmation = group.keep to track },
                    )
                    HorizontalDivider()
                }
                item { Spacer(Modifier.height(30.dp)) }
            }
        }
    }
}

@Composable
private fun DuplicateGroupRow(
    group: DuplicateGroup,
    spanish: Boolean,
    enabled: Boolean,
    onRequestDelete: (DuplicateTrack) -> Unit,
) {
    val exact = group.confidence == DuplicateConfidence.EXACT_BYTES
    Column(Modifier.fillMaxWidth().padding(vertical = 14.dp)) {
        Text(group.keep.title, style = MaterialTheme.typography.titleMedium, maxLines = 2)
        Text(group.keep.artist, style = MaterialTheme.typography.bodySmall)
        Text(
            if (exact) {
                if (spanish) "Copias idénticas verificadas" else "Verified identical copies"
            } else {
                if (spanish) "Posible duplicado · solo revisión" else "Possible duplicate · review only"
            },
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.padding(top = 6.dp, bottom = 4.dp),
        )
        for (track in group.tracks) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(vertical = 5.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                Column(Modifier.weight(1f)) {
                    Text(
                        track.path?.substringAfterLast('/') ?: track.uri.substringAfterLast('/'),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = if (track.uri == group.keep.uri) FontWeight.SemiBold else FontWeight.Normal,
                    )
                    Text(
                        "${track.format.uppercase()} · " +
                            (track.bytes?.let { "${it / 1_048_576L} MB" }
                                ?: if (spanish) "Tamaño desconocido" else "Unknown size"),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                if (track.uri == group.keep.uri) {
                    Text(
                        if (spanish) "Conservar" else "Keep",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.primary,
                    )
                } else if (exact && Build.VERSION.SDK_INT >= Build.VERSION_CODES.R &&
                    track.uri.startsWith("content://media/")
                ) {
                    TextButton(onClick = { onRequestDelete(track) }, enabled = enabled) {
                        Text(if (spanish) "Eliminar" else "Delete")
                    }
                }
            }
        }
    }
}
