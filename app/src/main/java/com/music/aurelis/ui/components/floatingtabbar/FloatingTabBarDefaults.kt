// Aurelis © 2026. GPL-3.0-or-later
package com.music.aurelis.ui.components.floatingtabbar

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

object FloatingTabBarDefaults {

    fun colors(
        backgroundColor: Color = MaterialTheme.colorScheme.surface,
        accessoryBackgroundColor: Color = MaterialTheme.colorScheme.surface,
        indicatorColor: Color = MaterialTheme.colorScheme.primary,
    ) = Colors(
        backgroundColor = backgroundColor,
        accessoryBackgroundColor = accessoryBackgroundColor,
        indicatorColor = indicatorColor,
    )

    fun elevations(
        inlineElevation: Dp = 0.dp,
        expandedElevation: Dp = 0.dp,
    ) = Elevations(
        inlineElevation = inlineElevation,
        expandedElevation = expandedElevation,
    )

    fun sizes(
        tabBarContentPadding: PaddingValues = PaddingValues(0.dp),
        tabExpandedContentPadding: PaddingValues = PaddingValues(0.dp),
        inlineComponentSpacing: Dp = 0.dp,
        inlineBarPadding: PaddingValues = PaddingValues(0.dp),
    ) = Sizes(
        tabBarContentPadding = tabBarContentPadding,
        tabExpandedContentPadding = tabExpandedContentPadding,
        inlineComponentSpacing = inlineComponentSpacing,
        inlineBarPadding = inlineBarPadding,
    )

    data class Colors(
        val backgroundColor: Color,
        val accessoryBackgroundColor: Color,
        val indicatorColor: Color,
    )

    data class Elevations(
        val inlineElevation: Dp,
        val expandedElevation: Dp,
    )

    data class Sizes(
        val tabBarContentPadding: PaddingValues,
        val tabExpandedContentPadding: PaddingValues,
        val inlineComponentSpacing: Dp,
        val inlineBarPadding: PaddingValues,
    )
}
