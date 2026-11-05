// Aurelis © 2026. GPL-3.0-or-later
package com.music.aurelis.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage

@Composable
fun MiniPlayer(
    title: String,
    artist: String,
    isPlaying: Boolean,
    onPlayPauseClick: () -> Unit,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    artworkUri: String? = null,
    isLoading: Boolean = false,
    enabled: Boolean = true,
    canNext: Boolean = true,
    onNext: () -> Unit = {},
    progress: Float = 0f,
) {
    Surface(modifier = modifier.fillMaxWidth().testTag("mini-player"),
        shape = RoundedCornerShape(20.dp), color = MaterialTheme.colorScheme.surfaceContainerHigh,
        tonalElevation = 4.dp, onClick = onClick) {
        Column {
            Row(Modifier.fillMaxWidth().heightIn(min = 64.dp).padding(start = 8.dp),
                verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.size(40.dp).clip(RoundedCornerShape(8.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant), contentAlignment = Alignment.Center) {
                    Icon(Icons.Rounded.MusicNote, null, Modifier.size(24.dp))
                    AsyncImage(artworkUri, null, Modifier.fillMaxSize())
                }
                Column(Modifier.weight(1f).padding(horizontal = 8.dp)) {
                    Text(title, maxLines = 1, overflow = TextOverflow.Ellipsis, style = MaterialTheme.typography.titleSmall)
                    Text(artist, maxLines = 1, overflow = TextOverflow.Ellipsis, style = MaterialTheme.typography.bodySmall)
                }
                // The full screen exposes previous; keep the compact bar usable at 320dp.
                IconButton(onClick = onPlayPauseClick, enabled = enabled,
                    modifier = Modifier.semantics { contentDescription = if (isPlaying) "Pausar" else "Reproducir" }) {
                    if (isLoading) CircularProgressIndicator(Modifier.size(20.dp))
                    else Icon(if (isPlaying) Icons.Rounded.Pause else Icons.Rounded.PlayArrow,
                        null)
                }
                IconButton(onClick = onNext, enabled = enabled && canNext) {
                    Icon(Icons.Rounded.SkipNext, "Siguiente")
                }
                IconButton(onClick = onClick) { Icon(Icons.Rounded.ExpandLess, "Abrir reproductor") }
            }
            LinearProgressIndicator(progress = { progress.coerceIn(0f, 1f) },
                modifier = Modifier.fillMaxWidth().height(2.dp))
        }
    }
}
