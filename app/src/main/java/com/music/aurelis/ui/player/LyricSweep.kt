// Adapted from BitChord PlayerLyrics.kt (GPLv3); original contributors and changes in NOTICE.
package com.music.aurelis.ui.player

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.ResolvedTextDirection
import com.music.aurelis.data.lyrics.LyricLine
import java.text.BreakIterator
import java.util.Locale

internal fun lyricCharacterBreaks(text: String): IntArray {
    val iterator = BreakIterator.getCharacterInstance(Locale.ROOT).apply { setText(text) }
    val ends = ArrayList<Int>()
    var at = iterator.first()
    while (at != BreakIterator.DONE) { ends += at; at = iterator.next() }
    return ends.toIntArray()
}

internal data class LyricReveal(val completed: Int, val next: Int, val fraction: Float)

internal fun lyricReveal(breaks: IntArray, revealed: Float): LyricReveal {
    if (breaks.isEmpty()) return LyricReveal(0, 0, 0f)
    val progress = if (revealed.isFinite()) revealed.coerceIn(0f, breaks.last().toFloat()) else 0f
    val search = breaks.binarySearch(progress.toInt())
    val index = (if (search >= 0) search else -search - 2).coerceAtLeast(0)
    val start = breaks[index]
    val end = breaks.getOrElse(index + 1) { start }
    return LyricReveal(start, end, if (end > start) ((progress - start) / (end - start)).coerceIn(0f, 1f) else 0f)
}

/** Two identically laid-out copies, clipped in draw; keeps shaping, bidi and native wrapping. */
@Composable
internal fun SweptLocalLyric(
    line: LyricLine,
    playhead: State<Long>,
    style: TextStyle,
    color: Color,
    modifier: Modifier = Modifier,
) {
    var layout by remember(line.text, style) { mutableStateOf<TextLayoutResult?>(null) }
    val breaks = remember(line.text) { lyricCharacterBreaks(line.text) }
    val phase = remember(line, playhead) { derivedStateOf {
        val time = playhead.value
        val end = line.timingSource?.endMs ?: line.words.lastOrNull()?.endMs ?: line.timeMs
        when { time <= line.timeMs -> 0; time >= end -> 2; else -> 1 }
    } }
    Box(modifier) {
        Text(line.text, style = style, color = color.copy(alpha = color.alpha * 0.4f),
            modifier = Modifier.fillMaxWidth(), onTextLayout = { layout = it })
        Text(line.text, style = style, color = color,
            modifier = Modifier.fillMaxWidth().clearAndSetSemantics { }.drawWithContent {
                val measured = layout ?: return@drawWithContent
                if (phase.value == 0) return@drawWithContent
                if (phase.value == 2) { drawContent(); return@drawWithContent }
                val progress = line.revealedChars(playhead.value)
                if (progress <= 0 || !progress.isFinite()) return@drawWithContent
                if (progress >= line.text.length) { drawContent(); return@drawWithContent }
                val reveal = lyricReveal(breaks, progress)
                if (reveal.completed > 0) clipPath(measured.getPathForRange(0, reveal.completed)) { this@drawWithContent.drawContent() }
                if (reveal.next > reveal.completed && reveal.fraction > 0) {
                    val path = measured.getPathForRange(reveal.completed, reveal.next)
                    val bounds = path.getBounds()
                    val rtl = measured.getBidiRunDirection(reveal.completed) == ResolvedTextDirection.Rtl
                    val edge = if (rtl) bounds.right - bounds.width * reveal.fraction else bounds.left + bounds.width * reveal.fraction
                    val left = if (rtl) edge else bounds.left
                    val right = if (rtl) bounds.right else edge
                    if (left < right && bounds.top < bounds.bottom) clipRect(left, bounds.top, right, bounds.bottom) {
                        clipPath(path) { this@drawWithContent.drawContent() }
                    }
                }
            })
    }
}
