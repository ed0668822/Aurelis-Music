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
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.activity.compose.BackHandler
import com.music.aurelis.data.*
import com.music.aurelis.data.model.*
import com.music.aurelis.ui.components.*
import com.music.aurelis.ui.icons.AurelisIcons
import com.music.aurelis.ui.navigation.*

@Composable
internal fun LocalMusicContent(library: LibraryState, songs: List<LocalSong>, playlists: List<LocalPlaylist>,
    currentSongId: String?, isPlaying: Boolean, sort: SortOption, onSort: (SortOption) -> Unit,
    onBack: () -> Unit, onRetry: () -> Unit, onPermission: () -> Unit,
    onPlay: (List<LocalSong>, Int) -> Unit, onOpen: (AppRoute.Collection) -> Unit,
    onActions: (LocalSong) -> Unit, onProfile: () -> Unit, bottomPadding: PaddingValues = PaddingValues()) {
    var selected by rememberSaveable { mutableIntStateOf(0) }
    var query by rememberSaveable { mutableStateOf("") }
    var search by rememberSaveable { mutableStateOf(false) }
    var sorting by remember { mutableStateOf(false) }
    val list = rememberLazyListState()
    val scope = rememberCoroutineScope()
    BackHandler(search) { search = false; query = "" }
    val success = library as? LibraryState.Success
    val filtered = remember(songs, query) { songs.filter { LocalSearch.matches(it, query) } }
    val visibleIds = remember(filtered) { filtered.map { it.id }.toHashSet() }
    val tabs = listOf("Canciones", "Artistas", "Álbumes", "Playlists", "Carpetas")
    BoxWithConstraints(Modifier.fillMaxSize()) {
        val sleeve = (maxWidth * 0.76f).coerceAtMost(maxHeight * 0.5f)
        LazyColumn(state = list, modifier = Modifier.fillMaxSize().testTag("local-music-list"),
            contentPadding = bottomPadding) {
            item(key = "header") {
                if (success != null && !search) LocalDetailHeader("Local Music", "Archivos de audio en el dispositivo",
                    "${songs.size} canciones · ${songs.sumOf { it.durationMs.coerceAtLeast(0) } / 60_000} min",
                    sleeve, null, AurelisIcons.Folder, { onPlay(filtered, 0) },
                    { onPlay(filtered.shuffled(), 0) }, filtered.isNotEmpty())
                else Text("Local Music", style = MaterialTheme.typography.headlineLarge,
                    modifier = Modifier.padding(start = 16.dp, top = topBarContentPadding(), bottom = 16.dp))
                if (search) SearchField(query, { query = it }, onSubmit = {}, modifier = Modifier.padding(horizontal = 16.dp))
                LocalFolderTabs(tabs, selected, { next ->
                    if (selected != next) { selected = next; scope.launch { list.scrollToItem(0) } }
                })
            }
            when (library) {
                LibraryState.Loading -> item { Box(Modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) { CircularProgressIndicator() } }
                is LibraryState.Error -> item { Column(Modifier.padding(24.dp)) {
                    Text(library.message)
                    TextButton(onClick = if (library.permissionRequired) onPermission else onRetry) {
                        Text(if (library.permissionRequired) "Conceder acceso a música" else "Reintentar")
                    }
                } }
                is LibraryState.Success -> when (selected) {
                    0 -> {
                        if (filtered.isEmpty()) item { Text("No hay canciones", Modifier.padding(24.dp)) }
                        itemsIndexed(filtered, key = { _, song -> song.id }) { index, song ->
                            SongListItem(song, currentSongId == song.id && isPlaying, { onPlay(filtered, index) }, onMore = { onActions(song) })
                            HorizontalDivider(Modifier.padding(start = 84.dp), thickness = 0.5.dp)
                        }
                    }
                    1 -> items(success?.artists.orEmpty().filter { it.songs.any { song -> song.id in visibleIds } }, key = { it.key }) {
                        LocalEntityRow(it.name, "${it.songCount} canciones", it.artworkUri, AurelisIcons.Artist,
                            round = true, onClick = { onOpen(AppRoute.Collection(LocalCollectionKind.ARTIST, it.key)) })
                    }
                    2 -> items(success?.albums.orEmpty().filter { it.songs.any { song -> song.id in visibleIds } }, key = { it.key }) {
                        LocalEntityRow(it.name, it.artist, it.artworkUri, AurelisIcons.Album,
                            onClick = { onOpen(AppRoute.Collection(LocalCollectionKind.ALBUM, it.key)) })
                    }
                    3 -> {
                        item { TextButton(onClick = { onOpen(AppRoute.Collection(LocalCollectionKind.PLAYLISTS)) }) { Text("Gestionar playlists") } }
                        items(playlists.filter { LocalSearch.normalize(it.name).contains(LocalSearch.normalize(query)) }, key = { it.id }) {
                            val playlist = it
                            LocalEntityRow(it.name, "${it.songIds.size} canciones",
                                songs.firstOrNull { song -> song.id == playlist.songIds.firstOrNull() }?.artworkUri,
                                AurelisIcons.Playlist, onClick = { onOpen(AppRoute.Collection(LocalCollectionKind.PLAYLIST, playlist.id)) })
                        }
                    }
                    4 -> items(LocalFolderRepository.groups(filtered), key = { it.key }) {
                        LocalEntityRow(it.label, "${it.songs.size} canciones", null, AurelisIcons.Folder,
                            onClick = { onOpen(AppRoute.Collection(LocalCollectionKind.FOLDER, it.key)) })
                    }
                }
            }
            if (success != null && selected in 1..2) {
                val empty = if (selected == 1) success.artists.none { artist -> artist.songs.any { it.id in visibleIds } }
                    else success.albums.none { album -> album.songs.any { it.id in visibleIds } }
                if (empty) item(key = "empty-category") {
                    Text(if (query.isNotBlank()) "Sin resultados" else if (selected == 1) "No hay artistas" else "No hay álbumes",
                        Modifier.padding(24.dp))
                }
            }
        }
        FrostedTopBar("Local Music", { false }, transparentBackdrop = true, artworkPageChrome = true,
            onBack = onBack, actions = {
                IconButton(onClick = {
                    search = !search
                    if (!search) query = ""
                    else scope.launch { list.scrollToItem(0) }
                }) { Icon(if (search) AurelisIcons.Close else AurelisIcons.Search,
                    if (search) "Cerrar búsqueda de esta lista" else "Buscar en esta lista") }
                Box {
                    IconButton(onClick = { sorting = true }) { Icon(AurelisIcons.Sort, "Ordenar") }
                    DropdownMenu(sorting, { sorting = false }) {
                        SortOption.entries.forEach { option -> DropdownMenuItem(text = { Text(option.label) },
                            leadingIcon = { if (option == sort) Icon(AurelisIcons.Check, null) },
                            onClick = { sorting = false; onSort(option) }) }
                    }
                }
                IconButton(onClick = onRetry) { Icon(AurelisIcons.Refresh, "Recargar") }
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
    val shape = RoundedCornerShape(percent = 50)
    Row(Modifier.padding(horizontal = 16.dp, vertical = 8.dp).fillMaxWidth().clip(shape)
        .background(MaterialTheme.colorScheme.surfaceVariant).horizontalScroll(rememberScrollState())
        .selectableGroup().padding(4.dp)) {
        tabs.forEachIndexed { index, label ->
            Box(Modifier.clip(shape).background(if (selected == index) MaterialTheme.colorScheme.background else Color.Transparent)
                .selectable(selected == index, role = Role.Tab) { onSelect(index) }
                .heightIn(min = 48.dp).padding(horizontal = 18.dp), contentAlignment = Alignment.Center) {
                Text(label, style = MaterialTheme.typography.bodyLarge)
            }
        }
    }
}
