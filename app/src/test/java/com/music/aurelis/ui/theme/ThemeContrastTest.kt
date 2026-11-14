package com.music.aurelis.ui.theme

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import org.junit.Assert.*
import org.junit.Test

class ThemeContrastTest {
    @Test fun textRolesAreOpaqueAndReadableOnTheirSurfaces() {
        for (scheme in listOf(LightColorScheme, DarkColorScheme)) {
            val pairs = listOf(
                scheme.onBackground to scheme.background,
                scheme.onSurface to scheme.surface,
                scheme.onSurfaceVariant to scheme.surfaceVariant,
                scheme.onSurfaceVariant to scheme.surfaceContainerHighest,
                scheme.onPrimary to scheme.primary,
                scheme.onPrimaryContainer to scheme.primaryContainer,
                scheme.onSecondary to scheme.secondary,
                scheme.onSecondaryContainer to scheme.secondaryContainer,
                scheme.onTertiary to scheme.tertiary,
                scheme.onTertiaryContainer to scheme.tertiaryContainer,
                scheme.onError to scheme.error,
                scheme.onErrorContainer to scheme.errorContainer,
            )
            for ((text, background) in pairs) {
                assertEquals(1f, text.alpha, 0f)
                assertEquals(1f, background.alpha, 0f)
                assertTrue("Text contrast below 4.5:1: $text on $background",
                    contrast(text, background) >= 4.5f)
            }
        }
    }

    @Test fun explicitModesOverrideSystemAndUnknownModeFollowsSystem() {
        for (systemDark in listOf(false, true)) {
            assertFalse(isAurelisDarkTheme(1, systemDark))
            assertTrue(isAurelisDarkTheme(2, systemDark))
            assertEquals(systemDark, isAurelisDarkTheme(0, systemDark))
            assertEquals(systemDark, isAurelisDarkTheme(99, systemDark))
        }
    }

    private fun contrast(a: Color, b: Color): Float {
        val bright = maxOf(a.luminance(), b.luminance())
        val dark = minOf(a.luminance(), b.luminance())
        return (bright + 0.05f) / (dark + 0.05f)
    }
}
