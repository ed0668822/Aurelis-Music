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
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.ContentCopy
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.music.aurelis.data.DuplicateConfidence
import com.music.aurelis.data.DuplicateGroup
import com.music.aurelis.data.DuplicateTrack
import com.music.aurelis.data.LocalDuplicateScanner
import com.music.aurelis.data.model.Song
import com.music.aurelis.ui.components.lightweightLiquidGlass
import kotlinx.coroutines.CancellationException
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
        try {
            groups = LocalDuplicateScanner.scan(context, songs)
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (_: Exception) {
            groups = emptyList()
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

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        shape = RoundedCornerShape(topStart = 32.dp, topEnd = 32.dp),
        containerColor = MaterialTheme.colorScheme.surface,
    ) {
        LazyColumn(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            item(key = "heading") {
                Column(Modifier.fillMaxWidth().libraryGlass(animate = false).padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        DuplicateGlyph(found = false)
                        Text(
                            if (spanish) "Canciones duplicadas" else "Duplicate songs",
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.SemiBold,
                        )
                    }
                    Spacer(Modifier.height(8.dp))
                    Text(
                        if (spanish) "Solo se pueden eliminar copias idénticas verificadas, con tu permiso."
                        else "Only verified identical files can be deleted, with your permission.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Spacer(Modifier.height(8.dp))
                    FilledTonalButton(onClick = { generation++ }, enabled = !busy && !scanning) {
                        Icon(Icons.Rounded.Refresh, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(8.dp))
                        Text(if (spanish) "Volver a analizar" else "Scan again")
                    }
                }
            }
            item(key = "status") {
                val status = when {
                    scanning || busy -> DuplicateReviewStatus.WORKING
                    error != null -> DuplicateReviewStatus.FAILED
                    groups.isEmpty() -> DuplicateReviewStatus.CLEAR
                    else -> DuplicateReviewStatus.MATCHES
                }
                AnimatedContent(
                    targetState = status,
                    transitionSpec = { fadeIn(tween(230)) togetherWith fadeOut(tween(100)) },
                    label = "duplicate review status",
                ) { state ->
                    Row(
                        Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        if (state == DuplicateReviewStatus.WORKING) {
                            CircularProgressIndicator(modifier = Modifier.size(24.dp), strokeWidth = 2.dp)
                        } else {
                            DuplicateGlyph(found = state == DuplicateReviewStatus.CLEAR)
                        }
                        Text(
                            text = when (state) {
                                DuplicateReviewStatus.WORKING -> if (spanish) "Revisando los archivos…" else "Checking your files…"
                                DuplicateReviewStatus.FAILED -> error.orEmpty()
                                DuplicateReviewStatus.CLEAR -> if (spanish) "No se encontraron duplicados." else "No duplicates found."
                                DuplicateReviewStatus.MATCHES -> if (spanish) "Coincidencias para revisar" else "Matches to review"
                            },
                            style = MaterialTheme.typography.titleSmall,
                            color = if (state == DuplicateReviewStatus.FAILED) MaterialTheme.colorScheme.error
                            else MaterialTheme.colorScheme.onSurface,
                        )
                    }
                }
            }
            items(
                if (scanning) emptyList() else groups,
                key = { group -> group.confidence.name + ":" + group.keep.uri },
            ) { group ->
                DuplicateGroupRow(
                    group = group,
                    spanish = spanish,
                    enabled = !busy && !scanning,
                    onRequestDelete = { track -> confirmation = group.keep to track },
                    modifier = Modifier.animateItem(),
                )
            }
            item(key = "bottom") { Spacer(Modifier.height(20.dp)) }
        }
    }
}

private enum class DuplicateReviewStatus { WORKING, FAILED, CLEAR, MATCHES }

@Composable
private fun DuplicateGlyph(found: Boolean) {
    Box(
        Modifier.size(38.dp).background(MaterialTheme.colorScheme.primaryContainer, CircleShape),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            if (found) Icons.Rounded.Check else Icons.Rounded.ContentCopy,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onPrimaryContainer,
            modifier = Modifier.size(20.dp),
        )
    }
}

/** A quiet moving reflection rendered in the draw phase, with a static option for reduced effects. */
@Composable
internal fun Modifier.libraryGlass(animate: Boolean): Modifier {
    val colors = MaterialTheme.colorScheme
    val shine = if (animate) {
        rememberInfiniteTransition(label = "local music glass").animateFloat(
            initialValue = 0f,
            targetValue = 1f,
            animationSpec = infiniteRepeatable(tween(4600), RepeatMode.Reverse),
            label = "local music reflection",
        )
    } else null
    return lightweightLiquidGlass(RoundedCornerShape(26.dp), colors.surfaceContainerHigh)
        .drawBehind {
            val phase = shine?.value ?: 0.35f
            drawRect(
                Brush.radialGradient(
                    listOf(colors.primary.copy(alpha = 0.18f), Color.Transparent),
                    center = Offset(size.width * (0.15f + phase * 0.65f), 0f),
                    radius = size.width * 0.8f,
                ),
            )
        }
}

@Composable
private fun DuplicateGroupRow(
    group: DuplicateGroup,
    spanish: Boolean,
    enabled: Boolean,
    onRequestDelete: (DuplicateTrack) -> Unit,
    modifier: Modifier = Modifier,
) {
    val exact = group.confidence == DuplicateConfidence.EXACT_BYTES
    Column(modifier.fillMaxWidth().libraryGlass(animate = false).padding(horizontal = 14.dp, vertical = 12.dp)) {
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
