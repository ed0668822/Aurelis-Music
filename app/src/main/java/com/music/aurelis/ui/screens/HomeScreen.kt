// Aurelis © 2026. GPL-3.0-or-later.
package com.music.aurelis.ui.screens

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.MusicNote
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.Button
import androidx.compose.material3.TextButton
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.OutlinedButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.platform.testTag
import coil3.compose.AsyncImage
import com.music.aurelis.R
import com.music.aurelis.data.model.LocalAlbum
import com.music.aurelis.data.model.LocalArtist
import com.music.aurelis.data.model.LocalSong
import com.music.aurelis.ui.components.*
import com.music.aurelis.ui.components.home.*
import com.music.aurelis.ui.icons.AurelisIcons
import com.music.aurelis.data.LibraryState
import com.music.aurelis.data.LocalPlaylist

private val HOME_TITLE_LIFT = 28.dp
private const val RECENT_TRACKS_PER_COLUMN = 4

@Composable
fun HomeScreen(
    recentlyPlayed: List<LocalSong>,
    mostPlayed: List<LocalSong>,
    recentlyAdded: List<LocalSong>,
    albums: List<LocalAlbum>,
    artists: List<LocalArtist>,
    listState: LazyListState,
    onSongClick: (LocalSong) -> Unit,
    onSongLongPress: (LocalSong) -> Unit,
    onAlbumClick: (LocalAlbum) -> Unit,
    onArtistClick: (LocalArtist) -> Unit,
    currentSong: LocalSong?,
    isPlaying: Boolean,
    modifier: Modifier = Modifier,
    contentPadding: PaddingValues,
    libraryState: LibraryState = LibraryState.Success(emptyList()),
    favorites: List<LocalSong> = emptyList(),
    recommendations: List<LocalSong> = emptyList(),
    playlists: List<LocalPlaylist> = emptyList(),
    onRetry: () -> Unit = {},
    onRequestPermission: () -> Unit = {},
    onOpenPlayer: () -> Unit = {},
    onAutoMix: () -> Unit = {},
    onOpenFavorites: () -> Unit = {},
    onOpenPlaylists: () -> Unit = {},
    onOpenPlaylist: (String) -> Unit = {},
    collectionsReadOnly: Boolean = false,
) {
    if (libraryState !is LibraryState.Success || libraryState.songs.isEmpty()) {
        Column(modifier.fillMaxSize().padding(contentPadding).padding(24.dp),
            verticalArrangement = Arrangement.Center, horizontalAlignment = Alignment.CenterHorizontally) {
            when (libraryState) {
                LibraryState.Loading -> { CircularProgressIndicator(); Text(stringResource(R.string.home_loading)) }
                is LibraryState.Error -> {
                    Text(libraryState.message)
                    Button(onClick = if (libraryState.permissionRequired) onRequestPermission else onRetry) {
                        Text(stringResource(if (libraryState.permissionRequired) R.string.home_grant_permission else R.string.home_retry))
                    }
                }
                is LibraryState.Success -> {
                    Text(stringResource(R.string.home_empty))
                    Button(onClick = onRetry) { Text(stringResource(R.string.home_refresh)) }
                }
            }
        }
        return
    }
    LazyColumn(
        state = listState,
        modifier = modifier.fillMaxSize().testTag("local-home"),
        contentPadding = PaddingValues(
            top = (contentPadding.calculateTopPadding() - HOME_TITLE_LIFT).coerceAtLeast(0.dp),
            bottom = contentPadding.calculateBottomPadding(),
        ),
    ) {
        item { HomeTitle(stringResource(R.string.home)) }
        if (collectionsReadOnly) item { Text(stringResource(R.string.collections_unavailable), Modifier.padding(16.dp)) }

        item(key = "local-actions") {
            Row(Modifier.fillMaxWidth().padding(horizontal = PAGE_GUTTER), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(onClick = onOpenFavorites, modifier = Modifier.weight(1f)) { Text(stringResource(R.string.home_favorites)) }
                OutlinedButton(onClick = onOpenPlaylists, modifier = Modifier.weight(1f)) { Text(stringResource(R.string.home_playlists)) }
            }
        }

        val hero = currentSong ?: recentlyPlayed.firstOrNull() ?: recentlyAdded.firstOrNull()
        if (hero != null) {
            item(key = "hero") {
                HomeHero(
                    song = hero,
                    onPlayClick = {
                        if (hero.id == currentSong?.id) onOpenPlayer() else onSongClick(hero)
                    },
                    modifier = Modifier.padding(horizontal = PAGE_GUTTER, vertical = 8.dp),
                    eyebrow = stringResource(if (hero.id == currentSong?.id) R.string.home_continue else R.string.home_listen),
                )
            }
        }

        item(key = "automix") {
            Button(onClick = onAutoMix, modifier = Modifier.fillMaxWidth().padding(horizontal = PAGE_GUTTER)) {
                Text(stringResource(R.string.home_automix))
            }
        }
        if (favorites.isNotEmpty()) item(key = "favorites") {
            SongCarousel(stringResource(R.string.home_favorites), favorites.take(20), currentSong?.id, isPlaying) { onSongClick(favorites[it]) }
        }
        if (playlists.isNotEmpty()) item(key = "playlists") {
            Column {
                SectionHeader(stringResource(R.string.home_playlists))
                playlists.forEach { playlist -> TextButton(onClick = { onOpenPlaylist(playlist.id) }) { Text(playlist.name) } }
            }
        }
        if (recommendations.isNotEmpty()) item(key = "recommendations") {
            SongCarousel(stringResource(R.string.home_recommendations), recommendations, currentSong?.id, isPlaying) { onSongClick(recommendations[it]) }
        }

        if (recentlyPlayed.isNotEmpty()) {
            item(key = "recents") {
                SongCarousel(
                    title = stringResource(R.string.recently_played),
                    songs = recentlyPlayed,
                    currentSongId = currentSong?.id,
                    isPlaying = isPlaying,
                    onSongClick = { index ->
                        recentlyPlayed.getOrNull(index)?.let { onSongClick(it) }
                    },
                )
            }
        }

        if (mostPlayed.isNotEmpty()) {
            item(key = "most-played") {
                SongCarousel(
                    title = stringResource(R.string.most_played),
                    songs = mostPlayed,
                    currentSongId = currentSong?.id,
                    isPlaying = isPlaying,
                    onSongClick = { index ->
                        mostPlayed.getOrNull(index)?.let { onSongClick(it) }
                    },
                )
            }
        }

        if (albums.isNotEmpty()) {
            item(key = "albums") {
                AlbumCarousel(
                    title = stringResource(R.string.albums),
                    albums = albums,
                    onAlbumClick = onAlbumClick,
                )
            }
        }

        if (artists.isNotEmpty()) {
            item(key = "artists") {
                ArtistCarousel(
                    title = stringResource(R.string.artists),
                    artists = artists,
                    onArtistClick = onArtistClick,
                )
            }
        }

        if (recentlyAdded.isNotEmpty()) {
            item(key = "recently-added") {
                SongCarousel(
                    title = stringResource(R.string.recently_added),
                    songs = recentlyAdded,
                    currentSongId = currentSong?.id,
                    isPlaying = isPlaying,
                    onSongClick = { index ->
                        recentlyAdded.getOrNull(index)?.let { onSongClick(it) }
                    },
                )
            }
        }

        item { Spacer(Modifier.height(100.dp)) }
    }
}

@Composable
private fun HomeTitle(title: String, modifier: Modifier = Modifier) {
    Column(modifier = modifier.padding(horizontal = PAGE_GUTTER, vertical = 8.dp)) {
        Icon(
            imageVector = AurelisIcons.Home,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onBackground,
            modifier = Modifier.padding(bottom = 14.dp).size(30.dp),
        )
        Text(
            text = title,
            style = MaterialTheme.typography.displayLarge,
            color = MaterialTheme.colorScheme.onBackground,
        )
    }
}
