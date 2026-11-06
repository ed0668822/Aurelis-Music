// Aurelis © 2026. GPL-3.0-or-later
package com.music.aurelis.ui.screens

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.material3.TextButton
import androidx.compose.foundation.layout.heightIn
import androidx.compose.material.icons.rounded.Timer
import androidx.compose.ui.platform.LocalContext
import com.music.aurelis.AurelisApplication
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
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.ui.unit.Dp
import com.music.aurelis.ui.player.LandscapePlayerLayout
import com.music.aurelis.ui.player.PlayerPane
import com.music.aurelis.ui.player.playerSwipeTrigger
import com.music.aurelis.ui.components.SourceQualityBadge
import androidx.compose.ui.platform.testTag
import androidx.compose.runtime.key
import androidx.compose.runtime.saveable.rememberSaveable
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
import com.music.aurelis.playback.LyricsState
import com.music.aurelis.playback.PlaybackViewModel
import com.music.aurelis.playback.TranslationState
import com.music.aurelis.ui.components.AnimatedArtwork
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
    val userStore by (LocalContext.current.applicationContext as AurelisApplication).libraries.collectAsState()
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

    var showQuality by rememberSaveable { mutableStateOf(false) }
    var showLyrics by rememberSaveable { mutableStateOf(false) }
    var showLyricsSources by rememberSaveable { mutableStateOf(false) }
    var showTranslationLanguages by remember { mutableStateOf(false) }
    var showLyricsOffset by remember { mutableStateOf(false) }

    BackHandler(enabled = isVisible && showLyrics) { showLyrics = false }

    val lyricsOffset by SettingsRepository.lyricsOffsetMs.collectAsState()
    val syncEnabled by SettingsRepository.syncedLyrics.collectAsState()
    val blurLyrics by SettingsRepository.blurUnfocusedLyrics.collectAsState()
    val reduceBlur by SettingsRepository.reduceDynamicBlur.collectAsState()
    val reduceAnimation by SettingsRepository.reduceAnimation.collectAsState()
    val hazeState = remember { HazeState() }
    val closePlayer: () -> Unit = { if (panelMotion != null && song != null) panelMotion.close() else onDismiss() }
    BackHandler(enabled = isVisible && !showLyrics) { closePlayer() }
    LaunchedEffect(isVisible, song?.id, panelMotion) {
        if (isVisible && song != null) panelMotion?.reveal()
    }

    if (showQuality && isVisible && song != null) androidx.compose.material3.AlertDialog(
        onDismissRequest = { showQuality = false }, title = { Text("Información de audio") },
        text = { Text("Origen: ${state.sourceQuality.details}\nDatos: ${state.sourceQuality.originLabel}\nSalida del dispositivo: no medida") },
        confirmButton = { androidx.compose.material3.TextButton(onClick = { showQuality = false }) { Text("Cerrar") } },
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
            Box(
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
                val meshPalette = rememberArtworkColors(s.artworkUri?.let { Uri.parse(it) })
                if (!reduceBlur) MeshGradientBackground(
                    palette = meshPalette,
                    trackKey = s.id,
                    continuous = false,
                    reduceAnimation = reduceAnimation,
                    modifier = Modifier.fillMaxSize(),
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

                val header: @Composable () -> Unit = {
                    Row(
                        modifier = Modifier.fillMaxWidth().testTag("player-handle").playerSwipeTrigger(1, closePlayer, panelMotion),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        IconButton(onClick = closePlayer) {
                            Icon(
                                imageVector = Icons.Rounded.KeyboardArrowDown,
                                contentDescription = "Cerrar reproductor",
                                tint = MaterialTheme.colorScheme.onSurface,
                                modifier = Modifier.size(32.dp),
                            )
                        }
                        Spacer(modifier = Modifier.weight(1f))
                        IconButton(onClick = { showSongActions = true }) {
                            Icon(Icons.AutoMirrored.Rounded.PlaylistAdd, "Opciones de canción")
                        }
                        IconButton(onClick = { showLyricsSources = true }) {
                            Icon(
                                imageVector = Icons.Rounded.MoreHoriz,
                                contentDescription = "Fuentes de letras",
                                tint = MaterialTheme.colorScheme.onSurface,
                                modifier = Modifier.size(26.dp),
                            )
                        }
                        IconButton(onClick = onOpenQueue) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Rounded.QueueMusic,
                                contentDescription = "Abrir cola",
                                tint = MaterialTheme.colorScheme.onSurface,
                                modifier = Modifier.size(28.dp),
                            )
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
                        modifier = artworkModifier.playerDockArtwork(dock, s.id, mini = false),
                        progressFraction = progressFraction,
                    )

                }
                val controls: @Composable () -> Unit = {
                    Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center,
                            modifier = Modifier.fillMaxWidth(),
                        ) {
                            Text(
                                modifier = Modifier.weight(1f),
                                text = s.title,
                                style = MaterialTheme.typography.headlineSmall,
                                color = MaterialTheme.colorScheme.onSurface,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                textAlign = TextAlign.Center,
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            androidx.compose.material3.TextButton(onClick = { showQuality = true }) {
                                SourceQualityBadge(state.sourceQuality.formatLabel)
                            }
                        }
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = s.artist,
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.fillMaxWidth(),
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Button(
                            onClick = { showLyrics = true },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                                contentColor = MaterialTheme.colorScheme.onSurface,
                            ),
                            shape = CircleShape,
                        ) {
                            Icon(
                                Icons.AutoMirrored.Rounded.Subject,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp),
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Ver Letras")
                        }
                        Spacer(modifier = Modifier.height(16.dp))
                        state.errorMessage?.let { message ->
                            Text(message, color = MaterialTheme.colorScheme.error)
                            androidx.compose.material3.TextButton(onClick = playbackViewModel::retry) { Text("Reintentar") }
                        }
                        key(s.id) {
                            PlayerSlider(state.positionMs, state.durationMs, playbackViewModel::seekTo,
                                enabled = state.isConnected && state.canSeek)
                        }
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                            IconButton(onClick = playbackViewModel::toggleShuffle, enabled = state.isConnected,
                                modifier = Modifier.semantics { stateDescription = if (state.shuffleEnabled) "Activado" else "Desactivado" }) {
                                Icon(Icons.Rounded.Shuffle, "Aleatorio",
                                    tint = if (state.shuffleEnabled) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface)
                            }
                            val repeatDescription = when (state.repeatMode) {
                                Player.REPEAT_MODE_ALL -> "Repetir todas"
                                Player.REPEAT_MODE_ONE -> "Repetir una"
                                else -> "Repetición desactivada"
                            }
                            IconButton(onClick = playbackViewModel::cycleRepeat, enabled = state.isConnected,
                                modifier = Modifier.semantics { stateDescription = repeatDescription }) {
                                Icon(if (state.repeatMode == Player.REPEAT_MODE_ONE) Icons.Rounded.RepeatOne else Icons.Rounded.Repeat,
                                    "Repetición", tint = if (state.repeatMode != Player.REPEAT_MODE_OFF)
                                        MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface)
                            }
                        }
                        PlayerControls(
                            isPlaying = state.isPlayRequested,
                            isLoading = state.isLoading,
                            onPlayPause = { playbackViewModel.togglePlayPause() },
                            onNext = { playbackViewModel.next() },
                            onPrevious = { playbackViewModel.previous() },
                            enabled = state.isConnected, canNext = state.canNext, canPrevious = state.canPrevious,
                            reduceAnimation = reduceAnimation,
                        )
                        Spacer(modifier = Modifier.height(24.dp))
                    }
                }
                BoxWithConstraints(Modifier.fillMaxSize().onGloballyPositioned { dock?.reportPanel(it) }
                    .then(if (showLyrics) Modifier.clearAndSetSemantics { } else Modifier)) {
                    val portraitArtworkSize = (maxWidth - 48.dp).coerceIn(160.dp, 320.dp)
                    SideEffect { dock?.attached = isVisible && !reduceAnimation && maxWidth <= maxHeight }
                    if (maxWidth > maxHeight) {
                        LandscapePlayerLayout(
                            pane = PlayerPane.Main, background = {},
                            artwork = { artwork(it, 320.dp) }, actions = {},
                            mainPane = {
                                Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState())
                                    .testTag("player-landscape"), horizontalAlignment = Alignment.CenterHorizontally) {
                                    header()
                                    controls()
                                }
                            }, lyricsPane = {}, queuePane = {},
                        )
                    } else {
                        Column(Modifier.fillMaxSize().statusBarsPadding().navigationBarsPadding()
                            .verticalScroll(rememberScrollState()).padding(horizontal = 24.dp)
                            .testTag("player-portrait"), horizontalAlignment = Alignment.CenterHorizontally) {
                            header()
                            Spacer(Modifier.height(24.dp))
                            artwork(Modifier, portraitArtworkSize)
                            Spacer(Modifier.height(32.dp))
                            controls()
                        }
                    }
                }

                if (showLyrics) {
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
                        Column(Modifier.fillMaxSize().statusBarsPadding().navigationBarsPadding()) {
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
                                TextButton(onClick = { importSongId = s.id; importLyrics.launch(arrayOf("text/*", "application/octet-stream", "application/x-subrip")) }) { Text("Importar LRC") }
                                TextButton(onClick = playbackViewModel::searchLyrics, enabled = state.lyrics !is LyricsState.Loading) { Text("Buscar en LRCLIB") }
                            }
                            val loaded = state.lyrics as? LyricsState.Loaded
                            if (loaded != null) Text(loaded.source.label + if (loaded.cached) " · Caché" else "",
                                modifier = Modifier.padding(horizontal = 16.dp), style = MaterialTheme.typography.labelMedium)
                            when (translation) {
                                TranslationState.Loading -> Text("Traduciendo…", modifier = Modifier.padding(16.dp))
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
                                val position = remember(s.id) { com.music.aurelis.playback.PlaybackPosition() }
                                LaunchedEffect(state.positionMs, lyricsOffset, state.isPlaying) {
                                    position.report(state.positionMs + lyricsOffset)
                                    position.advancing = state.isPlaying
                                }

                                SyncedLyricsPanel(
                                    lines = lines,
                                    trackKey = s.id,
                                    position = position,
                                    looking = true,
                                    isPlaying = state.isPlaying,
                                    onSeekToLine = { playbackViewModel.seekTo((it - lyricsOffset).coerceAtLeast(0)) },
                                    isSynced = loaded?.isSynced == true,
                                    translatedLines = translated,
                                    syncEnabled = syncEnabled, blurUnfocused = blurLyrics && !reduceBlur,
                                    modifier = Modifier.weight(1f),
                                )
                            }
                        }
                    }
                }

                if (showSongActions) SongActionsDialog(s, userStore,
                    onEnqueue = { playbackViewModel.enqueue(listOf(s)) }, onDismiss = { showSongActions = false },
                    onPlayNext = { playbackViewModel.playNext(listOf(s)) },
                    queueActionsEnabled = state.isConnected && state.canEditQueue,
                    playNextEnabled = !state.shuffleEnabled)
                if (showLyricsSources) {
                    LyricsSourcesDialog(onDismiss = { showLyricsSources = false })
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
