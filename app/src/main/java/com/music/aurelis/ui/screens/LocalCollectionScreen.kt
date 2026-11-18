// SPDX-FileCopyrightText: 2026 Aurelis
// SPDX-License-Identifier: LGPL-3.0-or-later
package com.music.aurelis.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.clickable
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import com.music.aurelis.data.LocalGenres
import com.music.aurelis.data.LocalSearch
import com.music.aurelis.data.LocalFolderRepository
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material.icons.rounded.FavoriteBorder
import androidx.compose.material.icons.automirrored.rounded.PlaylistAdd
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.MoreVert
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.listSaver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.DialogProperties
import com.music.aurelis.R
import com.music.aurelis.data.LibraryState
import com.music.aurelis.data.LocalUserLibrary
import com.music.aurelis.data.model.LocalSong
import com.music.aurelis.data.model.LocalAlbum
import com.music.aurelis.data.model.LocalArtist
import com.music.aurelis.ui.components.SongListItem
import com.music.aurelis.ui.components.LocalDetailHeader
import com.music.aurelis.ui.components.BrowseActionsMenu
import com.music.aurelis.ui.icons.AurelisIcons
import com.music.aurelis.ui.components.MessageState
import com.music.aurelis.ui.navigation.AppRoute
import com.music.aurelis.ui.navigation.LocalCollectionKind

