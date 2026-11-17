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
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
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
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.music.aurelis.data.AppUpdateChecker
import com.music.aurelis.data.AudioPermission
import com.music.aurelis.data.LocalGenres
import com.music.aurelis.data.LocalSearchHistory
import com.music.aurelis.ui.screens.ExploreScreen
import com.music.aurelis.ui.screens.LocalGenre
import com.music.aurelis.ui.screens.LocalSearchCategory
import com.music.aurelis.ui.screens.LocalSearchDestination
import com.music.aurelis.data.LocalSearch
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalDensity
import com.music.aurelis.ui.player.PlayerDock
import com.music.aurelis.ui.player.PlayerSheetMotion
import com.music.aurelis.ui.player.DockedArtworkOverlay
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
import com.music.aurelis.ui.components.FrostedTopBar
import com.music.aurelis.ui.components.LocalProfileButton
import com.music.aurelis.ui.components.topBarContentPadding
import com.music.aurelis.ui.components.TopBarContentGap
import com.music.aurelis.ui.components.UpdateAvailableDialog
import com.music.aurelis.ui.icons.AurelisIcons
import com.music.aurelis.ui.icons.AurelisNavigationIcons
import com.music.aurelis.ui.components.NAV_BAR_MAX_WIDTH
import com.music.aurelis.ui.components.floatingtabbar.rememberFloatingTabBarScrollConnection
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
    private var playerRequest by mutableIntStateOf(0)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        if (savedInstanceState == null && intent.getBooleanExtra(EXTRA_OPEN_PLAYER, false)) playerRequest++

        setContent {
            AurelisTheme {
                Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
                    AurelisApp(playerRequest)
                }
            }
        }
    }
    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        if (intent.getBooleanExtra(EXTRA_OPEN_PLAYER, false)) playerRequest++
    }
    companion object {
        const val EXTRA_OPEN_PLAYER = "com.music.aurelis.OPEN_PLAYER"
    }
}

