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

import androidx.compose.runtime.*
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * A [NestedScrollConnection] that handles scroll events to transition between inline and expanded states.
 *
 * @param initialIsInline Initial state of the tab bar (inline or expanded).
 * @param scrollThresholdPx The minimum scroll distance in pixels required to trigger a state change.
 * @param inlineBehavior Defines when the tab bar should transition to inline state.
 */
class FloatingTabBarScrollConnection(
    initialIsInline: Boolean = false,
    private val scrollThresholdPx: Float,
    private val inlineBehavior: FloatingTabBarInlineBehavior = FloatingTabBarInlineBehavior.OnScrollDown
) : NestedScrollConnection {
    var isInline by mutableStateOf(initialIsInline)
        private set

    private var accumulatedScroll = 0f

    fun expand() {
        isInline = false
        accumulatedScroll = 0f
    }

    fun inline() {
        isInline = true
        accumulatedScroll = 0f
    }

    override fun onPreScroll(available: Offset, source: NestedScrollSource): Offset {
        // If behavior is Never, don't change state
        if (source != NestedScrollSource.UserInput || inlineBehavior == FloatingTabBarInlineBehavior.Never) {
            return Offset.Zero
        }

        val scrollDelta = available.y

        // Reset accumulated scroll if changing direction
        if ((accumulatedScroll > 0 && scrollDelta < 0) || (accumulatedScroll < 0 && scrollDelta > 0)) {
            accumulatedScroll = 0f
        }

        // Accumulate scroll
        accumulatedScroll += scrollDelta

        when (inlineBehavior) {
            FloatingTabBarInlineBehavior.OnScrollDown -> {
                // Check if we've scrolled enough to trigger state change
                if (accumulatedScroll <= -scrollThresholdPx && !isInline) {
                    // Scrolling down enough - transition to inline mode
                    isInline = true
                    accumulatedScroll = 0f // Reset after state change
                } else if (accumulatedScroll >= scrollThresholdPx && isInline) {
                    // Scrolling up enough - transition to expanded mode
                    isInline = false
                    accumulatedScroll = 0f // Reset after state change
                }
            }

            FloatingTabBarInlineBehavior.OnScrollUp -> {
                // Check if we've scrolled enough to trigger state change
                if (accumulatedScroll >= scrollThresholdPx && !isInline) {
                    // Scrolling up enough - transition to inline mode
                    isInline = true
                    accumulatedScroll = 0f // Reset after state change
                } else if (accumulatedScroll <= -scrollThresholdPx && isInline) {
                    // Scrolling down enough - transition to expanded mode
                    isInline = false
                    accumulatedScroll = 0f // Reset after state change
                }
            }

            FloatingTabBarInlineBehavior.Never -> {
                // Already handled above, but included for completeness
            }
        }

        return Offset.Zero // Don't consume the scroll, let it pass through
    }
}

/**
 * Creates and remembers a [FloatingTabBarScrollConnection] instance.
 *
 * @param initialIsInline Initial state of the tab bar (inline or expanded). Default is false.
 * @param scrollThreshold The minimum scroll distance required to trigger a state change. Default is 50.dp.
 * @param inlineBehavior Defines when the tab bar should transition to inline state. Default is [FloatingTabBarInlineBehavior.OnScrollDown].
 * @return A remembered [FloatingTabBarScrollConnection] instance.
 */
@Composable
fun rememberFloatingTabBarScrollConnection(
    initialIsInline: Boolean = false,
    scrollThreshold: Dp = 50.dp,
    inlineBehavior: FloatingTabBarInlineBehavior = FloatingTabBarInlineBehavior.OnScrollDown
): FloatingTabBarScrollConnection = with(LocalDensity.current) {
    val scrollThresholdPx = scrollThreshold.toPx()
    remember(scrollThresholdPx, inlineBehavior, initialIsInline) {
        FloatingTabBarScrollConnection(initialIsInline, scrollThresholdPx, inlineBehavior)
    }
}

/**
 * Defines when the floating tab bar should transition to inline state.
 */
enum class FloatingTabBarInlineBehavior {
    /** Never transition to inline - it stays in expanded state */
    Never,

    /** Transition to inline when scrolling down */
    OnScrollDown,

    /** Transition to inline when scrolling up */
    OnScrollUp
}
