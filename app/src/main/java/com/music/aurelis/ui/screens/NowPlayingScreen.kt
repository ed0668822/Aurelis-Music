// Aurelis © 2026. GPL-3.0-or-later
package com.music.aurelis.ui.screens

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.material3.TextButton
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material.icons.rounded.Star
import androidx.compose.material.icons.rounded.StarBorder
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.material.icons.rounded.AllInclusive
import com.music.aurelis.ui.components.PlayerVolumeRow
import androidx.compose.material.icons.rounded.Timer
import androidx.compose.ui.platform.LocalContext
import com.music.aurelis.AurelisApplication
import com.music.aurelis.R
import com.music.aurelis.ui.components.SongActionsDialog
import androidx.compose.material.icons.automirrored.rounded.PlaylistAdd
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.material.icons.rounded.Shuffle
import androidx.compose.material.icons.rounded.Repeat
import androidx.compose.material.icons.rounded.RepeatOne
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.ui.unit.Dp
import com.music.aurelis.ui.player.LandscapePlayerLayout
import com.music.aurelis.ui.player.PlayerPane
import com.music.aurelis.ui.player.playerSwipeTrigger
import com.music.aurelis.ui.components.SourceQualityBadge
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.runtime.key
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.media3.common.Player
import com.music.aurelis.ui.components.PlayerSlider
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.QueueMusic
import androidx.compose.material.icons.automirrored.rounded.Subject
import androidx.compose.material.icons.rounded.KeyboardArrowDown
import androidx.compose.material.icons.rounded.MoreHoriz
import androidx.compose.material.icons.rounded.Translate
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.ui.platform.LocalView
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.foundation.gestures.detectTapGestures
import com.music.aurelis.ui.player.PlayerDock
import com.music.aurelis.ui.player.PlayerSheetMotion
import com.music.aurelis.ui.player.playerDockArtwork
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.music.aurelis.data.SettingsRepository
import com.music.aurelis.data.lyrics.TranslationLanguages
import com.music.aurelis.playback.LyricsLookupState
import com.music.aurelis.playback.LyricsState
import com.music.aurelis.playback.PlaybackViewModel
import com.music.aurelis.playback.TranslationState
import com.music.aurelis.ui.components.AnimatedArtwork
import com.music.aurelis.ui.components.lyricsFailureText
import com.music.aurelis.ui.components.LyricsSourcesDialog
import com.music.aurelis.ui.components.PlayerControls
import com.music.aurelis.ui.components.TranslationLanguageDialog
import com.music.aurelis.ui.player.LyricsOffsetSheet
import com.music.aurelis.ui.player.MeshGradientBackground
import com.music.aurelis.ui.player.SyncedLyricsPanel
import com.music.aurelis.ui.player.rememberArtworkColors
import com.music.aurelis.ui.theme.ArtworkColors
import com.music.aurelis.ui.theme.DynamicAurelisTheme
import com.music.aurelis.ui.theme.DynamicThemeViewModel
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.hazeSource

