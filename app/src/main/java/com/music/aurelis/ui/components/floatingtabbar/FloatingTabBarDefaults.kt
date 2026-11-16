// Aurelis © 2026. GPL-3.0-or-later
// Aurelis integration changes; vendored portions retain the Apache-2.0 notice below.
/*
 * FloatingTabBar v1.0.1 by Elyes Mansour
 * https://github.com/elyesmansour/compose-floating-tab-bar
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Vendored from the v1.0.1 tag: the published AAR is compiled against
 * Compose 1.8 and crashes with NoSuchMethodError on
 * SharedTransitionScope.sharedElement under the Compose version this app uses.
 * Compiling the source here keeps it binary-compatible.
 *
 * Local modifications on top of v1.0.1:
 * - Package renamed.
 * - InlineBar/ExpandedBar use Row/Column instead of ConstraintLayout, and the
 *   tab pill + standalone tab cluster is centered (iOS 26 style) instead of
 *   being pinned to opposite screen edges.
 * - The inline/expanded AnimatedContent is center-aligned so the collapse
 *   animation morphs around the center instead of the start edge.
 *
 * Taken from EchoMusicApp/Echo-Music, which carries the three modifications
 * listed above; BitChord's own use of it is in
 * [com.music.bitchord.ui.components.GlassNavBar]. One further change here:
 *
 * - `tabBarContentModifier` is a `@Composable () -> Modifier` factory rather
 *   than a single Modifier value. It is applied to three surfaces of three
 *   different sizes (the tab pill, the circular standalone tab, the accessory),
 *   and a liquid glass Modifier carries a shape-outline cache keyed on the size
 *   it last saw — so one shared instance is invalidated by whichever surface
 *   drew last and re-creates all three outlines every frame. A factory gives
 *   each surface its own.
 * - The expanded tab group's selection pill is a lifted glass lens in motion
 *   (see [GlassSelectionPill]): a tap inflates it past the bar, carries it to
 *   the tab on a spring, squashes it with its acceleration, and sheds the glass
 *   back into a flat fill on landing. Horizontal drags pick it up the same way,
 *   resist at the ends, tick across tab boundaries, and settle on release.
 */

package com.music.aurelis.ui.components.floatingtabbar

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Represents the colors used in [FloatingTabBar].
 */
@Immutable
data class FloatingTabBarColors(
    val backgroundColor: Color,
    val accessoryBackgroundColor: Color,
    val indicatorColor: Color,
)

/**
 * Represents the shapes used in [FloatingTabBar].
 */
@Immutable
data class FloatingTabBarShapes(
    val tabBarShape: Shape,
    val tabShape: Shape,
    val standaloneTabShape: Shape,
    val accessoryShape: Shape,
)

/**
 * Represents the elevations used in [FloatingTabBar].
 */
@Immutable
data class FloatingTabBarElevations(
    val inlineElevation: Dp,
    val expandedElevation: Dp,
)

/**
 * Represents the sizes and spacing used in [FloatingTabBar].
 */
@Immutable
data class FloatingTabBarSizes(
    val tabBarContentPadding: PaddingValues,
    val tabInlineContentPadding: PaddingValues,
    val tabExpandedContentPadding: PaddingValues,
    val componentSpacing: Dp,
    val tabSpacing: Dp,
    val inlineComponentSpacing: Dp = componentSpacing,
    val inlineBarPadding: PaddingValues = PaddingValues(),
)

/**
 * Contains the default values used by [FloatingTabBar].
 */
object FloatingTabBarDefaults {
    /**
     * Creates a [FloatingTabBarColors] that represents the default colors used in a [FloatingTabBar].
     *
     * @param backgroundColor the color used for the tab bar background
     * @param accessoryBackgroundColor the color used for the accessory background
     * @param indicatorColor the color used behind the selected tab in expanded state
     */
    @Composable
    fun colors(
        backgroundColor: Color = MaterialTheme.colorScheme.surfaceContainerHigh,
        accessoryBackgroundColor: Color = MaterialTheme.colorScheme.surfaceContainerHigh,
        indicatorColor: Color = MaterialTheme.colorScheme.primary.copy(alpha = 0.14f),
    ): FloatingTabBarColors = FloatingTabBarColors(
        backgroundColor = backgroundColor,
        accessoryBackgroundColor = accessoryBackgroundColor,
        indicatorColor = indicatorColor,
    )

    /**
     * Creates a [FloatingTabBarShapes] that represents the default shapes used in a [FloatingTabBar].
     *
     * @param tabBarShape the shape used to clip the tab bar
     * @param tabShape the shape used to clip individual tabs. Can be useful for example to control the click ripple effect shape
     * @param standaloneTabShape the shape used to clip the standalone tab
     * @param accessoryShape the shape used to clip the accessory container
     */
    @Composable
    fun shapes(
        tabBarShape: Shape = RoundedCornerShape(100),
        tabShape: Shape = RoundedCornerShape(100),
        standaloneTabShape: Shape = CircleShape,
        accessoryShape: Shape = RoundedCornerShape(100),
    ): FloatingTabBarShapes = FloatingTabBarShapes(
        tabBarShape = tabBarShape,
        tabShape = tabShape,
        standaloneTabShape = standaloneTabShape,
        accessoryShape = accessoryShape,
    )

    /**
     * Creates a [FloatingTabBarSizes] that represents the default sizes used in a [FloatingTabBar].
     *
     * @param tabBarContentPadding the padding applied to the tab bar content. This also applies to the standalone tab content.
     * @param tabInlineContentPadding the padding applied to tabs in inline state
     * @param tabExpandedContentPadding the padding applied to tabs in expanded state
     * @param componentSpacing the spacing between components
     * @param tabSpacing the spacing between tabs in expanded state
     * @param inlineComponentSpacing the spacing between components in inline state
     * @param inlineBarPadding padding around the whole bar in inline state, inside the bar's own
     */
    @Composable
    fun sizes(
        tabBarContentPadding: PaddingValues = PaddingValues(vertical = 4.dp, horizontal = 4.dp),
        tabInlineContentPadding: PaddingValues = PaddingValues(10.dp),
        tabExpandedContentPadding: PaddingValues = PaddingValues(vertical = 6.dp, horizontal = 20.dp),
        componentSpacing: Dp = 8.dp,
        tabSpacing: Dp = 0.dp,
        inlineComponentSpacing: Dp = componentSpacing,
        inlineBarPadding: PaddingValues = PaddingValues(),
    ): FloatingTabBarSizes = FloatingTabBarSizes(
        tabBarContentPadding = tabBarContentPadding,
        tabInlineContentPadding = tabInlineContentPadding,
        tabExpandedContentPadding = tabExpandedContentPadding,
        componentSpacing = componentSpacing,
        tabSpacing = tabSpacing,
        inlineComponentSpacing = inlineComponentSpacing,
        inlineBarPadding = inlineBarPadding,
    )

    /**
     * Creates a [FloatingTabBarElevations] that represents the default elevations used in a [FloatingTabBar].
     *
     * @param inlineElevation the elevation used for tabs in inline state
     * @param expandedElevation the elevation used for tabs in expanded state
     */
    @Composable
    fun elevations(
        inlineElevation: Dp = 6.dp,
        expandedElevation: Dp = 12.dp,
    ): FloatingTabBarElevations = FloatingTabBarElevations(
        inlineElevation = inlineElevation,
        expandedElevation = expandedElevation,
    )
}
