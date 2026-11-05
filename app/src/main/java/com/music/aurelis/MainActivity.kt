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
import com.music.aurelis.ui.screens.SearchScreen
import com.music.aurelis.ui.screens.SettingsScreen
import com.music.aurelis.ui.theme.AurelisTheme
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.hazeSource
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
    var hasPermission by remember { mutableStateOf(AudioPermission.isGranted(context)) }
    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission(),
    ) { granted -> hasPermission = granted }
    var showUpdateDialog by rememberSaveable { mutableStateOf(false) }
    val updateInfo by AppUpdateChecker.available.collectAsState()
    val downloadState by AppUpdateChecker.download.collectAsState()

    LaunchedEffect(Unit) {
        playbackViewModel.connect()
        AppUpdateChecker.check()
        if (!hasPermission) permissionLauncher.launch(AudioPermission.requiredPermission())
    }
    LaunchedEffect(hasPermission) {
        if (hasPermission) libraryViewModel.loadLibrary()
    }
    LaunchedEffect(updateInfo) {
        if (updateInfo != null) showUpdateDialog = true
    }
    Box(Modifier.fillMaxSize()) {
        AurelisNavigationContent(navigator, playbackViewModel, libraryViewModel, hazeState)
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
) {
    val playbackState by playbackViewModel.state.collectAsState()
    val libraryUiState by libraryViewModel.uiState.collectAsState()
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

    AppNavigationHost(
        navigator = navigator,
        modifier = Modifier.background(MaterialTheme.colorScheme.background).hazeSource(hazeState),
        tabContent = { tab ->
            when (tab) {
            AppTab.HOME -> {
                val listState = rememberLazyListState()
                HomeScreen(
                    recentlyPlayed = emptyList(),
                    mostPlayed = emptyList(),
                    recentlyAdded = librarySuccess?.recentlyAdded ?: emptyList(),
                    albums = librarySuccess?.albums ?: emptyList(),
                    artists = librarySuccess?.artists ?: emptyList(),
                    listState = listState,
                    onSongClick = { song: LocalSong ->
                        val idx = allSongs.indexOfFirst { it.id == song.id }
                        if (idx >= 0) playbackViewModel.playSongs(allSongs, idx)
                    },
                    onSongLongPress = { },
                    onAlbumClick = { },
                    onArtistClick = { },
                    currentSong = playbackState.currentSong,
                    isPlaying = playbackState.isPlaying,
                    contentPadding = PaddingValues(bottom = 180.dp),
                )
            }

            AppTab.EXPLORE -> Text(stringResource(R.string.navigation_explore_pending), modifier = Modifier.align(Alignment.Center))
            AppTab.LIBRARY -> {
                val listState = rememberLazyListState()
                val links = listOf(
                    LibraryLink(stringResource(R.string.songs), AurelisIcons.MusicNote) { navigator.push(AppRoute.LocalMusic) },
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
                val results: List<LocalSong>? = if (searchQuery.isBlank()) null else allSongs.filter {
                    it.title.contains(searchQuery, ignoreCase = true) ||
                        it.artist.contains(searchQuery, ignoreCase = true) ||
                        it.album?.contains(searchQuery, ignoreCase = true) == true
                }
                SearchScreen(
                    query = searchQuery,
                    onQueryChange = { searchQuery = it },
                    results = results,
                    listState = searchListState,
                    focusRequested = false,
                    onFocusHandled = { },
                    onSongClick = { songs, index -> playbackViewModel.playSongs(songs, index) },
                    onSongLongPress = { },
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
                )
                AppRoute.Settings -> SettingsScreen(
                    isVisible = true,
                    onDismiss = { navigator.pop() },
                    onOpenEqualizer = { navigator.push(AppRoute.Equalizer) },
                )
                AppRoute.Equalizer -> EqualizerScreen(onBack = { navigator.pop() })
                AppRoute.Replay -> ReplayScreen(onBack = { navigator.pop() })
                else -> error("Destination is not wired in the local graph: $page")
            }
        },
        bottomBar = {
            Column(
                modifier = Modifier.align(Alignment.BottomCenter).fillMaxWidth()
                    .navigationBarsPadding().padding(bottom = 16.dp, start = 12.dp, end = 12.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                // GlassNavBar already includes the current-track entry point and controls.
                GlassNavBar(
                    tabs = tabs,
                    selectedIndex = navigator.selectedTab.ordinal,
                    onTabSelected = { index -> AppTab.entries.getOrNull(index)?.let(navigator::selectTab) },
                    song = playbackState.currentSong,
                    isPlaying = playbackState.isPlaying,
                    isLoading = playbackState.isLoading,
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
}
