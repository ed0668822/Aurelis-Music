// SPDX-FileCopyrightText: © 2026 Aurelis

package com.music.aurelis.ui.player

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Intent
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Download
import androidx.compose.material.icons.rounded.IosShare
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.music.aurelis.R
import com.music.aurelis.ui.replay.ShareAction
import com.music.aurelis.ui.replay.saveToGallery
import dev.chrisbanes.haze.HazeState
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch
import java.io.File
import kotlin.math.roundToInt

private enum class LyricsExportAction { SAVE, SHARE }

private data class LyricsExportFailure(val action: LyricsExportAction, val message: Int)

@Composable
internal fun AndroidLyricsShareSheet(
    hazeState: HazeState,
    request: LyricsShareRequest,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val clipboard = remember(context) { context.getSystemService(ClipboardManager::class.java) }

    val timeline = remember(request) { lyricsShareTimeline(request.lines) }
    val localAudioUri = remember(request) { localLyricsAudioUri(request) }
    val hasSubText = request.lines.any { !it.subText.isNullOrBlank() }
    var asVideo by remember(request) { mutableStateOf(false) }
    var format by remember(request) { mutableStateOf(LyricsImageFormat.CARD) }
    var includeSubText by remember(request) { mutableStateOf(hasSubText) }
    var includeAudio by remember(request) { mutableStateOf(localAudioUri != null) }
    val renderFormat = if (asVideo) LyricsImageFormat.STORY else format
    val audioUri = localAudioUri.takeIf { includeAudio }
    var card by remember(request, renderFormat, includeSubText) { mutableStateOf<RenderedLyricsCard?>(null) }
    var rendering by remember(request, renderFormat, includeSubText) { mutableStateOf(true) }
    var renderError by remember(request, renderFormat, includeSubText) { mutableStateOf(false) }
    var renderAttempt by remember(request, renderFormat, includeSubText) { mutableStateOf(0) }
    var busy by remember(request) { mutableStateOf(false) }
    var saved by remember(request, asVideo, renderFormat, includeSubText, audioUri) { mutableStateOf(false) }
    var copied by remember(request, includeSubText) { mutableStateOf(false) }
    var failure by remember(request, asVideo, renderFormat, includeSubText, audioUri) {
        mutableStateOf<LyricsExportFailure?>(null)
    }
    var exportedVideo by remember(request, includeSubText, audioUri) { mutableStateOf<File?>(null) }

    LaunchedEffect(request, renderFormat, includeSubText, renderAttempt) {
        try {
            card = renderLyricsShareCard(context, request, renderFormat, includeSubText)
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (_: Exception) {
            renderError = true
        } finally {
            rendering = false
        }
    }

    fun export(action: LyricsExportAction) {
        val rendered = card ?: return
        if (busy) return
        busy = true
        failure = null
        saved = false
        scope.launch {
            var errorMessage = if (action == LyricsExportAction.SAVE) {
                R.string.lyrics_share_save_failed
            } else {
                R.string.lyrics_share_send_failed
            }
            try {
                val video = if (asVideo && timeline != null) {
                    val existing = exportedVideo?.takeIf { it.isFile && it.length() > 0L }
                    existing ?: run {
                        val newVideo = try {
                            exportLocalLyricsVideo(context, rendered, timeline, audioUri)
                        } catch (cancelled: CancellationException) {
                            throw cancelled
                        } catch (_: Exception) {
                            null
                        }
                        if (newVideo == null) {
                            errorMessage = if (audioUri != null) {
                                R.string.lyrics_share_audio_failed
                            } else {
                                R.string.lyrics_share_video_failed
                            }
                            error("Video export failed")
                        }
                        newVideo.also { exportedVideo = it }
                    }
                } else {
                    null
                }
                if (action == LyricsExportAction.SAVE) {
                    val success = if (video != null) {
                        saveLocalLyricsVideo(context, video, request.song.title)
                    } else {
                        saveToGallery(context, rendered.bitmap, request.song.title, "aurelis-lyrics")
                    }
                    check(success)
                    saved = true
                } else {
                    val intent = if (video != null) {
                        shareLocalLyricsVideoIntent(context, video)
                    } else {
                        cacheLyricsImage(context, rendered.bitmap)?.let { shareLyricsMediaIntent(it, "image/png") }
                    }
                    checkNotNull(intent)
                    context.startActivity(Intent.createChooser(intent, context.getString(R.string.lyrics_share_title)))
                }
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (_: Exception) {
                failure = LyricsExportFailure(action, errorMessage)
            } finally {
                busy = false
            }
        }
    }

    PlayerDrawer(
        hazeState = hazeState,
        title = stringResource(R.string.lyrics_share_title),
        onDismiss = onDismiss,
        modifier = modifier,
        titleGap = 2.dp,
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Row(Modifier.fillMaxWidth().selectableGroup(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                LyricsShareOption(stringResource(R.string.lyrics_share_image), !asVideo, !busy, Modifier.weight(1f)) {
                    asVideo = false
                }
                LyricsShareOption(
                    stringResource(R.string.lyrics_share_video), asVideo, !busy && timeline != null, Modifier.weight(1f),
                ) {
                    asVideo = true
                }
            }
            if (timeline == null) {
                Text(stringResource(R.string.lyrics_share_video_requires_timing), style = MaterialTheme.typography.bodySmall)
            }

            val rendered = card
            when {
                rendering -> CircularProgressIndicator(Modifier.size(26.dp))
                renderError -> {
                    Text(stringResource(R.string.couldnt_draw_picture), color = MaterialTheme.colorScheme.error)
                    TextButton(onClick = {
                        rendering = true
                        renderError = false
                        renderAttempt++
                    }) { Text(stringResource(R.string.retry)) }
                }
                rendered == null -> Text(
                    stringResource(R.string.lyrics_share_format_full),
                    color = MaterialTheme.colorScheme.error,
                    textAlign = TextAlign.Center,
                )
                else -> LyricsCardPreview(rendered, timeline.takeIf { asVideo }, busy)
            }

            if (!asVideo) {
                Row(Modifier.fillMaxWidth().selectableGroup(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    LyricsImageFormat.entries.forEach { choice ->
                        val label = when (choice) {
                            LyricsImageFormat.CARD -> R.string.lyrics_share_format_card
                            LyricsImageFormat.SQUARE -> R.string.lyrics_share_format_square
                            LyricsImageFormat.STORY -> R.string.lyrics_share_format_story
                        }
                        LyricsShareOption(stringResource(label), format == choice, !busy, Modifier.weight(1f)) {
                            format = choice
                        }
                    }
                }
            } else {
                if (localAudioUri != null) {
                    LyricsShareSwitch(
                        label = stringResource(if (includeAudio) R.string.lyrics_share_with_audio else R.string.lyrics_share_without_audio),
                        checked = includeAudio,
                        enabled = !busy,
                    ) { includeAudio = it }
                } else {
                    Text(stringResource(R.string.lyrics_share_without_audio), style = MaterialTheme.typography.bodyMedium)
                }
                if (timeline != null && timeline.windows.size < request.lines.count { !it.isGap }) {
                    Text(
                        stringResource(R.string.lyrics_share_partial_timing),
                        style = MaterialTheme.typography.bodySmall,
                        textAlign = TextAlign.Center,
                    )
                }
            }

            if (hasSubText) {
                LyricsShareSwitch(
                    label = stringResource(R.string.lyrics_share_include_subtext),
                    checked = includeSubText,
                    enabled = !busy,
                ) { includeSubText = it }
            }
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                TextButton(enabled = !busy, onClick = onDismiss) {
                    Text(stringResource(R.string.lyrics_share_edit))
                }
                TextButton(enabled = !busy && clipboard != null, onClick = {
                    if (clipboard == null) return@TextButton
                    val text = lyricsShareText(request.song.title, request.song.artist, request.lines, includeSubText)
                    clipboard.setPrimaryClip(ClipData.newPlainText(request.song.title, text))
                    copied = true
                }) {
                    Text(stringResource(if (copied) R.string.lyrics_share_copied else R.string.lyrics_share_copy))
                }
            }

            if (busy) {
                LinearProgressIndicator(Modifier.fillMaxWidth())
                Text(stringResource(R.string.lyrics_share_preparing), style = MaterialTheme.typography.bodySmall)
            }
            failure?.let { failed ->
                Text(
                    stringResource(failed.message),
                    color = MaterialTheme.colorScheme.error,
                    textAlign = TextAlign.Center,
                )
                TextButton(onClick = { export(failed.action) }) { Text(stringResource(R.string.retry)) }
            }
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                ShareAction(
                    label = stringResource(R.string.save),
                    icon = Icons.Rounded.Download,
                    accent = false,
                    enabled = rendered != null && !busy,
                    saved = saved,
                    savedLabel = stringResource(R.string.saved_bang),
                    modifier = Modifier.weight(1f),
                    onClick = { export(LyricsExportAction.SAVE) },
                )
                ShareAction(
                    label = stringResource(R.string.share),
                    icon = Icons.Rounded.IosShare,
                    accent = true,
                    enabled = rendered != null && !busy,
                    modifier = Modifier.weight(1f),
                    onClick = { export(LyricsExportAction.SHARE) },
                )
            }
        }
    }
}

internal fun lyricsShareText(
    title: String,
    artist: String,
    lines: List<LyricsShareLine>,
    includeSubText: Boolean,
): String = buildString {
    append(lines.joinToString("\n") { line ->
        when {
            line.isGap -> "…"
            includeSubText && !line.subText.isNullOrBlank() -> "${line.text}\n${line.subText}"
            else -> line.text
        }
    })
    val credit = listOf(title, artist).filter { it.isNotBlank() }.joinToString(" — ")
    if (credit.isNotEmpty()) append("\n\n").append(credit)
}

@Composable
private fun LyricsShareOption(
    label: String,
    selected: Boolean,
    enabled: Boolean,
    modifier: Modifier,
    onClick: () -> Unit,
) {
    Text(
        text = label,
        color = Color.White.copy(alpha = if (enabled) 1f else 0.45f),
        textAlign = TextAlign.Center,
        style = MaterialTheme.typography.labelLarge,
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .background(Color.White.copy(alpha = if (selected) 0.25f else 0.08f))
            .selectable(selected = selected, enabled = enabled, role = Role.RadioButton, onClick = onClick)
            .padding(horizontal = 6.dp, vertical = 12.dp),
    )
}

@Composable
private fun LyricsShareSwitch(label: String, checked: Boolean, enabled: Boolean, onCheckedChange: (Boolean) -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().toggleable(
            value = checked,
            enabled = enabled,
            role = Role.Switch,
            onValueChange = onCheckedChange,
        ),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(label, Modifier.weight(1f), style = MaterialTheme.typography.bodyMedium)
        Switch(checked, onCheckedChange = null, enabled = enabled)
    }
}

