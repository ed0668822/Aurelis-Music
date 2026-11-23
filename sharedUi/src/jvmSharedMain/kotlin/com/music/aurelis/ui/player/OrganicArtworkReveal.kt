// SPDX-FileCopyrightText: 2026 Aurelis
// SPDX-License-Identifier: LGPL-3.0-or-later
package com.music.aurelis.ui.player

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import kotlin.math.PI
import kotlin.math.sin

/**
 * A deterministic organic coverage mask for the incoming AutoMix cover.
 *
 * Unlike a timed UI animation, the mask has no clock of its own: the caller
 * supplies progress sampled from the real audio transition. This is a cheap
 * Compose fallback for devices without a working Android RuntimeShader.
 * Generation changes the pattern, never its pacing.
 */
internal fun DrawScope.drawOrganicArtworkMask(progress: Float, generation: Long) {
    val p = if (progress.isFinite()) progress.coerceIn(0f, 1f) else 0f
    if (p >= 1f) return // Keep the incoming picture fully opaque.
    if (p <= 0f) {
        drawRect(Color.Transparent, blendMode = BlendMode.DstIn)
        return
    }

    val width = size.width
    val height = size.height
    if (width <= 0f || height <= 0f) return

    // Start and end outside the cover: no abrupt bands at the edges.
    val leading = (-0.22f + 1.44f * p) * width
    val amplitude = width * 0.075f * (4f * p * (1f - p))
    val seed = ((generation ushr 7) % 997L).toDouble() / 997.0 * 2.0 * PI
    val path = Path().apply {
        moveTo(0f, 0f)
        lineTo(0f, height)
        val steps = 40
        for (step in steps downTo 0) {
            val y = height * step / steps
            val normalizedY = step.toDouble() / steps
            val wave = sin(normalizedY * 2.0 * PI * 2.4 + seed) * 0.68 +
                sin(normalizedY * 2.0 * PI * 5.3 + seed * 0.61) * 0.32
            val x = (leading + amplitude * wave.toFloat()).coerceIn(0f, width)
            lineTo(x, y)
        }
        close()
    }
    drawPath(path, Color.Black, blendMode = BlendMode.DstIn)
}
