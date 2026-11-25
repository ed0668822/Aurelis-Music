// SPDX-FileCopyrightText: © 2026 Aurelis

package com.music.aurelis.ui.player

import android.content.ClipData
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.net.Uri
import androidx.core.content.FileProvider
import com.music.aurelis.ui.replay.Fonts
import com.music.aurelis.ui.replay.drawArtwork
import com.music.aurelis.ui.replay.drawBackdrop
import com.music.aurelis.ui.replay.drawLogo
import com.music.aurelis.ui.replay.ellipsised
import com.music.aurelis.ui.replay.loadBitmap
import java.io.File
import java.util.concurrent.TimeUnit
import kotlin.math.roundToInt
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

internal enum class LyricsImageFormat(val height: Int?) {
    CARD(null), SQUARE(1080), STORY(1920),
}

internal data class RenderedLyricsCard(val bitmap: Bitmap, val lineBounds: Map<Int, RectF>)

/** Returns null when the selection would need text smaller than the legibility floor. */
internal suspend fun renderLyricsShareCard(
    context: Context,
    card: LyricsShareRequest,
    format: LyricsImageFormat,
    includeSubText: Boolean,
): RenderedLyricsCard? = withContext(Dispatchers.Default) {
    val type = Fonts(context)
    val headTop = HEADER_BOTTOM + GAP_ABOVE_BODY
    val reserved = headTop + GAP_BELOW_BODY + FOOTER_THUMB + FOOTER_PAD
    val frameRoom = (format.height ?: MAX_CARD_H) - reserved
    val plan = fittestPlan(type, card.lines, frameRoom, includeSubText) ?: return@withContext null
    val cardH = format.height?.toFloat() ?: reserved + plan.content
    val bodyTop = headTop + (cardH - reserved - plan.content) / 2f

    val cover = card.artworkUrl?.let { loadBitmap(context, it) }
    val bitmap = Bitmap.createBitmap(CARD_W, cardH.roundToInt(), Bitmap.Config.ARGB_8888)
    val canvas = Canvas(bitmap)
    drawBackdrop(canvas, cover, 0f, cardH)

    // ── Header ──────────────────────────────────────────────────────────────
    // The mark alone, at the top right — the same one the Replay carries, so
    // two of these cards still read as one family. Nothing else goes above
    // the passage: the picture is the verse, not a caption for it.
    val brand = type.heading(46f, 0xE6FFFFFF.toInt()).apply {
        textAlign = Paint.Align.RIGHT
    }
    canvas.drawText(BRAND, CARD_W - CARD_MARGIN, HEADER_BASELINE, brand)
    drawLogo(
        canvas,
        context,
        CARD_W - CARD_MARGIN - brand.measureText(BRAND) - LOGO_GAP,
        HEADER_BASELINE,
    )

    val lineBounds = mutableMapOf<Int, RectF>()
    var y = bodyTop
    for (row in plan.rows) {
        val rowTop = y + row.gapBefore
        val baseline = rowTop - row.paint.ascent() +
            if (row.isGap) row.paint.textSize * GAP_MARKER_OFFSET else 0f
        canvas.drawText(row.text, CARD_MARGIN, baseline, row.paint)
        row.lineIndex?.let { index ->
            val bounds = RectF(
                CARD_MARGIN - 14f,
                rowTop,
                CARD_MARGIN + row.paint.measureText(row.text) + 14f,
                rowTop + row.height,
            )
            lineBounds.getOrPut(index) { bounds }.union(bounds)
        }
        y = rowTop + row.height
    }

    // ── Footer ──────────────────────────────────────────────────────────────
    // Which song this is — which only the backdrop had been saying until now.
    val thumbY = cardH - FOOTER_PAD - FOOTER_THUMB
    drawArtwork(canvas, cover, card.song.title, CARD_MARGIN, thumbY, FOOTER_THUMB, false)
    val textX = CARD_MARGIN + FOOTER_THUMB + 34f
    val room = CARD_W - CARD_MARGIN - textX
    val title = type.body(50f, Color.WHITE, bold = true)
    canvas.drawText(ellipsised(card.song.title, title, room), textX, thumbY + 74f, title)
    val artist = type.body(42f, 0x99FFFFFF.toInt())
    canvas.drawText(ellipsised(card.song.artist, artist, room), textX, thumbY + 132f, artist)

    RenderedLyricsCard(bitmap, lineBounds)
}

