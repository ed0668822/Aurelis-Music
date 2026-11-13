// Aurelis © 2026. GPL-3.0-or-later
package com.music.aurelis.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.music.aurelis.data.SettingsRepository
import com.music.aurelis.ui.components.LocalPlayingBarsMotion

internal val AurelisShapes = Shapes(
    extraSmall = RoundedCornerShape(5.dp),
    small = RoundedCornerShape(11.dp),
    medium = RoundedCornerShape(14.dp),
    large = RoundedCornerShape(20.dp),
    extraLarge = RoundedCornerShape(28.dp),
)

val LocalReduceMotion = staticCompositionLocalOf { false }

internal fun isAurelisDarkTheme(mode: Int, systemDark: Boolean): Boolean = when (mode) {
    1 -> false
    2 -> true
    else -> systemDark
}

@Composable
fun AurelisTheme(
    darkTheme: Boolean? = null,
    content: @Composable () -> Unit
) {
    val hardware = com.music.aurelis.ui.components.rememberGlassHardware()
    val appFont by SettingsRepository.appFont.collectAsStateWithLifecycle()
    val typography = androidx.compose.runtime.remember(appFont) { typographyFor(appFont) }
    val mode by SettingsRepository.themeMode.collectAsStateWithLifecycle()
    val reduceMotion by SettingsRepository.reduceAnimation.collectAsStateWithLifecycle()
    val glass by SettingsRepository.liquidGlass.collectAsStateWithLifecycle()
    val reduceBlur by SettingsRepository.reduceDynamicBlur.collectAsStateWithLifecycle()
    val resolvedDark = darkTheme ?: isAurelisDarkTheme(mode, isSystemInDarkTheme())
    val heldMenus = com.music.aurelis.ui.components.rememberHeldMenuState()
    SystemBarIcons(dark = !resolvedDark)
    CompositionLocalProvider(
        LocalReduceMotion provides reduceMotion,
        com.music.aurelis.ui.components.LocalGlassHardware provides hardware,
        LocalLiquidGlassEnabled provides (glass && !reduceBlur),
        LocalPlayingBarsMotion provides !reduceMotion,
        com.music.aurelis.ui.components.LocalHeldMenuState provides heldMenus,
    ) {
        MaterialTheme(
            colorScheme = if (resolvedDark) DarkColorScheme else LightColorScheme,
            typography = typography,
            shapes = AurelisShapes,
            content = content,
        )
    }
}