@Composable
private fun LyricsCardPreview(card: RenderedLyricsCard, timeline: LyricsShareTimeline?, busy: Boolean) {
    val bitmap = remember(card) { card.bitmap.asImageBitmap() }
    val ratio = bitmap.width.toFloat() / bitmap.height
    val highlight = remember(card, timeline) { timeline?.let { LyricsVideoHighlight(card, it) } }
    val progress = remember(card, timeline) { Animatable(0f) }
    var playing by remember(card, timeline) { mutableStateOf(false) }

    LaunchedEffect(timeline, playing, busy) {
        if (timeline != null && playing && !busy) {
            if (progress.value >= 1f) progress.snapTo(0f)
            val remainingMs = (timeline.durationMs * (1f - progress.value)).roundToInt().coerceAtLeast(1)
            progress.animateTo(1f, tween(remainingMs, easing = LinearEasing))
            playing = false
        }
    }
    Box(
        Modifier
            .fillMaxWidth((0.85f * ratio).coerceIn(0.40f, 0.98f))
            .aspectRatio(ratio)
            .clip(RoundedCornerShape(16.dp)),
    ) {
        Image(bitmap, stringResource(R.string.your_lyrics_card), Modifier.fillMaxSize())
        if (highlight != null && timeline != null) {
            Canvas(Modifier.matchParentSize()) {
                val durationUs = timeline.durationMs * 1000L
                val positionUs = (durationUs.toDouble() * progress.value).toLong().coerceAtMost(durationUs - 1)
                highlight.draw(drawContext.canvas.nativeCanvas, size.width, size.height, positionUs)
            }
        }
    }
    if (timeline != null) {
        TextButton(enabled = !busy, onClick = { playing = !playing }) {
            Text(stringResource(if (playing) R.string.lyrics_share_pause_preview else R.string.lyrics_share_play_preview))
        }
    }
}
