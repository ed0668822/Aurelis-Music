// Adapted from BitChord b690dc146009d4815825fa4a20815ef2e4ee8898 BottomGlyph.
// Original BitChord contributors, GPLv3; see NOTICE and LICENSE_AUDIT.
package com.music.aurelis.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.music.aurelis.ui.haptics.Haptic
import com.music.aurelis.ui.haptics.rememberHaptics

/** Same 44/26dp glyph and 35/7dp cut-out tile, wired to Aurelis' local actions. */
@Composable
internal fun PlayerBottomGlyph(icon: ImageVector, label: String, onClick: () -> Unit, highlighted: Boolean = false) {
    val haptics = rememberHaptics()
    val tint = MaterialTheme.colorScheme.onSurface
    Box(Modifier.size(44.dp).then(if (highlighted) Modifier
        .graphicsLayer { compositingStrategy = CompositingStrategy.Offscreen }
        .drawWithContent {
            drawContent()
            val side = 35.dp.toPx()
            val corner = 7.dp.toPx()
            drawRoundRect(tint.copy(alpha = 0.62f), Offset((size.width - side) / 2, (size.height - side) / 2),
                Size(side, side), CornerRadius(corner, corner), blendMode = BlendMode.SrcOut)
        } else Modifier)
        .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) {
            haptics.play(Haptic.Tap); onClick()
        }.semantics { contentDescription = label; selected = highlighted }, contentAlignment = Alignment.Center) {
        Icon(icon, null, tint = tint.copy(alpha = if (highlighted) 1f else 0.75f), modifier = Modifier.size(26.dp))
    }
}
