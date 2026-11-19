// SPDX-FileCopyrightText: 2026 Aurelis
// SPDX-License-Identifier: LGPL-3.0-or-later
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
import com.music.aurelis.ui.components.PlayerBottomGlyph
import com.music.aurelis.ui.components.PlayerPlaybackActions
import com.music.aurelis.ui.components.PlaybackOriginCaption
import com.music.aurelis.data.model.PlaybackSourceType
import com.music.aurelis.ui.components.PlayerCircleGlyph
import com.music.aurelis.ui.components.PlayerVolumeRow
import com.music.aurelis.ui.player.showAndroidOutputSwitcher
import com.music.aurelis.ui.player.rememberAndroidAudioOutputs
import com.music.aurelis.ui.player.confirmedOutput
import com.music.aurelis.ui.player.outputDeviceLabel
import androidx.compose.material.icons.rounded.Speaker
import android.content.Intent
import android.provider.Settings
import androidx.compose.material.icons.rounded.IosShare
import com.music.aurelis.ui.player.LyricsShareRequest
import com.music.aurelis.ui.player.LyricsShareLine
import com.music.aurelis.ui.player.AndroidLyricsShareSheet
import com.music.aurelis.ui.player.rememberLyricsPicker
import com.music.aurelis.ui.player.LyricsPickBar
import com.music.aurelis.ui.haptics.rememberHaptics
import com.music.aurelis.ui.icons.AurelisPlayerIcons
import androidx.compose.ui.unit.sp
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.material.icons.rounded.Language
import androidx.compose.material.icons.rounded.MusicNote
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowRight
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
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.basicMarquee
import androidx.compose.foundation.clickable
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.material.icons.rounded.Shuffle
import androidx.compose.material.icons.rounded.Repeat
import androidx.compose.material.icons.rounded.RepeatOne
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.ui.unit.Dp
import com.music.aurelis.ui.player.LandscapePlayerLayout
import com.music.aurelis.ui.player.PlayerPane
import com.music.aurelis.ui.player.artistCredits
import com.music.aurelis.ui.player.artistCreditSegments
import com.music.aurelis.ui.player.matchedLocalAlbum
import com.music.aurelis.ui.player.matchedLocalArtist
import com.music.aurelis.ui.player.playerSwipeTrigger
import com.music.aurelis.ui.components.SourceQualityBadge
import com.music.aurelis.ui.components.MiniPlayerQualityDialog
import com.music.aurelis.ui.components.AudioPipelineDialog
import com.music.aurelis.ui.components.MiniPlayerExplicitBadge
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.runtime.key
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.media3.common.Player
import com.music.aurelis.ui.components.PlayerSlider
import com.music.aurelis.ui.components.CurrentLyricStrip
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
import androidx.compose.ui.layout.boundsInWindow
import androidx.compose.ui.unit.IntRect
import kotlin.math.roundToInt
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
import com.music.aurelis.data.model.LocalAlbum
import com.music.aurelis.data.model.LocalArtist
import com.music.aurelis.ui.navigation.AppRoute
import com.music.aurelis.ui.navigation.LocalCollectionKind
import com.music.aurelis.playback.TranslationState
import com.music.aurelis.ui.components.AnimatedArtwork
import com.music.aurelis.ui.components.lyricsSourceTitle
import com.music.aurelis.ui.components.lyricsFailureText
import com.music.aurelis.ui.components.LyricsSourcesDialog
import com.music.aurelis.ui.components.PlayerControls
import com.music.aurelis.ui.components.TranslationLanguageDialog
import com.music.aurelis.ui.player.LyricsOffsetSheet
import com.music.aurelis.ui.player.ArtworkMeshBackdrop
import com.music.aurelis.ui.player.FullArtworkBlurBackdrop
import com.music.aurelis.ui.player.rememberArtworkMesh
import com.music.aurelis.ui.player.rememberFullArtworkBlurImage
import com.music.aurelis.ui.player.SyncedLyricsPanel
import com.music.aurelis.ui.theme.ArtworkColors
import com.music.aurelis.ui.theme.DynamicAurelisTheme
import com.music.aurelis.ui.theme.DynamicThemeViewModel
import com.music.aurelis.ui.theme.StatusBarIcons
import com.music.aurelis.ui.theme.rememberArtworkTopBandLuminance
import com.music.aurelis.ui.theme.topBandScrimAlpha
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.hazeSource

/** BitChord caps the portrait sleeve at 560dp and sizes it to the vertical room. */
internal fun portraitArtworkSize(width: Dp, height: Dp): Dp {
    if (width <= 0.dp || height <= 0.dp) return 0.dp
    // Compact portrait windows leave more room for the credits and transport.
    // Width remains full-bleed on ordinary phones; on tablets the art is centred.
    val available = (height - 260.dp).coerceAtLeast(54.dp)
    // If the portrait already leaves 240dp beneath its square artwork,
    // maintain the full-width sleeve. This also preserves the 320x568
    // large-text/transport regression verified by PlayerIntegrationTest.
    if (width <= 560.dp && height - width >= 240.dp) return width
    return minOf(width, 560.dp, available)
}

