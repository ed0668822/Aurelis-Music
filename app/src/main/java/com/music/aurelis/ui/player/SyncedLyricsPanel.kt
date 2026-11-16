// Aurelis © 2026. GPL-3.0-or-later
package com.music.aurelis.ui.player

import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.foundation.Canvas
import androidx.compose.ui.geometry.Offset
import kotlinx.coroutines.delay
import androidx.compose.foundation.background
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.draw.clip
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.tween
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.sp
import androidx.compose.ui.res.stringResource
import com.music.aurelis.R
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.animateScrollBy
import androidx.compose.foundation.gestures.scrollBy
import androidx.compose.foundation.interaction.DragInteraction
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.music.aurelis.data.lyrics.LyricAlignment
import com.music.aurelis.data.lyrics.LyricLine
import com.music.aurelis.playback.PlaybackPosition
import kotlinx.coroutines.flow.collect
import com.music.aurelis.ui.theme.LocalReduceMotion
import kotlin.math.abs

// Focus constants from pinned BitChord PlayerLyrics.kt; distinct phone/centered falloff.
private val LyricFalloff = floatArrayOf(1f, 0.8f, 0.7f, 0.58f, 0.46f)
private val LyricPhoneFalloff = floatArrayOf(1f, 0.74f, 0.6f, 0.48f, 0.4f, 0.34f)
private val LyricBlurFalloff = floatArrayOf(0f, 1f, 1f, 1.7f, 2.4f)
private val LyricSettleEasing = CubicBezierEasing(0.41f, 0f, 0.12f, 0.99f)

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun SyncedLyricsPanel(
    lines: List<LyricLine>,
    trackKey: String,
    position: PlaybackPosition,
    looking: Boolean,
    isPlaying: Boolean,
    onSeekToLine: (Long) -> Unit,
    modifier: Modifier = Modifier,
    centerFocus: Boolean = false,
    isSynced: Boolean = true,
    translatedLines: List<LyricLine>? = null,
    syncEnabled: Boolean = true,
    blurUnfocused: Boolean = false,
    offsetMs: Int = 0,
    onLineLongClick: ((Int) -> Unit)? = null,
    picking: Boolean = false,
    picked: Set<Int> = emptySet(),
    onTogglePick: (Int) -> Unit = {},
    showFollowControls: Boolean = true,
    controlsOpen: Boolean = true,
    onRevealControls: () -> Unit = {},
    onHideControls: () -> Unit = {},
) {
    if (lines.isEmpty()) {
        Box(modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text(stringResource(R.string.lyrics_ui_unavailable), style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        return
    }
    val reduceMotion = LocalReduceMotion.current
    val contextInstrumental = stringResource(R.string.lyrics_ui_instrumental)
    val synchronise = isSynced && syncEnabled
    val playhead = rememberLyricsPlayhead(trackKey, position, isPlaying,
        looking && synchronise && !reduceMotion, offsetMs)
    val listState = remember(trackKey) { LazyListState() }
    var following by remember(trackKey) { mutableStateOf(true) }
    var manuallyPaused by remember(trackKey) { mutableStateOf(false) }
    var dragging by remember(trackKey) { mutableStateOf(false) }
    var viewportHeight by remember(trackKey) { mutableIntStateOf(0) }
    val density = LocalDensity.current
    LaunchedEffect(listState) {
        listState.interactionSource.interactions.collect { interaction ->
            when (interaction) {
                is DragInteraction.Start -> { dragging = true; following = false }
                is DragInteraction.Stop, is DragInteraction.Cancel -> dragging = false
                else -> Unit
            }
        }
    }
    // Focus changes only at known starts/ends, rather than rescanning all lines on every frame.
    val events = remember(lines) {
        buildSet<Long> { lines.forEach { add(it.timeMs); if (it.hasKnownEnd || it.background?.hasKnownEnd == true) add(it.endMs) } }.sorted().toLongArray()
    }
    val event by remember(events, playhead) { derivedStateOf {
        events.binarySearch(playhead.value).let { if (it >= 0) it else -it - 2 }
    } }
    val active = remember(lines, events, event, synchronise) {
        if (synchronise && event >= 0) activeLyricRows(lines, events[event]) else emptyList()
    }
    // The earliest unfinished vocal owns the scroll anchor (BitChord PlayerLyrics).
    val activeIndex = active.firstOrNull() ?: -1
    val duet = remember(lines) { lines.any { it.alignment == LyricAlignment.End } }
    val scrolling by remember(listState) { derivedStateOf { listState.isScrollInProgress } }
    val activeOnScreen by remember(listState, activeIndex) { derivedStateOf {
        listState.layoutInfo.visibleItemsInfo.any { it.index == activeIndex }
    } }
    LaunchedEffect(dragging, scrolling, following, manuallyPaused, isPlaying, picking, activeOnScreen) {
        if (!following && !manuallyPaused && !dragging && !scrolling && isPlaying && !picking) {
            delay(if (activeOnScreen) 600 else 5_000)
            following = true
        }
    }

    LaunchedEffect(trackKey, lines, activeIndex, following, looking, synchronise, dragging, picking, viewportHeight, centerFocus, reduceMotion) {
        if (!synchronise || !looking || !following || dragging || picking || activeIndex !in lines.indices || viewportHeight <= 0) return@LaunchedEffect
        if (!centerFocus) {
            if (reduceMotion) listState.scrollToItem(activeIndex) else listState.animateScrollToItem(activeIndex)
        } else {
            withFrameNanos { } // Measure after the padding/viewport update before reading geometry.
            if (listState.layoutInfo.visibleItemsInfo.none { it.index == activeIndex }) listState.scrollToItem(activeIndex)
            withFrameNanos { }
            val layout = listState.layoutInfo
            val item = layout.visibleItemsInfo.firstOrNull { it.index == activeIndex } ?: return@LaunchedEffect
            val delta = item.offset + item.size / 2f - (layout.viewportStartOffset + layout.viewportEndOffset) / 2f
            if (abs(delta) > 0.5f) {
                if (reduceMotion) listState.scrollBy(delta) else listState.animateScrollBy(delta)
            }
        }
    }
    val controlsOnScroll = rememberPlayerControlsOnScroll(enabled = !picking, onReveal = onRevealControls, onHide = onHideControls)
    Column(modifier.fillMaxSize()) {
        if (synchronise && (showFollowControls || !following)) TextButton(onClick = {
            following = !following; manuallyPaused = !following
        }, colors = ButtonDefaults.textButtonColors(contentColor = Color.White)) {
            Text(stringResource(if (following) R.string.lyrics_ui_pause_follow else R.string.lyrics_ui_follow))
        } else if (!synchronise) Text(stringResource(if (isSynced) R.string.lyrics_ui_sync_disabled else R.string.lyrics_ui_unsynced),
            style = MaterialTheme.typography.labelMedium)
        val pad = if (centerFocus) with(density) { (viewportHeight / 2f).toDp() } else 30.dp
        val bottom = with(density) { (viewportHeight * if (centerFocus) 0.5f else 0.8f).toDp() }
        LazyColumn(state = listState, contentPadding = PaddingValues(top = pad, bottom = bottom),
            modifier = Modifier.weight(1f).fillMaxWidth().nestedScroll(controlsOnScroll)
                .revealLyricsControlsOnTap(!controlsOpen && !picking, onRevealControls).testTag("lyrics-list").onSizeChanged { viewportHeight = it.height }) {
            itemsIndexed(lines) { index, line ->
                val focused = synchronise && index in active
                val style = MaterialTheme.typography.headlineMedium.copy(fontSize = if (isSynced) 34.sp else 30.sp, lineHeight = if (isSynced) 41.sp else 38.sp, fontWeight = FontWeight.ExtraBold,
                    textAlign = if (line.alignment == LyricAlignment.End) TextAlign.End else TextAlign.Start)
                val distance = abs(index - activeIndex)
                val falloff = if (centerFocus) LyricFalloff else LyricPhoneFalloff
                val lineAlpha by animateFloatAsState(when {
                    !synchronise -> 0.95f
                    focused -> 1f
                    !following || picking -> 0.8f
                    else -> falloff[distance.coerceAtMost(falloff.lastIndex)]
                }, tween(if (reduceMotion) 0 else 400, easing = LyricSettleEasing), label = "lyric-alpha")
                val color = Color.White
                val scale by animateFloatAsState(if (focused || !following || picking || !synchronise) 1f else 0.98f,
                    tween(if (reduceMotion) 0 else 400, easing = LyricSettleEasing), label = "lyric-depth")
                val rowBlur by animateFloatAsState(if (blurUnfocused && synchronise && following && activeIndex >= 0 && !focused && !picking) LyricBlurFalloff[distance.coerceAtMost(LyricBlurFalloff.lastIndex)] else 0f,
                    tween(if (reduceMotion) 0 else 400, easing = LyricSettleEasing), label = "lyric-distance-blur")
                val alignEnd = duet && line.alignment == LyricAlignment.End
                Column(Modifier.fillMaxWidth().testTag("lyrics-row-$index")
                    .padding(start = if (alignEnd) 44.dp else 0.dp, end = if (duet && !alignEnd) 44.dp else 0.dp)
                    .graphicsLayer { scaleX = scale; scaleY = scale; alpha = lineAlpha; transformOrigin = TransformOrigin(if (alignEnd) 1f else 0f, 0.5f) }
                    .blur(rowBlur.dp)) {
                    val mainModifier = Modifier.fillMaxWidth().testTag("lyrics-line-$index").semantics { selected = if (picking) index in picked else focused }
                        .clip(RoundedCornerShape(10.dp))
                        .background(when { index in picked -> Color.White.copy(alpha = 0.16f); picking -> Color.White.copy(alpha = 0.05f); else -> Color.Transparent })
                        .combinedClickable(enabled = isSynced || onLineLongClick != null || picking,
                            onLongClick = if (line.isGap) null else onLineLongClick?.let { action -> { action(index) } },
                            onClick = { if (picking) onTogglePick(index) else if (isSynced) { following = true; manuallyPaused = false; onSeekToLine(line.timeMs) } })
                        .padding(vertical = 10.dp, horizontal = 30.dp)
                    if (line.isGap && isSynced) {
                        Box(mainModifier.semantics { contentDescription = contextInstrumental }, contentAlignment = Alignment.CenterStart) {
                            Canvas(Modifier.size(49.dp, 13.dp)) {
                                val until = lines.getOrNull(index + 1)?.timeMs ?: line.endMs
                                val span = (until - line.timeMs).coerceAtLeast(1L)
                                val fraction = ((playhead.value - line.timeMs).toFloat() / span).coerceIn(0f, 1f)
                                repeat(3) { dot ->
                                    val lit = (fraction * 3 - dot).coerceIn(0f, 1f)
                                    drawCircle(Color.White.copy(alpha = 0.35f + 0.65f * lit), 6.5.dp.toPx(), Offset((6.5f + dot * 18f).dp.toPx(), size.height / 2))
                                }
                            }
                        }
                    } else if (focused && line.isWordSynced) {
                        SweptLocalLyric(line, playhead, style, color, mainModifier)
                    } else {
                        Text(if (line.isGap && isSynced) stringResource(R.string.lyrics_ui_instrumental) else line.text, style = style,
                            color = color, modifier = mainModifier)
                    }
                    line.background?.takeIf { it.text.isNotBlank() }?.let { backing ->
                        val backingStyle = MaterialTheme.typography.bodyLarge.copy(textAlign = style.textAlign)
                        val backingModifier = Modifier.fillMaxWidth().testTag("lyrics-background-$index")
                            .padding(horizontal = 30.dp).padding(bottom = 8.dp)
                        if (focused && backing.isWordSynced) SweptLocalLyric(backing, playhead, backingStyle, color, backingModifier)
                        else Text(backing.text, style = backingStyle, color = color, modifier = backingModifier)
                    }
                    translatedLines?.getOrNull(index)?.takeIf { it.text.isNotBlank() && it.text != line.text }?.let { translated ->
                        Text(translated.text, style = MaterialTheme.typography.bodyMedium, textAlign = style.textAlign,
                            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.75f),
                            modifier = Modifier.fillMaxWidth().padding(horizontal = 30.dp).padding(bottom = 8.dp))
                    }
                }
            }
        }
    }
}
