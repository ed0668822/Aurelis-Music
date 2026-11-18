// Aurelis © 2026. GPL-3.0-or-later
// Lazy header and five local-folder tabs adapted from BitChord b690dc1; see NOTICE.
package com.music.aurelis.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextOverflow
import coil3.compose.AsyncImage
import kotlinx.coroutines.launch
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.GridView
import androidx.compose.material.icons.rounded.ViewList
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.pluralStringResource
import com.music.aurelis.R
import androidx.compose.ui.unit.dp
import androidx.activity.compose.BackHandler
import com.music.aurelis.data.*
import com.music.aurelis.data.model.*
import com.music.aurelis.ui.components.*
import com.music.aurelis.ui.icons.AurelisIcons
import com.music.aurelis.ui.navigation.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun LocalMusicContent(library: LibraryState, songs: List<LocalSong>, playlists: List<LocalPlaylist>,
    currentSongId: String?, isPlaying: Boolean, sort: SortOption, onSort: (SortOption) -> Unit,
    onBack: () -> Unit, onRetry: () -> Unit, onPermission: () -> Unit,
    onPlay: (List<LocalSong>, Int) -> Unit, onOpen: (AppRoute.Collection) -> Unit,
    onActions: (LocalSong) -> Unit, onProfile: () -> Unit, bottomPadding: PaddingValues = PaddingValues(),
    contentBackdropModifier: Modifier = Modifier, isRefreshing: Boolean = false) {
    var selected by rememberSaveable { mutableIntStateOf(0) }
    var query by rememberSaveable { mutableStateOf("") }
    var search by rememberSaveable { mutableStateOf(false) }
    var sorting by remember { mutableStateOf(false) }
    var grid by rememberSaveable { mutableStateOf(false) }
    val list = rememberLazyListState()
    val scope = rememberCoroutineScope()
    val searchFocus = remember { FocusRequester() }
    val focusManager = LocalFocusManager.current
    val keyboardController = LocalSoftwareKeyboardController.current
    val imeVisible = WindowInsets.ime.getBottom(LocalDensity.current) > 0
    BackHandler(search) {
        if (imeVisible) {
            // The first Back dismisses the IME, rather than losing the query.
            focusManager.clearFocus()
            keyboardController?.hide()
        } else {
            search = false
            query = ""
        }
    }
    LaunchedEffect(search) {
        if (search) {
            list.scrollToItem(0)
            searchFocus.requestFocus()
            keyboardController?.show()
        } else {
            focusManager.clearFocus()
            keyboardController?.hide()
        }
    }
    val success = library as? LibraryState.Success
    val filtered = remember(songs, query) { songs.filter { LocalSearch.matches(it, query) } }
    val visibleIds = remember(filtered) { filtered.map { it.id }.toHashSet() }
    val tabs = listOf(R.string.songs, R.string.artists, R.string.albums, R.string.local_music_playlists, R.string.folders).map { stringResource(it) }
    val totalMinutes = remember(songs) { songs.fold(0L) { total, song ->
        total + song.durationMs.coerceAtLeast(0).coerceAtMost(Long.MAX_VALUE - total)
    } / 60_000 }
    val headerMetadata = stringResource(R.string.local_music_summary,
        pluralStringResource(R.plurals.settings_song_count, songs.size, songs.size), totalMinutes)
    BoxWithConstraints(Modifier.fillMaxSize()) {
        val sleeve = (maxWidth * 0.76f).coerceAtMost(maxHeight * 0.5f)
        val gridColumns = if (maxWidth >= 700.dp) 4 else if (maxWidth >= 500.dp) 3 else 2
        val pullState = androidx.compose.material3.pulltorefresh.rememberPullToRefreshState()
        LocalPullToRefresh(
            refreshing = isRefreshing,
            onRefresh = onRetry,
            state = pullState,
            modifier = Modifier.fillMaxSize().testTag("local-music-refresh"),
        ) {
        LazyColumn(state = list, modifier = Modifier.fillMaxSize().imePadding()
            .then(contentBackdropModifier).testTag("local-music-list"),
            contentPadding = bottomPadding) {
            item(key = "header") {
                if (success != null && !search) LocalDetailHeader(stringResource(R.string.local_music), stringResource(R.string.local_music_audio_files), headerMetadata,
                    sleeve, null, AurelisIcons.Folder, { onPlay(filtered, 0) },
                    { onPlay(filtered.shuffled(), 0) }, filtered.isNotEmpty())
                else Text(stringResource(R.string.local_music), style = MaterialTheme.typography.headlineLarge,
                    modifier = Modifier.padding(start = 16.dp, top = topBarContentPadding(), bottom = 16.dp))
                if (search) SearchField(query, { query = it }, onSubmit = {
                    focusManager.clearFocus()
                    keyboardController?.hide()
                }, focusRequester = searchFocus,
                    placeholder = stringResource(R.string.local_music_search_open), modifier = Modifier.padding(horizontal = 16.dp))
                LocalFolderTabs(tabs, selected, { next ->
                    if (selected != next) { selected = next; scope.launch { list.scrollToItem(0) } }
                })
            }
            when (library) {
                LibraryState.Loading -> item { Column(Modifier.fillMaxWidth().padding(32.dp),
                    horizontalAlignment = Alignment.CenterHorizontally) {
                    CircularProgressIndicator()
                    Spacer(Modifier.height(12.dp))
                    Text(stringResource(R.string.home_loading))
                } }
                is LibraryState.Error -> item {
                    MessageState(library.message,
                        actionLabel = stringResource(if (library.permissionRequired) R.string.local_music_grant else R.string.local_music_retry),
                        onAction = if (library.permissionRequired) onPermission else onRetry)
                }
                is LibraryState.Success -> if (isRefreshing && library.songs.isEmpty()) {
                    item { MessageState(stringResource(R.string.home_loading)) }
                } else when (selected) {
                    0 -> {
                        if (filtered.isEmpty()) item { Text(stringResource(R.string.local_music_empty), Modifier.padding(24.dp)) }
                        if (grid) {
                            localMusicGridRows(filtered, gridColumns, { it.id }) { song ->
                                val index = filtered.indexOf(song)
                                LocalMusicGridCard("song:" + song.id, song.title, song.artist,
                                    song.artworkUri, AurelisIcons.MusicNote,
                                    active = currentSongId == song.id && isPlaying,
                                    onClick = { onPlay(filtered, index) },
                                    onMore = { onActions(song) })
                            }
                        } else itemsIndexed(filtered, key = { _, song -> song.id }) { index, song ->
                            SongListItem(song, currentSongId == song.id && isPlaying,
                                { onPlay(filtered, index) }, onMore = { onActions(song) })
                            HorizontalDivider(Modifier.padding(start = 84.dp), thickness = 0.5.dp)
                        }
                    }
                    1 -> {
                        val artists = success?.artists.orEmpty().filter { group -> group.songs.any { it.id in visibleIds } }
                        if (grid) localMusicGridRows(artists, gridColumns, { it.key }) { artist ->
                            LocalMusicGridCard("artist:" + artist.key, artist.name,
                                pluralStringResource(R.plurals.settings_song_count, artist.songCount, artist.songCount),
                                artist.artworkUri, AurelisIcons.Artist,
                                onClick = { onOpen(AppRoute.Collection(LocalCollectionKind.ARTIST, artist.key)) })
                        } else items(artists, key = { it.key }) { artist ->
                            LocalEntityRow(artist.name,
                                pluralStringResource(R.plurals.settings_song_count, artist.songCount, artist.songCount),
                                artist.artworkUri, AurelisIcons.Artist, round = true,
                                onClick = { onOpen(AppRoute.Collection(LocalCollectionKind.ARTIST, artist.key)) })
                        }
                    }
                    2 -> {
                        val albums = success?.albums.orEmpty().filter { group -> group.songs.any { it.id in visibleIds } }
                        if (grid) localMusicGridRows(albums, gridColumns, { it.key }) { album ->
                            LocalMusicGridCard("album:" + album.key, album.name, album.artist,
                                album.artworkUri, AurelisIcons.Album,
                                onClick = { onOpen(AppRoute.Collection(LocalCollectionKind.ALBUM, album.key)) })
                        } else items(albums, key = { it.key }) { album ->
                            LocalEntityRow(album.name, album.artist, album.artworkUri, AurelisIcons.Album,
                                onClick = { onOpen(AppRoute.Collection(LocalCollectionKind.ALBUM, album.key)) })
                        }
                    }
                    3 -> {
                        item { TextButton(onClick = { onOpen(AppRoute.Collection(LocalCollectionKind.PLAYLISTS)) }) {
                            Text(stringResource(R.string.local_music_manage_playlists)) } }
                        val visiblePlaylists = playlists.filter { LocalSearch.normalize(it.name).contains(LocalSearch.normalize(query)) }
                        if (visiblePlaylists.isEmpty()) item {
                            Text(stringResource(R.string.local_music_no_playlists), Modifier.padding(24.dp))
                        }
                        if (grid) localMusicGridRows(visiblePlaylists, gridColumns, { it.id }) { playlist ->
                            LocalMusicGridCard("playlist:" + playlist.id, playlist.name,
                                pluralStringResource(R.plurals.settings_song_count, playlist.songIds.size, playlist.songIds.size),
                                songs.firstOrNull { song -> song.id == playlist.songIds.firstOrNull() }?.artworkUri,
                                AurelisIcons.Playlist,
                                onClick = { onOpen(AppRoute.Collection(LocalCollectionKind.PLAYLIST, playlist.id)) })
                        } else items(visiblePlaylists, key = { it.id }) { playlist ->
                            LocalEntityRow(playlist.name,
                                pluralStringResource(R.plurals.settings_song_count, playlist.songIds.size, playlist.songIds.size),
                                songs.firstOrNull { song -> song.id == playlist.songIds.firstOrNull() }?.artworkUri,
                                AurelisIcons.Playlist,
                                onClick = { onOpen(AppRoute.Collection(LocalCollectionKind.PLAYLIST, playlist.id)) })
                        }
                    }
                    4 -> {
                        val folders = LocalFolderRepository.groups(filtered)
                        if (folders.isEmpty()) item {
                            Text(stringResource(R.string.local_music_no_folders), Modifier.padding(24.dp))
                        }
                        if (grid) localMusicGridRows(folders, gridColumns, { it.key }) { folder ->
                            LocalMusicGridCard("folder:" + folder.key, folder.label,
                                pluralStringResource(R.plurals.settings_song_count, folder.songs.size, folder.songs.size),
                                null, AurelisIcons.Folder,
                                onClick = { onOpen(AppRoute.Collection(LocalCollectionKind.FOLDER, folder.key)) })
                        } else items(folders, key = { it.key }) { folder ->
                            LocalEntityRow(folder.label,
                                pluralStringResource(R.plurals.settings_song_count, folder.songs.size, folder.songs.size),
                                null, AurelisIcons.Folder,
                                onClick = { onOpen(AppRoute.Collection(LocalCollectionKind.FOLDER, folder.key)) })
                        }
                    }
                }
            }
            if (success != null && selected in 1..2) {
                val empty = if (selected == 1) success.artists.none { artist -> artist.songs.any { it.id in visibleIds } }
                    else success.albums.none { album -> album.songs.any { it.id in visibleIds } }
                if (empty) item(key = "empty-category") {
                    Text(stringResource(if (query.isNotBlank()) R.string.local_music_no_results else if (selected == 1) R.string.local_music_no_artists else R.string.local_music_no_albums),
                        Modifier.padding(24.dp))
                }
            }
        }
        }
        FrostedTopBar(stringResource(R.string.local_music), { false }, transparentBackdrop = true, artworkPageChrome = true,
            onBack = onBack, refreshing = isRefreshing, pullFraction = { pullState.distanceFraction }, actions = {
                IconButton(onClick = {
                    search = !search
                    if (!search) query = ""
                }) { Icon(if (search) AurelisIcons.Close else AurelisIcons.Search,
                    stringResource(if (search) R.string.local_music_search_close else R.string.local_music_search_open)) }
                IconButton(onClick = { grid = !grid }, modifier = Modifier.testTag("local-music-view-toggle")) {
                    Icon(if (grid) Icons.Rounded.ViewList else Icons.Rounded.GridView,
                        stringResource(if (grid) R.string.local_music_list_view else R.string.local_music_grid_view))
                }
                Box {
                    IconButton(onClick = { sorting = true }) { Icon(AurelisIcons.Sort, stringResource(R.string.local_music_sort)) }
                    DropdownMenu(sorting, { sorting = false }) {
                        SortOption.entries.forEach { option -> DropdownMenuItem(text = { Text(option.localizedLabel()) },
                            leadingIcon = { if (option == sort) Icon(AurelisIcons.Check, null) },
                            onClick = { sorting = false; onSort(option) }) }
                    }
                }
                IconButton(onClick = onRetry, enabled = !isRefreshing) { Icon(AurelisIcons.Refresh, stringResource(R.string.local_music_refresh)) }
                LocalProfileButton(onProfile)
            })
    }
}

