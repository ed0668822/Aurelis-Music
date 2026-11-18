// Adapted from BitChord b690dc146009d4815825fa4a20815ef2e4ee8898 CircleGlyph.
// Original BitChord contributors, GPLv3; see NOTICE. Aurelis honors reduced motion.
package com.music.aurelis.ui.components

import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.snap
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.selection.toggleable
import androidx.compose.ui.semantics.Role
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import com.music.aurelis.ui.haptics.Haptic
import com.music.aurelis.ui.haptics.rememberHaptics
import androidx.compose.material3.MaterialTheme
import com.music.aurelis.ui.theme.LocalReduceMotion

@Composable
internal fun PlayerCircleGlyph(icon: ImageVector, label: String, onClick: () -> Unit,
    active: Boolean = false, haptic: Haptic = Haptic.Tap,
    enabled: Boolean = true, checkable: Boolean = false) {
    val haptics = rememberHaptics()
    val tint = if (active) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
    val reduced = LocalReduceMotion.current
    val alpha by animateFloatAsState(if (active) 0.34f else 0.18f,
        animationSpec = if (reduced) snap() else spring(), label = "glyphDisc")
    val interaction = remember { MutableInteractionSource() }
    val touch = if (checkable) Modifier.toggleable(
        value = active, enabled = enabled, role = Role.Checkbox,
        interactionSource = interaction, indication = null,
        onValueChange = { haptics.play(haptic); onClick() },
    ) else Modifier.clickable(
        enabled = enabled, interactionSource = interaction, indication = null,
        onClick = { haptics.play(haptic); onClick() },
    )
    Box(Modifier.size(34.dp).clip(CircleShape)
        .background(tint.copy(alpha = if (enabled) alpha else alpha * 0.35f))
        .then(touch), contentAlignment = Alignment.Center) {
        Crossfade(icon, animationSpec = tween(if (reduced) 0 else 180), label = "playerMenuGlyph") { glyph ->
            Icon(glyph, label, tint = tint.copy(alpha = if (enabled) 1f else 0.38f),
                modifier = Modifier.size(19.dp))
        }
    }
}