@Composable
fun NowPlayingScreen(
    playbackViewModel: PlaybackViewModel,
    isVisible: Boolean,
    onDismiss: () -> Unit,
    onOpenQueue: () -> Unit,
    dock: PlayerDock? = null,
    panelMotion: PlayerSheetMotion? = null,
    localAlbums: List<LocalAlbum> = emptyList(),
    localArtists: List<LocalArtist> = emptyList(),
    onOpenCollection: (AppRoute.Collection) -> Unit = {},
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
    var songMenuAnchor by remember { mutableStateOf<IntRect?>(null) }
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

    val audioOutputs = rememberAndroidAudioOutputs()
    val effectiveOutput = confirmedOutput(audioOutputs)
    var showPlaybackTools by rememberSaveable { mutableStateOf(false) }
    var showOutputHelp by rememberSaveable { mutableStateOf(false) }
    val openOutput: () -> Unit = {
        if (!showAndroidOutputSwitcher(context)) showOutputHelp = true
    }
    var showQuality by rememberSaveable { mutableStateOf(false) }
    var showLyrics by rememberSaveable { mutableStateOf(false) }
    // Enter lyrics as the full reading pane; the transport can be summoned by a tap.
    var lyricsControlsOpen by rememberSaveable { mutableStateOf(false) }
    var showInlineQueue by rememberSaveable { mutableStateOf(false) }
    val queueListState = rememberLazyListState()
    var lyricsShareRequest by remember { mutableStateOf<LyricsShareRequest?>(null) }
    var showLyricsTools by remember { mutableStateOf(false) }
    LaunchedEffect(song?.queueEntryId, song?.id, isVisible) {
        lyricsShareRequest = null
        showLyricsTools = false
        // Switching tracks must not reveal the transport over full-screen lyrics.
        lyricsControlsOpen = !showLyrics
    }
    var showLyricsSources by rememberSaveable { mutableStateOf(false) }
    var showTranslationLanguages by remember { mutableStateOf(false) }
    var showLyricsOffset by remember { mutableStateOf(false) }

    BackHandler(enabled = isVisible && showInlineQueue) { showInlineQueue = false }
    BackHandler(enabled = isVisible && showLyrics) { showLyrics = false }
    BackHandler(enabled = isVisible && lyricsShareRequest != null) { lyricsShareRequest = null }

    val lyricsOffset by SettingsRepository.lyricsOffsetMs.collectAsState()
    val syncEnabled by SettingsRepository.syncedLyrics.collectAsState()
    val blurLyrics by SettingsRepository.blurUnfocusedLyrics.collectAsState()
    val reduceBlur by SettingsRepository.reduceDynamicBlur.collectAsState()
    val reduceAnimation by SettingsRepository.reduceAnimation.collectAsState()
    val hideVolumeBar by SettingsRepository.hideVolumeBar.collectAsState()
    val hazeState = remember { HazeState() }
    val closePlayer: () -> Unit = { if (panelMotion != null && song != null) panelMotion.close() else onDismiss() }
    BackHandler(enabled = isVisible && !showLyrics && !showInlineQueue) { closePlayer() }
    // A new track updates this already-open panel; it is not another presentation.
    LaunchedEffect(isVisible, song != null, panelMotion) {
        if (isVisible && song != null) panelMotion?.reveal()
    }

    if (showQuality && isVisible && song != null) {
        AudioPipelineDialog(
            onDismiss = { showQuality = false },
            info = state.sourceQuality,
            fileName = song.displayName ?: song.title,
            fileMimeType = song.mimeType,
            routedOutput = effectiveOutput,
            onOpenOutput = openOutput,
        )
    }

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
        // Resolve the current queue item against real MediaStore-backed collections.
        // A shared album title or artist name is not enough to choose a destination.
        val albumRoute = remember(s.id, localAlbums) {
            matchedLocalAlbum(s, localAlbums)?.let {
                AppRoute.Collection(LocalCollectionKind.ALBUM, it.key)
            }
        }
        val credits = remember(s.artist) { artistCredits(s.artist) }
        val artistRoutes = remember(s.id, credits, localArtists) {
            credits.mapNotNull { credit ->
                matchedLocalArtist(s, credit.name, localArtists)?.let { artist ->
                    credit.name to AppRoute.Collection(LocalCollectionKind.ARTIST, artist.key)
                }
            }.toMap()
        }
        DisposableEffect(panelMotion, dock) {
            onDispose { dock?.attached = false; dock?.releasePanel(); panelMotion?.onSheetGone() }
        }

        DynamicAurelisTheme(colors = artworkColors ?: fallbackColors) {
            StatusBarIcons(dark = false)
            val topBandLuminance = rememberArtworkTopBandLuminance(s.artworkUri)
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
                val compactLyrics = maxHeight < 600.dp
                // F051: use the original artwork-proportional, pre-blurred
                // default mesh instead of the legacy orbiting radial blobs.
                val coverUri = remember(s.artworkUri) {
                    s.artworkUri?.takeIf(String::isNotBlank)?.let(Uri::parse)
                }
                // F051-F052 regression: disabling dynamic blur removes the
                // mesh layer itself, not just its RenderEffect. This preserves
                // the existing accessibility/performance contract tested by
                // PlayerResizeContinuityTest under orientation/restoration.
                if (!reduceBlur) {
                    val artworkMesh = rememberArtworkMesh(coverUri)
                    val preparedBlur = rememberFullArtworkBlurImage(coverUri)
                    val seamlessCoverEnd = if (landscape) 0.dp else portraitArtworkSize(maxWidth, maxHeight)
                    ArtworkMeshBackdrop(
                        mesh = artworkMesh,
                        seam = seamlessCoverEnd,
                        reduceAnimation = reduceAnimation,
                        modifier = Modifier.fillMaxSize().testTag("player-mesh-background"),
                    )
                    // Original BitChord switches to its prepared still-art blur
                    // whenever lyrics/queue panels or a landscape layout are shown.
                    val blurBackdropAlpha by androidx.compose.animation.core.animateFloatAsState(
                        targetValue = if (preparedBlur != null && (landscape || showLyrics || showInlineQueue)) 1f else 0f,
                        animationSpec = if (reduceAnimation) androidx.compose.animation.core.snap()
                            else tween(360, easing = FastOutSlowInEasing),
                        label = "preparedBackdropAlpha",
                    )
                    if (blurBackdropAlpha > 0f) {
                        FullArtworkBlurBackdrop(
                            image = preparedBlur,
                            reduceAnimation = reduceAnimation,
                            modifier = Modifier.fillMaxSize().graphicsLayer { alpha = blurBackdropAlpha },
                        )
                    }
                }

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

                // The status icons stay white, while the top edge of a bright
                // album cover gets enough scrim to remain legible.
                Box(
                    Modifier.fillMaxWidth().height(88.dp).background(
                        Brush.verticalGradient(
                            listOf(
                                Color.Black.copy(alpha = topBandScrimAlpha(topBandLuminance)),
                                Color.Transparent,
                            ),
                        ),
                    ),
                )

                val openLyrics: () -> Unit = {
                    showInlineQueue = false
                    val opening = !showLyrics
                    showLyrics = opening
                    if (opening) {
                        lyricsControlsOpen = false
                        showLyricsTools = false
                    }
                }
                // F046: the queue belongs to this player in both orientations;
                // switching panes never creates a second MediaController or queue.
                val openQueue: () -> Unit = {
                    showLyrics = false
                    showInlineQueue = !showInlineQueue
                }
                val header: @Composable () -> Unit = {
                    if (!landscape) Column(Modifier.fillMaxWidth().testTag("player-handle").playerSwipeTrigger(1, closePlayer, panelMotion), horizontalAlignment = Alignment.CenterHorizontally) {
                        IconButton(onClick = closePlayer, modifier = Modifier.height(28.dp)) {
                            Box(Modifier.size(38.dp, 5.dp).background(MaterialTheme.colorScheme.onSurface, CircleShape)
                                .semantics { contentDescription = context.getString(R.string.player_close) })
                        }
                        val originName = s.playbackSource?.trim()?.takeIf { it.isNotEmpty() }
                        val originId = s.playbackSourceId?.takeIf { it.isNotBlank() }
                        // Link only to a source that the local catalog can still resolve.
                        val originRoute: AppRoute.Collection? = when (s.playbackSourceType) {
                            PlaybackSourceType.LOCAL_FOLDER -> originId?.let {
                                AppRoute.Collection(LocalCollectionKind.FOLDER, it)
                            }
                            PlaybackSourceType.HISTORY -> AppRoute.Collection(LocalCollectionKind.HISTORY)
                            PlaybackSourceType.BROWSE -> when {
                                albumRoute != null && originId == albumRoute.key &&
                                    originName?.equals(s.album, ignoreCase = true) == true -> albumRoute
                                else -> artistRoutes.entries.firstOrNull { (name, route) ->
                                    route.key == originId && name.equals(originName, ignoreCase = true)
                                }?.value ?: userLibraryState.playlists.firstOrNull {
                                    it.id == originId && it.name == originName && s.id in it.songIds
                                }?.let { AppRoute.Collection(LocalCollectionKind.PLAYLIST, it.id) }
                            }
                            else -> null
                        }
                        val originClick: (() -> Unit)? = when {
                            originRoute != null -> { { onOpenCollection(originRoute) } }
                            originName == null -> openQueue
                            else -> null
                        }
                        PlaybackOriginCaption(
                            text = stringResource(R.string.player_playing_album,
                                originName ?: stringResource(R.string.queue)),
                            onClick = originClick,
                            modifier = Modifier.testTag("player-playback-origin"),
                        )
                    } else Row(Modifier.fillMaxWidth().height(48.dp).testTag("player-handle")
                        .playerSwipeTrigger(1, closePlayer, panelMotion), verticalAlignment = Alignment.CenterVertically) {
                        IconButton(onClick = closePlayer) {
                            Icon(Icons.Rounded.KeyboardArrowDown, stringResource(R.string.player_close), Modifier.size(28.dp))
                        }
                        Spacer(Modifier.weight(1f))
                        val timerState = if (state.sleepRemainingMs > 0 || state.sleepAfterTrack)
                            stringResource(R.string.player_timer_active) else stringResource(R.string.player_disabled)
                        IconButton(onClick = openOutput) {
                            Icon(Icons.Rounded.Speaker, stringResource(R.string.audio_output))
                        }
                        IconButton(onClick = { showPlaybackTools = true }, modifier = Modifier.semantics { stateDescription = timerState }) {
                            Icon(Icons.Rounded.Timer, stringResource(R.string.player_tools))
                        }
                        IconButton(onClick = { showLyricsSources = true }) {
                            Icon(Icons.AutoMirrored.Rounded.Subject, stringResource(R.string.player_sources))
                        }
                        IconButton(onClick = { showSongActions = true },
                            modifier = Modifier.onGloballyPositioned { coordinates ->
                                val b = coordinates.boundsInWindow()
                                songMenuAnchor = IntRect(b.left.roundToInt(), b.top.roundToInt(),
                                    b.right.roundToInt(), b.bottom.roundToInt())
                            }) {
                            Icon(Icons.Rounded.MoreHoriz, stringResource(R.string.player_song_actions), Modifier.size(26.dp))
                        }
                    }
                }
                val artwork: @Composable (Modifier, Dp) -> Unit = { artworkModifier, artworkSize ->
                    val sleeveCorner = if (landscape || maxWidth > 600.dp) 10.dp else 0.dp
                    val progressFraction = if (state.durationMs > 0) {
                        (state.positionMs.toFloat() / state.durationMs.toFloat()).coerceIn(0f, 1f)
                    } else 0f

                    AnimatedArtwork(
                        songKey = s.id,
                        artworkUri = s.artworkUri,
                        title = s.title,
                        size = artworkSize,
                        corner = sleeveCorner,
                        modifier = artworkModifier.testTag("player-artwork")
                            .playerDockArtwork(dock, s.id, mini = false, corner = sleeveCorner),
                        progressFraction = progressFraction,
                    )

                }
                val credits: @Composable (Boolean) -> Unit = { compact ->
                    Row(Modifier.fillMaxWidth().padding(top = if (compact) 0.dp else 8.dp, bottom = 6.dp),
                        verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) {
                            val allowMarquee = !reduceAnimation && isVisible
                            val titleStyle = MaterialTheme.typography.titleLarge.copy(
                                fontSize = if (compact) 16.sp else 20.sp,
                                lineHeight = if (compact) 20.sp else 24.sp,
                                letterSpacing = (-0.2).sp, fontWeight = FontWeight.Bold)
                            Row(Modifier.fillMaxWidth().testTag("player-credit-title")
                                .then(if (albumRoute != null) Modifier.clickable {
                                    onOpenCollection(albumRoute)
                                } else Modifier),
                                verticalAlignment = Alignment.CenterVertically) {
                                // The explicit badge stays stationary while only the title scrolls.
                                if (s.isExplicit == true) {
                                    MiniPlayerExplicitBadge(Color.White.copy(alpha = 0.55f))
                                    Spacer(Modifier.width(6.dp))
                                }
                                Text(s.title, style = titleStyle, maxLines = 1, softWrap = false,
                                    overflow = TextOverflow.Clip,
                                    modifier = Modifier.weight(1f, fill = false)
                                        .then(if (allowMarquee) Modifier.basicMarquee(initialDelayMillis = 1_250)
                                              else Modifier))
                            }
                            val artistStyle = MaterialTheme.typography.titleLarge.copy(
                                fontSize = if (compact) 16.sp else 20.sp,
                                lineHeight = if (compact) 20.sp else 24.sp, fontWeight = FontWeight.W500)
                            val spans = remember(s.artist, credits) { artistCreditSegments(s.artist, credits) }
                            Row(Modifier.fillMaxWidth().testTag("player-credit-artists")
                                .then(if (allowMarquee) Modifier.basicMarquee(initialDelayMillis = 2_100)
                                      else Modifier),
                                verticalAlignment = Alignment.CenterVertically) {
                                spans.forEach { span ->
                                    val destination = span.creditName?.let(artistRoutes::get)
                                    Text(span.text, style = artistStyle,
                                        color = Color.White.copy(alpha = 0.55f),
                                        maxLines = 1, softWrap = false, overflow = TextOverflow.Clip,
                                        modifier = if (destination != null) Modifier.clickable {
                                            onOpenCollection(destination)
                                        } else Modifier)
                                }
                            }
                            state.errorMessage?.let { message ->
                                Text(message, maxLines = 2, style = MaterialTheme.typography.labelSmall)
                                TextButton(onClick = playbackViewModel::retry) { Text(stringResource(R.string.player_retry)) }
                            }
                        }
                        Spacer(Modifier.width(10.dp))
                        val favorite = s.id in userLibraryState.favorites
                        PlayerCircleGlyph(if (favorite) AurelisPlayerIcons.StarFilled else AurelisPlayerIcons.Star,
                            stringResource(if (favorite) R.string.player_remove_favorite else R.string.player_add_favorite),
                            { userStore.toggleFavorite(s.id) }, active = favorite,
                            haptic = if (favorite) com.music.aurelis.ui.haptics.Haptic.ToggleOff else com.music.aurelis.ui.haptics.Haptic.ToggleOn,
                            enabled = !userLibraryState.readOnly, checkable = true)
                        if (!landscape) {
                            Spacer(Modifier.width(8.dp))
                            PlayerCircleGlyph(
                                Icons.Rounded.MoreHoriz, stringResource(R.string.player_song_actions),
                                { showSongActions = true },
                                modifier = Modifier.onGloballyPositioned { coordinates ->
                                    val b = coordinates.boundsInWindow()
                                    songMenuAnchor = IntRect(b.left.roundToInt(), b.top.roundToInt(),
                                        b.right.roundToInt(), b.bottom.roundToInt())
                                },
                            )
                        }
                    }
                }
                val actions: @Composable () -> Unit = {
                    PlayerPlaybackActions(
                        lyricsOpen = showLyrics,
                        queueOpen = showInlineQueue,
                        shuffleEnabled = state.shuffleEnabled,
                        repeatMode = state.repeatMode,
                        autoplayEnabled = state.autoplayEnabled,
                        connected = state.isConnected,
                        canSetAutoplay = state.canSetAutoplay,
                        reduceMotion = reduceAnimation,
                        onToggleLyrics = openLyrics,
                        onToggleQueue = openQueue,
                        onToggleShuffle = playbackViewModel::toggleShuffle,
                        onCycleRepeat = playbackViewModel::cycleRepeat,
                        onToggleAutoplay = { playbackViewModel.setAutoplay(!state.autoplayEnabled) },
                        onOpenOutput = openOutput,
                    )
                }
                val transport: @Composable (Boolean) -> Unit = { compact ->
                    PlayerControls(state.isPlayRequested, state.isLoading, playbackViewModel::togglePlayPause,
                        playbackViewModel::next, playbackViewModel::previous, enabled = state.isConnected,
                        canNext = state.canNext, canPrevious = state.canPrevious, reduceAnimation = reduceAnimation,
                        blendChrome = !reduceBlur && LocalView.current.isHardwareAccelerated, compact = compact)
                }
                val deck: @Composable (Boolean, Boolean) -> Unit = { compact, lyricsMode ->
                    Column(Modifier.fillMaxWidth().testTag("player-control-deck"), horizontalAlignment = Alignment.CenterHorizontally) {
                        if (!compact && !lyricsMode) {
                            key(s.queueEntryId ?: s.id) {
                                CurrentLyricStrip(
                                    lyrics = state.lyrics,
                                    positionMs = state.positionMs,
                                    offsetMs = lyricsOffset,
                                    syncEnabled = syncEnabled,
                                    reduceMotion = reduceAnimation,
                                    onOpenLyrics = openLyrics,
                                )
                            }
                        }
                        key(s.queueEntryId ?: s.id) {
                            PlayerSlider(state.positionMs, state.durationMs, playbackViewModel::seekTo,
                                enabled = state.isConnected && state.canSeek, showRemaining = true,
                                centerLabel = {
                                    TextButton(onClick = { showQuality = true },
                                        contentPadding = androidx.compose.foundation.layout.PaddingValues(0.dp),
                                        modifier = Modifier.align(Alignment.Center).height(24.dp)) {
                                        SourceQualityBadge(state.sourceQuality.formatLabel)
                                    }
                                })
                        }
                        Spacer(Modifier.height(if (compact) 0.dp else 8.dp))
                        transport(compact)
                        Spacer(Modifier.height(if (compact) 0.dp else 6.dp))
                        if (hideVolumeBar) {
                            Box(Modifier.fillMaxWidth().height(32.dp), contentAlignment = Alignment.CenterEnd) {
                                IconButton(onClick = openOutput, modifier = Modifier.size(32.dp)) {
                                    Icon(Icons.Rounded.Speaker, stringResource(R.string.audio_output))
                                }
                            }
                        } else PlayerVolumeRow(onOpenOutput = openOutput)
                        actions()
                        if (!compact && !lyricsMode) Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center) {
                            IconButton(onClick = { showPlaybackTools = true }) { Icon(Icons.Rounded.Timer, stringResource(R.string.player_tools)) }
                            IconButton(onClick = openOutput) { Icon(Icons.Rounded.Speaker, stringResource(R.string.audio_output)) }
                            IconButton(onClick = { showLyricsSources = true }) { Icon(Icons.AutoMirrored.Rounded.Subject, stringResource(R.string.player_sources)) }
                        }
                        if (!compact) Text(
                            text = effectiveOutput?.let { outputDeviceLabel(it) }
                                ?: stringResource(R.string.pipeline_route_not_confirmed),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(top = 12.dp, bottom = 6.dp)
                                .testTag("player-output-caption")
                                .clickable(onClick = openOutput)
                                .semantics { contentDescription = context.getString(R.string.audio_output) },
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                }
                val lyricsContent: @Composable () -> Unit = {
                    val lyricsState = state.lyrics
                    val loadedLyrics = lyricsState as? LyricsState.Loaded
                    val lines = loadedLyrics?.lines.orEmpty()
                    val translation = loadedLyrics?.translation
                    val translated = (translation as? TranslationState.Done)?.lines
                    val picker = rememberLyricsPicker(s, lines, translated, s.artworkUri, rememberHaptics(), { lyricsShareRequest = it })
                    BackHandler(enabled = isVisible && showLyrics && picker.picking && lyricsShareRequest == null) { picker.cancel() }

                    androidx.compose.material3.Surface(
                        modifier = Modifier.fillMaxSize().testTag("lyrics-overlay"),
                        // F061: the lyrics must be their own opaque visual
                        // plane. A transparent surface was exposing the main
                        // player's cover, credits and a second transport deck.
                        color = if (landscape) Color.Transparent else Color(0xFF121213),
                        contentColor = Color.White,
                    ) {
                        Box(Modifier.fillMaxSize().then(if (lyricsShareRequest != null) Modifier.clearAndSetSemantics { } else Modifier)) {
                        // An inexpensive prepared blurred image replaces the
                        // UNDERLYING screen. It never blurs controls or text.
                        // The bitmap comes from the same small artwork cache as
                        // the main player, so no full-size GPU blur is scheduled.
                        if (!landscape && !reduceBlur) {
                            FullArtworkBlurBackdrop(
                                image = rememberFullArtworkBlurImage(coverUri),
                                modifier = Modifier.fillMaxSize().testTag("lyrics-own-backdrop"),
                                reduceAnimation = reduceAnimation,
                            )
                        }
                        Column(Modifier.fillMaxSize().then(if (landscape) Modifier else Modifier.statusBarsPadding().navigationBarsPadding())) {
                            if (!landscape) Column(Modifier.fillMaxWidth().padding(horizontal = 24.dp)) {
                                // BitChord keeps the artwork, credits and menu in a compact
                                // collapsed sleeve above an otherwise full-height lyric pane.
                                IconButton(onClick = { showLyrics = false }, modifier = Modifier.align(Alignment.CenterHorizontally).height(22.dp)) {
                                    Box(Modifier.size(38.dp, 5.dp).background(Color.White, CircleShape)
                                        .semantics { contentDescription = context.getString(R.string.lyrics_ui_close) })
                                }
                                Row(Modifier.fillMaxWidth().padding(top = 2.dp, bottom = 6.dp), verticalAlignment = Alignment.CenterVertically) {
                                    AnimatedArtwork(s.id, s.artworkUri, size = 56.dp, corner = 8.dp)
                                    Spacer(Modifier.width(12.dp))
                                    Box(Modifier.weight(1f)) { credits(true) }
                                }
                            } else Row(Modifier.fillMaxWidth().padding(horizontal = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                                IconButton(onClick = { showLyrics = false }) { Icon(Icons.Rounded.KeyboardArrowDown, stringResource(R.string.lyrics_ui_close)) }
                                Text(s.title, Modifier.weight(1f), maxLines = 1, overflow = TextOverflow.Ellipsis)
                            }
                            if (showLyricsTools || lines.isEmpty()) Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                                TextButton(onClick = { importSongId = s.id; importLyrics.launch(arrayOf("text/*", "application/ttml+xml", "application/xml", "application/octet-stream", "application/x-subrip")) }) { Text(stringResource(R.string.lyrics_ui_import)) }
                                TextButton(onClick = playbackViewModel::searchLyrics,
                                    enabled = state.lyrics !is LyricsState.Loading && state.lyricsLookup !is LyricsLookupState.Loading) {
                                    Text(stringResource(R.string.lyrics_search_lrclib))
                                }
                            }
                            if (showLyricsTools) TextButton(onClick = { showLyricsOffset = true }) { Text(stringResource(R.string.lyrics_ui_offset)) }
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
                            when (translation) {
                                TranslationState.Loading -> Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(stringResource(R.string.lyrics_ui_translating), modifier = Modifier.weight(1f).padding(16.dp))
                                    TextButton(onClick = playbackViewModel::clearTranslation) { Text(stringResource(R.string.lyrics_cancel_translation)) }
                                }
                                is TranslationState.Error -> Column {
                                    Text(lyricsFailureText(translation.failure), modifier = Modifier.padding(16.dp))
                                    TextButton(onClick = { showTranslationLanguages = true }) { Text(stringResource(R.string.lyrics_retry_translation)) }
                                }
                                TranslationState.Failed -> Text(stringResource(R.string.lyrics_ui_translation_failed), modifier = Modifier.padding(16.dp))
                                is TranslationState.SameLanguage -> Text(stringResource(R.string.lyrics_ui_same_language), modifier = Modifier.padding(16.dp))
                                is TranslationState.Done -> Row {
                                    Text(stringResource(R.string.lyrics_ui_translation_language, TranslationLanguages.supportedLanguages[translation.language] ?: translation.language) + if (translation.cached) " · " + stringResource(R.string.lyrics_ui_cached) else "", modifier = Modifier.weight(1f).padding(16.dp))
                                    TextButton(onClick = playbackViewModel::clearTranslation) { Text(stringResource(R.string.lyrics_ui_hide_translation)) }
                                }
                                else -> Unit
                            }
                            if (lines.isEmpty()) {
                                Box(Modifier.weight(1f), contentAlignment = Alignment.Center) {
                                    Text(
                                        modifier = Modifier.padding(16.dp),
                                        text = when (lyricsState) {
                                            is LyricsState.Loading -> stringResource(R.string.lyrics_ui_loading)
                                            is LyricsState.Missing -> stringResource(R.string.lyrics_ui_missing)
                                            is LyricsState.Failed -> lyricsState.message
                                            else -> stringResource(R.string.lyrics_ui_loading)
                                        },
                                        color = Color.White.copy(alpha = 0.7f),
                                    )
                                }
                            } else {
                                val lyricsTrackKey = s.queueEntryId ?: s.id
                                val position = remember(lyricsTrackKey) { com.music.aurelis.playback.PlaybackPosition() }
                                LaunchedEffect(lyricsTrackKey, state.positionMs, state.positionDiscontinuity, state.isPlaying) {
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
                                    centerFocus = landscape,
                                    isSynced = loaded?.isSynced == true,
                                    translatedLines = translated,
                                    syncEnabled = syncEnabled, blurUnfocused = blurLyrics && !reduceBlur,
                                    onLineLongClick = { index -> lines.getOrNull(index)?.takeIf { !it.isGap }?.let { picker.pick(index) } },
                                    picking = picker.picking, picked = picker.picks, onTogglePick = picker.toggle,
                                    showFollowControls = false, controlsOpen = lyricsControlsOpen || landscape,
                                    onRevealControls = { lyricsControlsOpen = true }, onHideControls = { if (!landscape) lyricsControlsOpen = false },
                                    modifier = Modifier.weight(1f),
                                )
                            }
                            if (picker.picking) LyricsPickBar(picker.picks.isNotEmpty(), picker.cancel, picker.share)
                            if (picker.overBudget) Text(stringResource(R.string.lyrics_share_pick_limit), Modifier.padding(horizontal = 24.dp), color = Color.White)
                            AnimatedVisibility(visible = lyricsControlsOpen || landscape,
                                enter = if (reduceAnimation) androidx.compose.animation.EnterTransition.None else expandVertically(tween(420, easing = FastOutSlowInEasing)) + fadeIn(tween(420)),
                                exit = if (reduceAnimation) androidx.compose.animation.ExitTransition.None else shrinkVertically(tween(420, easing = FastOutSlowInEasing)) + fadeOut(tween(420))) {
                                Column(Modifier.then(if (!lyricsControlsOpen && !landscape) Modifier.clearAndSetSemantics { } else Modifier)) {
                            Row(Modifier.fillMaxWidth().padding(horizontal = 24.dp), verticalAlignment = Alignment.CenterVertically) {
                                IconButton(onClick = { showLyricsTools = !showLyricsTools }) { Icon(Icons.Rounded.Language, stringResource(R.string.lyrics_ui_tools)) }
                                TextButton(onClick = { showLyricsSources = true }, modifier = Modifier.weight(1f)) {
                                    Text(loaded?.let { lyricsSourceTitle(it.source) + if (it.cached) " · " + stringResource(R.string.lyrics_ui_cached) else "" } ?: stringResource(R.string.player_sources), maxLines = 1, overflow = TextOverflow.Ellipsis,
                                        textDecoration = TextDecoration.Underline, color = Color.White.copy(alpha = 0.75f))
                                }
                                IconButton(onClick = { showTranslationLanguages = true }) { Icon(Icons.Rounded.Translate, stringResource(R.string.lyrics_ui_translate)) }
                                IconButton(onClick = picker.beginPicking, enabled = lines.any { !it.isGap && it.text.isNotBlank() }) { Icon(Icons.Rounded.IosShare, stringResource(R.string.lyrics_share_title)) }
                            }
                            if (!landscape) Box(Modifier.padding(horizontal = 30.dp)) { deck(compactLyrics, true) }
                            else Row(Modifier.fillMaxWidth().padding(horizontal = 16.dp), verticalAlignment = Alignment.CenterVertically) {
                                Box(Modifier.weight(1f)) { transport(true) }
                            }
                                }
                            }
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
                                if (showInlineQueue) { transport(true); actions() }
                                header()
                            } },
                            mainPane = { compact ->
                                Column(Modifier.fillMaxSize(), horizontalAlignment = Alignment.CenterHorizontally) {
                                    Box(Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                                        Column(Modifier.verticalScroll(rememberScrollState())) { credits(compact) }
                                    }
                                    deck(true, false)
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
                        val portraitArtwork = portraitArtworkSize(maxWidth, maxHeight)
                        val compactPortrait = maxHeight < 650.dp
                        Box(
                            Modifier.fillMaxSize().testTag("player-portrait")
                                // Keep geometry/dock tracking in place, but
                                // never paint the main player behind an opened
                                // lyrics or queue pane. It caused duplicate
                                // transport buttons and overlapping captions.
                                .graphicsLayer {
                                    alpha = if (showLyrics || showInlineQueue) 0f else 1f
                                },
                        ) {
                            Box(Modifier.fillMaxWidth().align(Alignment.TopCenter),
                                contentAlignment = Alignment.TopCenter) {
                                Box(Modifier.size(portraitArtwork)) {
                                    artwork(Modifier, portraitArtwork)
                                    Box(Modifier.fillMaxSize().background(
                                        Brush.verticalGradient(listOf(Color.Transparent, MaterialTheme.colorScheme.background))))
                                }
                            }
                            Column(Modifier.fillMaxSize().statusBarsPadding().navigationBarsPadding().padding(horizontal = 30.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                                header()
                                Spacer(Modifier.weight(1f))
                                credits(compactPortrait)
                                deck(compactPortrait, false)
                            }
                        }
                    }
                }
                AnimatedVisibility(visible = showLyrics && !landscape,
                    enter = if (reduceAnimation) androidx.compose.animation.EnterTransition.None else fadeIn(tween(420, easing = FastOutSlowInEasing)),
                    exit = if (reduceAnimation) androidx.compose.animation.ExitTransition.None else fadeOut(tween(420, easing = FastOutSlowInEasing))) {
                    Box(Modifier.fillMaxSize().then(if (!showLyrics) Modifier.clearAndSetSemantics { } else Modifier)) { lyricsContent() }
                }
                // The queue is a pane of the current player, not an independent
                // 85%-height sheet. Keep transport and queue-mode controls anchored
                // while the same canonical Media3 queue scrolls above them.
                if (showInlineQueue && !landscape) {
                    Box(Modifier.fillMaxSize().background(Color(0xFF121213))
                        .testTag("player-queue-pane")) {
                        // F062: a real artwork-coloured queue page rather than
                        // the former full-screen, hard-edged black panel.
                        // The upper layout still uses the existing Media3 queue.
                        if (!reduceBlur) {
                            FullArtworkBlurBackdrop(
                                image = rememberFullArtworkBlurImage(coverUri),
                                modifier = Modifier.fillMaxSize().testTag("queue-own-backdrop"),
                                reduceAnimation = reduceAnimation,
                            )
                        }
                        Column(Modifier.fillMaxSize().statusBarsPadding().navigationBarsPadding()
                            .padding(horizontal = 12.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                            Box(Modifier.weight(1f).fillMaxWidth()) {
                                QueueScreen(playbackViewModel, true,
                                    { showInlineQueue = false }, embedded = true,
                                    listState = queueListState)
                            }
                            Spacer(Modifier.height(4.dp))
                            transport(true)
                            actions()
                            Spacer(Modifier.height(12.dp))
                        }
                    }
                }

                lyricsShareRequest?.let { request -> AndroidLyricsShareSheet(hazeState, request, { lyricsShareRequest = null }) }
                if (showOutputHelp) androidx.compose.material3.AlertDialog(
                    onDismissRequest = { showOutputHelp = false },
                    title = { Text(stringResource(R.string.audio_output)) },
                    text = { Text(stringResource(R.string.player_output_system_unavailable)) },
                    confirmButton = { TextButton(onClick = {
                        showOutputHelp = false
                        runCatching { context.startActivity(Intent(Settings.ACTION_SOUND_SETTINGS)) }
                    }) { Text(stringResource(R.string.player_output_open_settings)) } },
                    dismissButton = { TextButton(onClick = { showOutputHelp = false }) {
                        Text(stringResource(R.string.player_close_dialog))
                    } },
                )
                if (showPlaybackTools) com.music.aurelis.ui.components.PlaybackToolsDialog(
                    state, { duration, after -> playbackViewModel.setSleepTimer(duration, after) },
                    playbackViewModel::setPlaybackSpeed, playbackViewModel::setSkipSilence, playbackViewModel::setAutoplay, { showPlaybackTools = false })
                if (showSongActions) SongActionsDialog(s, userStore,
                    onEnqueue = { playbackViewModel.enqueue(listOf(s)) }, onDismiss = { showSongActions = false },
                    onPlayNext = { playbackViewModel.playNext(listOf(s)) },
                    queueActionsEnabled = state.isConnected && state.canEditQueue,
                    playNextEnabled = !state.shuffleEnabled || state.canEditShuffleQueue,
                    onOpenAlbum = albumRoute?.let { destination -> { onOpenCollection(destination) } },
                    onOpenArtist = artistRoutes.values.distinct().singleOrNull()?.let { destination ->
                        { onOpenCollection(destination) }
                    },
                    anchorBounds = songMenuAnchor,
                    onLyricsOffset = { showLyricsOffset = true },
                    onPlaybackTools = { showPlaybackTools = true },
                )
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
