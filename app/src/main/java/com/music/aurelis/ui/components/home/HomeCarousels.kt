// Aurelis © 2026. GPL-3.0-or-later.
package com.music.aurelis.ui.components.home

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.Text
import androidx.compose.runtime.remember
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import com.music.aurelis.data.model.LocalAlbum
import com.music.aurelis.data.model.LocalArtist
import com.music.aurelis.data.model.LocalSong
import com.music.aurelis.ui.components.*
import com.music.aurelis.ui.theme.rememberArtworkPalette

@Composable
fun SectionHeader(title: String, modifier: Modifier = Modifier) {
    Text(
        text = title,
        style = MaterialTheme.typography.titleLarge,
        fontWeight = FontWeight.Bold,
        color = MaterialTheme.colorScheme.onSurface,
        modifier = modifier.padding(horizontal = 20.dp, vertical = 12.dp),
    )
}

@Composable
fun SongCarousel(
    title: String,
    songs: List<LocalSong>,
    currentSongId: String?,
    isPlaying: Boolean,
    onSongLongPress: ((LocalSong) -> Unit)? = null,
    onSongClick: (index: Int) -> Unit,
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        SectionHeader(title)
        LazyRow(
            contentPadding = PaddingValues(horizontal = 20.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            itemsIndexed(songs, key = { _, s -> s.id }) { index, song ->
                val isCurrent = currentSongId == song.id
                SongCard(
                    song = song,
                    isCurrent = isCurrent,
                    isPlaying = isCurrent && isPlaying,
                    onLongPress = onSongLongPress?.let { { it(song) } },
                    onClick = { onSongClick(index) },
                )
            }
        }
    }
}