@Composable
fun NowPlayingScreen(
    playbackViewModel: PlaybackViewModel,
    isVisible: Boolean,
    onDismiss: () -> Unit,
    onOpenQueue: () -> Unit,
    dock: PlayerDock? = null,
    panelMotion: PlayerSheetMotion? = null,
) {
    val state by playbackViewModel.state.collectAsState()
    var importSongId by rememberSaveable { mutableStateOf<String?>(null) }
    val importLyrics = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) playbackViewModel.importLyrics(uri, importSongId)
    }
    val song = state.currentSong
    val context = LocalContext.current
    val application = context.applicationContext as AurelisApplication
    val userStore by application.libraries.collectAsState()
    val userLibraryState by userStore.state.collectAsState()
    var showSongActions by remember { mutableStateOf(false) }
    val themeViewModel: DynamicThemeViewModel = viewModel()

    LaunchedEffect(song?.artworkUri) {
        themeViewModel.extractForArtwork(song?.artworkUri)
    }
    val artworkColors by themeViewModel.artworkColors.collectAsState()

    val fallbackColors = ArtworkColors(
        primary = Color(0xFFFA2D48),
        secondary = Color(0xFF8B5CF6),
        background = Color(0xFF0E0E14),
    )

    var showPlaybackTools by rememberSaveable { mutableStateOf(false) }
    var showQuality by rememberSaveable { mutableStateOf(false) }
    var showLyrics by rememberSaveable { mutableStateOf(false) }
    var showInlineQueue by rememberSaveable { mutableStateOf(false) }
    val queueListState = rememberLazyListState()
    var showLyricsSources by rememberSaveable { mutableStateOf(false) }
    var showTranslationLanguages by remember { mutableStateOf(false) }
    var showLyricsOffset by remember { mutableStateOf(false) }

    BackHandler(enabled = isVisible && showInlineQueue) { showInlineQueue = false }
    BackHandler(enabled = isVisible && showLyrics) { showLyrics = false }

    val lyricsOffset by SettingsRepository.lyricsOffsetMs.collectAsState()
    val syncEnabled by SettingsRepository.syncedLyrics.collectAsState()
    val blurLyrics by SettingsRepository.blurUnfocusedLyrics.collectAsState()
    val reduceBlur by SettingsRepository.reduceDynamicBlur.collectAsState()
    val reduceAnimation by SettingsRepository.reduceAnimation.collectAsState()
    val hideVolumeBar by SettingsRepository.hideVolumeBar.collectAsState()
    val hazeState = remember { HazeState() }
    val closePlayer: () -> Unit = { if (panelMotion != null && song != null) panelMotion.close() else onDismiss() }
    BackHandler(enabled = isVisible && !showLyrics && !showInlineQueue) { closePlayer() }
    LaunchedEffect(isVisible, song?.id, panelMotion) {
        if (isVisible && song != null) panelMotion?.reveal()
    }

    if (showQuality && isVisible && song != null) androidx.compose.material3.AlertDialog(
        onDismissRequest = { showQuality = false }, title = { Text(stringResource(R.string.player_audio_info)) },
        text = { Text(stringResource(R.string.player_audio_details, state.sourceQuality.details, state.sourceQuality.originLabel)) },
        confirmButton = { androidx.compose.material3.TextButton(onClick = { showQuality = false }) { Text(stringResource(R.string.player_close_dialog)) } },
    )

    AnimatedVisibility(
        visible = isVisible && song != null,
        enter = if (reduceAnimation || panelMotion != null) androidx.compose.animation.EnterTransition.None else slideInVertically(
            animationSpec = tween(durationMillis = 380, easing = FastOutSlowInEasing),
            initialOffsetY = { it },
        ) + fadeIn(animationSpec = tween(280)),
        exit = if (reduceAnimation || panelMotion != null) androidx.compose.animation.ExitTransition.None else slideOutVertically(
            animationSpec = tween(durationMillis = 320, easing = FastOutSlowInEasing),
            targetOffsetY = { it },
        ) + fadeOut(animationSpec = tween(220)),
    ) {
        val s = song ?: return@AnimatedVisibility
        DisposableEffect(panelMotion, dock) {
            onDispose { dock?.attached = false; dock?.releasePanel(); panelMotion?.onSheetGone() }
        }

        DynamicAurelisTheme(colors = artworkColors ?: fallbackColors) {
            // Intercept new touches outside the moving panel; an existing mini-player
            // drag retains its original pointer path until release or cancellation.
            Box(Modifier.fillMaxSize().pointerInput(panelMotion) { detectTapGestures { closePlayer() } })
            BoxWithConstraints(
                modifier = Modifier
                    .fillMaxSize()
                    .onSizeChanged { if (it.width > 0 && it.height > 0) panelMotion?.laidOut = true }
                    .graphicsLayer {
                        translationY = panelMotion?.position()?.takeIf { it.isFinite() } ?: 0f
                        alpha = if (dock?.coversArtwork(s.id) == true) dock.openFraction() else 1f
                    }
                    .pointerInput(Unit) { detectTapGestures { } }
                    .background(MaterialTheme.colorScheme.background)
                    .hazeSource(hazeState),
            ) {
                val landscape = maxWidth > maxHeight
                val meshPalette = if (!reduceBlur) rememberArtworkColors(s.artworkUri?.let { Uri.parse(it) }) else null
                if (meshPalette != null) MeshGradientBackground(
                    palette = meshPalette,
                    trackKey = s.id,
                    continuous = false,
                    reduceAnimation = reduceAnimation,
                    modifier = Modifier.fillMaxSize().testTag("player-mesh-background"),
                )

                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Brush.verticalGradient(
                                colors = listOf(
                                    Color.Black.copy(alpha = 0.15f),
                                    Color.Transparent,
                                    Color.Black.copy(alpha = 0.25f),
                                ),
                            ),
                        ),
                )

                val openLyrics: () -> Unit = { showInlineQueue = false; showLyrics = !showLyrics }
                val openQueue: () -> Unit = {
                    if (landscape) { showLyrics = false; showInlineQueue = !showInlineQueue }
                    else onOpenQueue()
                }
                val header: @Composable () -> Unit = {
                    if (!landscape) Column(Modifier.fillMaxWidth().playerSwipeTrigger(1, closePlayer, panelMotion), horizontalAlignment = Alignment.CenterHorizontally) {
                        IconButton(onClick = closePlayer, modifier = Modifier.height(28.dp)) {
                            Box(Modifier.size(38.dp, 5.dp).background(MaterialTheme.colorScheme.onSurface, CircleShape)
                                .semantics { contentDescription = context.getString(R.string.player_close) })
                        }
                        Text(stringResource(R.string.player_playing_album, s.album), style = MaterialTheme.typography.labelMedium,
                            maxLines = 1, overflow = TextOverflow.Ellipsis)
                    } else Row(Modifier.fillMaxWidth().height(48.dp).testTag("player-handle")
                        .playerSwipeTrigger(1, closePlayer, panelMotion), verticalAlignment = Alignment.CenterVertically) {
                        IconButton(onClick = closePlayer) {
                            Icon(Icons.Rounded.KeyboardArrowDown, stringResource(R.string.player_close), Modifier.size(28.dp))
                        }
                        Spacer(Modifier.weight(1f))
                        val timerState = if (state.sleepRemainingMs > 0 || state.sleepAfterTrack)
                            stringResource(R.string.player_timer_active) else stringResource(R.string.player_disabled)
                        IconButton(onClick = { showPlaybackTools = true }, modifier = Modifier.semantics { stateDescription = timerState }) {
                            Icon(Icons.Rounded.Timer, stringResource(R.string.player_tools))
                        }
                        IconButton(onClick = { showLyricsSources = true }) {
                            Icon(Icons.AutoMirrored.Rounded.Subject, stringResource(R.string.player_sources))
                        }
                        IconButton(onClick = { showSongActions = true }) {
                            Icon(Icons.Rounded.MoreHoriz, stringResource(R.string.player_song_actions), Modifier.size(26.dp))
                        }
                    }
                }
                val artwork: @Composable (Modifier, Dp) -> Unit = { artworkModifier, artworkSize ->
                    val progressFraction = if (state.durationMs > 0) {
                        (state.positionMs.toFloat() / state.durationMs.toFloat()).coerceIn(0f, 1f)
                    } else 0f

                    AnimatedArtwork(
                        songKey = s.id,
                        artworkUri = s.artworkUri,
                        title = s.title,
                        size = artworkSize,
                        corner = if (landscape) 10.dp else 0.dp,
                        modifier = artworkModifier.testTag("player-artwork").playerDockArtwork(dock, s.id, mini = false),
                        progressFraction = progressFraction,
                    )

                }
                val credits: @Composable (Boolean) -> Unit = { compact ->
                    Row(Modifier.fillMaxWidth().padding(top = if (compact) 0.dp else 8.dp, bottom = 6.dp),
                        verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) {
                            Text(s.title, fontWeight = FontWeight.Bold, style = if (compact) MaterialTheme.typography.titleLarge else MaterialTheme.typography.headlineSmall,
                                maxLines = 1, overflow = TextOverflow.Ellipsis)
                            Text(s.artist, style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1, overflow = TextOverflow.Ellipsis)
                            state.errorMessage?.let { message ->
                                Text(message, maxLines = 2, style = MaterialTheme.typography.labelSmall)
                                TextButton(onClick = playbackViewModel::retry) { Text(stringResource(R.string.player_retry)) }
                            }
                        }
                        IconButton(onClick = { userStore.toggleFavorite(s.id) }) {
                            val favorite = s.id in userLibraryState.favorites
                            Icon(if (favorite) Icons.Rounded.Star else Icons.Rounded.StarBorder,
                                stringResource(if (favorite) R.string.player_remove_favorite else R.string.player_add_favorite))
                        }
                        if (!landscape) IconButton(onClick = { showSongActions = true }) {
                            Icon(Icons.Rounded.MoreHoriz, stringResource(R.string.player_song_actions))
                        }
                    }
                }
                val actions: @Composable () -> Unit = {
                    val activeLabel = stringResource(R.string.player_enabled)
                    val inactiveLabel = stringResource(R.string.player_disabled)
                    val repeatLabel = stringResource(when (state.repeatMode) {
                        Player.REPEAT_MODE_ALL -> R.string.player_repeat_all
                        Player.REPEAT_MODE_ONE -> R.string.player_repeat_one
                        else -> R.string.player_repeat_off
                    })
                    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween) {
                        IconButton(onClick = openLyrics) {
                            Icon(Icons.AutoMirrored.Rounded.Subject, stringResource(R.string.player_lyrics), Modifier.size(25.dp))
                        }
                        androidx.compose.material3.Surface(shape = CircleShape,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.10f)) {
                            Row {
                                IconButton(onClick = playbackViewModel::toggleShuffle, enabled = state.isConnected,
                                    modifier = Modifier.size(40.dp).semantics { stateDescription = if (state.shuffleEnabled) activeLabel else inactiveLabel }) {
                                    Icon(Icons.Rounded.Shuffle, stringResource(R.string.player_shuffle),
                                        tint = MaterialTheme.colorScheme.onSurface.copy(alpha = if (state.shuffleEnabled) 1f else 0.55f))
                                }
                                IconButton(onClick = playbackViewModel::cycleRepeat, enabled = state.isConnected,
                                    modifier = Modifier.size(40.dp).semantics { stateDescription = repeatLabel }) {
                                    Icon(if (state.repeatMode == Player.REPEAT_MODE_ONE) Icons.Rounded.RepeatOne else Icons.Rounded.Repeat,
                                        stringResource(R.string.player_repeat),
                                        tint = MaterialTheme.colorScheme.onSurface.copy(alpha = if (state.repeatMode == Player.REPEAT_MODE_OFF) 0.55f else 1f))
                                }
                                IconButton(onClick = { playbackViewModel.setAutoplay(!state.autoplayEnabled) },
                                    enabled = state.canSetAutoplay, modifier = Modifier.size(40.dp).semantics {
                                        stateDescription = if (state.autoplayEnabled) activeLabel else inactiveLabel
                                    }) {
                                    Icon(Icons.Rounded.AllInclusive, stringResource(R.string.player_autoplay))
                                }
                            }
                        }
                        IconButton(onClick = openQueue) {
                            Icon(Icons.AutoMirrored.Rounded.QueueMusic, stringResource(R.string.player_queue), Modifier.size(25.dp))
                        }
                    }
                }
                val transport: @Composable (Boolean) -> Unit = { compact ->
                    PlayerControls(state.isPlayRequested, state.isLoading, playbackViewModel::togglePlayPause,
                        playbackViewModel::next, playbackViewModel::previous, enabled = state.isConnected,
                        canNext = state.canNext, canPrevious = state.canPrevious, reduceAnimation = reduceAnimation,
                        blendChrome = !reduceBlur && LocalView.current.isHardwareAccelerated, compact = compact)
                }
                val deck: @Composable (Boolean) -> Unit = { compact ->
                    Column(Modifier.fillMaxWidth().testTag("player-control-deck"), horizontalAlignment = Alignment.CenterHorizontally) {
                        if (!compact) {
                            val time = com.music.aurelis.ui.player.adjustedLyricsPosition(state.positionMs, lyricsOffset)
                            val line = (state.lyrics as? LyricsState.Loaded)?.lines?.takeIf { syncEnabled }
                                ?.lastOrNull { it.timeMs <= time && !it.isGap }?.text
                            TextButton(onClick = openLyrics, contentPadding = androidx.compose.foundation.layout.PaddingValues(0.dp),
                                modifier = Modifier.fillMaxWidth().height(32.dp)
                                .semantics { contentDescription = context.getString(R.string.player_lyrics_preview) }) {
                                Text(line ?: stringResource(R.string.player_no_lyrics_preview), maxLines = 1,
                                    overflow = TextOverflow.Ellipsis, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                        Box(Modifier.fillMaxWidth()) {
                            key(s.queueEntryId ?: s.id) {
                                PlayerSlider(state.positionMs, state.durationMs, playbackViewModel::seekTo,
                                    enabled = state.isConnected && state.canSeek, showRemaining = true)
                            }

                        }
                        Spacer(Modifier.height(if (compact) 0.dp else 8.dp))
                        transport(compact)
                        Spacer(Modifier.height(if (compact) 0.dp else 6.dp))
                        if (hideVolumeBar) Spacer(Modifier.fillMaxWidth().height(40.dp)) else PlayerVolumeRow()
                        actions()
                        if (!compact) Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center) {
                            IconButton(onClick = { showPlaybackTools = true }) { Icon(Icons.Rounded.Timer, stringResource(R.string.player_tools)) }
                            TextButton(onClick = { showQuality = true }) { SourceQualityBadge(state.sourceQuality.formatLabel) }
                            IconButton(onClick = { showLyricsSources = true }) { Icon(Icons.AutoMirrored.Rounded.Subject, stringResource(R.string.player_sources)) }
                        }
                    }
                }
                val lyricsContent: @Composable () -> Unit = {
                    val lyricsState = state.lyrics
                    val loadedLyrics = lyricsState as? LyricsState.Loaded
                    val lines = loadedLyrics?.lines.orEmpty()
                    val translation = loadedLyrics?.translation
                    val translated = (translation as? TranslationState.Done)?.lines

                    androidx.compose.material3.Surface(
                        modifier = Modifier.fillMaxSize().testTag("lyrics-overlay"),
                        color = MaterialTheme.colorScheme.background,
                        contentColor = MaterialTheme.colorScheme.onBackground,
                    ) {
                        Column(Modifier.fillMaxSize().then(if (landscape) Modifier else Modifier.statusBarsPadding().navigationBarsPadding())) {
                            Row(
                                modifier = Modifier.fillMaxWidth().padding(16.dp),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                IconButton(onClick = { showLyrics = false }) {
                                    Icon(Icons.Rounded.KeyboardArrowDown, "Cerrar letras", tint = Color.White)
                                }
                                Spacer(Modifier.weight(1f))
                                IconButton(onClick = { showLyricsOffset = true }) { Icon(Icons.Rounded.Timer, "Ajustar sincronización") }
                                IconButton(onClick = { showTranslationLanguages = true }) {
                                    Icon(Icons.Rounded.Translate, "Traducir", tint = Color.White)
                                }
                            }

                            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                                TextButton(onClick = { importSongId = s.id; importLyrics.launch(arrayOf("text/*", "application/ttml+xml", "application/xml", "application/octet-stream", "application/x-subrip")) }) { Text("Importar LRC / TTML") }
                                TextButton(onClick = playbackViewModel::searchLyrics,
                                    enabled = state.lyrics !is LyricsState.Loading && state.lyricsLookup !is LyricsLookupState.Loading) {
                                    Text(stringResource(R.string.lyrics_search_lrclib))
                                }
                            }
                            when (val lookup = state.lyricsLookup) {
                                LyricsLookupState.Loading -> Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(stringResource(R.string.lyrics_lookup_loading), modifier = Modifier.weight(1f).padding(horizontal = 16.dp))
                                    TextButton(onClick = playbackViewModel::cancelLyricsLookup) { Text(stringResource(R.string.lyrics_cancel_lookup)) }
                                }
                                LyricsLookupState.NotFound -> Text(stringResource(R.string.lyrics_lookup_not_found), modifier = Modifier.padding(16.dp))
                                is LyricsLookupState.Failed -> Text(lyricsFailureText(lookup.failure), modifier = Modifier.padding(16.dp))
                                else -> Unit
                            }
                            val loaded = state.lyrics as? LyricsState.Loaded
                            if (loaded != null) Text(loaded.source.label + if (loaded.cached) " · Caché" else "",
                                modifier = Modifier.padding(horizontal = 16.dp), style = MaterialTheme.typography.labelMedium)
                            when (translation) {
                                TranslationState.Loading -> Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text("Traduciendo…", modifier = Modifier.weight(1f).padding(16.dp))
                                    TextButton(onClick = playbackViewModel::clearTranslation) { Text(stringResource(R.string.lyrics_cancel_translation)) }
                                }
                                is TranslationState.Error -> Column {
                                    Text(lyricsFailureText(translation.failure), modifier = Modifier.padding(16.dp))
                                    TextButton(onClick = { showTranslationLanguages = true }) { Text(stringResource(R.string.lyrics_retry_translation)) }
                                }
                                TranslationState.Failed -> Text("No se pudo traducir. Revisa el servidor, la conexión o los idiomas y reintenta.", modifier = Modifier.padding(16.dp))
                                is TranslationState.SameLanguage -> Text("La letra ya está en el idioma elegido.", modifier = Modifier.padding(16.dp))
                                is TranslationState.Done -> Row {
                                    Text("Traducción: ${TranslationLanguages.supportedLanguages[translation.language] ?: translation.language}" + if (translation.cached) " · Caché" else "", modifier = Modifier.weight(1f).padding(16.dp))
                                    TextButton(onClick = playbackViewModel::clearTranslation) { Text("Ocultar traducción") }
                                }
                                else -> Unit
                            }
                            if (lines.isEmpty()) {
                                Box(Modifier.weight(1f), contentAlignment = Alignment.Center) {
                                    Text(
                                        modifier = Modifier.padding(16.dp),
                                        text = when (lyricsState) {
                                            is LyricsState.Loading -> "Buscando letras..."
                                            is LyricsState.Missing -> "No hay letras locales o guardadas. Importa un LRC o busca en LRCLIB (envía título y artista)."
                                            is LyricsState.Failed -> lyricsState.message
                                            else -> "Cargando..."
                                        },
                                        color = Color.White.copy(alpha = 0.7f),
                                    )
                                }
                            } else {
                                val lyricsTrackKey = s.queueEntryId ?: s.id
                                val position = remember(lyricsTrackKey) { com.music.aurelis.playback.PlaybackPosition() }
                                LaunchedEffect(state.positionMs, state.positionDiscontinuity, state.isPlaying) {
                                    position.report(state.positionMs)
                                    position.advancing = state.isPlaying
                                    position.seeks = state.positionDiscontinuity
                                }

                                SyncedLyricsPanel(
                                    lines = lines,
                                    trackKey = lyricsTrackKey,
                                    position = position,
                                    looking = true,
                                    isPlaying = state.isPlaying,
                                    onSeekToLine = { playbackViewModel.seekTo(com.music.aurelis.ui.player.adjustedLyricsSeekTarget(it, lyricsOffset)) },
                                    offsetMs = lyricsOffset,
                                    centerFocus = true,
                                    isSynced = loaded?.isSynced == true,
                                    translatedLines = translated,
                                    syncEnabled = syncEnabled, blurUnfocused = blurLyrics && !reduceBlur,
                                    modifier = Modifier.weight(1f),
                                )
                            }
                        }
                    }
                }

                BoxWithConstraints(Modifier.fillMaxSize().onGloballyPositioned { dock?.reportPanel(it) }
                    .then(if ((showLyrics || showInlineQueue) && !landscape) Modifier.clearAndSetSemantics { } else Modifier)) {
                    SideEffect { dock?.attached = isVisible && !reduceAnimation && maxWidth <= maxHeight }
                    if (landscape) {
                        LandscapePlayerLayout(
                            pane = when { showLyrics -> PlayerPane.Lyrics; showInlineQueue -> PlayerPane.Queue; else -> PlayerPane.Main },
                            background = {}, modifier = Modifier.testTag("player-landscape"), reduceMotion = reduceAnimation,
                            artwork = { artwork(it, 320.dp) },
                            actions = { Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                if (showLyrics || showInlineQueue) { transport(true); actions() }
                                header()
                            } },
                            mainPane = { compact ->
                                Column(Modifier.fillMaxSize(), horizontalAlignment = Alignment.CenterHorizontally) {
                                    Box(Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                                        Column(Modifier.verticalScroll(rememberScrollState())) { credits(compact) }
                                    }
                                    deck(true)
                                }
                            },
                            lyricsPane = { Box(Modifier.fillMaxSize().testTag("landscape-lyrics")) { lyricsContent() } },
                            queuePane = { Box(Modifier.fillMaxSize().testTag("landscape-queue")) {
                                QueueScreen(playbackViewModel, true, { showInlineQueue = false }, embedded = true, listState = queueListState)
                            } },
                        )
                    } else {
                        // Measure the deck at its natural height first; artwork receives
                        // the remaining space. Transport never depends on scrolling.
                        Box(Modifier.fillMaxSize().testTag("player-portrait")) {
                            BoxWithConstraints(Modifier.fillMaxWidth().align(Alignment.TopCenter)) {
                                artwork(Modifier, maxWidth)
                                Box(Modifier.fillMaxWidth().height(maxWidth).background(Brush.verticalGradient(
                                    listOf(Color.Transparent, MaterialTheme.colorScheme.background))))
                            }
                            Column(Modifier.fillMaxSize().statusBarsPadding().navigationBarsPadding().padding(horizontal = 30.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                                header()
                                Spacer(Modifier.weight(1f))
                                credits(false)
                                deck(false)
                            }
                        }
                    }
                }
                if (showLyrics && !landscape) lyricsContent()
                if (showInlineQueue && !landscape) QueueScreen(playbackViewModel, true, { showInlineQueue = false }, listState = queueListState)

                if (showPlaybackTools) com.music.aurelis.ui.components.PlaybackToolsDialog(
                    state, { duration, after -> playbackViewModel.setSleepTimer(duration, after) },
                    playbackViewModel::setPlaybackSpeed, playbackViewModel::setSkipSilence, playbackViewModel::setAutoplay, { showPlaybackTools = false })
                if (showSongActions) SongActionsDialog(s, userStore,
                    onEnqueue = { playbackViewModel.enqueue(listOf(s)) }, onDismiss = { showSongActions = false },
                    onPlayNext = { playbackViewModel.playNext(listOf(s)) },
                    queueActionsEnabled = state.isConnected && state.canEditQueue,
                    playNextEnabled = !state.shuffleEnabled || state.canEditShuffleQueue)
                if (showLyricsSources) {
                    LyricsSourcesDialog(onDismiss = { showLyricsSources = false },
                        currentSource = (state.lyrics as? LyricsState.Loaded)?.source,
                        lookupState = state.lyricsLookup,
                        onSearch = { showLyricsSources = false; playbackViewModel.searchLyrics() })
                }
                if (showTranslationLanguages) {
                    TranslationLanguageDialog(onDismiss = { showTranslationLanguages = false },
                        onTranslate = { language -> showTranslationLanguages = false; playbackViewModel.translateLyrics(language) })
                }
                if (showLyricsOffset) {
                    LyricsOffsetSheet(
                        hazeState = hazeState,
                        offsetMs = lyricsOffset,
                        onOffsetChange = { SettingsRepository.setLyricsOffsetMs(it) },
                        onDismiss = { showLyricsOffset = false },
                    )
                }
            }
        }
    }
}
