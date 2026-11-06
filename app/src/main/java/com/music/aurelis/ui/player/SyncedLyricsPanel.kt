// Aurelis © 2026. GPL-3.0-or-later
package com.music.aurelis.ui.player

import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.animateScrollBy
import androidx.compose.foundation.gestures.scrollBy
import androidx.compose.foundation.interaction.DragInteraction
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.music.aurelis.data.lyrics.LyricAlignment
import com.music.aurelis.data.lyrics.LyricLine
import com.music.aurelis.playback.PlaybackPosition
import kotlinx.coroutines.flow.collect
import com.music.aurelis.ui.theme.LocalReduceMotion
import kotlin.math.abs

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
    offsetMs: Int = 0,
) {
    if (lines.isEmpty()) {
        Box(modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("Letras no disponibles", style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        return
    }
    val reduceMotion = LocalReduceMotion.current
    val synchronise = isSynced && syncEnabled
    val playhead = rememberLyricsPlayhead(trackKey, position, isPlaying,
        looking && synchronise && !reduceMotion, offsetMs)
    val listState = remember(trackKey) { LazyListState() }
    var following by remember(trackKey) { mutableStateOf(true) }
    var dragging by remember(trackKey) { mutableStateOf(false) }
    var viewportHeight by remember(trackKey) { mutableIntStateOf(0) }
    val density = LocalDensity.current
    LaunchedEffect(listState) {
        listState.interactionSource.interactions.collect { interaction ->
            when (interaction) {
                is DragInteraction.Start -> { dragging = true; following = false }
                is DragInteraction.Stop, is DragInteraction.Cancel -> dragging = false
                else -> Unit
            }
        }
    }
    // Focus changes only at known starts/ends, rather than rescanning all lines on every frame.
    val events = remember(lines) {
        buildSet<Long> { lines.forEach { add(it.timeMs); if (it.hasKnownEnd || it.background?.hasKnownEnd == true) add(it.endMs) } }.sorted().toLongArray()
    }
    val event by remember(events, playhead) { derivedStateOf {
        events.binarySearch(playhead.value).let { if (it >= 0) it else -it - 2 }
    } }
    val active = remember(lines, events, event, synchronise) {
        if (synchronise && event >= 0) activeLyricRows(lines, events[event]) else emptyList()
    }
    val activeIndex = active.lastOrNull() ?: -1
    LaunchedEffect(trackKey, lines, activeIndex, following, looking, synchronise, dragging, viewportHeight, centerFocus, reduceMotion) {
        if (!synchronise || !looking || !following || dragging || activeIndex !in lines.indices || viewportHeight <= 0) return@LaunchedEffect
        if (!centerFocus) {
            if (reduceMotion) listState.scrollToItem(activeIndex) else listState.animateScrollToItem(activeIndex)
        } else {
            withFrameNanos { } // Measure after the padding/viewport update before reading geometry.
            if (listState.layoutInfo.visibleItemsInfo.none { it.index == activeIndex }) listState.scrollToItem(activeIndex)
            withFrameNanos { }
            val layout = listState.layoutInfo
            val item = layout.visibleItemsInfo.firstOrNull { it.index == activeIndex } ?: return@LaunchedEffect
            val delta = item.offset + item.size / 2f - (layout.viewportStartOffset + layout.viewportEndOffset) / 2f
            if (abs(delta) > 0.5f) {
                if (reduceMotion) listState.scrollBy(delta) else listState.animateScrollBy(delta)
            }
        }
    }
    Column(modifier.fillMaxSize()) {
        if (synchronise) TextButton(onClick = { following = !following }) {
            Text(if (following) "Pausar seguimiento" else "Seguir letras")
        } else Text(if (isSynced) "Sincronización desactivada en ajustes" else "Letra sin sincronización",
            style = MaterialTheme.typography.labelMedium)
        val pad = if (centerFocus) with(density) { (viewportHeight / 2f).toDp() } else 0.dp
        LazyColumn(state = listState, contentPadding = PaddingValues(vertical = pad),
            modifier = Modifier.weight(1f).fillMaxWidth().testTag("lyrics-list").onSizeChanged { viewportHeight = it.height }) {
            itemsIndexed(lines) { index, line ->
                val focused = synchronise && index in active
                val style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold,
                    textAlign = if (line.alignment == LyricAlignment.End) TextAlign.End else TextAlign.Start)
                val color = if (focused) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onBackground.copy(alpha = 0.65f)
                Column(Modifier.fillMaxWidth().testTag("lyrics-row-$index")
                    .blur(if (blurUnfocused && synchronise && following && activeIndex >= 0 && !focused) 1.dp else 0.dp)) {
                    val mainModifier = Modifier.fillMaxWidth().testTag("lyrics-line-$index").semantics { selected = focused }
                        .clickable(enabled = isSynced) { following = true; onSeekToLine(line.timeMs) }
                        .padding(vertical = 12.dp, horizontal = 20.dp)
                    if (focused && line.isWordSynced) {
                        SweptLocalLyric(line, playhead, style, color, mainModifier)
                    } else {
                        Text(if (line.isGap && isSynced) "Instrumental" else line.text, style = style,
                            color = color, modifier = mainModifier)
                    }
                    line.background?.takeIf { it.text.isNotBlank() }?.let { backing ->
                        val backingStyle = MaterialTheme.typography.bodyLarge.copy(textAlign = style.textAlign)
                        val backingModifier = Modifier.fillMaxWidth().testTag("lyrics-background-$index")
                            .padding(horizontal = 20.dp).padding(bottom = 8.dp)
                        if (focused && backing.isWordSynced) SweptLocalLyric(backing, playhead, backingStyle, color, backingModifier)
                        else Text(backing.text, style = backingStyle, color = color, modifier = backingModifier)
                    }
                    translatedLines?.getOrNull(index)?.takeIf { it.text.isNotBlank() && it.text != line.text }?.let { translated ->
                        Text(translated.text, style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.75f),
                            modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp).padding(bottom = 8.dp))
                    }
                }
            }
        }
    }
}
