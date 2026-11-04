// Aurelis © 2026. GPL-3.0-or-later
package com.music.aurelis.ui.components

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import com.music.aurelis.data.model.LocalSong
import com.music.aurelis.ui.icons.AurelisIcons

val PlayingAccent = Color(0xFFB4A7F5)

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun SongRow(
    song: LocalSong,
    onClick: () -> Unit,
    onLongPress: () -> Unit,
    isCurrent: Boolean,
    isPlaying: Boolean,
    searchPlayingStyle: Boolean = false,
    activeTint: Color = PlayingAccent,
    onAutoMixClick: (() -> Unit)? = null,
    onLyricsClick: (() -> Unit)? = null,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .combinedClickable(onClick = onClick, onLongClick = onLongPress)
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        AsyncImage(
            model = song.artworkUri,
            contentDescription = null,
            modifier = Modifier
                .size(48.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant),
        )
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(
                text = song.title,
                style = MaterialTheme.typography.titleMedium,
                color = if (isCurrent && searchPlayingStyle) activeTint else MaterialTheme.colorScheme.onBackground,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = song.artist,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
        Spacer(Modifier.width(8.dp))
        if (onAutoMixClick != null || onLyricsClick != null) {
            Row(Modifier.size(32.dp)) {
                if (onAutoMixClick != null) {
                    IconButton(
                        onClick = { onAutoMixClick() },
                        modifier = Modifier.size(32.dp),
                    ) {
                        Icon(
                            imageVector = AurelisIcons.Heart,
                            contentDescription = "AutoMix",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(20.dp),
                        )
                    }
                }
                if (onLyricsClick != null) {
                    IconButton(
                        onClick = { onLyricsClick() },
                        modifier = Modifier.size(32.dp),
                    ) {
                        Icon(
                            imageVector = AurelisIcons.Lyrics,
                            contentDescription = "Ver Letras",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(20.dp),
                        )
                    }
                }
            }
        }
    }
}