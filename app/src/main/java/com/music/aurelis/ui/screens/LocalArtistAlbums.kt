// SPDX-FileCopyrightText: 2026 Aurelis
// SPDX-License-Identifier: LGPL-3.0-or-later
package com.music.aurelis.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import com.music.aurelis.R
import com.music.aurelis.data.model.LocalAlbum
import com.music.aurelis.data.model.LocalArtist
import com.music.aurelis.ui.icons.AurelisIcons

/** Only catalogue albums that actually contain a currently indexed song from this artist. */
internal fun localArtistAlbums(artist: LocalArtist, albums: List<LocalAlbum>): List<LocalAlbum> {
    val ids = artist.songs.mapTo(HashSet()) { it.id }
    return albums.filter { album -> album.songs.any { it.id in ids } }
        .distinctBy { it.key }
}

@Composable
internal fun LocalArtistAlbumShelf(albums: List<LocalAlbum>, onOpen: (LocalAlbum) -> Unit) {
    Column(Modifier.fillMaxWidth().padding(vertical = 12.dp)
        .testTag("artist-album-shelf")) {
        Text(stringResource(R.string.albums),
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp))
        LazyRow(contentPadding = PaddingValues(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            items(albums, key = { it.key }) { album ->
                Column(Modifier.width(136.dp).clickable { onOpen(album) }
                    .testTag("artist-album:" + album.key)) {
                    Box(Modifier.fillMaxWidth().aspectRatio(1f).clip(RoundedCornerShape(12.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant),
                        contentAlignment = Alignment.Center) {
                        Icon(AurelisIcons.Album, null, Modifier.size(34.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant)
                        if (album.artworkUri != null) {
                            AsyncImage(album.artworkUri, null, Modifier.fillMaxSize(),
                                contentScale = ContentScale.Crop)
                        }
                    }
                    Text(album.name, style = MaterialTheme.typography.titleSmall,
                        maxLines = 2, overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.padding(top = 8.dp))
                    Text(album.artist, style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
            }
        }
    }
}