/** Collection IDs are resolved against the current library; stale media is never played. */
@Composable
fun LocalCollectionScreen(
    route: AppRoute.Collection,
    library: LibraryState,
    store: LocalUserLibrary,
    currentSongId: String?,
    isPlaying: Boolean,
    onBack: () -> Unit,
    onOpen: (AppRoute.Collection) -> Unit,
    onPlay: (List<LocalSong>, Int) -> Unit,
    onRetry: () -> Unit = {},
    onRequestPermission: () -> Unit = {},
    onPlayNext: ((List<LocalSong>) -> Unit)? = null,
    onAddToQueue: ((List<LocalSong>) -> Unit)? = null,
) {
    val user by store.state.collectAsState()
    val snapshot = library as? LibraryState.Success
    val allSongs = snapshot?.songs.orEmpty()
    val playlist = user.playlists.firstOrNull { it.id == route.key }
    val album = snapshot?.albums?.let { resolveLocalAlbum(it, route.key) }
    val artist = snapshot?.artists?.let { resolveLocalArtist(it, route.key) }
    val artistAlbums = if (route.kind == LocalCollectionKind.ARTIST && artist != null)
        localArtistAlbums(artist, snapshot?.albums.orEmpty()) else emptyList()
    val byId = remember(allSongs) { allSongs.associateBy { it.id } }
    val genres = remember(allSongs) { LocalGenres.groups(allSongs) }
    val folderSongs = if (route.kind == LocalCollectionKind.FOLDER)
        LocalFolderRepository.songsIn(allSongs, route.key) else emptyList()
    val folderChildren = if (route.kind == LocalCollectionKind.FOLDER)
        LocalFolderRepository.browse(allSongs, route.key) else emptyList()
    val directory = when (route.kind) {
        LocalCollectionKind.ALBUMS -> snapshot?.albums.orEmpty().map { it.name to AppRoute.Collection(LocalCollectionKind.ALBUM, it.key) }
        LocalCollectionKind.ARTISTS -> snapshot?.artists.orEmpty().map { it.name to AppRoute.Collection(LocalCollectionKind.ARTIST, it.key) }
        LocalCollectionKind.FOLDERS -> LocalFolderRepository.browse(allSongs)
            .map { it.label to AppRoute.Collection(LocalCollectionKind.FOLDER, it.key) }
        else -> null
    }
    val songs = when (route.kind) {
        LocalCollectionKind.FAVORITES -> allSongs.filter { it.id in user.favorites }
        LocalCollectionKind.PLAYLIST -> playlist?.songIds?.mapNotNull(byId::get).orEmpty()
        LocalCollectionKind.ALBUM -> album?.songs.orEmpty()
        LocalCollectionKind.ARTIST -> artist?.songs.orEmpty()
        LocalCollectionKind.FOLDER -> folderSongs
        LocalCollectionKind.GENRE -> genres.firstOrNull { it.key == route.key }?.songs.orEmpty()
        LocalCollectionKind.HISTORY -> user.listening.entries.filter { it.value.lastPlayedAt > 0 }
            .sortedByDescending { it.value.lastPlayedAt }.mapNotNull { byId[it.key] }
        LocalCollectionKind.PLAYLISTS, LocalCollectionKind.ALBUMS, LocalCollectionKind.ARTISTS, LocalCollectionKind.FOLDERS -> emptyList()
    }
    val title = when (route.kind) {
        LocalCollectionKind.FOLDER -> LocalFolderRepository.folderLabel(route.key)
        LocalCollectionKind.GENRE -> if (route.key == "unknown") stringResource(R.string.explore_unknown_genre)
            else genres.firstOrNull { it.key == route.key }?.name.orEmpty()
        LocalCollectionKind.HISTORY -> "Historial"
        LocalCollectionKind.ALBUMS -> "Álbumes"
        LocalCollectionKind.ARTISTS -> "Artistas"
        LocalCollectionKind.FOLDERS -> "Carpetas"
        LocalCollectionKind.FAVORITES -> stringResource(R.string.home_favorites)
        LocalCollectionKind.PLAYLISTS -> stringResource(R.string.home_playlists)
        LocalCollectionKind.PLAYLIST -> playlist?.name.orEmpty()
        LocalCollectionKind.ALBUM -> album?.name.orEmpty()
        LocalCollectionKind.ARTIST -> artist?.name.orEmpty()
    }
    var grid by rememberSaveable { mutableStateOf(false) }
    var selectionMode by rememberSaveable(route) { mutableStateOf(false) }
    var selected by rememberSaveable(route, stateSaver = listSaver<Set<String>, String>(
        save = { it.toList() }, restore = { it.toSet() },
    )) { mutableStateOf(emptySet<String>()) }
    var bulkPlaylist by remember { mutableStateOf(false) }
    var descending by rememberSaveable(route) { mutableStateOf(false) }
    var sortMenu by remember { mutableStateOf(false) }
    var dateSort by rememberSaveable(route) { mutableStateOf("none") }
    // Keep source indices for playlist moves/removal, including inaccessible media entries.
    // Playlists retain their saved manual order even when other collections are sorted.
    val sourceEntries = if (playlist != null)
        playlist.songIds.mapIndexedNotNull { position, id -> byId[id]?.let { IndexedValue(position, it) } }
    else songs.mapIndexed { position, song -> IndexedValue(position, song) }
    val displayEntries = if (playlist == null) sortCollectionEntries(sourceEntries, descending, dateSort)
        else sourceEntries
    val displaySongs = displayEntries.map { it.value }
    // Resolve saved IDs against visible media, never against stale playlist snapshots.
    val selectedSongs = displaySongs.filter { it.id in selected }
    val allVisibleSelected = displaySongs.isNotEmpty() && displaySongs.all { it.id in selected }
    var orderSong by remember { mutableStateOf<String?>(null) }
    var editing by rememberSaveable { mutableStateOf(false) }
    var name by rememberSaveable { mutableStateOf("") }
    var deleting by remember { mutableStateOf(false) }
    var addSongId by rememberSaveable { mutableStateOf<String?>(null) }
    var collectionActionsExpanded by remember { mutableStateOf(false) }
    val hasDetailHeader = route.kind in setOf(LocalCollectionKind.ALBUM, LocalCollectionKind.ARTIST,
        LocalCollectionKind.FOLDER, LocalCollectionKind.GENRE)

    Column(Modifier.fillMaxSize().statusBarsPadding().padding(16.dp)) {
        TextButton(onClick = onBack) { Text(stringResource(R.string.back)) }
        if (!hasDetailHeader) Text(title.ifBlank { stringResource(R.string.collection_unavailable) }, style = MaterialTheme.typography.headlineMedium)
        if (user.readOnly) Text(stringResource(R.string.collections_unavailable))
        // Private playlist listing does not require a successful media scan.
        if (route.kind != LocalCollectionKind.PLAYLISTS) {
            when (library) {
                LibraryState.Loading -> Column(Modifier.fillMaxWidth().padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally) {
                    CircularProgressIndicator()
                    Spacer(Modifier.height(12.dp))
                    Text(stringResource(R.string.home_loading))
                }
                is LibraryState.Error -> MessageState(library.message,
                    actionLabel = stringResource(if (library.permissionRequired)
                        R.string.home_grant_permission else R.string.home_retry),
                    onAction = if (library.permissionRequired) onRequestPermission else onRetry)
                is LibraryState.Success -> Unit
            }
        }
        if (directory != null) {
            TextButton(onClick = { grid = !grid }, modifier = Modifier.testTag("collection-view-toggle")) {
                Text(stringResource(if (grid) R.string.local_music_list_view else R.string.local_music_grid_view))
            }
            if (library is LibraryState.Success && directory.isEmpty()) Text(stringResource(R.string.collection_empty))
            if (grid && library is LibraryState.Success) LazyVerticalGrid(GridCells.Adaptive(140.dp), Modifier.weight(1f)) {
                items(directory, key = { it.second.key }) { entry ->
                    TextButton(onClick = { onOpen(entry.second) }, modifier = Modifier.heightIn(min = 96.dp)) { Text(entry.first) }
                }
            } else if (library is LibraryState.Success) LazyColumn(Modifier.weight(1f)) {
                items(directory, key = { it.second.key }) { entry -> TextButton(onClick = { onOpen(entry.second) }) { Text(entry.first) } }
            }
        } else if (route.kind == LocalCollectionKind.PLAYLISTS) {
            Button(onClick = { name = ""; editing = true }, enabled = !user.readOnly) { Text(stringResource(R.string.playlist_create)) }
            if (user.playlists.isEmpty()) Text(stringResource(R.string.playlists_empty))
            LazyColumn(Modifier.weight(1f)) {
                itemsIndexed(user.playlists, key = { _, item -> item.id }) { _, item ->
                    TextButton(onClick = { onOpen(AppRoute.Collection(LocalCollectionKind.PLAYLIST, item.id)) }) {
                        Text(item.name)
                    }
                }
            }
        } else {
            if (route.kind == LocalCollectionKind.PLAYLIST && playlist != null) Row {
                TextButton(onClick = { name = playlist.name; editing = true }, enabled = !user.readOnly) { Text(stringResource(R.string.playlist_rename)) }
                TextButton(onClick = { deleting = true }, enabled = !user.readOnly) { Text(stringResource(R.string.playlist_delete)) }
            }
            if (songs.isEmpty() && library is LibraryState.Success) Text(stringResource(R.string.collection_empty))
            if (playlist != null && playlist.songIds.size > songs.size) Text(stringResource(R.string.collection_missing_tracks))
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (!hasDetailHeader) Button(onClick = { onPlay(displaySongs, 0) }, enabled = displaySongs.isNotEmpty()) {
                    Text(stringResource(R.string.collection_play))
                }
                Box {
                    IconButton(onClick = { collectionActionsExpanded = true }) {
                        Icon(Icons.Rounded.MoreVert, "Acciones de colección")
                    }
                    BrowseActionsMenu(onDismiss = { collectionActionsExpanded = false },
                        expanded = collectionActionsExpanded,
                        onPlay = if (displaySongs.isEmpty()) null else ({ onPlay(displaySongs, 0) }),
                        onShuffle = if (displaySongs.isEmpty()) null else ({ onPlay(displaySongs.shuffled(), 0) }),
                        onPlayNext = if (displaySongs.isEmpty() || onPlayNext == null) null
                            else ({ onPlayNext(displaySongs) }),
                        onAddToQueue = if (displaySongs.isEmpty() || onAddToQueue == null) null
                            else ({ onAddToQueue(displaySongs) }),
                        onSelect = { selectionMode = true; selected = emptySet() },
                        onRename = if (playlist != null && !user.readOnly) ({ name = playlist.name; editing = true }) else null,
                        onDelete = if (playlist != null && !user.readOnly) ({ deleting = true }) else null)
                }
            }
            Row {
                TextButton(onClick = { selectionMode = !selectionMode; selected = emptySet() }) {
                    Text(if (selectionMode) "Cancelar selección" else "Seleccionar")
                }
                if (playlist == null) {
                    TextButton(onClick = { dateSort = "none"; descending = !descending }) {
                        Text(if (descending && dateSort == "none") "Orden original" else "Título Z–A")
                    }
                    Box {
                        TextButton(onClick = { sortMenu = true }, modifier = Modifier.testTag("collection-sort-menu")) {
                            Text(stringResource(R.string.local_music_sort))
                        }
                        DropdownMenu(expanded = sortMenu, onDismissRequest = { sortMenu = false }) {
                            listOf(
                                "none" to R.string.collection_original_order,
                                "added" to R.string.collection_sort_date_added,
                                "modified" to R.string.collection_sort_date_modified,
                            ).forEach { (key, title) ->
                                DropdownMenuItem(text = { Text(stringResource(title)) },
                                    leadingIcon = {
                                        if (dateSort == key && (key != "none" || !descending))
                                            Icon(AurelisIcons.Check, null)
                                    },
                                    onClick = { dateSort = key; descending = false; sortMenu = false })
                            }
                        }
                    }
                }
            }
            if (selectionMode) {
                Text(
                    pluralStringResource(R.plurals.settings_song_count, selectedSongs.size, selectedSongs.size),
                    modifier = Modifier.testTag("collection-selection-count"),
                    style = MaterialTheme.typography.bodyMedium,
                )
                Row {
                    TextButton(onClick = { selected = displaySongs.mapTo(linkedSetOf()) { it.id } },
                        enabled = displaySongs.isNotEmpty() && !allVisibleSelected) { Text("Todas") }
                    TextButton(onClick = { selected = emptySet() },
                        enabled = selectedSongs.isNotEmpty()) { Text("Ninguna") }
                    TextButton(enabled = selectedSongs.isNotEmpty(), onClick = { onPlay(selectedSongs, 0) }) { Text("Reproducir") }
                    TextButton(enabled = selectedSongs.isNotEmpty() && !user.readOnly, onClick = {
                        selectedSongs.filterNot { it.id in user.favorites }.forEach { store.toggleFavorite(it.id) }
                    }) { Text("Favoritos") }
                }
                TextButton(enabled = selectedSongs.isNotEmpty() && !user.readOnly, onClick = { bulkPlaylist = true }) { Text("Añadir selección a playlist") }
            }
            LazyColumn(Modifier.weight(1f).testTag("collection-tracks"),
                contentPadding = PaddingValues(bottom = 24.dp)) {
                if (hasDetailHeader) item(key = "collection-detail-header") {
                    val artwork = when (route.kind) {
                        LocalCollectionKind.ALBUM -> album?.artworkUri
                        LocalCollectionKind.ARTIST -> artist?.artworkUri
                        else -> songs.firstNotNullOfOrNull { it.artworkUri }
                    }
                    val placeholder = when (route.kind) {
                        LocalCollectionKind.ARTIST -> AurelisIcons.Artist
                        LocalCollectionKind.FOLDER -> AurelisIcons.Folder
                        else -> AurelisIcons.Album
                    }
                    BoxWithConstraints(Modifier.fillMaxWidth()) {
                        // The device's available viewport limits the cover in landscape.
                        val screenHeight = LocalConfiguration.current.screenHeightDp.dp
                        val sleeve = if (route.kind == LocalCollectionKind.ALBUM)
                            (maxWidth * 0.76f).coerceAtMost(screenHeight * 0.5f)
                                .coerceAtLeast(144.dp)
                        else 156.dp
                        val knownYear = if (route.kind == LocalCollectionKind.ALBUM)
                            songs.mapNotNull { it.year }.distinct().singleOrNull() else null
                        val countLabel = pluralStringResource(R.plurals.settings_song_count, songs.size, songs.size)
                        val totalMs = songs.fold(0L) { total, song ->
                            total + song.durationMs.coerceIn(0L, Long.MAX_VALUE - total)
                        }
                        val durationLabel = if (route.kind == LocalCollectionKind.ALBUM && totalMs >= 60_000L)
                            stringResource(R.string.album_duration_minutes, totalMs / 60_000L) else null
                        val albumCountLabel = if (route.kind == LocalCollectionKind.ARTIST && artist != null)
                            pluralStringResource(R.plurals.artist_album_count, artistAlbums.size, artistAlbums.size) else null
                        val metadata = buildString {
                            append(countLabel)
                            if (knownYear != null) append(" · ").append(knownYear)
                            if (durationLabel != null) append(" · ").append(durationLabel)
                            if (albumCountLabel != null) append(" · ").append(albumCountLabel)
                        }
                        LocalDetailHeader(title.ifBlank { stringResource(R.string.collection_unavailable) },
                            if (route.kind == LocalCollectionKind.ALBUM) album?.artist.orEmpty() else "",
                            metadata, sleeve, artwork, placeholder,
                            onPlay = { onPlay(displaySongs, 0) },
                            onShuffle = { onPlay(displaySongs.shuffled(), 0) },
                            canPlay = displaySongs.isNotEmpty())
                    }
                }
                if (route.kind == LocalCollectionKind.FOLDER && folderChildren.isNotEmpty()) {
                    item(key = "folder-children") {
                        Column(Modifier.fillMaxWidth().testTag("folder-children")) {
                            Text(stringResource(R.string.folder_subfolders),
                                style = MaterialTheme.typography.titleMedium,
                                modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp))
                            folderChildren.forEach { child ->
                                Row(Modifier.fillMaxWidth()
                                    .clickable { onOpen(AppRoute.Collection(LocalCollectionKind.FOLDER, child.key)) }
                                    .testTag("folder-child:" + child.key)
                                    .padding(horizontal = 16.dp, vertical = 12.dp),
                                    verticalAlignment = Alignment.CenterVertically) {
                                    Icon(AurelisIcons.Folder, null, Modifier.size(28.dp))
                                    Column(Modifier.weight(1f).padding(start = 12.dp)) {
                                        Text(child.label, style = MaterialTheme.typography.titleSmall)
                                        Text(pluralStringResource(R.plurals.settings_song_count,
                                            child.songs.size, child.songs.size),
                                            style = MaterialTheme.typography.bodySmall)
                                    }
                                    Icon(AurelisIcons.ChevronRight, null)
                                }
                            }
                        }
                    }
                }
                if (route.kind == LocalCollectionKind.ARTIST && artistAlbums.isNotEmpty()) {
                    item(key = "artist-albums") {
                        LocalArtistAlbumShelf(artistAlbums) { chosen ->
                            onOpen(AppRoute.Collection(LocalCollectionKind.ALBUM, chosen.key))
                        }
                    }
                }
                if (route.kind == LocalCollectionKind.ARTIST && displayEntries.isNotEmpty()) {
                    item(key = "artist-songs-title") {
                        Text(stringResource(R.string.artist_local_songs),
                            style = MaterialTheme.typography.titleMedium,
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp))
                    }
                }
                itemsIndexed(displayEntries, key = { _, entry -> "${entry.index}:${entry.value.id}" }) { index, entry ->
                    val song = entry.value
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        if (selectionMode) Checkbox(checked = song.id in selected, onCheckedChange = { checked ->
                            selected = if (checked) selected + song.id else selected - song.id
                        }, modifier = Modifier.semantics { contentDescription = "Seleccionar ${song.title}" })
                        if (route.kind == LocalCollectionKind.ALBUM) {
                            // Original release numbering does not change when the view sorts.
                            val number = when {
                                song.trackNumber != null && (song.discNumber ?: 1) > 1 ->
                                    "${song.discNumber}.${song.trackNumber}"
                                song.trackNumber != null -> song.trackNumber.toString()
                                else -> (entry.index + 1).toString()
                            }
                            Text(number, modifier = Modifier.width(38.dp)
                                .testTag("album-track-number:" + song.id),
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 1, overflow = TextOverflow.Ellipsis,
                                textAlign = TextAlign.Center)
                        }
                        SongListItem(song, song.id == currentSongId && isPlaying, {
                            if (selectionMode) selected = if (song.id in selected) selected - song.id else selected + song.id
                            else onPlay(displaySongs, index)
                        }, Modifier.weight(1f))
                        IconButton(onClick = { store.toggleFavorite(song.id) },
                            enabled = !selectionMode && !user.readOnly) {
                            Icon(if (song.id in user.favorites) Icons.Rounded.Favorite else Icons.Rounded.FavoriteBorder,
                                stringResource(R.string.favorite_action, song.title))
                        }
                        IconButton(onClick = { name = ""; addSongId = song.id },
                            enabled = !selectionMode && !user.readOnly) {
                            Icon(Icons.AutoMirrored.Rounded.PlaylistAdd, stringResource(R.string.playlist_add_track, song.title))
                        }
                        if (playlist != null && !selectionMode) Box {
                            IconButton(onClick = { orderSong = song.id }, enabled = !user.readOnly) { Icon(Icons.Rounded.MoreVert, "Orden de ${song.title}") }
                            val savedIndex = entry.index
                            DropdownMenu(expanded = orderSong == song.id, onDismissRequest = { orderSong = null }) {
                                DropdownMenuItem(text = { Text("Subir") }, enabled = savedIndex > 0,
                                    onClick = { store.movePlaylistSong(playlist.id, savedIndex, savedIndex - 1); orderSong = null })
                                DropdownMenuItem(text = { Text("Bajar") }, enabled = savedIndex < playlist.songIds.lastIndex,
                                    onClick = { store.movePlaylistSong(playlist.id, savedIndex, savedIndex + 1); orderSong = null })
                            }
                        }
                        if (playlist != null && !selectionMode) IconButton(onClick = {
                            store.setPlaylistSongs(playlist.id, playlist.songIds.filterIndexed { position, _ -> position != entry.index })
                        }, enabled = !user.readOnly) { Icon(Icons.Rounded.Delete, stringResource(R.string.playlist_remove_track, song.title)) }
                    }
                }
            }
        }
    }
    if (bulkPlaylist) AlertDialog(
        onDismissRequest = { bulkPlaylist = false }, title = { Text("Añadir selección a playlist") },
        text = { Column(Modifier.heightIn(max = 360.dp).verticalScroll(rememberScrollState())) {
            if (user.playlists.isEmpty()) Text("Crea una playlist en Biblioteca para añadir canciones")
            user.playlists.forEach { item -> TextButton(enabled = !user.readOnly, onClick = {
                store.setPlaylistSongs(item.id, item.songIds + selectedSongs.map { it.id }); bulkPlaylist = false
            }) { Text(item.name) } }
        } }, confirmButton = { TextButton(onClick = { bulkPlaylist = false }) { Text("Cerrar") } },
    )
    if (editing) AlertDialog(
        modifier = Modifier.width(320.dp),
        properties = DialogProperties(usePlatformDefaultWidth = false),
        onDismissRequest = { editing = false },
        title = { Text(stringResource(R.string.home_playlists)) },
        text = { OutlinedTextField(name, { name = it.take(100) }, singleLine = true, label = { Text(stringResource(R.string.playlist_name)) }) },
        confirmButton = { TextButton(enabled = name.isNotBlank(), onClick = {
            if (playlist == null) store.createPlaylist(name) else store.renamePlaylist(playlist.id, name)
            editing = false
        }) { Text(stringResource(R.string.collection_save)) } },
        dismissButton = { TextButton(onClick = { editing = false }) { Text(stringResource(R.string.collection_cancel)) } },
    )
    if (deleting && playlist != null) AlertDialog(
        modifier = Modifier.width(320.dp),
        properties = DialogProperties(usePlatformDefaultWidth = false),
        onDismissRequest = { deleting = false }, title = { Text(stringResource(R.string.playlist_delete)) },
        text = { Text(stringResource(R.string.playlist_delete_confirm, playlist.name)) },
        confirmButton = { TextButton(onClick = { store.deletePlaylist(playlist.id); deleting = false; onBack() }) { Text(stringResource(R.string.playlist_delete)) } },
        dismissButton = { TextButton(onClick = { deleting = false }) { Text(stringResource(R.string.collection_cancel)) } },
    )
    if (addSongId != null) AlertDialog(
        modifier = Modifier.width(320.dp),
        properties = DialogProperties(usePlatformDefaultWidth = false),
        onDismissRequest = { addSongId = null }, title = { Text(stringResource(R.string.home_playlists)) },
        text = { Column(Modifier.heightIn(max = 360.dp).verticalScroll(rememberScrollState())) {
            user.playlists.forEach { item -> TextButton(onClick = {
                store.setPlaylistSongs(item.id, item.songIds + requireNotNull(addSongId)); addSongId = null
            }) { Text(item.name) } }
            OutlinedTextField(name, { name = it.take(100) }, singleLine = true, label = { Text(stringResource(R.string.playlist_name)) })
        } },
        confirmButton = { TextButton(enabled = name.isNotBlank(), onClick = {
            store.createPlaylist(name, listOf(requireNotNull(addSongId))); addSongId = null
        }) { Text(stringResource(R.string.playlist_create)) } },
        dismissButton = { TextButton(onClick = { addSongId = null }) { Text(stringResource(R.string.collection_cancel)) } },
    )
}

