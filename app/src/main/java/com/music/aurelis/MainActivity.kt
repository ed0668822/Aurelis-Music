// Aurelis © 2026. GPL-3.0-or-later
package com.music.aurelis

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Explore
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.LibraryMusic
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.music.aurelis.data.AppUpdateChecker
import com.music.aurelis.data.AudioPermission
import com.music.aurelis.data.LocalSearch
import androidx.compose.runtime.DisposableEffect
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import android.provider.Settings
import com.music.aurelis.data.LocalHomeData
import com.music.aurelis.ui.navigation.LocalCollectionKind
import com.music.aurelis.ui.screens.LocalCollectionScreen
import com.music.aurelis.data.LibraryState
import com.music.aurelis.data.model.LocalSong
import com.music.aurelis.playback.PlaybackViewModel
import com.music.aurelis.ui.components.BottomTab
import com.music.aurelis.ui.components.GlassNavBar
import com.music.aurelis.ui.components.UpdateAvailableDialog
import com.music.aurelis.ui.icons.AurelisIcons
import com.music.aurelis.ui.screens.HomeScreen
import com.music.aurelis.ui.screens.LibraryLink
import com.music.aurelis.ui.screens.LibraryScreen
import com.music.aurelis.ui.screens.LocalMusicScreen
import com.music.aurelis.ui.screens.NowPlayingScreen
import com.music.aurelis.ui.screens.QueueScreen
import com.music.aurelis.ui.screens.ReplayScreen
import com.music.aurelis.ui.components.SongActionsDialog
import com.music.aurelis.ui.screens.SearchScreen
import com.music.aurelis.ui.screens.SettingsScreen
import com.music.aurelis.ui.theme.AurelisTheme
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.hazeSource
import com.music.aurelis.ui.components.backdrop.backdrops.layerBackdrop
import kotlinx.coroutines.launch

import androidx.compose.ui.res.stringResource
import androidx.compose.material3.TextButton
import androidx.compose.foundation.layout.Arrangement
import com.music.aurelis.ui.navigation.AppNavigator
import com.music.aurelis.ui.navigation.AppNavigationHost
import com.music.aurelis.ui.navigation.AppRoute
import com.music.aurelis.ui.navigation.AppTab
import com.music.aurelis.ui.navigation.rememberAppNavigator
import com.music.aurelis.ui.screens.EqualizerScreen

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            AurelisTheme {
                Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
                    AurelisApp()
                }
            }
        }
    }
}

@Composable
private fun AurelisApp() {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val playbackViewModel: PlaybackViewModel = viewModel()
    val libraryViewModel: LibraryViewModel = viewModel()
    val navigator = rememberAppNavigator()
    val hazeState = remember { HazeState() }
    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission(),
    ) { libraryViewModel.onForeground() }
    var showUpdateDialog by rememberSaveable { mutableStateOf(false) }
    val updateInfo by AppUpdateChecker.available.collectAsState()
    val downloadState by AppUpdateChecker.download.collectAsState()

    val meta = remember(context) { context.getSharedPreferences("app_meta", android.content.Context.MODE_PRIVATE) }
    val requestAudioAccess = {
        if (AudioPermission.needsSettings(context, meta.getBoolean("audio_permission_requested", false))) {
            context.startActivity(Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.parse("package:${context.packageName}")))
        } else {
            meta.edit().putBoolean("audio_permission_requested", true).apply()
            permissionLauncher.launch(AudioPermission.requiredPermission())
        }
    }
    LaunchedEffect(Unit) {
        playbackViewModel.connect()
        if (!AudioPermission.isGranted(context) && !meta.getBoolean("audio_permission_requested", false)) requestAudioAccess()
    }
    // An offline/slow update server must not delay audio permission or catalogue loading.
    LaunchedEffect(Unit) { AppUpdateChecker.check() }
    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner, libraryViewModel) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) libraryViewModel.onForeground()
            if (event == Lifecycle.Event.ON_STOP) {
                libraryViewModel.onBackground()
                com.music.aurelis.data.stats.ListeningStats.flush()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer); libraryViewModel.onBackground() }
    }
    LaunchedEffect(updateInfo) {
        if (updateInfo != null) showUpdateDialog = true
    }
    Box(Modifier.fillMaxSize()) {
        AurelisNavigationContent(navigator, playbackViewModel, libraryViewModel, hazeState,
            onRequestAudioPermission = requestAudioAccess)
        if (showUpdateDialog && updateInfo != null) {
            UpdateAvailableDialog(
                version = updateInfo!!.version,
                hazeState = hazeState,
                onDismiss = {
                    showUpdateDialog = false
                    AppUpdateChecker.resetDownload()
                },
                onDownload = { scope.launch { AppUpdateChecker.downloadApk(context) } },
                onCancelDownload = { AppUpdateChecker.cancelDownload() },
                onInstall = {
                    val state = downloadState
                    if (state is AppUpdateChecker.DownloadState.Ready) {
                        AppUpdateChecker.installApk(context, state.file)
                    }
                },
                onOpenReleasePage = {
                    val intent = Intent(Intent.ACTION_VIEW, Uri.parse(updateInfo!!.releaseUrl))
                    context.startActivity(intent)
                },
            )
        }
        // This modal owns Back before the page below it; no navigation is discarded.
        BackHandler(enabled = showUpdateDialog && updateInfo != null) {
            showUpdateDialog = false
            AppUpdateChecker.resetDownload()
        }
    }
}

