// SPDX-FileCopyrightText: 2026 Aurelis
// SPDX-License-Identifier: LGPL-3.0-or-later
package com.music.aurelis.ui.theme

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ArtworkPaletteF050Test {
    private fun samplePalette(accent: Color) = ArtworkPalette(
        background = Color(0xFF17212B),
        wash = Color(0xFF1A2635),
        elevated = Color(0xFF273848),
        accent = accent,
        onBackground = Color.White,
        onBackgroundVariant = Color(0xFFCBD9E9),
        divider = Color.White.copy(alpha = 0.12f),
    )

    @Test fun extractedThemeKeepsCoverDerivedSurfaces() {
        val palette = samplePalette(Color(0xFFE2C8A2))
        val result = palette.toArtworkColors()
        assertEquals(palette.background, result.background)
        assertEquals(palette.wash, result.surface)
        assertEquals(palette.elevated, result.surfaceVariant)
        assertEquals(palette.accent, result.primary)
        assertEquals(palette.onBackground, result.onBackground)
        assertEquals(palette.onBackgroundVariant, result.onSurfaceVariant)
    }

    @Test fun accentTextRemainsReadableOnVeryLightAndVeryDarkCovers() {
        for (accent in listOf(Color(0xFFEEE4D0), Color(0xFF1D1E40), Color(0xFFFA2D48))) {
            val result = samplePalette(accent).toArtworkColors()
            assertTrue("Unreadable accent: $accent", contrast(result.primary, result.onPrimary) >= 4.5f)
            assertTrue("Unreadable secondary role", contrast(result.secondary, result.onSecondary) >= 4.5f)
        }
    }

    private fun contrast(a: Color, b: Color): Float {
        val brighter = maxOf(a.luminance(), b.luminance())
        val darker = minOf(a.luminance(), b.luminance())
        return (brighter + 0.05f) / (darker + 0.05f)
    }
}