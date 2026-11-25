// SPDX-FileCopyrightText: © 2026 Aurelis

package com.music.aurelis.ui.player

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.IosShare
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.music.aurelis.data.lyrics.LyricLine
import com.music.aurelis.data.model.Song
import com.music.aurelis.sharedui.resources.Res
import com.music.aurelis.sharedui.resources.cancel
import com.music.aurelis.sharedui.resources.lyrics_share_preview
import com.music.aurelis.ui.haptics.Haptic
import com.music.aurelis.ui.haptics.Haptics
import com.music.aurelis.ui.haptics.rememberHaptics
import org.jetbrains.compose.resources.stringResource

/** A selected verse, or a marker for omitted verses between two selected verses. */
data class LyricsShareLine(
    val text: String,
    val subText: String?,
    val isGap: Boolean = false,
    /** Adjusted synchronized line start, absent for plain lyrics or a gap. */
    val startMs: Long? = null,
    /** Adjusted synchronized line end, absent when unknown. */
    val endMs: Long? = null,
)

/**
 * Everything the platform's card renderer needs, gathered once by the player.
 *
 * The drawing itself is not here: it needs a bitmap and a canvas, which the
 * desktop has no answer for. The player builds one of these and hands it to
 * [PlayerHost.LyricsShareSheet], and the phone is what turns it into a picture.
 */
data class LyricsShareRequest(
    val song: Song,
    /** Resolved sleeve — remote URL or local file, whatever the player found. */
    val artworkUrl: String?,
    val lines: List<LyricsShareLine>,
)

/**
 * Whether lines can be put on a card at all here.
 *
 * Reading false on a platform is the whole of what keeps the pick mode off a
 * desktop: there is nowhere to put the finished picture, so a mode whose Share
 * button did nothing would be worse than no mode.
 */
val lyricsShareAvailable: Boolean
    get() = PlayerPlatform.host.lyricsShareAvailable

/** Selection and actions shared by the full and inline lyrics panels. */
internal class LyricsPicker(
    /** Whether the reader is choosing lines to put on a share card. */
    val picking: Boolean,
    /** Which lines are chosen, as indices into the panel's own list. */
    val picks: Set<Int>,
    /** Long-press on a line: start picking, or fold that line into the pick. */
    val pick: (Int) -> Unit,
    /** Tap while picking: add or drop that line. */
    val toggle: (Int) -> Unit,
    val cancel: () -> Unit,
    /** Opens a preview while keeping the selected lines available for editing. */
    val preview: () -> Unit,
    /** Header share button: enter picking with nothing preselected. */
    val beginPicking: () -> Unit,
)