/** UI graph separated from startup/service effects so navigation can be verified offline. */
@Composable
internal fun AurelisNavigationContent(
    navigator: AppNavigator,
    playbackViewModel: PlaybackViewModel,
    libraryViewModel: LibraryViewModel,
    hazeState: HazeState = remember { HazeState() },
    onRequestAudioPermission: () -> Unit = {},
) {
    val playbackState by playbackViewModel.state.collectAsState()
    val libraryUiState by libraryViewModel.uiState.collectAsState()
    val userStore by (LocalContext.current.applicationContext as AurelisApplication).libraries.collectAsState()
    val userState by userStore.state.collectAsState()
    var actionSong by remember { mutableStateOf<LocalSong?>(null) }
    val route = navigator.visibleRoute()
    LaunchedEffect(route) {
        // Let the existing ViewModel report missing permission instead of leaving Loading forever.
        if (route == AppRoute.LocalMusic) libraryViewModel.refresh()
    }
    val tabs = listOf(
        BottomTab(stringResource(R.string.home), Icons.Filled.Home),
        BottomTab(stringResource(R.string.explore), Icons.Filled.Explore),
        BottomTab(stringResource(R.string.library), Icons.Filled.LibraryMusic),
        BottomTab(stringResource(R.string.search), Icons.Filled.Search),
    )
    val librarySuccess = libraryUiState.libraryState as? LibraryState.Success
    val allSongs = librarySuccess?.songs ?: emptyList()
    val home = remember(allSongs, userState, playbackState.currentSong?.id) {
        LocalHomeData.build(allSongs, userState, playbackState.currentSong?.id)
    }

    val appBackdrop = com.music.aurelis.ui.components.backdrop.backdrops.rememberLayerBackdrop()
    val captureGlass = com.music.aurelis.ui.components.LocalGlassHardware.current && com.music.aurelis.ui.components.LocalLiquidGlassEnabled.current
    androidx.compose.runtime.CompositionLocalProvider(
        com.music.aurelis.ui.components.LocalAppBackdrop provides appBackdrop,
        com.music.aurelis.ui.components.LocalAppHazeState provides hazeState,
    ) {
    AppNavigationHost(
        navigator = navigator,
        modifier = Modifier.background(MaterialTheme.colorScheme.background),
        pageModifier = Modifier.hazeSource(hazeState).then(if (captureGlass) Modifier.layerBackdrop(appBackdrop) else Modifier),
        tabContent = { tab ->
            when (tab) {
            AppTab.HOME -> {
                val listState = rememberLazyListState()
                HomeScreen(
                    recentlyPlayed = home.recent,
                    mostPlayed = home.mostPlayed,
                    recentlyAdded = librarySuccess?.recentlyAdded ?: emptyList(),
                    albums = librarySuccess?.albums ?: emptyList(),
                    artists = librarySuccess?.artists ?: emptyList(),
                    listState = listState,
                    onSongClick = { song: LocalSong ->
                        val idx = allSongs.indexOfFirst { it.id == song.id }
                        if (idx >= 0) playbackViewModel.playSongs(allSongs, idx)
                    },
                    onSongLongPress = { actionSong = it },
                    onAlbumClick = { navigator.push(AppRoute.Collection(LocalCollectionKind.ALBUM, it.key)) },
                    onArtistClick = { navigator.push(AppRoute.Collection(LocalCollectionKind.ARTIST, it.key)) },
                    currentSong = playbackState.currentSong,
                    isPlaying = playbackState.isPlaying,
                    contentPadding = PaddingValues(bottom = 180.dp),
                    libraryState = libraryUiState.libraryState,
                    favorites = home.favorites,
                    recommendations = home.recommendations,
                    playlists = userState.playlists,
                    collectionsReadOnly = userState.readOnly,
                    onRetry = { libraryViewModel.refresh() },
                    onRequestPermission = onRequestAudioPermission,
                    onOpenPlayer = { playbackViewModel.resume(); navigator.push(AppRoute.NowPlaying) },
                    onAutoMix = { playbackViewModel.startAutoMix(allSongs, userState) },
                    isCreatingMix = playbackState.isCreatingMix, mixError = playbackState.mixError,
                    onOpenFavorites = { navigator.push(AppRoute.Collection(LocalCollectionKind.FAVORITES)) },
                    onOpenPlaylists = { navigator.push(AppRoute.Collection(LocalCollectionKind.PLAYLISTS)) },
                    onOpenPlaylist = { navigator.push(AppRoute.Collection(LocalCollectionKind.PLAYLIST, it)) },
                )
            }

            AppTab.EXPLORE -> Text(stringResource(R.string.navigation_explore_pending), modifier = Modifier.align(Alignment.Center))
            AppTab.LIBRARY -> {
                val listState = rememberLazyListState()
                val links = listOf(
                    LibraryLink(stringResource(R.string.songs), AurelisIcons.MusicNote) { navigator.push(AppRoute.LocalMusic) },
                    LibraryLink(stringResource(R.string.home_favorites), AurelisIcons.Heart) { navigator.push(AppRoute.Collection(LocalCollectionKind.FAVORITES)) },
                    LibraryLink(stringResource(R.string.home_playlists), AurelisIcons.Playlist) { navigator.push(AppRoute.Collection(LocalCollectionKind.PLAYLISTS)) },
                    LibraryLink(stringResource(R.string.replay), AurelisIcons.Clock) { navigator.push(AppRoute.Replay) },
                    LibraryLink(stringResource(R.string.navigation_settings), AurelisIcons.Home) { navigator.push(AppRoute.Settings) },
                )
                LibraryScreen(
                    listState = listState,
                    links = links,
                    onShowAll = {},
                    contentPadding = PaddingValues(bottom = 180.dp),
                )
            }
            AppTab.SEARCH -> {
                var searchQuery by rememberSaveable { mutableStateOf("") }
                val searchListState = rememberLazyListState()
                val results = remember(searchQuery, allSongs, libraryUiState.sortOption) {
                    if (searchQuery.isBlank()) null else {
                        val key = LocalSearch.normalize(searchQuery)
                        LocalSearch.sort(allSongs.filter { LocalSearch.matchesNormalized(it, key) }, libraryUiState.sortOption)
                    }
                }
                SearchScreen(
                    filter = libraryUiState.sortOption,
                    onFilterChange = libraryViewModel::onSortOptionChanged,
                    query = searchQuery,
                    onQueryChange = { searchQuery = it },
                    results = results,
                    listState = searchListState,
                    focusRequested = false,
                    onFocusHandled = { },
                    onSongClick = { songs, index -> playbackViewModel.playSongs(songs, index) },
                    onSongLongPress = { actionSong = it },
                    history = emptyList(),
                    onSubmit = { },
                    onHistoryClick = { },
                    onHistoryRemove = { },
                    onHistoryClear = { },
                    currentSong = playbackState.currentSong,
                    isPlaying = playbackState.isPlaying,
                    contentPadding = PaddingValues(bottom = 180.dp),
                    topPadding = 0.dp,
                )
            }
            }
        },
        routeContent = { page ->
            when (page) {
                AppRoute.LocalMusic -> LocalMusicScreen(
                    playbackViewModel = playbackViewModel,
                    libraryViewModel = libraryViewModel,
                    onBackClick = { navigator.pop() },
                    onAlbumOpen = { navigator.push(AppRoute.Collection(LocalCollectionKind.ALBUM, it.key)) },
                    onArtistOpen = { navigator.push(AppRoute.Collection(LocalCollectionKind.ARTIST, it.key)) },
                    onRequestPermission = onRequestAudioPermission,
                )
                AppRoute.Settings -> SettingsScreen(
                    isVisible = true,
                    onDismiss = { navigator.pop() },
                    onOpenEqualizer = { navigator.push(AppRoute.Equalizer) },
                    localSongs = allSongs,
                    onOpenFolder = { navigator.push(AppRoute.Collection(LocalCollectionKind.FOLDER, it)) },
                )
                AppRoute.Equalizer -> EqualizerScreen(onBack = { navigator.pop() })
                AppRoute.Replay -> ReplayScreen(onBack = { navigator.pop() }, store = userStore,
                    songs = allSongs, onPlay = { playbackViewModel.playSongs(listOf(it)) })
                is AppRoute.Collection -> LocalCollectionScreen(
                    page, libraryUiState.libraryState, userStore,
                    playbackState.currentSong?.id, playbackState.isPlaying,
                    onBack = { navigator.pop() }, onOpen = { navigator.push(it) },
                    onPlay = playbackViewModel::playSongs,
                )
                else -> error("Destination is not wired in the local graph: $page")
            }
        },
        bottomBar = {
            Column(
                modifier = Modifier.align(Alignment.BottomCenter).fillMaxWidth()
                    .navigationBarsPadding().padding(bottom = 16.dp, start = 12.dp, end = 12.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                if (!playbackState.isConnected && playbackState.errorMessage != null) {
                    Surface(shape = MaterialTheme.shapes.medium) {
                        Column(Modifier.fillMaxWidth().padding(12.dp)) {
                            Text(playbackState.errorMessage.orEmpty())
                            TextButton(onClick = playbackViewModel::retry) { Text("Reintentar") }
                        }
                    }
                }
                // GlassNavBar already includes the current-track entry point and controls.
                GlassNavBar(
                    tabs = tabs,
                    selectedIndex = navigator.selectedTab.ordinal,
                    onTabSelected = { index -> AppTab.entries.getOrNull(index)?.let(navigator::selectTab) },
                    song = playbackState.currentSong,
                    isPlaying = playbackState.isPlayRequested,
                    isLoading = playbackState.isLoading,
                    controlsLocked = !playbackState.isConnected,
                    canNext = playbackState.canNext,
                    progress = if (playbackState.durationMs > 0)
                        playbackState.positionMs.toFloat() / playbackState.durationMs else 0f,
                    onPlayPause = { playbackViewModel.togglePlayPause() },
                    onNext = { playbackViewModel.next() },
                    onPrevious = { playbackViewModel.previous() },
                    onExpand = { if (playbackState.currentSong != null) navigator.push(AppRoute.NowPlaying) },
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        },
        playbackOverlays = {
            NowPlayingScreen(
                playbackViewModel = playbackViewModel,
                isVisible = route == AppRoute.NowPlaying,
                onDismiss = { navigator.pop() },
                onOpenQueue = { navigator.push(AppRoute.Queue) },
            )
            QueueScreen(
                playbackViewModel = playbackViewModel,
                isVisible = route == AppRoute.Queue,
                onDismiss = { navigator.pop() },
            )
            if (route == AppRoute.NowPlaying && playbackState.currentSong == null) {
                // Restoring navigation must not depend on the service already being connected.
                Surface(Modifier.fillMaxSize()) {
                    Column(
                        Modifier.fillMaxSize().padding(24.dp),
                        verticalArrangement = Arrangement.Center,
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) {
                        Text(stringResource(R.string.navigation_no_song))
                        TextButton(onClick = { navigator.pop() }) { Text(stringResource(R.string.back)) }
                    }
                }
            }
        },
    )
    actionSong?.let { song -> SongActionsDialog(song, userStore,
        onEnqueue = { playbackViewModel.enqueue(listOf(song)) }, onDismiss = { actionSong = null }) }
    }

}