@Composable
fun SongCard(
    song: LocalSong,
    isCurrent: Boolean,
    isPlaying: Boolean,
    onLongPress: (() -> Unit)? = null,
    onClick: () -> Unit,
) {
    Column(Modifier.width(SHELF_CARD_WIDTH).then(if (onLongPress != null)
        Modifier.heldContextMenuInteractions(onClick, onLongPress) else Modifier.clickable(onClick = onClick))) {
        Box(
            modifier = Modifier.size(SHELF_CARD_WIDTH)
                .clip(RoundedCornerShape(12.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant),
            contentAlignment = Alignment.Center,
        ) {
            if (song.artworkUri != null) {
                AsyncImage(
                    model = song.artworkUri,
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize(),
                )
            } else {
                Text("♪", style = MaterialTheme.typography.displaySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        Text(
            text = song.title,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.SemiBold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            color = when {
                isPlaying -> MaterialTheme.colorScheme.primary
                isCurrent -> MaterialTheme.colorScheme.primary.copy(alpha = 0.75f)
                else -> MaterialTheme.colorScheme.onSurface
            },
            modifier = Modifier.padding(top = 8.dp),
        )
        Text(
            text = song.artist,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

@Composable
fun AlbumCarousel(title: String, albums: List<LocalAlbum>, onAlbumClick: (LocalAlbum) -> Unit) {
    Column(Modifier.fillMaxWidth()) {
        SectionHeader(title)
        LazyRow(
            contentPadding = PaddingValues(horizontal = 20.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            items(albums.size, key = { albums[it].key }) { index ->
                AlbumCard(album = albums[index], onClick = { onAlbumClick(albums[index]) })
            }
        }
    }
}

@Composable
fun AlbumCard(album: LocalAlbum, onClick: () -> Unit) {
    Column(Modifier.width(150.dp).clickable(onClick = onClick)) {
        Box(
            Modifier.size(150.dp).clip(RoundedCornerShape(12.dp)).background(MaterialTheme.colorScheme.surfaceVariant),
            contentAlignment = Alignment.Center,
        ) {
            if (album.artworkUri != null) {
                AsyncImage(model = album.artworkUri, contentDescription = null, contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize())
            } else {
                Text("♫", style = MaterialTheme.typography.displaySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        Text(
            text = album.name,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.SemiBold,
            maxLines = 1, overflow = TextOverflow.Ellipsis,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.padding(top = 8.dp),
        )
        Text(album.artist, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1, overflow = TextOverflow.Ellipsis)
    }
}

@Composable
fun ArtistCarousel(title: String, artists: List<LocalArtist>, onArtistClick: (LocalArtist) -> Unit) {
    Column(Modifier.fillMaxWidth()) {
        SectionHeader(title)
        LazyRow(
            contentPadding = PaddingValues(horizontal = 20.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            items(artists.size, key = { artists[it].key }) { index ->
                ArtistCard(artist = artists[index], onClick = { onArtistClick(artists[index]) })
            }
        }
    }
}

@Composable
fun ArtistCard(artist: LocalArtist, onClick: () -> Unit) {
    Column(Modifier.width(100.dp).clickable(onClick = onClick), horizontalAlignment = Alignment.CenterHorizontally) {
        Box(
            Modifier.size(100.dp).clip(CircleShape).background(MaterialTheme.colorScheme.surfaceVariant),
            contentAlignment = Alignment.Center,
        ) {
            if (artist.artworkUri != null) {
                AsyncImage(model = artist.artworkUri, contentDescription = null, contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize())
            } else {
                Text(
                    artist.name.firstOrNull()?.uppercase() ?: "♪",
                    style = MaterialTheme.typography.displaySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        Spacer(Modifier.height(8.dp))
        Text(artist.name, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis, color = MaterialTheme.colorScheme.onSurface)
        Text("${artist.songCount} canciones", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1, overflow = TextOverflow.Ellipsis)
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun HeroShelf(
    songs: List<LocalSong>,
    onSongClick: (LocalSong) -> Unit,
    currentSong: LocalSong?,
    isPlaying: Boolean,
    onSongLongPress: ((LocalSong) -> Unit)? = null,
) {
    BoxWithConstraints {
        val cardWidth = heroCardWidth(maxWidth)
        LazyRow(
            contentPadding = PaddingValues(horizontal = PAGE_GUTTER),
            horizontalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            items(songs, key = { it.id }) { song ->
                Box(
                    modifier = Modifier.width(cardWidth).aspectRatio(HERO_CARD_RATIO)
                        .clip(RoundedCornerShape(18.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant)
                        .then(if (onSongLongPress != null) Modifier.heldContextMenuInteractions(
                            { onSongClick(song) }, { onSongLongPress(song) })
                            else Modifier.clickable { onSongClick(song) }),
                ) {
                    if (song.artworkUri != null) {
                        AsyncImage(model = song.artworkUri, contentDescription = null, contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize())
                    }
                    if (currentSong?.id == song.id && isPlaying) SearchPlayingBars(Modifier.align(Alignment.Center))
                    Column(
                        Modifier.fillMaxWidth().align(Alignment.BottomStart)
                            .background(Brush.verticalGradient(listOf(Color.Transparent, Color.Black.copy(alpha = 0.78f))))
                            .padding(start = 16.dp, end = 16.dp, top = 34.dp, bottom = 14.dp),
                    ) {
                        Text(song.title, style = MaterialTheme.typography.titleLarge, color = Color.White, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        if (song.artist.isNotBlank()) {
                            Text(song.artist, style = MaterialTheme.typography.bodyMedium, color = Color.White.copy(alpha = 0.72f), maxLines = 2, overflow = TextOverflow.Ellipsis)
                        }
                    }
                }
            }
        }
    }
}

/** Four-row recents pages, adapted from BitChord HomeScreen RecentShelf/CompactTrackRow. */
@Composable
fun RecentSongShelf(title: String, songs: List<LocalSong>, currentId: String?, isPlaying: Boolean,
    onSongLongPress: (LocalSong) -> Unit, onSongClick: (Int) -> Unit) {
    Column(Modifier.testTag("home-recents").padding(bottom = 26.dp)) {
        SectionHeader(title)
        BoxWithConstraints {
            val columnWidth = trackColumnWidth(maxWidth)
            LazyRow(contentPadding = PaddingValues(horizontal = PAGE_GUTTER),
                horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                itemsIndexed(songs.chunked(4), key = { _, page -> page.first().id }) { pageIndex, page ->
                    Column(Modifier.width(columnWidth)) {
                        page.forEachIndexed { rowIndex, song ->
                            Row(Modifier.fillMaxWidth().heldContextMenuInteractions(
                                { onSongClick(pageIndex * 4 + rowIndex) }, { onSongLongPress(song) })
                                .padding(vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                                Box(Modifier.size(48.dp).clip(RoundedCornerShape(7.dp))
                                    .background(MaterialTheme.colorScheme.surfaceVariant)) {
                                    AsyncImage(song.artworkUri, null, contentScale = ContentScale.Crop,
                                        modifier = Modifier.fillMaxSize())
                                    if (song.id == currentId && isPlaying) SearchPlayingBars(Modifier.align(Alignment.Center))
                                }
                                Spacer(Modifier.width(12.dp))
                                Column(Modifier.weight(1f)) {
                                    Text(song.title, style = MaterialTheme.typography.titleMedium, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                    Text(song.artist, style = MaterialTheme.typography.bodyMedium, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                }
                                IconButton(onClick = { onSongLongPress(song) }) {
                                    Icon(Icons.Default.MoreVert, stringResource(com.music.aurelis.R.string.home_song_actions, song.title))
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun PlaylistCarousel(playlists: List<com.music.aurelis.data.LocalPlaylist>, songs: List<LocalSong>, onOpen: (String) -> Unit) {
    val byId = remember(songs) { songs.associateBy { it.id } }
    Column(Modifier.testTag("home-playlists")) {
        SectionHeader(stringResource(com.music.aurelis.R.string.home_playlists))
        LazyRow(contentPadding = PaddingValues(horizontal = PAGE_GUTTER), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            items(playlists, key = { it.id }) { playlist ->
                val artwork = playlist.songIds.firstNotNullOfOrNull { byId[it]?.artworkUri }
                val available = playlist.songIds.count { it in byId }
                Column(Modifier.width(SHELF_CARD_WIDTH).clickable { onOpen(playlist.id) }) {
                    Box(Modifier.size(SHELF_CARD_WIDTH).clip(RoundedCornerShape(12.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant), contentAlignment = Alignment.Center) {
                        if (artwork == null) Text("♪", style = MaterialTheme.typography.displaySmall)
                        else AsyncImage(artwork, null, contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize())
                    }
                    Text(playlist.name, style = MaterialTheme.typography.bodyMedium, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    Text(androidx.compose.ui.res.pluralStringResource(com.music.aurelis.R.plurals.home_available_song_count, available, available),
                        style = MaterialTheme.typography.bodySmall)
                }
            }
        }
    }
}
