// Aurelis © 2026. GPL-3.0-or-later
// Library previews adapted from BitChord b690dc1 (GPLv3); attribution in NOTICE.
package com.music.aurelis.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import com.music.aurelis.data.LocalPlaylist
import com.music.aurelis.data.model.LocalSong
import com.music.aurelis.ui.components.SHELF_CARD_WIDTH
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyGridState
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import com.music.aurelis.R
import com.music.aurelis.data.model.LocalAlbum
import com.music.aurelis.ui.components.LIBRARY_GRID_SPACING
import com.music.aurelis.ui.components.PAGE_GUTTER
import com.music.aurelis.ui.icons.AurelisIcons

data class LibraryLink(val title: String, val icon: ImageVector, val onClick: () -> Unit)

@Composable
fun LibraryScreen(
    listState: LazyListState,
    links: List<LibraryLink>,
    onShowAll: (String) -> Unit,
    modifier: Modifier = Modifier,
    contentPadding: PaddingValues,
    showTitle: Boolean = true,
    replay: @Composable () -> Unit = {},
    albums: List<LocalAlbum> = emptyList(),
    playlists: List<LocalPlaylist> = emptyList(),
    songs: List<LocalSong> = emptyList(),
    onAlbumOpen: (LocalAlbum) -> Unit,
    onPlaylistOpen: (String) -> Unit,
    onNewPlaylist: () -> Unit,
    canCreatePlaylist: Boolean = true,
) {
    val songsById = remember(songs) { songs.associateBy { it.id } }
    LazyColumn(
        state = listState,
        modifier = modifier.fillMaxSize().testTag("library-page"),
        contentPadding = contentPadding,
    ) {
        if (showTitle) {
            item {
                Text(
                    text = stringResource(R.string.library),
                    style = MaterialTheme.typography.displayLarge,
                    color = MaterialTheme.colorScheme.onBackground,
                    modifier = Modifier.padding(horizontal = PAGE_GUTTER, vertical = 8.dp),
                )
            }
        }
        item(key = "replay") { replay() }
        if (links.isNotEmpty()) {
            item(key = "links") { LibraryLinkList(links = links) }
        }
        if (albums.isNotEmpty()) item(key = "on-device") {
            LocalLibraryShelf(stringResource(R.string.on_device), "albums",
                if (albums.size > 5) ({ onShowAll("albums") }) else null) {
                items(albums.take(5), key = { it.key }) { album ->
                    LibraryPreviewCard(album.name, album.artist, album.artworkUri,
                        AurelisIcons.Album, { onAlbumOpen(album) })
                }
            }
        }
        item(key = "playlists") {
            LocalLibraryShelf(stringResource(R.string.your_playlists), "playlists",
                if (playlists.size + 1 > 5) ({ onShowAll("playlists") }) else null) {
                item(key = "new") {
                    LibraryPreviewCard(stringResource(R.string.new_playlist),
                        stringResource(R.string.library_playlist_local_hint), null,
                        Icons.Rounded.Add, onNewPlaylist, canCreatePlaylist, newPlaylist = true)
                }
                items(playlists.take(4), key = { it.id }) { playlist ->
                    val artwork = playlist.songIds.firstNotNullOfOrNull { songsById[it]?.artworkUri }
                    LibraryPreviewCard(playlist.name, "", artwork, AurelisIcons.Playlist,
                        { onPlaylistOpen(playlist.id) })
                }
            }
        }
    }
}

/** The available local aggregate, labelled all-time rather than an invented period. */
@Composable
fun LibraryReplayPreview(listenedMs: Long, holder: String, onOpen: () -> Unit) {
    if (listenedMs <= 0L) return
    val replayLabel = stringResource(R.string.replay)
    LazyRow(Modifier.padding(vertical = 6.dp), contentPadding = PaddingValues(horizontal = PAGE_GUTTER),
        horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        item(key = "all-time") {
            com.music.aurelis.ui.replay.ReplayCreditCard(
                label = stringResource(R.string.minutes_listened),
                value = com.music.aurelis.ui.replay.formatMinutes(listenedMs),
                detail = stringResource(R.string.all_time), holder = holder, memberSince = null,
                onClick = onOpen,
                modifier = Modifier.width(300.dp).semantics { contentDescription = replayLabel })
        }
    }
}