@Composable
internal fun rememberLyricsPicker(
    song: Song,
    /** The panel's lines, index for index with the picks. */
    lines: List<LyricLine>?,
    /** The translation or romanization drawn under each line, if any. */
    subLines: List<LyricLine>?,
    artworkUrl: String?,
    lyricsOffsetMs: Int,
    haptics: Haptics,
    /** Where the finished card goes; never called on a platform that has none. */
    onCard: (LyricsShareRequest) -> Unit,
): LyricsPicker {
    var picking by remember(song.videoId, lines) { mutableStateOf(false) }
    var picks by remember(song.videoId, lines) { mutableStateOf<Set<Int>>(emptySet()) }

    val toggle: (Int) -> Unit = { index ->
        val line = lines?.getOrNull(index)
        if (line != null && line.text.isNotBlank()) {
            picks = if (index in picks) picks - index else picks + index
        }
    }
    val beginPicking: () -> Unit = {
        if (!picking) {
            haptics.play(Haptic.Select)
            picking = true
            picks = emptySet()
        }
    }
    val pick: (Int) -> Unit = { index ->
        if (!lines?.getOrNull(index)?.text.isNullOrBlank()) {
            beginPicking()
            toggle(index)
        }
    }
    val cancel: () -> Unit = {
        picking = false
        picks = emptySet()
    }
    val preview: () -> Unit = {
        val source = lines.orEmpty()
        val chosen = picks.sorted()
        val picked = buildList {
            chosen.forEachIndexed { position, index ->
                // A jump over unchosen lines is marked on the card, so the
                // picture says the verse was cut here rather than letting two
                // distant halves read as if they ran on from each other.
                if (position > 0 && index > chosen[position - 1] + 1) {
                    add(LyricsShareLine(text = "", subText = null, isGap = true))
                }
                val line = source.getOrNull(index) ?: return@forEachIndexed
                val sub = subLines?.getOrNull(index)?.text
                // Plain LRC often has no line end. Use the next observed start when no end exists.
                val end = line.sungUntilMs?.takeIf { it > line.timeMs }
                    ?: line.words.lastOrNull()?.endMs?.takeIf { it > line.timeMs }
                    ?: ((index + 1) until source.size).firstNotNullOfOrNull { next ->
                        source[next].timeMs.takeIf { it > line.timeMs }
                    }
                add(
                    LyricsShareLine(
                        text = line.text,
                        subText = sub?.takeIf { it.isNotBlank() && it != line.text },
                        startMs = line.timeMs.takeIf { it >= 0L }?.let {
                            adjustedLyricsSeekTarget(it, lyricsOffsetMs)
                        },
                        endMs = end?.let { adjustedLyricsSeekTarget(it, lyricsOffsetMs) },
                    ),
                )
            }
        }.filter { it.isGap || it.text.isNotBlank() }
        // An empty pick draws nothing worth sending, so it is simply left alone
        // rather than putting an empty card on screen to be dismissed.
        if (picked.isNotEmpty()) {
            onCard(LyricsShareRequest(song = song, artworkUrl = artworkUrl, lines = picked))
        }
    }

    return LyricsPicker(
        picking = picking,
        picks = picks,
        pick = pick,
        toggle = toggle,
        cancel = cancel,
        preview = preview,
        beginPicking = beginPicking,
    )
}

@Composable
internal fun LyricsPickBar(
    previewEnabled: Boolean,
    onCancel: () -> Unit,
    onPreview: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val haptics = rememberHaptics()
    Row(
        modifier = modifier
            .padding(horizontal = 16.dp, vertical = 12.dp)
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .background(Color(0xFF141414).copy(alpha = 0.92f))
            .padding(12.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        PickAction(
            label = stringResource(Res.string.cancel),
            icon = Icons.Rounded.Close,
            accent = false,
            enabled = true,
            modifier = Modifier.weight(1f),
            onClick = {
                haptics.play(Haptic.Tap)
                onCancel()
            },
        )
        PickAction(
            label = stringResource(Res.string.lyrics_share_preview),
            icon = Icons.Rounded.IosShare,
            accent = true,
            enabled = previewEnabled,
            modifier = Modifier.weight(1f),
            onClick = {
                haptics.play(Haptic.Select)
                onPreview()
            },
        )
    }
}

/**
 * One button of [LyricsPickBar], in the player's own colours.
 *
 * White alphas rather than the theme's, because that is what every other
 * control in this player is drawn with — see [AudioOutputSheet] — and a bar
 * picked out of a different palette reads as a sheet borrowed from another app.
 * Share is the solid one: it is the only thing the whole mode is for, and it
 * should be the thing the eye lands on.
 */
@Composable
private fun RowScope.PickAction(
    label: String,
    icon: ImageVector,
    /** The one that sends it, which is the one that should be reached for. */
    accent: Boolean,
    enabled: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
) {
    val background = if (accent) {
        Color.White.copy(alpha = if (enabled) 1f else 0.3f)
    } else {
        Color.White.copy(alpha = if (enabled) 0.10f else 0.05f)
    }
    // The drawer's own base colour, so the label on the solid button reads as
    // cut out of it rather than as black paint laid over white.
    val foreground = if (accent) {
        Color(0xFF121212).copy(alpha = if (enabled) 1f else 0.4f)
    } else {
        Color.White.copy(alpha = if (enabled) 0.85f else 0.4f)
    }
    Row(
        modifier = modifier
            .clip(RoundedCornerShape(14.dp))
            .background(background)
            .clickable(enabled = enabled, onClick = onClick)
            .padding(horizontal = 18.dp, vertical = 15.dp),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = foreground,
            modifier = Modifier.size(19.dp),
        )
        Spacer(Modifier.width(10.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.W700,
            color = foreground,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}
