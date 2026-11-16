// Aurelis © 2026. GPL-3.0-or-later
package com.music.aurelis.ui.navigation

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import com.music.aurelis.ui.components.floatingtabbar.FloatingTabBarInlineBehavior
import com.music.aurelis.ui.components.floatingtabbar.FloatingTabBarScrollConnection
import org.junit.Assert.*
import org.junit.Test

class NavigationBarScrollTest {
    @Test fun userScrollFoldsAndReversesWithoutConsumingThePageGesture() {
        val bar = FloatingTabBarScrollConnection(scrollThresholdPx = 50f)
        assertEquals(Offset.Zero, bar.onPreScroll(Offset(0f, -30f), NestedScrollSource.UserInput))
        assertFalse(bar.isInline)
        bar.onPreScroll(Offset(0f, -25f), NestedScrollSource.UserInput)
        assertTrue(bar.isInline)
        bar.onPreScroll(Offset(0f, 30f), NestedScrollSource.UserInput)
        assertTrue(bar.isInline)
        bar.onPreScroll(Offset(0f, 25f), NestedScrollSource.UserInput)
        assertFalse(bar.isInline)
    }

    @Test fun programmaticScrollingCannotFoldTheBarAndDirectionChangesResetTheDistance() {
        val bar = FloatingTabBarScrollConnection(scrollThresholdPx = 50f)
        bar.onPreScroll(Offset(0f, -500f), NestedScrollSource.SideEffect)
        assertFalse(bar.isInline)
        bar.onPreScroll(Offset(0f, -40f), NestedScrollSource.UserInput)
        bar.onPreScroll(Offset(0f, 10f), NestedScrollSource.UserInput)
        bar.onPreScroll(Offset(0f, -20f), NestedScrollSource.UserInput)
        assertFalse(bar.isInline)
        bar.onPreScroll(Offset(0f, -35f), NestedScrollSource.UserInput)
        assertTrue(bar.isInline)
        bar.expand()
        bar.onPreScroll(Offset(0f, -20f), NestedScrollSource.UserInput)
        assertFalse(bar.isInline)
    }

    @Test fun explicitAndReversePoliciesKeepTheirOriginalBehavior() {
        val fixed = FloatingTabBarScrollConnection(scrollThresholdPx = 50f,
            inlineBehavior = FloatingTabBarInlineBehavior.Never)
        fixed.onPreScroll(Offset(0f, -500f), NestedScrollSource.UserInput)
        assertFalse(fixed.isInline)
        fixed.inline()
        fixed.onPreScroll(Offset(0f, 500f), NestedScrollSource.UserInput)
        assertTrue(fixed.isInline)
        val reverse = FloatingTabBarScrollConnection(scrollThresholdPx = 50f,
            inlineBehavior = FloatingTabBarInlineBehavior.OnScrollUp)
        reverse.onPreScroll(Offset(0f, 60f), NestedScrollSource.UserInput)
        assertTrue(reverse.isInline)
        reverse.onPreScroll(Offset(0f, -60f), NestedScrollSource.UserInput)
        assertFalse(reverse.isInline)
    }
}
