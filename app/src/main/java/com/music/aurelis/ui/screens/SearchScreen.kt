// Aurelis © 2026. GPL-3.0-or-later
// Result sections/rows adapted from BitChord b690dc146009d4815825fa4a20815ef2e4ee8898, GPLv3.
// Original contributors retained; Aurelis local catalog and callbacks remain authoritative.
package com.music.aurelis.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.History
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import coil3.request.ImageRequest
import kotlinx.coroutines.launch
import com.music.aurelis.R
import com.music.aurelis.data.model.LocalSong
import com.music.aurelis.data.model.SortOption
import com.music.aurelis.ui.components.*

enum class LocalSearchCategory(@get:androidx.annotation.StringRes val labelRes: Int) {
    ALL(R.string.local_search_all), SONGS(R.string.songs), ALBUMS(R.string.albums),
    ARTISTS(R.string.artists), PLAYLISTS(R.string.local_music_playlists), GENRES(R.string.local_search_genres)
}

data class LocalSearchDestination(
    val title: String,
    val detail: String,
    val category: LocalSearchCategory,
    val key: String,
    val artworkUri: String? = null,
    val onOpen: () -> Unit,
)

@Composable
fun SearchScreen(
    query: String,
    onQueryChange: (String) -> Unit,
    results: List<LocalSong>?,
    listState: LazyListState,
    focusRequested: Boolean,
    onFocusHandled: () -> Unit,
    onSongClick: (List<LocalSong>, Int) -> Unit,
    onSongLongPress: (LocalSong) -> Unit,
    history: List<String>,
    onSubmit: () -> Unit,
    onHistoryClick: (String) -> Unit,
    onHistoryRemove: (String) -> Unit,
    onHistoryClear: () -> Unit,
    currentSong: LocalSong?,
    isPlaying: Boolean,
    filter: SortOption = SortOption.TITLE_ASC,
    onFilterChange: (SortOption) -> Unit = {},
    modifier: Modifier = Modifier,
    contentPadding: PaddingValues,
    topPadding: Dp,
    destinations: List<LocalSearchDestination> = emptyList(),
    category: LocalSearchCategory = LocalSearchCategory.ALL,
    onCategoryChange: (LocalSearchCategory) -> Unit = {},
    onOpenProfile: (() -> Unit)? = null,
    contentBackdropModifier: Modifier = Modifier,
) {
    val focusRequester = remember { FocusRequester() }
    val scope = rememberCoroutineScope()
    val keyboardController = LocalSoftwareKeyboardController.current

    LaunchedEffect(focusRequested) {
        if (focusRequested) {
            focusRequester.requestFocus()
            keyboardController?.show()
            onFocusHandled()
        }
    }

    // A category with no matches must retain the controls that let the user leave it.
    val songs = if (category == LocalSearchCategory.ALL || category == LocalSearchCategory.SONGS) results.orEmpty() else emptyList()
    val collections = destinations.filter { category == LocalSearchCategory.ALL || it.category == category }

    Column(modifier = modifier.fillMaxSize()) {
        FrostedTopBar(title = stringResource(R.string.search), scrolled = { false },
            transparentBackdrop = true, artworkPageChrome = true,
            accessory = { SearchField(
                query = query,
                onQueryChange = onQueryChange,
                onSubmit = onSubmit,
                focusRequester = focusRequester,
                placeholder = stringResource(R.string.local_search_hint),
            ) }, actions = { onOpenProfile?.let { LocalProfileButton(it) } })
        Column(modifier = Modifier.weight(1f).fillMaxWidth()
            .then(contentBackdropModifier).padding(top = topPadding)) {
            if (results != null) {
                PillSegmentedControl(
                    items = LocalSearchCategory.entries.map { stringResource(it.labelRes) },
                    selectedIndex = category.ordinal,
                    onItemSelected = {
                        onCategoryChange(LocalSearchCategory.entries[it])
                        scope.launch { listState.scrollToItem(0) }
                    },
                    modifier = Modifier.padding(horizontal = PAGE_GUTTER).padding(bottom = 6.dp)
                        .testTag("local-search-categories"),
                )
            }
            if (results != null && category in listOf(LocalSearchCategory.ALL, LocalSearchCategory.SONGS)) {
                val orders = listOf(SortOption.TITLE_ASC, SortOption.ARTIST_ASC, SortOption.ALBUM_ASC)
                PillSegmentedControl(
                    items = listOf(R.string.local_search_title, R.string.local_search_artist, R.string.local_search_album)
                        .map { stringResource(it) },
                    selectedIndex = orders.indexOf(filter),
                    onItemSelected = {
                        onFilterChange(orders[it])
                        scope.launch { listState.scrollToItem(0) }
                    },
                    modifier = Modifier.padding(horizontal = PAGE_GUTTER).padding(bottom = 6.dp)
                        .testTag("local-search-order"),
                )
            }
            LazyColumn(
                state = listState,
                modifier = Modifier.weight(1f).fillMaxWidth().testTag("local-search-results"),
                contentPadding = PaddingValues(bottom = contentPadding.calculateBottomPadding()),
            ) {
                when {
                    results == null && query.isEmpty() -> {
                        if (history.isEmpty()) {
                            item { MessageState(stringResource(R.string.local_search_start)) }
                        } else {
                            recentSearches(
                                history = history,
                                onClick = onHistoryClick,
                                onRemove = onHistoryRemove,
                                onClear = onHistoryClear,
                            )
                        }
                    }
                    results == null -> item { MessageState(stringResource(R.string.local_search_type)) }
                    songs.isEmpty() && collections.isEmpty() -> item {
                        MessageState(stringResource(R.string.search_library_no_results, query))
                    }
                    else -> {
                        // Original ordering: songs, artists, albums, playlists, then other local groups.
                        if (songs.isNotEmpty()) {
                            if (category == LocalSearchCategory.ALL) searchSection(LocalSearchCategory.SONGS)
                            itemsIndexed(songs, key = { index, song -> "song:${song.id}:$index" }) { index, song ->
                                SongRow(
                                    song = song,
                                    onClick = { onSongClick(songs, index) },
                                    onLongPress = { onSongLongPress(song) },
                                    isCurrent = currentSong?.id == song.id,
                                    isPlaying = isPlaying && currentSong?.id == song.id,
                                    searchPlayingStyle = true,
                                    activeTint = PlayingAccent,
                                )
                                if (index < songs.lastIndex) SearchDivider()
                            }
                        }
                        for (kind in listOf(LocalSearchCategory.ARTISTS, LocalSearchCategory.ALBUMS,
                            LocalSearchCategory.PLAYLISTS, LocalSearchCategory.GENRES)) {
                            val section = collections.filter { it.category == kind }
                            if (section.isNotEmpty()) {
                                if (category == LocalSearchCategory.ALL) searchSection(kind)
                                itemsIndexed(section, key = { _, row -> "destination:${row.category}:${row.key}" }) { index, row ->
                                    SearchDestinationRow(row)
                                    if (index < section.lastIndex) SearchDivider()
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

private fun LazyListScope.searchSection(category: LocalSearchCategory) {
    item(key = "search-section:${category.name}") {
        Text(
            text = stringResource(category.labelRes),
            modifier = Modifier.padding(start = PAGE_GUTTER, end = PAGE_GUTTER, top = 16.dp, bottom = 6.dp)
                .testTag("search-section:${category.name}"),
            style = MaterialTheme.typography.titleSmall,
            color = MaterialTheme.colorScheme.onSurface,
        )
    }
}

@Composable
private fun SearchDivider() {
    HorizontalDivider(
        modifier = Modifier.padding(start = ROW_DIVIDER_INSET),
        thickness = 0.5.dp,
        color = MaterialTheme.colorScheme.outline,
    )
}

@Composable
private fun SearchDestinationRow(destination: LocalSearchDestination) {
    val shape = if (destination.category == LocalSearchCategory.ARTISTS) CircleShape else RoundedCornerShape(8.dp)
    val context = LocalContext.current
    val image = remember(context, destination.artworkUri) {
        ImageRequest.Builder(context).data(destination.artworkUri).size(104).build()
    }
    Row(
        modifier = Modifier.fillMaxWidth().testTag("search-destination:${destination.category}:${destination.key}")
            .clickable(onClick = destination.onOpen)
            .padding(horizontal = PAGE_GUTTER, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        AsyncImage(
            model = image,
            contentDescription = null,
            modifier = Modifier.size(52.dp).clip(shape).background(MaterialTheme.colorScheme.surfaceVariant),
        )
        Spacer(Modifier.width(14.dp))
        Column(Modifier.weight(1f)) {
            Text(destination.title, style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onBackground, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(destination.detail, style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
    }
}

private fun androidx.compose.foundation.lazy.LazyListScope.recentSearches(
    history: List<String>,
    onClick: (String) -> Unit,
    onRemove: (String) -> Unit,
    onClear: () -> Unit,
) {
    item(key = "recent:header") {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = PAGE_GUTTER, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = stringResource(R.string.local_search_recent),
                style = MaterialTheme.typography.headlineMedium,
                color = MaterialTheme.colorScheme.onBackground,
                modifier = Modifier.weight(1f),
            )
            Text(
                text = stringResource(R.string.local_search_clear_history),
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier
                    .clip(RoundedCornerShape(percent = 50))
                    .clickable(onClick = onClear)
                    .padding(horizontal = 10.dp, vertical = 4.dp),
            )
        }
    }
    // Query text can be "header"; keep item keys disjoint from recent:header.
    itemsIndexed(history, key = { _, term -> "recent:query:$term" }) { _, term ->
        Row(
            modifier = Modifier.fillMaxWidth()
                .clickable { onClick(term) }
                .padding(start = PAGE_GUTTER, end = 8.dp, top = 6.dp, bottom = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                Icons.Rounded.History,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(22.dp),
            )
            Spacer(Modifier.width(16.dp))
            Text(
                text = term,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onBackground,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f),
            )
            Box(
                modifier = Modifier.size(40.dp).clip(CircleShape).clickable { onRemove(term) },
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    Icons.Rounded.Close,
                    contentDescription = stringResource(R.string.local_search_remove_history, term),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(18.dp),
                )
            }
        }
    }
}
