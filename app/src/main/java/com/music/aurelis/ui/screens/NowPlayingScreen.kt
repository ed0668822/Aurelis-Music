// Aurelis © 2026. GPL-3.0-or-later
package com.music.aurelis.ui.screens

import android.net.Uri
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
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.runtime.key
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
import androidx.compose.runtime.LaunchedEffect
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
) {
    val state by playbackViewModel.state.collectAsState()
    val song = state.currentSong
    val userStore = (LocalContext.current.applicationContext as AurelisApplication).userLibrary
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

    var showLyrics by remember { mutableStateOf(false) }
    var showLyricsSources by remember { mutableStateOf(false) }
    var showTranslationLanguages by remember { mutableStateOf(false) }
    var showLyricsOffset by remember { mutableStateOf(false) }

    BackHandler(enabled = isVisible && showLyrics) { showLyrics = false }

    val lyricsOffset by SettingsRepository.lyricsOffsetMs.collectAsState()
    val reduceAnimation by SettingsRepository.reduceAnimation.collectAsState()
    val hazeState = remember { HazeState() }

    AnimatedVisibility(
        visible = isVisible && song != null,
        enter = if (reduceAnimation) androidx.compose.animation.EnterTransition.None else slideInVertically(
            animationSpec = tween(durationMillis = 380, easing = FastOutSlowInEasing),
            initialOffsetY = { it },
        ) + fadeIn(animationSpec = tween(280)),
        exit = if (reduceAnimation) androidx.compose.animation.ExitTransition.None else slideOutVertically(
            animationSpec = tween(durationMillis = 320, easing = FastOutSlowInEasing),
            targetOffsetY = { it },
        ) + fadeOut(animationSpec = tween(220)),
    ) {
        val s = song ?: return@AnimatedVisibility

        DynamicAurelisTheme(colors = artworkColors ?: fallbackColors) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(MaterialTheme.colorScheme.background)
                    .hazeSource(hazeState),
            ) {
                val meshPalette = rememberArtworkColors(s.artworkUri?.let { Uri.parse(it) })
                MeshGradientBackground(
                    palette = meshPalette,
                    trackKey = s.id,
                    continuous = state.isPlaying && !reduceAnimation,
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

                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .statusBarsPadding()
                        .navigationBarsPadding()
                        .verticalScroll(rememberScrollState())
                        .padding(horizontal = 24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        IconButton(onClick = onDismiss) {
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

                    Spacer(modifier = Modifier.height(24.dp))

                    val progressFraction = if (state.durationMs > 0) {
                        (state.positionMs.toFloat() / state.durationMs.toFloat()).coerceIn(0f, 1f)
                    } else 0f

                    AnimatedArtwork(
                        songKey = s.id,
                        artworkUri = s.artworkUri,
                        title = s.title,
                        size = (LocalConfiguration.current.screenWidthDp - 48).coerceIn(160, 320).dp,
                        progressFraction = progressFraction,
                    )

                    Spacer(modifier = Modifier.height(32.dp))

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
                        val qualityLabel = "Archivo local"
                        // FIX: Chip cambió de firma en Material3 — usamos Box+Text
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(12.dp))
                                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                                .padding(horizontal = 8.dp, vertical = 3.dp),
                        ) {
                            Text(
                                text = qualityLabel,
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
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

                if (showLyrics) {
                    val lyricsState = state.lyrics
                    val lines = when (lyricsState) {
                        is LyricsState.Loaded -> {
                            val trans = lyricsState.translation
                            if (trans is TranslationState.Done) trans.lines else lyricsState.lines
                        }
                        else -> emptyList()
                    }

                    Box(
                        Modifier
                            .fillMaxSize()
                            .background(MaterialTheme.colorScheme.background.copy(alpha = 0.95f))
                            .statusBarsPadding(),
                    ) {
                        Column(Modifier.fillMaxSize()) {
                            Row(
                                modifier = Modifier.fillMaxWidth().padding(16.dp),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                IconButton(onClick = { showLyrics = false }) {
                                    Icon(Icons.Rounded.KeyboardArrowDown, "Cerrar letras", tint = Color.White)
                                }
                                Spacer(Modifier.weight(1f))
                                IconButton(onClick = { showTranslationLanguages = true }) {
                                    Icon(Icons.Rounded.Translate, "Traducir", tint = Color.White)
                                }
                            }

                            if (lines.isEmpty()) {
                                Box(Modifier.weight(1f), contentAlignment = Alignment.Center) {
                                    Text(
                                        text = when (lyricsState) {
                                            is LyricsState.Loading -> "Buscando letras..."
                                            is LyricsState.Missing -> "No se encontraron letras."
                                            else -> "Cargando..."
                                        },
                                        color = Color.White.copy(alpha = 0.7f),
                                    )
                                }
                            } else {
                                val position = remember { com.music.aurelis.playback.PlaybackPosition() }
                                LaunchedEffect(state.positionMs, lyricsOffset) {
                                    position.report(state.positionMs + lyricsOffset)
                                    position.advancing = state.isPlaying
                                }

                                SyncedLyricsPanel(
                                    lines = lines,
                                    trackKey = s.id,
                                    position = position,
                                    looking = true,
                                    isPlaying = state.isPlaying,
                                    onSeekToLine = { playbackViewModel.seekTo(it) },
                                    modifier = Modifier.weight(1f),
                                )
                            }
                        }
                    }
                }

                if (showSongActions) SongActionsDialog(s, userStore,
                    onEnqueue = { playbackViewModel.enqueue(listOf(s)) }, onDismiss = { showSongActions = false })
                if (showLyricsSources) {
                    LyricsSourcesDialog(onDismiss = { showLyricsSources = false })
                }
                if (showTranslationLanguages) {
                    TranslationLanguageDialog(
                        onDismiss = {
                            showTranslationLanguages = false
                            val currentLang = SettingsRepository.translationLanguage.value
                            if (currentLang.isNotBlank()) {
                                playbackViewModel.translateLyrics(currentLang)
                            }
                        },
                    )
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
