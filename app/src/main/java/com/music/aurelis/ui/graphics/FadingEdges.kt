// SPDX-FileCopyrightText: 2026 Aurelis
// SPDX-License-Identifier: LGPL-3.0-or-later
package com.music.aurelis.ui.graphics

import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.LayoutDirection

fun Modifier.fadingEdges(width: Dp, start: () -> Float = NoFade, end: () -> Float = NoFade): Modifier =
    graphicsLayer { compositingStrategy = CompositingStrategy.Offscreen }
        .drawWithCache {
            val fade = width.toPx().coerceAtMost(size.width / 2f)
            val stops = Array(FADE_CURVE_STOPS + 1) { i ->
                val t = i / FADE_CURVE_STOPS.toFloat()
                t to Color.Black.copy(alpha = 1f - smootherStep(t))
            }
            val leftBrush = Brush.horizontalGradient(*stops, startX = 0f, endX = fade)
            val rightBrush = Brush.horizontalGradient(*stops, startX = size.width, endX = size.width - fade)
            val rtl = layoutDirection == LayoutDirection.Rtl
            onDrawWithContent {
                drawContent()
                val left = (if (rtl) end() else start()).coerceIn(0f, 1f)
                if (left > 0f) {
                    drawRect(leftBrush, Offset.Zero, Size(fade, size.height), alpha = left, blendMode = BlendMode.DstOut)
                }
                val right = (if (rtl) start() else end()).coerceIn(0f, 1f)
                if (right > 0f) {
                    drawRect(
                        rightBrush,
                        Offset(size.width - fade, 0f),
                        Size(fade, size.height),
                        alpha = right,
                        blendMode = BlendMode.DstOut,
                    )
                }
            }
        }

/**
 * F061: vertically fade a scrollable panel's content, not the page backdrop.
 * Both fades are controlled by whether the list can scroll in that direction,
 * so a one-row queue has no artificial transparent strips.
 */
fun Modifier.verticalFadingEdges(
    height: Dp,
    top: () -> Float = NoFade,
    bottom: () -> Float = NoFade,
): Modifier = graphicsLayer { compositingStrategy = CompositingStrategy.Offscreen }
    .drawWithCache {
        val fade = height.toPx().coerceAtMost(size.height / 2f)
        if (fade <= 0f) {
            onDrawWithContent { drawContent() }
        } else {
            val stops = Array(FADE_CURVE_STOPS + 1) { i ->
                val t = i / FADE_CURVE_STOPS.toFloat()
                t to Color.Black.copy(alpha = 1f - smootherStep(t))
            }
            val topBrush = Brush.verticalGradient(*stops, startY = 0f, endY = fade)
            val bottomBrush = Brush.verticalGradient(*stops,
                startY = size.height, endY = size.height - fade)
            onDrawWithContent {
                drawContent()
                val head = top().coerceIn(0f, 1f)
                val foot = bottom().coerceIn(0f, 1f)
                if (head > 0f) drawRect(topBrush, Offset.Zero,
                    Size(size.width, fade), alpha = head, blendMode = BlendMode.DstOut)
                if (foot > 0f) drawRect(bottomBrush,
                    Offset(0f, size.height - fade), Size(size.width, fade),
                    alpha = foot, blendMode = BlendMode.DstOut)
            }
        }
    }

private val NoFade = { 0f }
