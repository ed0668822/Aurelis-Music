// Aurelis © 2026. GPL-3.0-or-later
// Partial adaptation of BitChord acf219a7 PlayerChrome (GPLv3); see NOTICE.
package com.music.aurelis.ui.player

import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Paint

/** Only enable over the player backdrop on a hardware-rendered view. */
internal fun Modifier.playerAdditiveChrome(enabled: Boolean): Modifier =
    if (!enabled) this else drawWithCache {
        // Canvas layers work for both hardware rendering and bitmap captures.
        // A View can be accelerated while its current capture Canvas is software.
        val paint = Paint().apply { blendMode = BlendMode.Plus }
        onDrawWithContent {
            val canvas = drawContext.canvas
            canvas.saveLayer(Rect(Offset.Zero, size), paint)
            try {
                drawContent()
            } finally {
                canvas.restore()
            }
        }
    }

internal fun playerControlTint(base: Color, enabled: Boolean, blended: Boolean): Color =
    if (blended) Color.White.copy(alpha = (if (enabled) 0.73f else 0.38f) - 0.15f)
    else base.copy(alpha = base.alpha * if (enabled) 1f else 0.38f)