// ── Fitting ────────────────────────────────────────────────────────────────

private class Row(
    val text: String,
    val paint: Paint,
    val height: Float,
    val gapBefore: Float,
    val lineIndex: Int? = null,
    val isGap: Boolean = false,
)

/** The rows at one size, with everything needed to place them. */
private class Plan(val rows: List<Row>, val ascent: Float) {
    /** Distance from the top of the first line to the bottom of the last. */
    val content: Float
        get() = ascent + rows.fold(0f) { total, row -> total + row.gapBefore + row.height }
}

private fun fittestPlan(
    type: Fonts,
    lines: List<LyricsShareLine>,
    room: Float,
    includeSubText: Boolean,
): Plan? {
    for (size in FIT_LADDER) {
        val plan = planAt(type, lines, size, includeSubText)
        if (plan.content <= room) return plan
    }
    return null
}

private fun planAt(type: Fonts, lines: List<LyricsShareLine>, size: Float, includeSubText: Boolean): Plan {
    val primary = type.heading(size, Color.WHITE).apply { letterSpacing = TRACKING }
    val subSize = (size * SUB_RATIO).coerceAtLeast(SUB_FLOOR)
    val secondary = type.heading(subSize, SUB_COLOR).apply { letterSpacing = SUB_TRACKING }

    val rows = mutableListOf<Row>()
    lines.forEachIndexed { index, line ->
        if (line.isGap) {
            // The skipped lines, as "(...)" — the conventional editorial mark
            // for an omission. Hung off the left edge with the words, smaller
            // and a dim grey. Keep its row compact so the marker follows the
            // preceding lyric closely; the regular gap on the next lyric line
            // provides the separation below it.
            val marker = type.body(size * 0.65f, 0x66FFFFFF.toInt())
            val markerHeight = marker.descent() - marker.ascent()
            rows += Row("(...)", marker, markerHeight, 0f, isGap = true)
            return@forEachIndexed
        }
        var firstOfLine = true
        fun add(text: String, paint: Paint, height: Float) {
            // One verse line is one paragraph: its first row carries the space
            // above it and its wraps sit tight underneath, so a phrase reads as
            // a block rather than as a list of separate sentences.
            val gap = when {
                rows.isEmpty() -> 0f
                firstOfLine && index > 0 -> size * 0.55f
                else -> 0f
            }
            rows += Row(text, paint, height, gap, lineIndex = index)
            firstOfLine = false
        }
        wrap(line.text, primary, CARD_CONTENT_W).forEach { add(it, primary, size * LEAD) }
        val sub = line.subText
        if (includeSubText && !sub.isNullOrBlank() && sub != line.text) {
            wrap(sub, secondary, CARD_CONTENT_W).forEach { add(it, secondary, subSize * SUB_LEAD) }
        }
    }
    return Plan(rows, -primary.ascent())
}

// ── Text ───────────────────────────────────────────────────────────────────

/**
 * Greedy wrap at [width], breaking wherever the line has to.
 *
 * Not a plain word-wrap: Japanese, Chinese and Korean lyrics carry no spaces at
 * all, and a wrapper that only split on spaces would hand the whole verse to
 * `drawText` and let it run off the card. `breakText` finds the character that
 * fits, and only then is the break walked back to a space — so scripts that keep
 * their words apart lose nothing, and scripts that don't still get several lines.
 */
private fun wrap(text: String, paint: Paint, width: Float): List<String> {
    val source = text.trim()
    if (source.isEmpty()) return listOf("")

    val out = mutableListOf<String>()
    var rest = source
    while (rest.isNotEmpty()) {
        val fit = paint.breakText(rest, true, width, null)
        if (fit <= 0) {
            out += rest
            break
        }
        // Walk back to the last space inside the fit, but only when something
        // still has to follow it — otherwise the fit is the whole string and
        // cutting at the space would drop the final word.
        val space = rest.lastIndexOf(' ', fit - 1)
        val take = if (space > 0 && fit < rest.length) space else fit
        if (take <= 0) {
            out += rest
            break
        }
        out += rest.substring(0, take).trimEnd()
        rest = rest.substring(take).trimStart()
    }
    return out.filter { it.isNotEmpty() }.ifEmpty { listOf("") }
}

