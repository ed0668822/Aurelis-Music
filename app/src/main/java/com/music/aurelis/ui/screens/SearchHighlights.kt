// Aurelis © 2026. GPL-3.0-or-later
// Adapted from BitChord b690dc146009d4815825fa4a20815ef2e4ee8898 (GPLv3).
// Original contributors retained; Android resources, local callbacks and image bounds adapted.
package com.music.aurelis.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.PlaylistAdd
import androidx.compose.material.icons.rounded.MoreVert
import androidx.compose.material.icons.rounded.NorthWest
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import coil3.request.ImageRequest
import com.music.aurelis.R
import com.music.aurelis.data.model.LocalSong
import com.music.aurelis.ui.components.PAGE_GUTTER
import com.music.aurelis.ui.components.heldContextMenuInteractions

@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun LocalTopResultCard(song: LocalSong, onPlay: () -> Unit, onPlaylist: () -> Unit, onLongPress: () -> Unit) {
    val context = LocalContext.current
    val image = remember(context, song.artworkUri) {
        ImageRequest.Builder(context).data(song.artworkUri).size(144).build()
    }
    Column(Modifier.fillMaxWidth().testTag("search-top-result:${song.id}")
        .background(MaterialTheme.colorScheme.background)
        .padding(start = PAGE_GUTTER, end = PAGE_GUTTER, top = 18.dp, bottom = 8.dp)
        .heldContextMenuInteractions(onClick = onPlay, onLongClick = onLongPress)) {
        Text(stringResource(R.string.local_search_top_result), style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurface)
        Spacer(Modifier.height(10.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            AsyncImage(image, null, contentScale = ContentScale.Crop,
                modifier = Modifier.size(72.dp).clip(RoundedCornerShape(10.dp)).testTag("search-top-art"))
            Spacer(Modifier.width(14.dp))
            Column(Modifier.weight(1f)) {
                Text(song.title, style = MaterialTheme.typography.titleLarge, color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 2, overflow = TextOverflow.Ellipsis)
                Spacer(Modifier.height(4.dp))
                Text(song.artist, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
            Box(Modifier.size(36.dp).clip(CircleShape).clickable(onClick = onLongPress)
                .testTag("search-top-actions"), contentAlignment = Alignment.Center) {
                Icon(Icons.Rounded.MoreVert, stringResource(R.string.more), Modifier.size(24.dp), MaterialTheme.colorScheme.onSurface)
            }
        }
        Spacer(Modifier.height(12.dp))
        FlowRow(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedButton(onClick = onPlay, modifier = Modifier.testTag("search-top-play"),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.onSurface)) {
                Icon(Icons.Rounded.PlayArrow, null)
                Spacer(Modifier.width(6.dp))
                Text(stringResource(R.string.play))
            }
            OutlinedButton(onClick = onPlaylist, modifier = Modifier.testTag("search-top-playlist"),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.onSurface)) {
                Icon(Icons.AutoMirrored.Rounded.PlaylistAdd, null)
                Spacer(Modifier.width(6.dp))
                Text(stringResource(R.string.local_search_playlist_action))
            }
        }
    }
}

internal fun LazyListScope.localSearchSuggestions(suggestions: List<String>, onSearch: (String) -> Unit, onFill: (String) -> Unit) {
    item(key = "suggestions:top-inset") { Spacer(Modifier.height(8.dp)) }
    itemsIndexed(suggestions.drop(1).take(3), key = { _, term -> "suggest:$term" }) { _, term ->
        Row(Modifier.fillMaxWidth().testTag("search-suggestion:$term").clickable { onSearch(term) }
            .padding(start = PAGE_GUTTER, end = 8.dp, top = 6.dp, bottom = 6.dp),
            verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Rounded.Search, null, Modifier.size(22.dp), MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(Modifier.width(16.dp))
            Text(term, Modifier.weight(1f), style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onBackground, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Box(Modifier.size(40.dp).clip(CircleShape).clickable { onFill(term) }, contentAlignment = Alignment.Center) {
                Icon(Icons.Rounded.NorthWest, stringResource(R.string.local_search_edit_suggestion, term),
                    Modifier.size(18.dp), MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
    item(key = "suggestions:divider") {
        HorizontalDivider(Modifier.padding(horizontal = PAGE_GUTTER), thickness = 0.5.dp,
            color = MaterialTheme.colorScheme.outline.copy(alpha = 0.4f))
    }
}