@Composable
private fun LocalEntityRow(title: String, subtitle: String, artwork: String?, icon: ImageVector,
    round: Boolean = false, onClick: () -> Unit) {
    Row(Modifier.fillMaxWidth().clickable(onClick = onClick).padding(horizontal = 16.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically) {
        Box(Modifier.size(52.dp).clip(RoundedCornerShape(if (round) 26.dp else 8.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant), contentAlignment = Alignment.Center) {
            Icon(icon, null)
            if (artwork != null) AsyncImage(artwork, null, Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
        }
        Column(Modifier.weight(1f).padding(start = 14.dp)) {
            Text(title, style = MaterialTheme.typography.titleMedium, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(subtitle, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
    }
}

@Composable
private fun LocalFolderTabs(tabs: List<String>, selected: Int, onSelect: (Int) -> Unit) {
    PillSegmentedControl(tabs, selected, onSelect,
        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp))
}

/** Keep every grid row inside the existing lazy list and navigation state. */
private fun <T> LazyListScope.localMusicGridRows(
    values: List<T>, columns: Int, key: (T) -> String,
    content: @Composable (T) -> Unit,
) {
    items(values.chunked(columns), key = { row -> "grid:" + key(row.first()) }) { row ->
        Row(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 6.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            row.forEach { value -> Box(Modifier.weight(1f)) { content(value) } }
            repeat(columns - row.size) { Spacer(Modifier.weight(1f)) }
        }
    }
}

@Composable
private fun LocalMusicGridCard(
    key: String, title: String, subtitle: String, artwork: String?, icon: ImageVector,
    active: Boolean = false, onClick: () -> Unit, onMore: (() -> Unit)? = null,
) {
    Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp))
        .background(if (active) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant)
        .clickable(onClick = onClick).testTag("local-music-card:" + key).padding(10.dp)) {
        Box(Modifier.fillMaxWidth().aspectRatio(1f).clip(RoundedCornerShape(10.dp))
            .background(MaterialTheme.colorScheme.surface), contentAlignment = Alignment.Center) {
            Icon(icon, null, modifier = Modifier.size(38.dp),
                tint = MaterialTheme.colorScheme.onSurfaceVariant)
            if (artwork != null) AsyncImage(artwork, null, Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop)
        }
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f).padding(top = 8.dp)) {
                Text(title, style = MaterialTheme.typography.titleSmall, maxLines = 2,
                    overflow = TextOverflow.Ellipsis)
                Text(subtitle, style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
            if (onMore != null) IconButton(onClick = onMore) {
                Icon(AurelisIcons.MoreVert,
                    contentDescription = stringResource(R.string.local_music_song_options, title))
            }
        }
    }
}
