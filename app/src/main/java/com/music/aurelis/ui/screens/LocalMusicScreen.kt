// SPDX-FileCopyrightText: 2026 Aurelis
// SPDX-License-Identifier: LGPL-3.0-or-later
package com.music.aurelis.ui.screens

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.ui.Modifier
import androidx.compose.runtime.*
import androidx.lifecycle.viewmodel.compose.viewModel
import com.music.aurelis.LibraryViewModel
import com.music.aurelis.data.LibraryState
import com.music.aurelis.data.model.LocalAlbum
import com.music.aurelis.data.model.LocalArtist
import com.music.aurelis.data.model.LocalSong
import com.music.aurelis.playback.PlaybackViewModel

@Composable
fun LocalMusicScreen(
    playbackViewModel: PlaybackViewModel,
    onBackClick: () -> Unit,
    libraryViewModel: LibraryViewModel = viewModel(),
    onAlbumOpen: ((LocalAlbum) -> Unit)? = null,
    onArtistOpen: ((LocalArtist) -> Unit)? = null,
    onRequestPermission: () -> Unit = {},
    onCollectionOpen: ((com.music.aurelis.ui.navigation.AppRoute.Collection) -> Unit)? = null,
    onSongActions: (LocalSong) -> Unit = {},
    onProfile: () -> Unit = {},
    contentPadding: PaddingValues = PaddingValues(),
    contentBackdropModifier: Modifier = Modifier,
) {
    val uiState by libraryViewModel.uiState.collectAsState()
    val playback by playbackViewModel.state.collectAsState()
    val store by com.music.aurelis.ui.AppUi.host.libraries.collectAsState()
    val user by store.state.collectAsState()
    LocalMusicContent(uiState.libraryState, uiState.filteredSongs, user.playlists,
        playback.currentSong?.id, playback.isPlaying, uiState.sortOption,
        libraryViewModel::onSortOptionChanged, onBackClick, { libraryViewModel.refresh() }, onRequestPermission,
        playbackViewModel::playSongs, { page ->
            if (onCollectionOpen != null) onCollectionOpen(page)
            else when (page.kind) {
                com.music.aurelis.ui.navigation.LocalCollectionKind.ALBUM ->
                    (uiState.libraryState as? LibraryState.Success)?.albums?.firstOrNull { it.key == page.key }?.let { onAlbumOpen?.invoke(it) }
                com.music.aurelis.ui.navigation.LocalCollectionKind.ARTIST ->
                    (uiState.libraryState as? LibraryState.Success)?.artists?.firstOrNull { it.key == page.key }?.let { onArtistOpen?.invoke(it) }
                else -> Unit
            }
        }, onSongActions, onProfile, contentPadding, contentBackdropModifier,
        isRefreshing = uiState.isRefreshing)
}