// ── Card geometry ──────────────────────────────────────────────────────────

private const val CARD_W = 1080

private const val MAX_CARD_H = 1920

private const val CARD_MARGIN = 72f
private const val CARD_CONTENT_W = CARD_W - CARD_MARGIN * 2

private const val LOGO_GAP = 20f
private const val BRAND = "Aurelis Music"

/** Baseline of the mark at the top right, and just under it where the gap starts. */
private const val HEADER_BASELINE = 132f
private const val HEADER_BOTTOM = 170f

/** Air kept between the fixed furniture and the passage, both ends alike. */
private const val GAP_ABOVE_BODY = 20f
private const val GAP_BELOW_BODY = 20f

/** The sleeve square in the footer, and the air kept under it. */
private const val FOOTER_THUMB = 160f
private const val FOOTER_PAD = 96f

/** Smallest the translation line is allowed to get before it stops being legible. */
private const val SUB_FLOOR = 24f

/** Small visual correction so the omission marker sits below the line above. */
private const val GAP_MARKER_OFFSET = 0.18f

/**
 * The sizes tried, largest first.
 *
 * A ladder rather than a search: a dozen candidates is nothing to lay out, and
 * stepping by a visible amount keeps two cards with adjacent line counts from
 * coming out nearly the same size and reading as a mistake.
 *
 * The last rung is the smallest legible size supported by the card.
 */
private val FIT_LADDER = floatArrayOf(96f, 88f, 80f, 72f, 64f, 56f, 50f, 44f, 38f, 32f)

// ── The panel's type, as ratios ────────────────────────────────────────────
// Every number here is one the lyrics panel uses, divided by the size it uses
// it at so it holds at whatever size the ladder lands on.

/** Tracking of the panel's headline: -0.7sp against its 34sp. */
private const val TRACKING = -0.7f / 34f

/** The same -0.7sp, against the 20sp it sets the line underneath in. */
private const val SUB_TRACKING = -0.7f / 20f

/** Line advance: 41sp over 34sp for the words, 25sp over 20sp for the sub. */
private const val LEAD = 41f / 34f
private const val SUB_LEAD = 25f / 20f

/** The sub-line against the words above it, and the white it is drawn in. */
private const val SUB_RATIO = 20f / 34f
private val SUB_COLOR = 0xD9FFFFFF.toInt()

private const val LYRICS_CACHE_PREFIX = "aurelis-lyrics-"
private val LEGACY_LYRICS_FILE = Regex("lyrics-(background-)?-?[0-9]+[.](mp4|png)")

private fun lyricsShareCache(context: Context): File {
    val folder = File(context.cacheDir, "shared").apply { mkdirs() }
    val expiredBefore = System.currentTimeMillis() - TimeUnit.HOURS.toMillis(24)
    folder.listFiles()?.forEach { file ->
        val name = file.name
        val isLyricsFile = name.startsWith(LYRICS_CACHE_PREFIX) || name == "lyrics.png" ||
            LEGACY_LYRICS_FILE.matches(name)
        if (isLyricsFile && file.isFile && file.lastModified() < expiredBefore) file.delete()
    }
    return folder
}

internal suspend fun cacheLyricsImage(context: Context, bitmap: Bitmap): Uri? =
    withContext(Dispatchers.IO) {
        val file = File.createTempFile(LYRICS_CACHE_PREFIX, ".png", lyricsShareCache(context))
        var success = false
        try {
            if (!file.outputStream().use { bitmap.compress(Bitmap.CompressFormat.PNG, 100, it) }) {
                return@withContext null
            }
            FileProvider.getUriForFile(context, context.packageName + ".fileprovider", file)
                .also { success = true }
        } finally {
            if (!success) file.delete()
        }
    }

internal fun shareLyricsImageIntent(uri: Uri): Intent =
    Intent(Intent.ACTION_SEND).apply {
        type = "image/png"
        putExtra(Intent.EXTRA_STREAM, uri)
        clipData = ClipData.newRawUri(null, uri)
        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
    }
