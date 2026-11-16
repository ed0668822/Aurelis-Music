// Aurelis © 2026. GPL-3.0-or-later
package com.music.aurelis.ui.player

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.Shader
import android.text.Layout
import android.text.StaticLayout
import android.text.TextPaint
import android.text.TextUtils

/** Wrapped original text and optional translation; no network or metadata changes. */
internal fun renderLyricsShare(request: LyricsShareRequest, story: Boolean): Bitmap {
    val height = if (story) 1920 else 1350
    val image = Bitmap.createBitmap(1080, height, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(image)
    val background = Paint().apply { shader = LinearGradient(0f, 0f, 1080f, height.toFloat(),
        Color.rgb(55, 46, 75), Color.rgb(12, 14, 20), Shader.TileMode.CLAMP) }
    canvas.drawRect(0f, 0f, 1080f, height.toFloat(), background)
    val ink = TextPaint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.WHITE; textSize = 48f; isFakeBoldText = true }
    canvas.drawText(TextUtils.ellipsize(request.song.title, ink, 888f, TextUtils.TruncateAt.END).toString(), 96f, 140f, ink)
    ink.textSize = 34f; ink.isFakeBoldText = false
    canvas.drawText(TextUtils.ellipsize(request.song.artist, ink, 888f, TextUtils.TruncateAt.END).toString(), 96f, 194f, ink)
    ink.textSize = 58f; ink.isFakeBoldText = true
    val content = request.lines.joinToString("\n\n") { line -> listOfNotNull(line.text, line.subText).joinToString("\n") }
    val layout = StaticLayout.Builder.obtain(content, 0, content.length, ink, 888)
        .setAlignment(Layout.Alignment.ALIGN_NORMAL).setIncludePad(false).setLineSpacing(12f, 1f).build()
    val available = height - 450f
    val scale = minOf(1f, available / layout.height.coerceAtLeast(1))
    canvas.save(); canvas.translate(96f, 300f); canvas.scale(scale, scale); layout.draw(canvas); canvas.restore()
    ink.textSize = 30f; ink.isFakeBoldText = false
    canvas.drawText("Aurelis Music", 96f, height - 100f, ink)
    return image
}
