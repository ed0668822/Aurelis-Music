// Aurelis © 2026. GPL-3.0-or-later
// TopPicksShelf/ReleaseArt/CardCaption adapted from BitChord b690dc146009d4815825fa4a20815ef2e4ee8898 (GPLv3).
// Original contributors retained. Local songs replace remote releases and station targets.
package com.music.aurelis.ui.components.home

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import com.music.aurelis.R
import com.music.aurelis.data.model.LocalSong
import com.music.aurelis.ui.components.PAGE_GUTTER
import com.music.aurelis.ui.components.SearchPlayingBars
import com.music.aurelis.ui.components.ShelfRow
import com.music.aurelis.ui.components.heldContextMenuInteractions

@Composable
internal fun LocalRecommendationsShelf(
    songs: List<LocalSong>,
    currentSong: LocalSong?,
    isPlaying: Boolean,
    onPlay: (List<LocalSong>, Int) -> Unit,
    onHold: (LocalSong) -> Unit,
) {
    if (songs.isEmpty()) return
    Column(Modifier.testTag("home-recommendations").padding(bottom = 26.dp)) {
        SectionHeader(stringResource(R.string.home_top_picks))
        BoxWithConstraints {
            val width = minOf(maxWidth * 0.60f, 280.dp)
            ShelfRow(contentPadding = PaddingValues(horizontal = PAGE_GUTTER),
                horizontalArrangement = Arrangement.spacedBy(14.dp), modifier = Modifier.testTag("recommendation-row")) {
                itemsIndexed(songs, key = { _, song -> song.id }) { index, song ->
                    val shape = RoundedCornerShape(12.dp)
                    Box(Modifier.width(width).aspectRatio(0.75f).clip(shape)
                        .border(1.dp, MaterialTheme.colorScheme.onSurface.copy(alpha = 0.15f), shape)
                        .background(MaterialTheme.colorScheme.surfaceVariant)
                        .heldContextMenuInteractions({ onPlay(songs, index) }, { onHold(song) })
                        .semantics { role = Role.Button; selected = currentSong?.id == song.id }
                        .testTag("recommendation:${song.id}")) {
                        LocalRecommendationArt(song.artworkUri)
                        if (currentSong?.id == song.id && isPlaying)
                            SearchPlayingBars(Modifier.align(Alignment.Center))
                        Column(Modifier.align(Alignment.BottomStart).fillMaxWidth()
                            .padding(start = 16.dp, end = 16.dp, bottom = 16.dp)) {
                            Text(stringResource(R.string.home_local_pick),
                                style = MaterialTheme.typography.bodyMedium.copy(fontSize = 13.sp, lineHeight = 16.sp),
                                color = Color.White.copy(alpha = 0.62f), maxLines = 1, overflow = TextOverflow.Ellipsis)
                            Text(song.title, style = MaterialTheme.typography.titleMedium.copy(
                                fontSize = 17.sp, lineHeight = 21.sp, fontWeight = FontWeight.SemiBold),
                                color = Color.White, maxLines = 2, overflow = TextOverflow.Ellipsis)
                            if (song.artist.isNotBlank()) Text(song.artist,
                                style = MaterialTheme.typography.bodyMedium.copy(fontSize = 13.sp, lineHeight = 16.sp),
                                color = Color.White.copy(alpha = 0.86f), maxLines = 2, overflow = TextOverflow.Ellipsis)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun BoxScope.LocalRecommendationArt(artwork: String?) {
    Box(Modifier.fillMaxSize().background(Color.Black)) {
        AsyncImage(rememberRecommendationBackdrop(artwork), null, contentScale = ContentScale.FillBounds,
            modifier = Modifier.align(Alignment.BottomCenter).fillMaxWidth().fillMaxHeight(0.42f))
        Box(Modifier.fillMaxSize().background(Brush.verticalGradient(
            0.6f to Color.Black.copy(alpha = 0.08f), 1f to Color.Black.copy(alpha = 0.38f))))
        AsyncImage(artwork, null, contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxWidth().aspectRatio(1f)
                .graphicsLayer { compositingStrategy = CompositingStrategy.Offscreen }
                .drawWithContent {
                    drawContent()
                    drawRect(Brush.verticalGradient(0.78f to Color.Black, 1f to Color.Transparent), blendMode = BlendMode.DstIn)
                })
    }
}
