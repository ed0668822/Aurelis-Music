// Aurelis © 2026. GPL-3.0-or-later.
package com.music.aurelis.ui.screens

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
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
import androidx.compose.material3.LinearProgressIndicator
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
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
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
    isCreatingMix: Boolean = false,
    mixError: String? = null,
    onOpenFavorites: () -> Unit = {},
    onOpenPlaylists: () -> Unit = {},
    onOpenPlaylist: (String) -> Unit = {},
    collectionsReadOnly: Boolean = false,
    onPlayShelf: ((List<LocalSong>, Int) -> Unit)? = null,
    isRefreshing: Boolean = false,
) {
    val playShelf: (List<LocalSong>, Int) -> Unit = { songs, index ->
        songs.getOrNull(index)?.let { song ->
            if (onPlayShelf != null) onPlayShelf(songs, index) else onSongClick(song)
        }
    }
    val homePadding = PaddingValues(
        top = (contentPadding.calculateTopPadding() - HOME_TITLE_LIFT).coerceAtLeast(0.dp),
        bottom = contentPadding.calculateBottomPadding(),
    )
    if (libraryState !is LibraryState.Success || libraryState.songs.isEmpty()) {
        // Keep recovery actions reachable with large text or a long scanner error.
        // Separate state scroll from the populated feed's saved scroll position.
        LazyColumn(
            state = rememberLazyListState(),
            modifier = modifier.fillMaxSize().testTag("home-state"),
            contentPadding = homePadding,
        ) {
            item(key = "title") { HomeTitle(stringResource(R.string.home)) }
            item(key = "library-state") {
                val announcement = Modifier.semantics { liveRegion = LiveRegionMode.Polite }
                when (libraryState) {
                    LibraryState.Loading -> HomeLoadingState(announcement)
                    is LibraryState.Error -> MessageState(
                        message = libraryState.message,
                        modifier = announcement,
                        actionLabel = stringResource(if (libraryState.permissionRequired) R.string.home_grant_permission else R.string.home_retry),
                        onAction = if (libraryState.permissionRequired) onRequestPermission else onRetry,
                    )
                    is LibraryState.Success -> if (isRefreshing) {
                        HomeLoadingState(announcement)
                    } else {
                        MessageState(
                            message = stringResource(R.string.home_empty),
                            modifier = announcement,
                            actionLabel = stringResource(R.string.home_refresh),
                            onAction = onRetry,
                        )
                    }
                }
            }
        }
        return
    }
    LazyColumn(
        state = listState,
        modifier = modifier.fillMaxSize().testTag("local-home"),
        contentPadding = homePadding,
    ) {
        item(key = "title") { HomeTitle(stringResource(R.string.home)) }
        if (isRefreshing) item(key = "refreshing") {
            LinearProgressIndicator(Modifier.fillMaxWidth().padding(horizontal = PAGE_GUTTER).testTag("home-refresh"))
        }
        if (collectionsReadOnly) item { Text(stringResource(R.string.collections_unavailable), Modifier.padding(16.dp)) }

        item(key = "local-actions") {
            Row(Modifier.fillMaxWidth().padding(horizontal = PAGE_GUTTER), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(onClick = onOpenFavorites, modifier = Modifier.weight(1f)) { Text(stringResource(R.string.home_favorites)) }
                OutlinedButton(onClick = onOpenPlaylists, modifier = Modifier.weight(1f)) { Text(stringResource(R.string.home_playlists)) }
            }
        }

        val hero = currentSong?.takeIf { current -> libraryState.songs.any { it.id == current.id } }
            ?: recentlyPlayed.firstOrNull() ?: recentlyAdded.firstOrNull() ?: libraryState.songs.firstOrNull()
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
            Button(onClick = onAutoMix, enabled = !isCreatingMix, modifier = Modifier.fillMaxWidth().padding(horizontal = PAGE_GUTTER)) {
                Text(if (isCreatingMix) "Creando mezcla…" else stringResource(R.string.home_automix))
            }
        }
        if (mixError != null) item(key = "mix-error") { Text(mixError, modifier = Modifier.padding(horizontal = PAGE_GUTTER), color = MaterialTheme.colorScheme.error) }
        if (favorites.isNotEmpty()) item(key = "favorites") {
            Column(Modifier.testTag("home-favorites")) {
                SongCarousel(stringResource(R.string.home_favorites), favorites.take(20), currentSong?.id, isPlaying,
                    onSongLongPress = onSongLongPress) { playShelf(favorites, it) }
            }
        }
        if (playlists.isNotEmpty()) item(key = "playlists") {
            PlaylistCarousel(playlists, libraryState.songs, onOpenPlaylist)
        }
        if (recommendations.isNotEmpty()) item(key = "recommendations") {
            Column(Modifier.testTag("home-recommendations")) {
                SectionHeader(stringResource(R.string.home_recommendations))
                HeroShelf(recommendations, { song -> playShelf(recommendations, recommendations.indexOfFirst { it.id == song.id }) },
                    currentSong, isPlaying, onSongLongPress)
            }
        }

        if (recentlyPlayed.isNotEmpty()) {
            item(key = "recents") {
                RecentSongShelf(stringResource(R.string.recently_played), recentlyPlayed,
                    currentSong?.id, isPlaying, onSongLongPress) { playShelf(recentlyPlayed, it) }
            }
        }

        if (mostPlayed.isNotEmpty()) {
            item(key = "most-played") {
                SongCarousel(
                    title = stringResource(R.string.most_played),
                    songs = mostPlayed,
                    currentSongId = currentSong?.id,
                    isPlaying = isPlaying,
                    onSongLongPress = onSongLongPress,
                    onSongClick = { index -> playShelf(mostPlayed, index) },
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
                    onSongLongPress = onSongLongPress,
                    onSongClick = { index -> playShelf(recentlyAdded, index) },
                )
            }
        }
    }
}

@Composable
private fun HomeLoadingState(modifier: Modifier = Modifier) {
    Column(
        modifier.fillMaxWidth().padding(horizontal = PAGE_GUTTER + 6.dp, vertical = 48.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        CircularProgressIndicator()
        Text(stringResource(R.string.home_loading), color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun HomeTitle(title: String, modifier: Modifier = Modifier) {
    // BitChord HomeTitle proportion, retaining the Aurelis icon and heading.
    val titleStyle = MaterialTheme.typography.displayLarge
    val markSize = with(LocalDensity.current) { (titleStyle.fontSize * 0.9f).toDp() }
    Column(modifier = modifier.padding(horizontal = PAGE_GUTTER, vertical = 8.dp)) {
        Icon(
            imageVector = AurelisIcons.Home,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onBackground,
            modifier = Modifier.padding(bottom = 14.dp).size(markSize),
        )
        Text(
            text = title,
            style = titleStyle,
            color = MaterialTheme.colorScheme.onBackground,
        )
    }
}
