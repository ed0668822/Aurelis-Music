// Aurelis © 2026. GPL-3.0-or-later.
package com.music.aurelis.ui.player

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.scrollToItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
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
    centerFocus: Boolean = false,
    modifier: Modifier = Modifier,
) {
    if (lines.isEmpty()) {
        androidx.compose.foundation.layout.Box(modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text(
                text = "Letras no disponibles",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        return
    }

    val listState = remember { LazyListState() }

    // Línea activa: la última cuyo timeMs <= posición actual
    val activeIndex by derivedStateOf {
        var idx = -1
        for (i in lines.indices) {
            if (lines[i].timeMs <= position.positionMs) {
                idx = i
            } else {
                break
            }
        }
        idx
    }

    // Scroll automático a la línea activa cuando cambia
    val lastReportedIndex by rememberUpdatedState(activeIndex)
    LaunchedEffect(lastReportedIndex) {
        if (lastReportedIndex >= 0) {
            listState.animateScrollToItem(lastReportedIndex)
        }
    }

    LazyColumn(
        state = listState,
        modifier = modifier.fillMaxSize(),
    ) {
        itemsIndexed(lines) { index, line ->
            val isActive = index == activeIndex
            Text(
                text = line.text,
                style = MaterialTheme.typography.bodyLarge,
                color = if (isActive) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onBackground,
                fontWeight = if (isActive) androidx.compose.ui.text.font.FontWeight.Bold else androidx.compose.ui.text.font.FontWeight.Normal,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp)
                    .clickable { onSeekToLine(line.timeMs) },
            )
        }
    }
}
