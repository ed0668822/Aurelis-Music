// Aurelis © 2026. GPL-3.0-or-later
package com.music.aurelis.ui.player

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

@Composable
fun FullScreenPlayerHost(
    title: String,
    artist: String,
    isPlaying: Boolean,
    progress: Float,
    onPlayPauseClick: () -> Unit,
    onSeek: (Float) -> Unit,
    onClose: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(Color(0xFF1E1E2C), Color(0xFF0F0F17))
                )
            )
            .padding(24.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.SpaceBetween,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onClose) {
                    Icon(
                        painter = androidx.compose.ui.res.painterResource(id = android.R.drawable.ic_menu_close_clear_cancel),
                        contentDescription = "Cerrar",
                        tint = Color.White
                    )
                }
                Text(
                    text = "Reproduciendo ahora",
                    style = MaterialTheme.typography.titleMedium,
                    color = Color.White
                )
                Spacer(modifier = Modifier.width(48.dp))
            }

            Box(
                modifier = Modifier
                    .size(280.dp)
                    .clip(CircleShape)
                    .background(Color.DarkGray),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = title.take(2).uppercase(),
                    style = MaterialTheme.typography.displayLarge,
                    color = Color.White
                )
            }

            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.headlineSmall,
                    color = Color.White
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = artist,
                    style = MaterialTheme.typography.bodyLarge,
                    color = Color.Gray
                )
                Spacer(modifier = Modifier.height(24.dp))

                Slider(
                    value = progress,
                    onValueChange = onSeek,
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(16.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(
                        onClick = onPlayPauseClick,
                        modifier = Modifier
                            .size(64.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.primary)
                    ) {
                        Icon(
                            painter = androidx.compose.ui.res.painterResource(
                                id = if (isPlaying) android.R.drawable.ic_media_pause else android.R.drawable.ic_media_play
                            ),
                            contentDescription = "Play/Pause",
                            tint = Color.White
                        )
                    }
                }
            }
        }
    }
}
