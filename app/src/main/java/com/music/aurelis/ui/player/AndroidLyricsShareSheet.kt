// SPDX-FileCopyrightText: © 2026 Aurelis

package com.music.aurelis.ui.player

import android.content.Intent
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

private enum class LyricsExportAction { SAVE, SHARE }

@Composable
internal fun AndroidLyricsShareSheet(
    hazeState: HazeState,
    request: LyricsShareRequest,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val shareTitle = stringResource(R.string.lyrics_share_title)
    val scope = rememberCoroutineScope()
    val hasSubText = request.lines.any { !it.subText.isNullOrBlank() }
    var format by remember(request) { mutableStateOf(LyricsImageFormat.CARD) }
    var includeSubText by remember(request) { mutableStateOf(hasSubText) }
    var card by remember(request, format, includeSubText) { mutableStateOf<RenderedLyricsCard?>(null) }
    var rendering by remember(request, format, includeSubText) { mutableStateOf(true) }
    var renderError by remember(request, format, includeSubText) { mutableStateOf(false) }
    var renderAttempt by remember(request, format, includeSubText) { mutableStateOf(0) }
    var busy by remember(request) { mutableStateOf(false) }
    var saved by remember(request, format, includeSubText) { mutableStateOf(false) }
    var failure by remember(request, format, includeSubText) { mutableStateOf<LyricsExportAction?>(null) }

    LaunchedEffect(request, format, includeSubText, renderAttempt) {
        try {
            card = renderLyricsShareCard(context, request, format, includeSubText)
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
            try {
                when (action) {
                    LyricsExportAction.SAVE -> {
                        check(saveToGallery(context, rendered.bitmap, request.song.title, "aurelis-lyrics"))
                        saved = true
                    }
                    LyricsExportAction.SHARE -> {
                        val uri = checkNotNull(cacheLyricsImage(context, rendered.bitmap))
                        context.startActivity(Intent.createChooser(shareLyricsImageIntent(uri), shareTitle))
                    }
                }
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (_: Exception) {
                failure = action
            } finally {
                busy = false
            }
        }
    }

    PlayerDrawer(
        hazeState = hazeState,
        title = shareTitle,
        onDismiss = onDismiss,
        modifier = modifier,
        titleGap = 2.dp,
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
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
                else -> LyricsCardPreview(rendered)
            }

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

            if (hasSubText) {
                LyricsShareSwitch(
                    label = stringResource(R.string.lyrics_share_include_subtext),
                    checked = includeSubText,
                    enabled = !busy,
                ) { includeSubText = it }
            }
            TextButton(enabled = !busy, onClick = onDismiss, modifier = Modifier.align(Alignment.Start)) {
                Text(stringResource(R.string.lyrics_share_edit))
            }

            if (busy) {
                LinearProgressIndicator(Modifier.fillMaxWidth())
                Text(stringResource(R.string.lyrics_share_preparing), style = MaterialTheme.typography.bodySmall)
            }
            failure?.let { failed ->
                Text(
                    stringResource(
                        if (failed == LyricsExportAction.SAVE) R.string.lyrics_share_save_failed
                        else R.string.lyrics_share_send_failed,
                    ),
                    color = MaterialTheme.colorScheme.error,
                    textAlign = TextAlign.Center,
                )
                TextButton(onClick = { export(failed) }) { Text(stringResource(R.string.retry)) }
            }
            val canExport = rendered != null && !busy
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                ShareAction(
                    label = stringResource(R.string.save),
                    icon = Icons.Rounded.Download,
                    accent = false,
                    enabled = canExport,
                    saved = saved,
                    savedLabel = stringResource(R.string.saved_bang),
                    modifier = Modifier.weight(1f),
                    onClick = { export(LyricsExportAction.SAVE) },
                )
                ShareAction(
                    label = stringResource(R.string.share),
                    icon = Icons.Rounded.IosShare,
                    accent = true,
                    enabled = canExport,
                    modifier = Modifier.weight(1f),
                    onClick = { export(LyricsExportAction.SHARE) },
                )
            }
        }
    }
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
private fun LyricsCardPreview(card: RenderedLyricsCard) {
    val bitmap = remember(card) { card.bitmap.asImageBitmap() }
    val ratio = bitmap.width.toFloat() / bitmap.height
    Box(
        Modifier
            .fillMaxWidth((0.85f * ratio).coerceIn(0.40f, 0.98f))
            .aspectRatio(ratio)
            .clip(RoundedCornerShape(16.dp)),
    ) {
        Image(bitmap, stringResource(R.string.your_lyrics_card), Modifier.fillMaxSize())
    }
}
