// Aurelis © 2026. GPL-3.0-or-later
package com.music.aurelis.ui.player

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.interaction.collectIsDraggedAsState
import androidx.compose.material3.TextButton
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.selected
import com.music.aurelis.data.lyrics.Lyrics
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.draw.blur
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.music.aurelis.data.lyrics.LyricLine
import com.music.aurelis.playback.PlaybackPosition

@Composable
fun SyncedLyricsPanel(
    lines: List<LyricLine>,
    trackKey: String,
    position: PlaybackPosition,
    looking: Boolean,
    isPlaying: Boolean,
    onSeekToLine: (Long) -> Unit,
    modifier: Modifier = Modifier,
    centerFocus: Boolean = false,
    isSynced: Boolean = true,
    translatedLines: List<LyricLine>? = null,
    syncEnabled: Boolean = true,
    blurUnfocused: Boolean = false,
) {
    if (lines.isEmpty()) {
        Box(modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text(
                text = "Letras no disponibles",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        return
    }

    val listState = remember(trackKey) { LazyListState() }
    var following by remember(trackKey) { mutableStateOf(true) }
    val dragging by listState.interactionSource.collectIsDraggedAsState()
    LaunchedEffect(dragging) { if (dragging) following = false }
    val synchronise = isSynced && syncEnabled
    val lyrics = remember(lines, synchronise) { Lyrics(lines, synchronise) }
    val activeIndex = lyrics.activeIndexAt(position.positionMs)
    LaunchedEffect(trackKey, activeIndex, following, looking) {
        if (synchronise && looking && following && !dragging && activeIndex >= 0) listState.animateScrollToItem(activeIndex)
    }
    Column(modifier.fillMaxSize()) {
        if (synchronise) TextButton(onClick = { following = !following }) { Text(if (following) "Pausar seguimiento" else "Seguir letras") }
        else Text(if (isSynced) "Sincronización desactivada en ajustes" else "Letra sin sincronización", style = MaterialTheme.typography.labelMedium)
        LazyColumn(state = listState, modifier = Modifier.weight(1f).fillMaxWidth()) {
            itemsIndexed(lines) { index, line ->
                val isActive = synchronise && index == activeIndex
                Text(text = line.text, style = MaterialTheme.typography.bodyLarge,
                    color = if (isActive) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onBackground,
                    fontWeight = if (isActive) FontWeight.Bold else FontWeight.Normal,
                    modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp, horizontal = 16.dp)
                        .blur(if (blurUnfocused && synchronise && following && activeIndex >= 0 && !isActive) 1.dp else 0.dp)
                        .semantics { selected = isActive }
                        .clickable(enabled = isSynced) { onSeekToLine(line.timeMs) })
                translatedLines?.getOrNull(index)?.takeIf { it.text.isNotBlank() && it.text != line.text }?.let { translated ->
                    Text(translated.text, style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.75f),
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp).padding(bottom = 8.dp))
                }
            }
        }
    }
}