@Composable
private fun LocalLibraryShelf(title: String, key: String, onShowAll: (() -> Unit)?, content: LazyListScope.() -> Unit) {
    Column(Modifier.padding(bottom = 26.dp).testTag("library-shelf-$key")) {
        Row(Modifier.padding(horizontal = PAGE_GUTTER, vertical = 10.dp).fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically) {
            Text(title, style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onBackground, maxLines = 2,
                overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f))
            if (onShowAll != null) Text(stringResource(R.string.show_all),
                style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.clickable(onClick = onShowAll).padding(start = 12.dp, top = 10.dp, bottom = 10.dp))
        }
        LazyRow(contentPadding = PaddingValues(horizontal = PAGE_GUTTER),
            horizontalArrangement = Arrangement.spacedBy(LIBRARY_GRID_SPACING),
            modifier = Modifier.testTag("library-row-$key"), content = content)
    }
}

@Composable
private fun LibraryPreviewCard(title: String, subtitle: String, artworkUri: String?, icon: ImageVector,
    onClick: () -> Unit, enabled: Boolean = true, newPlaylist: Boolean = false) {
    Column(Modifier.width(SHELF_CARD_WIDTH).clickable(enabled = enabled, onClick = onClick)) {
        Box(Modifier.fillMaxWidth().aspectRatio(1f).clip(RoundedCornerShape(12.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant), contentAlignment = Alignment.Center) {
            if (artworkUri != null) AsyncImage(artworkUri, null, Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
            else Icon(icon, null, Modifier.size(if (newPlaylist) 34.dp else 40.dp), tint = MaterialTheme.colorScheme.primary)
        }
        Spacer(Modifier.height(10.dp))
        Text(title, style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onBackground,
            maxLines = 1, overflow = TextOverflow.Ellipsis)
        if (subtitle.isNotBlank()) Text(subtitle, style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1, overflow = TextOverflow.Ellipsis)
    }
}

@Composable
private fun LibraryLinkList(links: List<LibraryLink>) {
    Column(Modifier.padding(top = 4.dp, bottom = 22.dp)) {
        links.forEachIndexed { index, link ->
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 54.dp)
                    .clickable { link.onClick() }
                    .padding(horizontal = PAGE_GUTTER),
            ) {
                Icon(
                    imageVector = link.icon,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(24.dp),
                )
                Spacer(Modifier.width(16.dp))
                Text(
                    text = link.title,
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onBackground,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f),
                )
                Icon(
                    imageVector = AurelisIcons.ChevronRight,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = .6f),
                    modifier = Modifier.size(20.dp),
                )
            }
            if (index < links.lastIndex) {
                HorizontalDivider(
                    modifier = Modifier.padding(start = PAGE_GUTTER + 24.dp + 16.dp),
                    thickness = 0.5.dp,
                    color = MaterialTheme.colorScheme.outline,
                )
            }
        }
    }
}

@Composable
fun LibraryGridPage(
    shelf: LocalAlbum,
    gridState: LazyGridState,
    onAlbumClick: (LocalAlbum) -> Unit,
    contentPadding: PaddingValues,
    modifier: Modifier = Modifier,
) {
    LazyVerticalGrid(
        columns = GridCells.Adaptive(140.dp),
        state = gridState,
        contentPadding = contentPadding,
        horizontalArrangement = Arrangement.spacedBy(LIBRARY_GRID_SPACING),
        verticalArrangement = Arrangement.spacedBy(20.dp),
        modifier = modifier.fillMaxSize().padding(horizontal = PAGE_GUTTER),
    ) {
        items(1, key = { shelf.key }) {
            AlbumGridCard(album = shelf, onClick = { onAlbumClick(shelf) })
        }
    }
}

@Composable
private fun AlbumGridCard(
    album: LocalAlbum,
    onClick: () -> Unit,
) {
    Column(Modifier.clickable(onClick = onClick)) {
        Box(
            Modifier
                .fillMaxWidth()
                .aspectRatio(1f)
                .clip(RoundedCornerShape(12.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant),
        ) {
            val artwork = album.artworkUri
            if (artwork != null) {
                AsyncImage(
                    model = artwork,
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize(),
                )
            }
        }
        Text(
            text = album.name,
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onBackground,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.padding(top = 10.dp),
        )
        Text(
            text = album.artist,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}
