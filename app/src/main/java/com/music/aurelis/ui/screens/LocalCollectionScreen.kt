package com.music.aurelis.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material.icons.rounded.FavoriteBorder
import androidx.compose.material.icons.automirrored.rounded.PlaylistAdd
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.DialogProperties
import com.music.aurelis.R
import com.music.aurelis.data.LibraryState
import com.music.aurelis.data.LocalUserLibrary
import com.music.aurelis.data.model.LocalSong
import com.music.aurelis.ui.components.SongListItem
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
) {
    val user by store.state.collectAsState()
    val snapshot = library as? LibraryState.Success
    val allSongs = snapshot?.songs.orEmpty()
    val playlist = user.playlists.firstOrNull { it.id == route.key }
    val album = snapshot?.albums?.firstOrNull { item -> item.key == route.key || item.songs.any {
        "${item.name.length}:${item.name}${it.artist}" == route.key
    } }
    val artist = snapshot?.artists?.firstOrNull { it.key == route.key || it.name == route.key }
    val byId = remember(allSongs) { allSongs.associateBy { it.id } }
    val songs = when (route.kind) {
        LocalCollectionKind.FAVORITES -> allSongs.filter { it.id in user.favorites }
        LocalCollectionKind.PLAYLIST -> playlist?.songIds?.mapNotNull(byId::get).orEmpty()
        LocalCollectionKind.ALBUM -> album?.songs.orEmpty()
        LocalCollectionKind.ARTIST -> artist?.songs.orEmpty()
        LocalCollectionKind.PLAYLISTS -> emptyList()
    }
    val title = when (route.kind) {
        LocalCollectionKind.FAVORITES -> stringResource(R.string.home_favorites)
        LocalCollectionKind.PLAYLISTS -> stringResource(R.string.home_playlists)
        LocalCollectionKind.PLAYLIST -> playlist?.name.orEmpty()
        LocalCollectionKind.ALBUM -> album?.name.orEmpty()
        LocalCollectionKind.ARTIST -> artist?.name.orEmpty()
    }
    var editing by rememberSaveable { mutableStateOf(false) }
    var name by rememberSaveable { mutableStateOf("") }
    var deleting by remember { mutableStateOf(false) }
    var addSongId by rememberSaveable { mutableStateOf<String?>(null) }

    Column(Modifier.fillMaxSize().statusBarsPadding().padding(16.dp)) {
        TextButton(onClick = onBack) { Text(stringResource(R.string.back)) }
        Text(title.ifBlank { stringResource(R.string.collection_unavailable) }, style = MaterialTheme.typography.headlineMedium)
        if (user.readOnly) Text(stringResource(R.string.collections_unavailable))
        if (route.kind == LocalCollectionKind.PLAYLISTS) {
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
            if (library == LibraryState.Loading) CircularProgressIndicator()
            if (library is LibraryState.Error) Text(library.message)
            if (songs.isEmpty() && library is LibraryState.Success) Text(stringResource(R.string.collection_empty))
            if (playlist != null && playlist.songIds.size > songs.size) Text(stringResource(R.string.collection_missing_tracks))
            Button(onClick = { onPlay(songs, 0) }, enabled = songs.isNotEmpty()) { Text(stringResource(R.string.collection_play)) }
            LazyColumn(Modifier.weight(1f), contentPadding = PaddingValues(bottom = 24.dp)) {
                itemsIndexed(songs, key = { _, item -> item.id }) { index, song ->
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        SongListItem(song, song.id == currentSongId && isPlaying, { onPlay(songs, index) }, Modifier.weight(1f))
                        IconButton(onClick = { store.toggleFavorite(song.id) }, enabled = !user.readOnly) {
                            Icon(if (song.id in user.favorites) Icons.Rounded.Favorite else Icons.Rounded.FavoriteBorder,
                                stringResource(R.string.favorite_action, song.title))
                        }
                        IconButton(onClick = { name = ""; addSongId = song.id }, enabled = !user.readOnly) {
                            Icon(Icons.AutoMirrored.Rounded.PlaylistAdd, stringResource(R.string.playlist_add_track, song.title))
                        }
                        if (playlist != null) IconButton(onClick = {
                            store.setPlaylistSongs(playlist.id, playlist.songIds.filterNot { it == song.id })
                        }, enabled = !user.readOnly) { Icon(Icons.Rounded.Delete, stringResource(R.string.playlist_remove_track, song.title)) }
                    }
                }
            }
        }
    }
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