@Composable
private fun AurelisApp(playerRequest: Int = 0) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val playbackViewModel: PlaybackViewModel = viewModel()
    val libraryViewModel: LibraryViewModel = viewModel()
    val navigator = rememberAppNavigator()
    LaunchedEffect(playerRequest) { if (playerRequest > 0) navigator.push(AppRoute.NowPlaying) }
    val hazeState = remember { HazeState() }
    var audioGranted by remember { mutableStateOf(AudioPermission.isGranted(context)) }
    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission(),
    ) { audioGranted = AudioPermission.isGranted(context); libraryViewModel.onForeground() }
    var showUpdateDialog by rememberSaveable { mutableStateOf(false) }
    val updateInfo by AppUpdateChecker.available.collectAsState()
    val downloadState by AppUpdateChecker.download.collectAsState()

    val meta = remember(context) { context.getSharedPreferences("app_meta", android.content.Context.MODE_PRIVATE) }
    val localProfile by com.music.aurelis.data.ProfileRepository.state.collectAsState()
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
    }
    // An offline/slow update server must not delay audio permission or catalogue loading.
    LaunchedEffect(Unit) { AppUpdateChecker.check() }
    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner, libraryViewModel) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                audioGranted = AudioPermission.isGranted(context)
                libraryViewModel.onForeground()
            }
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
    com.music.aurelis.ui.onboarding.LocalOnboardingGate(
        preferences = meta, initialName = localProfile.profile.name,
        profileReadOnly = localProfile.readOnly,
        onNameChosen = { chosen ->
            if (!localProfile.readOnly && chosen != localProfile.profile.name) {
                com.music.aurelis.data.ProfileRepository.rename(chosen)
            }
        }, permissionGranted = audioGranted, onRequestPermission = requestAudioAccess,
    ) {
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
    val userStore by com.music.aurelis.ui.AppUi.host.libraries.collectAsState()
    val userState by userStore.state.collectAsState()
    val localProfile by com.music.aurelis.data.ProfileRepository.state.collectAsState()
    var actionSong by remember { mutableStateOf<LocalSong?>(null) }
    val route = navigator.visibleRoute()
    val navBarScroll = rememberFloatingTabBarScrollConnection()
    var bottomChromeHeight by remember { mutableIntStateOf(0) }
    var searchFocusRequested by remember { mutableStateOf(false) }
    val focusManager = LocalFocusManager.current
    val keyboard = LocalSoftwareKeyboardController.current
    val openSettings = {
        focusManager.clearFocus()
        keyboard?.hide()
        navigator.push(AppRoute.Settings)
    }
    val bottomContentPadding = with(LocalDensity.current) { bottomChromeHeight.toDp() } + TopBarContentGap
    val listPadding = PaddingValues(top = topBarContentPadding(), bottom = bottomContentPadding)
    val dock = remember { PlayerDock() }
    val panelScope = rememberCoroutineScope()
    val panelMotion = remember(navigator, dock, panelScope) {
        PlayerSheetMotion(panelScope, dock,
            shown = { navigator.visibleRoute() == AppRoute.NowPlaying },
            setShown = { show ->
                if (show) navigator.push(AppRoute.NowPlaying)
                else if (navigator.visibleRoute() == AppRoute.NowPlaying) navigator.pop()
            }, inlineMode = true,
            reduceMotion = { com.music.aurelis.data.SettingsRepository.reduceAnimation.value },
        ).also { dock.motion = it }
    }
    DisposableEffect(panelMotion) { onDispose { panelMotion.onSheetGone() } }
    val flingVelocity = with(LocalDensity.current) { 900.dp.toPx() }
    SideEffect { panelMotion.flingVelocity = flingVelocity }
    LaunchedEffect(route) {
        // Let the existing ViewModel report missing permission instead of leaving Loading forever.
        if (route == AppRoute.LocalMusic) libraryViewModel.refresh()
    }
    val tabs = listOf(
        BottomTab(stringResource(R.string.home), AurelisNavigationIcons.Home),
        BottomTab(stringResource(R.string.explore), AurelisNavigationIcons.TabExplore),
        BottomTab(stringResource(R.string.library), AurelisNavigationIcons.TabLibrary),
        BottomTab(stringResource(R.string.search), AurelisNavigationIcons.TabSearch),
    )
    val librarySuccess = libraryUiState.libraryState as? LibraryState.Success
    val allSongs = librarySuccess?.songs ?: emptyList()
    val home = remember(allSongs, userState, playbackState.currentSong?.id) {
        LocalHomeData.build(allSongs, userState, playbackState.currentSong?.id)
    }

    // Match BitChord's opaque recording: transparent page pixels must not reveal
    // an unblurred second copy of the page through the refracting surface.
    val windowBackground = MaterialTheme.colorScheme.background
    val paintBackdrop: androidx.compose.ui.graphics.drawscope.ContentDrawScope.() -> Unit = remember(windowBackground) {
        { drawRect(windowBackground); drawContent() }
    }
    val genreGroups = remember(allSongs) { LocalGenres.groups(allSongs) }
    val appBackdrop = com.music.aurelis.ui.components.backdrop.backdrops.rememberLayerBackdrop(onDraw = paintBackdrop)
    val reduceBlur by com.music.aurelis.data.SettingsRepository.reduceDynamicBlur.collectAsState()
    val captureGlass = com.music.aurelis.ui.components.shouldSampleGlass(
        com.music.aurelis.ui.components.LocalLiquidGlassEnabled.current, reduceBlur,
        com.music.aurelis.ui.components.LocalGlassHardware.current,
    )
    val pageBackdropModifier = Modifier.hazeSource(hazeState)
        .then(if (captureGlass) Modifier.layerBackdrop(appBackdrop) else Modifier)
    androidx.compose.runtime.CompositionLocalProvider(
        com.music.aurelis.ui.components.LocalAppBackdrop provides appBackdrop,
        com.music.aurelis.ui.components.LocalAppHazeState provides hazeState,
    ) {
    AppNavigationHost(
        navigator = navigator,
        modifier = Modifier.background(MaterialTheme.colorScheme.background)
            .nestedScroll(navBarScroll)
            .onSizeChanged { dock.sheetTravel = it.height.toFloat() }.onGloballyPositioned { dock.reportHost(it) },
        pageModifier = pageBackdropModifier,
        pageHandlesBackdrop = { page ->
            page == AppRoute.Settings || page is AppRoute.SettingsSection || page == AppRoute.LocalMusic || page == AppRoute.Replay ||
                page == null && navigator.selectedTab == AppTab.SEARCH
        },
        pageBottomPadding = bottomContentPadding,
        routeHandlesBottomPadding = { it == AppRoute.LocalMusic || it == AppRoute.Replay },
        topBar = {
            if (route == null && navigator.selectedTab != AppTab.SEARCH) {
                FrostedTopBar(title = tabs[navigator.selectedTab.ordinal].label, scrolled = { false },
                    transparentBackdrop = true, artworkPageChrome = true,
                    modifier = Modifier.align(Alignment.TopCenter),
                    actions = { LocalProfileButton(openSettings) })
            }
        },
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
                    contentPadding = listPadding,
                    libraryState = libraryUiState.libraryState,
                    isRefreshing = libraryUiState.isRefreshing,
                    favorites = home.favorites,
                    recommendations = home.recommendations,
                    playlists = userState.playlists,
                    collectionsReadOnly = userState.readOnly,
                    onPlayShelf = playbackViewModel::playSongs,
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

            AppTab.EXPLORE -> {
                val genres = genreGroups
                ExploreScreen(genres.map { LocalGenre(
                    if (it.key == "unknown") stringResource(R.string.explore_unknown_genre) else it.name,
                    it.songs.size, null, it.key,
                    it.songs.firstNotNullOfOrNull { song -> song.artworkUri?.takeIf(String::isNotBlank) }) },
                    rememberLazyListState(), { genre ->
                        genres.firstOrNull { it.key == genre.key }?.let {
                            navigator.push(AppRoute.Collection(LocalCollectionKind.GENRE, it.key))
                        }
                    }, listPadding, libraryState = libraryUiState.libraryState,
                    onRetry = { libraryViewModel.refresh() }, onRequestPermission = onRequestAudioPermission)
            }
            AppTab.LIBRARY -> {
                val listState = rememberLazyListState()
                var creatingPlaylist by rememberSaveable { mutableStateOf(false) }
                var playlistName by rememberSaveable { mutableStateOf("") }
                var playlistError by remember { mutableStateOf<String?>(null) }
                val context = LocalContext.current
                val listenedMs = remember(userState.listening) {
                    com.music.aurelis.ui.replay.totalListeningMs(userState.listening)
                }
                LibraryScreen(
                    listState = listState,
                    links = listOf(LibraryLink(stringResource(R.string.local_music), AurelisIcons.Folder) {
                        navigator.push(AppRoute.LocalMusic)
                    }),
                    onShowAll = { shelf -> navigator.push(AppRoute.Collection(
                        if (shelf == "albums") LocalCollectionKind.ALBUMS else LocalCollectionKind.PLAYLISTS)) },
                    contentPadding = listPadding,
                    replay = { com.music.aurelis.ui.screens.LibraryReplayPreview(listenedMs,
                        localProfile.profile.name, { navigator.push(AppRoute.Replay) }) },
                    albums = librarySuccess?.albums.orEmpty(), playlists = userState.playlists, songs = allSongs,
                    onAlbumOpen = { navigator.push(AppRoute.Collection(LocalCollectionKind.ALBUM, it.key)) },
                    onPlaylistOpen = { navigator.push(AppRoute.Collection(LocalCollectionKind.PLAYLIST, it)) },
                    onNewPlaylist = { playlistName = ""; playlistError = null; creatingPlaylist = true },
                    canCreatePlaylist = !userState.readOnly,
                )
                if (creatingPlaylist) androidx.compose.material3.AlertDialog(
                    modifier = Modifier.width(320.dp),
                    properties = androidx.compose.ui.window.DialogProperties(usePlatformDefaultWidth = false),
                    onDismissRequest = { creatingPlaylist = false },
                    title = { Text(stringResource(R.string.new_playlist)) },
                    text = { Column {
                        androidx.compose.material3.OutlinedTextField(value = playlistName,
                            onValueChange = { playlistName = it.take(100); playlistError = null },
                            label = { Text(stringResource(R.string.playlist_name)) }, singleLine = true)
                        playlistError?.let { Text(it, color = MaterialTheme.colorScheme.error) }
                    } },
                    confirmButton = { TextButton(enabled = playlistName.isNotBlank() && !userState.readOnly,
                        onClick = {
                            try { userStore.createPlaylist(playlistName); creatingPlaylist = false }
                            catch (_: Exception) { playlistError = context.getString(R.string.library_playlist_save_error) }
                        }) { Text(stringResource(R.string.save)) } },
                    dismissButton = { TextButton(onClick = { creatingPlaylist = false }) { Text(stringResource(R.string.cancel)) } },
                )
            }
            AppTab.SEARCH -> {
                var searchQuery by rememberSaveable { mutableStateOf("") }
                var searchCategory by rememberSaveable { mutableStateOf(LocalSearchCategory.ALL) }
                val searchListState = rememberLazyListState()
                val searchContext = LocalContext.current
                val searchHistory = remember(userStore) { LocalSearchHistory(searchContext) }
                var history by remember(searchHistory) { mutableStateOf(searchHistory.entries) }
                fun saveQuery() { searchHistory.submit(searchQuery); history = searchHistory.entries }
                val key = LocalSearch.normalize(searchQuery)
                val searchSongsById = remember(allSongs) { allSongs.associateBy { it.id } }
                val unknownGenreLabel = searchContext.getString(R.string.explore_unknown_genre)
                val destinations = if (key.isBlank()) emptyList() else buildList {
                    val allTypes = searchCategory == LocalSearchCategory.ALL
                    if (allTypes || searchCategory == LocalSearchCategory.ALBUMS) librarySuccess?.albums?.filter { LocalSearch.normalize(it.name).contains(key) }?.forEach {
                        add(LocalSearchDestination(it.name, searchContext.getString(R.string.local_search_album),
                            LocalSearchCategory.ALBUMS, it.key, it.artworkUri) { saveQuery(); navigator.push(AppRoute.Collection(LocalCollectionKind.ALBUM, it.key)) })
                    }
                    if (allTypes || searchCategory == LocalSearchCategory.ARTISTS) librarySuccess?.artists?.filter { LocalSearch.normalize(it.name).contains(key) }?.forEach {
                        add(LocalSearchDestination(it.name, searchContext.getString(R.string.local_search_artist),
                            LocalSearchCategory.ARTISTS, it.key, it.artworkUri) { saveQuery(); navigator.push(AppRoute.Collection(LocalCollectionKind.ARTIST, it.key)) })
                    }
                    if (allTypes || searchCategory == LocalSearchCategory.PLAYLISTS) userState.playlists.filter { LocalSearch.normalize(it.name).contains(key) }.forEach {
                        add(LocalSearchDestination(it.name, searchContext.getString(R.string.local_search_playlist),
                            LocalSearchCategory.PLAYLISTS, it.id,
                            it.songIds.firstNotNullOfOrNull { id -> searchSongsById[id]?.artworkUri }) { saveQuery(); navigator.push(AppRoute.Collection(LocalCollectionKind.PLAYLIST, it.id)) })
                    }
                    if (allTypes || searchCategory == LocalSearchCategory.GENRES) genreGroups.filter { LocalSearch.normalize(if (it.key == "unknown") unknownGenreLabel else it.name).contains(key) }.forEach {
                        add(LocalSearchDestination(if (it.key == "unknown") unknownGenreLabel else it.name,
                            searchContext.getString(R.string.local_search_genre), LocalSearchCategory.GENRES, it.key,
                            it.songs.firstNotNullOfOrNull { song -> song.artworkUri }) { saveQuery(); navigator.push(AppRoute.Collection(LocalCollectionKind.GENRE, it.key)) })
                    }
                }
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
                    results = if (results == null || searchCategory in listOf(LocalSearchCategory.ALL, LocalSearchCategory.SONGS)) results else emptyList(),
                    category = searchCategory, onCategoryChange = { searchCategory = it },
                    listState = searchListState,
                    focusRequested = searchFocusRequested,
                    onFocusHandled = { searchFocusRequested = false },
                    onOpenProfile = openSettings,
                    contentBackdropModifier = pageBackdropModifier,
                    onSongClick = { songs, index -> saveQuery(); playbackViewModel.playSongs(songs, index) },
                    onSongLongPress = { actionSong = it },
                    history = history,
                    destinations = destinations,
                    onSubmit = { saveQuery() },
                    onHistoryClick = { searchQuery = it },
                    onHistoryRemove = { searchHistory.remove(it); history = searchHistory.entries },
                    onHistoryClear = { searchHistory.clear(); history = searchHistory.entries },
                    currentSong = playbackState.currentSong,
                    isPlaying = playbackState.isPlaying,
                    contentPadding = PaddingValues(bottom = bottomContentPadding),
                    topPadding = TopBarContentGap,
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
                    onCollectionOpen = { navigator.push(it) },
                    onSongActions = { actionSong = it },
                    onProfile = openSettings,
                    contentBackdropModifier = pageBackdropModifier,
                    contentPadding = PaddingValues(bottom = bottomContentPadding),
                )
                AppRoute.Settings, is AppRoute.SettingsSection -> SettingsScreen(
                    isVisible = true,
                    onDismiss = { navigator.pop() },
                    onOpenEqualizer = { navigator.push(AppRoute.Equalizer) },
                    localSongs = allSongs,
                    onOpenFolder = { navigator.push(AppRoute.Collection(LocalCollectionKind.FOLDER, it)) },
                    section = (page as? AppRoute.SettingsSection)?.category,
                    onOpenCategory = { navigator.push(AppRoute.SettingsSection(it)) },
                    onOpenReplay = { navigator.push(AppRoute.Replay) },
                    contentBackdropModifier = pageBackdropModifier,
                )
                AppRoute.Equalizer -> EqualizerScreen(onBack = { navigator.pop() })
                AppRoute.Replay -> ReplayScreen(onBack = { navigator.pop() }, store = userStore,
                    contentBackdropModifier = pageBackdropModifier, bottomPadding = listPadding,
                    songs = allSongs, onPlay = { playbackViewModel.playSongs(listOf(it)) }, onProfile = openSettings)
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
                modifier = Modifier.align(Alignment.BottomCenter).widthIn(max = NAV_BAR_MAX_WIDTH).fillMaxWidth()
                    .onSizeChanged { bottomChromeHeight = it.height }.imePadding(),
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
                    onTabSelected = { index -> AppTab.entries.getOrNull(index)?.let { selected ->
                        navigator.selectTab(selected)
                        if (selected == AppTab.SEARCH) searchFocusRequested = true
                        else { focusManager.clearFocus(); keyboard?.hide() }
                    } },
                    song = playbackState.currentSong,
                    isPlaying = playbackState.isPlayRequested,
                    isLoading = playbackState.isLoading,
                    controlsLocked = !playbackState.isConnected,
                    canNext = playbackState.canNext,
                    canPrevious = playbackState.canPrevious,
                    qualityLabel = playbackState.sourceQuality.takeIf { it.mimeType != null }?.formatLabel,
                    progress = if (playbackState.durationMs > 0)
                        playbackState.positionMs.toFloat() / playbackState.durationMs else 0f,
                    onPlayPause = { playbackViewModel.togglePlayPause() },
                    onNext = { playbackViewModel.next() },
                    onPrevious = { playbackViewModel.previous() },
                    onExpand = { if (playbackState.currentSong != null) {
                        focusManager.clearFocus(); keyboard?.hide(); panelMotion.open()
                    } },
                    dock = dock, pull = panelMotion,
                    scrollConnection = navBarScroll,
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
                dock = dock, panelMotion = panelMotion,
            )
            DockedArtworkOverlay(dock, playbackState.currentSong)
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
        onEnqueue = { playbackViewModel.enqueue(listOf(song)) }, onDismiss = { actionSong = null },
        onPlayNext = { playbackViewModel.playNext(listOf(song)) },
        queueActionsEnabled = playbackState.isConnected && playbackState.canEditQueue,
        playNextEnabled = !playbackState.shuffleEnabled) }
    }

}
