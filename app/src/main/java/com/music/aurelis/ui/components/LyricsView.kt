// Aurelis © 2026. GPL-3.0-or-later
package com.music.aurelis.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.music.aurelis.data.lyrics.LyricLine

@Composable
fun LyricsView(
    lines: List<LyricLine>,
    currentPositionMs: Long,
    onLineClick: (Long) -> Unit,
    modifier: Modifier = Modifier
) {
    val currentIndex = remember(currentPositionMs, lines) {
        lines.indexOfLast { it.timeMs <= currentPositionMs }.coerceAtLeast(0)
    }

    Box(modifier = modifier.background(Color.Black.copy(alpha = 0.8f))) {
        if (lines.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text(
                    text = "No hay letras disponibles",
                    color = Color.White.copy(alpha = 0.6f),
                    style = MaterialTheme.typography.bodyLarge
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(vertical = 64.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(24.dp)
            ) {
                items(lines.size) { index ->
                    val line = lines[index]
                    val isCurrent = index == currentIndex
                    val textColor = if (isCurrent) Color.White else Color.White.copy(alpha = 0.4f)
                    val scale by animateFloatAsState(targetValue = if (isCurrent) 1.1f else 1.0f)

                    Text(
                        text = line.text,
                        color = textColor,
                        style = MaterialTheme.typography.titleMedium,
                        textAlign = TextAlign.Center,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 32.dp)
                    )
                }
            }
        }
    }
}