/** Prefer an unambiguous local ID; legacy name-based routes must not open the wrong album. */
internal fun resolveLocalAlbum(albums: List<LocalAlbum>, routeKey: String): LocalAlbum? {
    albums.firstOrNull { it.key == routeKey }?.let { return it }
    if (routeKey.startsWith("media:")) return null
    return albums.filter { album ->
        album.songs.any { song ->
            "${album.name.length}:${album.name}${song.artist}" == routeKey
        }
    }.singleOrNull()
}

/** Existing MediaStore identity wins; a display-name fallback may resolve only one artist. */
internal fun resolveLocalArtist(artists: List<LocalArtist>, routeKey: String): LocalArtist? =
    artists.firstOrNull { it.key == routeKey }
        ?: if (routeKey.startsWith("media:")) null
        else artists.filter { it.name == routeKey }.singleOrNull()


/** Sort a current-catalogue snapshot without mutating its playlist source indexes. */
internal fun sortCollectionEntries(
    entries: List<IndexedValue<LocalSong>>, descending: Boolean, dateSort: String,
): List<IndexedValue<LocalSong>> = when (dateSort) {
    "added" -> entries.sortedWith(compareByDescending<IndexedValue<LocalSong>> {
        it.value.dateAddedMs.takeIf { date -> date > 0L } ?: Long.MIN_VALUE
    }.thenBy { LocalSearch.normalize(it.value.title) })
    "modified" -> entries.sortedWith(compareByDescending<IndexedValue<LocalSong>> {
        it.value.dateModifiedMs.takeIf { date -> date > 0L } ?: Long.MIN_VALUE
    }.thenBy { LocalSearch.normalize(it.value.title) })
    else -> if (descending) entries.sortedWith(compareByDescending<IndexedValue<LocalSong>> {
        LocalSearch.normalize(it.value.title)
    }.thenBy { it.index }) else entries
}
