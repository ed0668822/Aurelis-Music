// Aurelis © 2026. GPL-3.0-or-later
// Aurelis integration changes; vendored portions retain the Apache-2.0 notice below.
@file:OptIn(ExperimentalSharedTransitionApi::class)

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
 * [com.music.aurelis.ui.components.GlassNavBar]. One further change here:
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

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibilityScope
import androidx.compose.animation.EnterExitState
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.SharedTransitionLayout
import androidx.compose.animation.SharedTransitionScope
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.AnimationSpec
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.keyframes
import androidx.compose.animation.core.snap
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Indication
import androidx.compose.foundation.LocalIndication
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.BlurEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.music.aurelis.data.SettingsRepository as AppSettings
import com.music.aurelis.ui.components.GlassSpring
import com.music.aurelis.ui.components.LocalGlassExport
import com.music.aurelis.ui.components.backdrop.backdrops.layerBackdrop
import com.music.aurelis.ui.components.backdrop.backdrops.rememberLayerBackdrop
import com.music.aurelis.ui.components.LocalGlassHardware
import com.music.aurelis.ui.components.LocalLiquidGlassEnabled
import com.music.aurelis.ui.components.shouldSampleGlass
import com.music.aurelis.ui.theme.LocalReduceMotion
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.animation.BoundsTransform
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.Spring
import com.music.aurelis.ui.haptics.Haptic
import com.music.aurelis.ui.haptics.rememberHaptics
import kotlin.math.roundToInt

/** Hoisted so the default is one stable lambda rather than a new one per call. */
private val DefaultTabBarContentModifier: @Composable () -> Modifier = { Modifier }

/**
 * A floating tab bar that can transition between inline and expanded states.
 *
 * @param isInline controls the FloatingTabBar's inline state
 * @param selectedTabKey the key of the currently selected tab
 * @param modifier the modifier to be applied to the tab bar
 * @param colors the colors used by the tab bar components
 * @param shapes the shapes used by the tab bar components
 * @param sizes the sizes and spacing used by the tab bar components
 * @param elevations the elevation values used by the tab bar components
 * @param tabBarContentModifier modifier applied to the tab bar sections containing the grouped tabs and standalone tab.
 * It is applied after the default styling (background, shadow, clip) but before any content padding.
 * @param inlineAccessory the accessory composable that appears in inline state (e.g., compact media player)
 * @param expandedAccessory the accessory composable that appears in expanded state (e.g., full media player)
 * @param contentKey optional key that when changed retriggers the content lambda
 * @param content the content defining the tabs
 */
@Composable
fun FloatingTabBar(
    isInline: Boolean,
    selectedTabKey: Any?,
    modifier: Modifier = Modifier,
    tabBarContentModifier: @Composable () -> Modifier = DefaultTabBarContentModifier,
    inlineAccessory: (@Composable SharedTransitionScope.(Modifier, AnimatedVisibilityScope) -> Unit)? = null,
    expandedAccessory: (@Composable SharedTransitionScope.(Modifier, AnimatedVisibilityScope) -> Unit)? = null,
    colors: FloatingTabBarColors = FloatingTabBarDefaults.colors(),
    shapes: FloatingTabBarShapes = FloatingTabBarDefaults.shapes(),
    sizes: FloatingTabBarSizes = FloatingTabBarDefaults.sizes(),
    elevations: FloatingTabBarElevations = FloatingTabBarDefaults.elevations(),
    contentKey: Any? = null,
    content: FloatingTabBarScope.() -> Unit
) {
    val scrollConnection = rememberFloatingTabBarScrollConnection(
        initialIsInline = isInline,
        inlineBehavior = FloatingTabBarInlineBehavior.Never
    )

    LaunchedEffect(isInline) {
        if (isInline) scrollConnection.inline() else scrollConnection.expand()
    }

    FloatingTabBar(
        selectedTabKey = selectedTabKey,
        scrollConnection = scrollConnection,
        modifier = modifier,
        tabBarContentModifier = tabBarContentModifier,
        inlineAccessory = inlineAccessory,
        expandedAccessory = expandedAccessory,
        colors = colors,
        shapes = shapes,
        sizes = sizes,
        elevations = elevations,
        contentKey = contentKey,
        content = content
    )
}

/**
 * A floating tab bar that transitions between inline and expanded states based on scroll behavior.
 *
 * @param selectedTabKey the key of the currently selected tab
 * @param scrollConnection the scroll connection that handles state transitions
 * @param modifier the modifier to be applied to the tab bar
 * @param colors the colors used by the tab bar components
 * @param shapes the shapes used by the tab bar components
 * @param sizes the sizes and spacing used by the tab bar components
 * @param elevations the elevation values used by the tab bar components
 * @param tabBarContentModifier modifier applied to the tab bar sections containing the grouped tabs and standalone tab.
 * It is applied after the default styling (background, shadow, clip) but before any content padding.
 * @param inlineAccessory the accessory composable that appears in inline state (e.g., compact media player)
 * @param expandedAccessory the accessory composable that appears in expanded state (e.g., full media player)
 * @param contentKey optional key that when changed retriggers the content lambda
 * @param content the content defining the tabs
 */
@Composable
fun FloatingTabBar(
    selectedTabKey: Any?,
    scrollConnection: FloatingTabBarScrollConnection,
    modifier: Modifier = Modifier,
    tabBarContentModifier: @Composable () -> Modifier = DefaultTabBarContentModifier,
    inlineAccessory: (@Composable SharedTransitionScope.(Modifier, AnimatedVisibilityScope) -> Unit)? = null,
    expandedAccessory: (@Composable SharedTransitionScope.(Modifier, AnimatedVisibilityScope) -> Unit)? = null,
    colors: FloatingTabBarColors = FloatingTabBarDefaults.colors(),
    shapes: FloatingTabBarShapes = FloatingTabBarDefaults.shapes(),
    sizes: FloatingTabBarSizes = FloatingTabBarDefaults.sizes(),
    elevations: FloatingTabBarElevations = FloatingTabBarDefaults.elevations(),
    contentKey: Any? = null,
    content: FloatingTabBarScope.() -> Unit
) {
    val reduced = LocalReduceMotion.current
    val scope = remember(contentKey) { FloatingTabBarScopeImpl().apply { content() } }

    val isAccessoryShared = inlineAccessory != null && expandedAccessory != null

    SharedTransitionLayout(modifier = modifier) {
        AnimatedContent(
            targetState = scrollConnection.isInline,
            transitionSpec = { fadeIn(if (reduced) snap() else androidx.compose.animation.core.tween()) togetherWith
                fadeOut(if (reduced) snap() else androidx.compose.animation.core.tween()) },
            contentAlignment = Alignment.BottomCenter
        ) { isInline ->
            Box(Modifier.then(if (isInline != scrollConnection.isInline) Modifier.clearAndSetSemantics { } else Modifier)) {
            if (isInline) {
                InlineBar(
                    scope = scope,
                    selectedTabKey = selectedTabKey,
                    accessory = inlineAccessory,
                    isAccessoryShared = isAccessoryShared,
                    onInlineTabClick = { scrollConnection.expand() },
                    colors = colors,
                    shapes = shapes,
                    sizes = sizes,
                    elevations = elevations,
                    tabBarContentModifier = tabBarContentModifier,
                    animatedVisibilityScope = this@AnimatedContent
                )
            } else {
                ExpandedBar(
                    scope = scope,
                    selectedTabKey = selectedTabKey,
                    accessory = expandedAccessory,
                    isAccessoryShared = isAccessoryShared,
                    colors = colors,
                    shapes = shapes,
                    sizes = sizes,
                    elevations = elevations,
                    tabBarContentModifier = tabBarContentModifier,
                    animatedVisibilityScope = this@AnimatedContent
                )
            }
            }
        }
    }
}

interface FloatingTabBarScope {
    /**
     * Adds a regular tab to the floating tab bar.
     *
     * @param key Unique identifier for the tab
     * @param title Composable content for the tab title
     * @param icon Composable content for the tab icon
     * @param onClick Callback invoked when the tab is clicked
     * @param indication Optional indication provider for touch feedback, defaults to LocalIndication.current
     */
    fun tab(
        key: Any,
        title: @Composable () -> Unit,
        icon: @Composable () -> Unit,
        onClick: () -> Unit,
        indication: (@Composable () -> Indication)? = { LocalIndication.current }
    )

    /**
     * Adds a standalone tab to the floating tab bar.
     *
     * Note: Calling this method more than once will override the previous standalone tab value.
     *
     * @param key Unique identifier for the standalone tab
     * @param icon Composable content for the tab icon
     * @param onClick Callback invoked when the tab is clicked
     * @param indication Optional indication provider for touch feedback, defaults to LocalIndication.current
     */
    fun standaloneTab(
        key: Any,
        icon: @Composable () -> Unit,
        onClick: () -> Unit,
        indication: (@Composable () -> Indication)? = { LocalIndication.current }
    )
}

@Composable
private fun SharedTransitionScope.InlineBar(
    scope: FloatingTabBarScopeImpl,
    selectedTabKey: Any?,
    accessory: (@Composable SharedTransitionScope.(Modifier, AnimatedVisibilityScope) -> Unit)?,
    isAccessoryShared: Boolean,
    onInlineTabClick: () -> Unit,
    colors: FloatingTabBarColors,
    shapes: FloatingTabBarShapes,
    sizes: FloatingTabBarSizes,
    elevations: FloatingTabBarElevations,
    tabBarContentModifier: @Composable () -> Modifier,
    animatedVisibilityScope: AnimatedVisibilityScope
) {
    val inlineTab = scope.getInlineTab(selectedTabKey)
    val standaloneTab = scope.standaloneTab
    val hasInlineTab = inlineTab != null

    // With an accessory the row spans the full width ([tab][accessory][standalone],
    // iOS 26 Apple Music style); without one, the cluster is packed and centered.
    Row(
        horizontalArrangement = Arrangement.spacedBy(sizes.inlineComponentSpacing),
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .padding(sizes.inlineBarPadding)
            .then(if (accessory == null) Modifier.wrapContentWidth() else Modifier)
            .height(IntrinsicSize.Max)
    ) {
        if (hasInlineTab) {
            InlineTab(
                inlineTab = inlineTab,
                isSelected = inlineTab.key == selectedTabKey,
                onInlineTabClick = onInlineTabClick,
                shapes = shapes,
                sizes = sizes,
                colors = colors,
                elevations = elevations,
                animatedVisibilityScope = animatedVisibilityScope,
                tabBarContentModifier = tabBarContentModifier,
                modifier = Modifier
            )
        }

        if (accessory != null) {
            InlineAccessory(
                accessory = accessory,
                isAccessoryShared = isAccessoryShared,
                shapes = shapes,
                colors = colors,
                elevations = elevations,
                animatedVisibilityScope = animatedVisibilityScope,
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
            )
        }

        if (standaloneTab != null) {
            InlineStandaloneTab(
                standaloneTab = standaloneTab,
                isSelected = standaloneTab.key == selectedTabKey,
                shapes = shapes,
                colors = colors,
                elevations = elevations,
                animatedVisibilityScope = animatedVisibilityScope,
                tabBarContentModifier = tabBarContentModifier,
                modifier = Modifier
                    .fillMaxHeight()
                    .aspectRatio(1f)
            )
        }
    }
}

@Composable
private fun SharedTransitionScope.InlineTab(
    inlineTab: FloatingTabBarTab,
    isSelected: Boolean,
    onInlineTabClick: () -> Unit,
    shapes: FloatingTabBarShapes,
    sizes: FloatingTabBarSizes,
    colors: FloatingTabBarColors,
    elevations: FloatingTabBarElevations,
    animatedVisibilityScope: AnimatedVisibilityScope,
    modifier: Modifier,
    tabBarContentModifier: @Composable () -> Modifier
) {
    Box(
        modifier = modifier.testTag("navigation-group")
            .sharedElement(
                rememberSharedContentState("tabGroup"),
                animatedVisibilityScope = animatedVisibilityScope,
                boundsTransform = navBoundsTransform(),
                zIndexInOverlay = 1f
            )
            .shadow(
                shape = shapes.tabBarShape,
                elevation = elevations.inlineElevation
            )
            .background(
                color = colors.backgroundColor,
                shape = shapes.tabBarShape
            )
            .clip(shapes.tabBarShape)
            .then(tabBarContentModifier())
            .selectable(
                selected = isSelected, role = Role.Tab,
                onClick = {
                    onInlineTabClick()
                    inlineTab.onClick()
                },
                indication = inlineTab.indication?.invoke(),
                interactionSource = remember { MutableInteractionSource() }
            )
            .padding(sizes.tabInlineContentPadding)
    ) {
        Tab(
            icon = {
                Box(
                    Modifier.sharedElement(
                        rememberSharedContentState("tab#${inlineTab.key}-icon"),
                        animatedVisibilityScope = animatedVisibilityScope,
                        boundsTransform = navBoundsTransform(),
                zIndexInOverlay = 1f
                    )
                ) {
                    inlineTab.icon()
                }
            },
            title = { inlineTab.title() },
            isInline = true
        )
    }
}

@Composable
private fun SharedTransitionScope.InlineStandaloneTab(
    standaloneTab: FloatingTabBarTab,
    isSelected: Boolean,
    shapes: FloatingTabBarShapes,
    colors: FloatingTabBarColors,
    elevations: FloatingTabBarElevations,
    animatedVisibilityScope: AnimatedVisibilityScope,
    modifier: Modifier,
    tabBarContentModifier: @Composable () -> Modifier
) {
    Tab(
        icon = standaloneTab.icon,
        title = standaloneTab.title,
        isInline = true,
        isStandalone = true,
        modifier = modifier
            .sharedElement(
                rememberSharedContentState("standaloneTab"),
                animatedVisibilityScope = animatedVisibilityScope,
                boundsTransform = navBoundsTransform(),
                zIndexInOverlay = 1f
            )
            .shadow(
                shape = shapes.standaloneTabShape,
                elevation = elevations.inlineElevation
            )
            .background(
                color = colors.backgroundColor,
                shape = shapes.standaloneTabShape
            )
            .clip(shapes.standaloneTabShape)
            .then(tabBarContentModifier())
            .selectable(
                selected = isSelected, role = Role.Tab,
                onClick = standaloneTab.onClick,
                indication = standaloneTab.indication?.invoke(),
                interactionSource = remember { MutableInteractionSource() }
            )
    )
}

@Composable
private fun SharedTransitionScope.InlineAccessory(
    accessory: (@Composable SharedTransitionScope.(Modifier, AnimatedVisibilityScope) -> Unit)?,
    isAccessoryShared: Boolean,
    colors: FloatingTabBarColors,
    shapes: FloatingTabBarShapes,
    elevations: FloatingTabBarElevations,
    animatedVisibilityScope: AnimatedVisibilityScope,
    modifier: Modifier
) {
    accessory?.let { accessory ->
        Box(
            modifier = modifier
                .then(
                    if (isAccessoryShared) {
                        Modifier.sharedElement(
                            rememberSharedContentState("accessory"),
                            animatedVisibilityScope = animatedVisibilityScope,
                            boundsTransform = navBoundsTransform()
                        )
                    } else {
                        Modifier.animateEnterExitAccessory(
                            sharedTransitionScope = this,
                            animatedVisibilityScope = animatedVisibilityScope
                        )
                    }
                )
        ) {
            accessory(
                Modifier
                    .fillMaxSize()
                    .shadow(
                        shape = shapes.accessoryShape,
                        elevation = elevations.inlineElevation
                    )
                    .background(color = colors.accessoryBackgroundColor, shapes.accessoryShape)
                    .clip(shapes.accessoryShape),
                animatedVisibilityScope
            )
        }
    }
}

@Composable
private fun SharedTransitionScope.ExpandedBar(
    scope: FloatingTabBarScopeImpl,
    selectedTabKey: Any?,
    accessory: (@Composable SharedTransitionScope.(Modifier, AnimatedVisibilityScope) -> Unit)?,
    isAccessoryShared: Boolean,
    colors: FloatingTabBarColors,
    shapes: FloatingTabBarShapes,
    sizes: FloatingTabBarSizes,
    elevations: FloatingTabBarElevations,
    tabBarContentModifier: @Composable () -> Modifier,
    animatedVisibilityScope: AnimatedVisibilityScope
) {
    val standaloneTab = scope.standaloneTab
    val hasTabGroup = scope.tabs.isNotEmpty()

    // The accessory spans the full width above the tab row; the tab pill and the
    // standalone tab are packed together and centered (iOS 26 style).
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(sizes.componentSpacing),
        modifier = Modifier.fillMaxWidth()
    ) {
        if (accessory != null) {
            ExpandedAccessory(
                accessory = accessory,
                isAccessoryShared = isAccessoryShared,
                shapes = shapes,
                colors = colors,
                elevations = elevations,
                animatedVisibilityScope = animatedVisibilityScope,
                modifier = Modifier.fillMaxWidth()
            )
        }

        // Full width, and the tab pill takes whatever the standalone tab leaves,
        // so the expanded bar measures the same as the app's plain nav bar
        // instead of shrinking to a narrow centred cluster. It also removes the
        // overflow this row used to be able to hit: the pill is now given a
        // width to divide rather than growing past the screen to fit its labels.
        Row(
            horizontalArrangement = Arrangement.spacedBy(sizes.componentSpacing),
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
                .height(IntrinsicSize.Max)
        ) {
            if (hasTabGroup) {
                ExpandedTabs(
                    scope = scope,
                    selectedTabKey = selectedTabKey,
                    shapes = shapes,
                    sizes = sizes,
                    colors = colors,
                    elevations = elevations,
                    animatedVisibilityScope = animatedVisibilityScope,
                    tabBarContentModifier = tabBarContentModifier,
                    modifier = Modifier.weight(1f)
                )
            }

            if (standaloneTab != null) {
                ExpandedStandaloneTab(
                    standaloneTab = standaloneTab,
                isSelected = standaloneTab.key == selectedTabKey,
                    shapes = shapes,
                    colors = colors,
                    elevations = elevations,
                    animatedVisibilityScope = animatedVisibilityScope,
                    tabBarContentModifier = tabBarContentModifier,
                    modifier = Modifier
                        .fillMaxHeight()
                        .aspectRatio(1f)
                )
            }
        }
    }
}

@Composable
private fun SharedTransitionScope.ExpandedAccessory(
    accessory: @Composable SharedTransitionScope.(Modifier, AnimatedVisibilityScope) -> Unit,
    isAccessoryShared: Boolean,
    colors: FloatingTabBarColors,
    shapes: FloatingTabBarShapes,
    elevations: FloatingTabBarElevations,
    animatedVisibilityScope: AnimatedVisibilityScope,
    modifier: Modifier
) {
    Box(
        modifier = modifier
            .then(
                if (isAccessoryShared) {
                    Modifier.sharedElement(
                        rememberSharedContentState("accessory"),
                        animatedVisibilityScope = animatedVisibilityScope,
                        boundsTransform = navBoundsTransform()
                    )
                } else {
                    Modifier.animateEnterExitAccessory(
                        sharedTransitionScope = this,
                        animatedVisibilityScope = animatedVisibilityScope
                    )
                }
            )
    ) {
        accessory(
            Modifier
                .shadow(
                    shape = shapes.accessoryShape,
                    elevation = elevations.expandedElevation
                )
                .background(color = colors.accessoryBackgroundColor, shapes.accessoryShape)
                .clip(shapes.accessoryShape),
            animatedVisibilityScope
        )
    }
}
@Composable
private fun SharedTransitionScope.ExpandedTabs(
    scope: FloatingTabBarScopeImpl,
    selectedTabKey: Any?,
    shapes: FloatingTabBarShapes,
    sizes: FloatingTabBarSizes,
    colors: FloatingTabBarColors,
    elevations: FloatingTabBarElevations,
    animatedVisibilityScope: AnimatedVisibilityScope,
    modifier: Modifier,
    tabBarContentModifier: @Composable () -> Modifier
) {
    val inlineTab = scope.getInlineTab(selectedTabKey)
    val selectedTabIndex = scope.tabs.indexOfFirst { it.key == selectedTabKey }
    val currentSelectedTabIndex by rememberUpdatedState(selectedTabIndex)
    val reduceAnimation = LocalReduceMotion.current
    val reduceDynamicBlur by AppSettings.reduceDynamicBlur.collectAsStateWithLifecycle()
    val selectionSpec: AnimationSpec<Float> = if (reduceAnimation) {
        snap()
    } else {
        GlassSpring
    }
    val haptics = rememberHaptics()

    val coroutineScope = rememberCoroutineScope()
    val pill = remember { GlassPillMotion(coroutineScope, selectedTabIndex.coerceAtLeast(0)) }
    pill.reduceMotion = reduceAnimation
    // The lens samples the recorded page, and "reduce dynamic blur" stops that
    // recording. There the flat pill carries the whole motion on its own.
    val glassPill = shouldSampleGlass(LocalLiquidGlassEnabled.current, reduceDynamicBlur, LocalGlassHardware.current)
    val barGlass = rememberLayerBackdrop()
    val tabRow = rememberLayerBackdrop()

    var rowSize by remember { mutableStateOf(IntSize.Zero) }
    var lastHapticTab by remember { mutableIntStateOf(selectedTabIndex) }
    var lastSelectedTabIndex by remember { mutableIntStateOf(selectedTabIndex) }
    val density = LocalDensity.current
    val layoutDirection = LocalLayoutDirection.current
    val isRtl = layoutDirection == LayoutDirection.Rtl
    val tabCount = scope.tabs.size
    val tabSpacingPx = with(density) { sizes.tabSpacing.toPx() }
    val tabWidthPx = if (rowSize.width > 0 && tabCount > 0) {
        (rowSize.width - tabSpacingPx * (tabCount - 1)) / tabCount
    } else {
        0f
    }
    val tabStepPx = if (rowSize.width > 0 && tabCount > 0) {
        (rowSize.width + tabSpacingPx) / tabCount
    } else {
        0f
    }
    pill.stepDp = tabStepPx / density.density
    // For the row's drag, which runs in one coroutine for the life of the row
    // and would otherwise keep the step from its first touch.
    val currentTabStepPx by rememberUpdatedState(tabStepPx)
    val currentIsRtl by rememberUpdatedState(isRtl)

    // The pill rests in its tab's cell and lifts to stand [PILL_GROW_HEIGHT]
    // taller than the whole bar, keeping the cell's proportions as it grows.
    val contentPadding = sizes.tabBarContentPadding
    val padLeftPx = with(density) { contentPadding.calculateLeftPadding(layoutDirection).toPx() }
    val padTopPx = with(density) { contentPadding.calculateTopPadding().toPx() }
    val padBottomPx = with(density) { contentPadding.calculateBottomPadding().toPx() }
    val rowHeightPx = rowSize.height.toFloat()
    val restSize = Size(tabWidthPx, rowHeightPx)
    val liftedHeightPx = rowHeightPx + padTopPx + padBottomPx +
        with(density) { PILL_GROW_HEIGHT.toPx() }
    val liftedSize = if (rowHeightPx > 0f) {
        Size(liftedHeightPx * tabWidthPx / rowHeightPx, liftedHeightPx)
    } else {
        restSize
    }
    // Where the pill's centre is in the row, for a position in tab steps.
    val pillCenterInRow: (Float) -> Offset = { position ->
        val x = position * tabStepPx + tabWidthPx / 2f
        Offset(if (isRtl) rowSize.width - x else x, rowHeightPx / 2f)
    }
    val showPill = selectedTabIndex >= 0 && tabWidthPx > 0f

    LaunchedEffect(selectedTabIndex, reduceAnimation) {
        if (selectedTabIndex >= 0) {
            // Back from the standalone tab there is no tab to travel from.
            if (lastSelectedTabIndex < 0 || reduceAnimation) {
                pill.snapTo(selectedTabIndex)
            } else {
                pill.animateTo(selectedTabIndex)
            }
        }
        lastSelectedTabIndex = selectedTabIndex
        lastHapticTab = selectedTabIndex
    }

    // Unclipped: the lifted pill stands past the bar's edges, so it is drawn
    // here, over the bar, rather than inside the bar's clip.
    Box(
        modifier = modifier.testTag("navigation-group")
            .sharedElement(
                rememberSharedContentState("tabGroup"),
                animatedVisibilityScope = animatedVisibilityScope,
                boundsTransform = navBoundsTransform(),
                zIndexInOverlay = 1f
            )
    ) {
        // The bar's glass is exported for the lens to refract.
        CompositionLocalProvider(LocalGlassExport provides barGlass) {
            Box(
                modifier = Modifier
                    .shadow(
                        shape = shapes.tabBarShape,
                        elevation = elevations.expandedElevation
                    )
                    .background(
                        color = colors.backgroundColor,
                        shape = shapes.tabBarShape
                    )
                    .clip(shapes.tabBarShape)
                    .then(tabBarContentModifier())
                    .padding(contentPadding)
                    .animateContentSize()
            ) {
                if (showPill && (!glassPill || pill.isFlat)) {
                    FlatSelectionPill(
                        color = colors.indicatorColor,
                        modifier = Modifier
                            .matchParentSize()
                            .pillPlacement(
                                center = { pillCenterInRow(pill.position) },
                                size = { pill.liveSize(restSize, liftedSize) },
                            ),
                    )
                }

                Row(
                    horizontalArrangement = Arrangement.spacedBy(sizes.tabSpacing),
                    modifier = Modifier
                        .fillMaxWidth()
                        .selectableGroup()
                        .onSizeChanged { rowSize = it }
                        .then(if (glassPill) Modifier.layerBackdrop(tabRow) else Modifier)
                        .pointerInput(Unit) {
                            var totalDrag = 0f
                            var dragStartIndex = currentSelectedTabIndex
                            var direction = 1f
                            var step = 0f
                            // Decided per drag, not once for the coroutine: on the
                            // standalone tab there is no pill in this row to pick up,
                            // but that is no reason to stop listening for good.
                            var holding = false
                            detectHorizontalDragGestures(
                                onDragStart = {
                                    step = currentTabStepPx
                                    holding = currentSelectedTabIndex >= 0 && step > 0f
                                    if (!holding) return@detectHorizontalDragGestures
                                    // In tab order, whichever way the row runs.
                                    direction = if (currentIsRtl) -1f else 1f
                                    totalDrag = 0f
                                    dragStartIndex = currentSelectedTabIndex
                                    pill.startDrag()
                                },
                                onDragCancel = {
                                    if (holding) pill.release(currentSelectedTabIndex)
                                    holding = false
                                },
                                onDragEnd = {
                                    if (!holding) return@detectHorizontalDragGestures
                                    holding = false
                                    val absoluteIndexFloat = (dragStartIndex + totalDrag / step)
                                        .coerceIn(0f, scope.tabs.lastIndex.toFloat())
                                    val newIndex = absoluteIndexFloat.roundToInt()
                                    pill.release(newIndex)
                                    if (newIndex != currentSelectedTabIndex) {
                                        scope.tabs[newIndex].onClick()
                                    }
                                },
                                onHorizontalDrag = { _, delta ->
                                    if (!holding) return@detectHorizontalDragGestures
                                    totalDrag += delta * direction
                                    val absoluteIndexFloat = (dragStartIndex + totalDrag / step)
                                        .coerceIn(0f, scope.tabs.lastIndex.toFloat())
                                    pill.dragTo(absoluteIndexFloat)

                                    val approximateTab = absoluteIndexFloat.roundToInt()
                                    if (approximateTab != lastHapticTab) {
                                        haptics.play(Haptic.Tick)
                                        lastHapticTab = approximateTab
                                    }
                                }
                            )
                        }
                ) {
                    scope.tabs.forEach { tab ->
                        val isSelected = tab.key == selectedTabKey
                        val iconScale by animateFloatAsState(
                            targetValue = if (isSelected) 1.08f else 1f,
                            animationSpec = selectionSpec,
                            label = "glassTabScale"
                        )
                        Tab(
                            icon = {
                                Box(
                                    modifier = (if (tab.key == inlineTab?.key) {
                                        Modifier.sharedElement(
                                            rememberSharedContentState("tab#${tab.key}-icon"),
                                            animatedVisibilityScope = animatedVisibilityScope,
                                            boundsTransform = navBoundsTransform(),
                zIndexInOverlay = 1f
                                        )
                                    } else {
                                        Modifier.animateEnterExitTab(
                                            sharedTransitionScope = this@ExpandedTabs,
                                            animatedVisibilityScope = animatedVisibilityScope
                                        )
                                    }).graphicsLayer {
                                        scaleX = iconScale
                                        scaleY = iconScale
                                    }
                                ) {
                                    tab.icon()
                                }
                            },
                            title = {
                                Box(
                                    Modifier.animateEnterExitTab(
                                        sharedTransitionScope = this@ExpandedTabs,
                                        animatedVisibilityScope = animatedVisibilityScope
                                    )
                                ) {
                                    tab.title()
                                }
                            },
                            isInline = false,
                            // An equal share of the pill each, the way the plain nav bar
                            // divides its own width, rather than each tab being as wide as
                            // its label happens to be.
                            modifier = Modifier
                                .weight(1f)
                                .skipToLookaheadSize()
                                .clip(shapes.tabShape)
                                .selectable(
                                    selected = isSelected, role = Role.Tab,
                                    onClick = tab.onClick,
                                    indication = tab.indication?.invoke(),
                                    interactionSource = remember { MutableInteractionSource() }
                                )
                                .padding(sizes.tabExpandedContentPadding)
                        )
                    }
                }
            }
        }

        if (glassPill && showPill && !pill.isFlat) {
            GlassSelectionPill(
                motion = pill,
                fillColor = colors.indicatorColor,
                barGlass = barGlass,
                tabRow = tabRow,
                modifier = Modifier
                    .matchParentSize()
                    .pillPlacement(
                        center = { pillCenterInRow(pill.position) + Offset(padLeftPx, padTopPx) },
                        size = { pill.liveSize(restSize, liftedSize) },
                    ),
            )
        }
    }
}

@Composable
private fun SharedTransitionScope.ExpandedStandaloneTab(
    standaloneTab: FloatingTabBarTab,
    isSelected: Boolean,
    shapes: FloatingTabBarShapes,
    colors: FloatingTabBarColors,
    elevations: FloatingTabBarElevations,
    animatedVisibilityScope: AnimatedVisibilityScope,
    modifier: Modifier,
    tabBarContentModifier: @Composable () -> Modifier
) {
    Tab(
        icon = standaloneTab.icon,
        title = standaloneTab.title,
        isInline = false,
        isStandalone = true,
        modifier = modifier
            .sharedElement(
                rememberSharedContentState("standaloneTab"),
                animatedVisibilityScope = animatedVisibilityScope,
                boundsTransform = navBoundsTransform(),
                zIndexInOverlay = 1f
            )
            .shadow(
                shape = shapes.standaloneTabShape,
                elevation = elevations.expandedElevation
            )
            .background(
                color = colors.backgroundColor,
                shape = shapes.standaloneTabShape
            )
            .clip(shapes.standaloneTabShape)
            .then(tabBarContentModifier())
            .selectable(
                selected = isSelected, role = Role.Tab,
                onClick = standaloneTab.onClick,
                indication = standaloneTab.indication?.invoke(),
                interactionSource = remember { MutableInteractionSource() }
            )
    )
}

@Composable
private fun Tab(
    icon: @Composable () -> Unit,
    title: @Composable () -> Unit,
    isInline: Boolean,
    modifier: Modifier = Modifier,
    isStandalone: Boolean = false
) {
    Column(
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = modifier
    ) {
        icon()
        if (!isStandalone && !isInline) {
            title()
        }
    }
}

/**
 * A custom modifier that provides smooth enter/exit animations without clipping shadows or other content.
 * This is an alternative to animateEnterExit that uses renderInSharedTransitionScopeOverlay to prevent clipping.
 */
@Composable
private fun Modifier.animateEnterExitAccessory(
    sharedTransitionScope: SharedTransitionScope,
    animatedVisibilityScope: AnimatedVisibilityScope
): Modifier = with(sharedTransitionScope) {
    with(animatedVisibilityScope) {
        val animatedAlpha by transition.animateFloat { targetState ->
            when (targetState) {
                EnterExitState.Visible -> 1f
                else -> 0f
            }
        }

        this@animateEnterExitAccessory
            .renderInSharedTransitionScopeOverlay()
            .graphicsLayer(
                compositingStrategy = CompositingStrategy.ModulateAlpha,
                alpha = animatedAlpha
            )
    }
}

/**
 * A custom modifier that provides smooth enter/exit animations with fade and blur effects.
 */
@Composable
private fun Modifier.animateEnterExitTab(
    sharedTransitionScope: SharedTransitionScope,
    animatedVisibilityScope: AnimatedVisibilityScope
): Modifier = with(sharedTransitionScope) {
    with(animatedVisibilityScope) {
        val enterStartFraction = 0.5f
        val enterEndFraction = 0.8f
        val durationMs = if (LocalReduceMotion.current) 0 else 150

        val animatedAlpha by transition.animateFloat(
            transitionSpec = {
                keyframes {
                    durationMillis = durationMs
                    if (targetState == EnterExitState.Visible) {
                        0f atFraction enterStartFraction using FastOutSlowInEasing
                        1f atFraction enterEndFraction
                    }
                }
            }
        ) { targetState ->
            when (targetState) {
                EnterExitState.Visible -> 1f
                else -> 0f
            }
        }

        // 12dp, down from the library's 50. A tab glyph is 28dp across, so a 50dp
        // radius is twice the thing it is blurring — everything past about half
        // the element's width has already dissolved it to nothing and is spent
        // on a wider and wider sample of empty space. 12 reaches the same nothing
        // over a quarter of the area, and there are six of these running at once
        // in the middle of the fold, over glass that is sampling the screen.
        val reduced = LocalReduceMotion.current
        val reduceBlur by AppSettings.reduceDynamicBlur.collectAsStateWithLifecycle()
        val blurRadius = with(LocalDensity.current) { if (reduced || reduceBlur) 0f else 12.dp.toPx() }
        val animatedBlur by transition.animateFloat(
            transitionSpec = {
                keyframes {
                    durationMillis = durationMs
                    if (targetState == EnterExitState.Visible) {
                        blurRadius atFraction enterStartFraction using FastOutSlowInEasing
                        0f atFraction enterEndFraction
                    }
                }
            }
        ) { targetState ->
            when (targetState) {
                EnterExitState.Visible -> 0f
                else -> blurRadius
            }
        }

        graphicsLayer {
            alpha = animatedAlpha
            // Null at rest, rather than a zero-radius BlurEffect. A layer holding
            // any render effect at all is composited offscreen for as long as it
            // holds one, so the library's unconditional BlurEffect(0, 0) bought
            // every tab glyph and every tab label a permanent offscreen buffer to
            // pay for a blur of nothing — on a bar that is on screen the whole
            // time the app is.
            renderEffect =
                if (animatedBlur > 0f) BlurEffect(animatedBlur, animatedBlur) else null
        }
    }
}

private class FloatingTabBarScopeImpl : FloatingTabBarScope {
    val tabs = mutableStateListOf<FloatingTabBarTab>()
    var standaloneTab: FloatingTabBarTab? by mutableStateOf(null)
        private set
    private var inlineTab: FloatingTabBarTab? = null

    fun getInlineTab(selectedTabKey: Any?): FloatingTabBarTab? {
        return if (selectedTabKey != standaloneTab?.key) {
            val selectedTab = tabs.find { it.key == selectedTabKey }
            if (selectedTab != null) {
                inlineTab = selectedTab
                selectedTab
            } else {
                inlineTab ?: tabs.firstOrNull()
            }
        } else {
            inlineTab ?: tabs.firstOrNull()
        }
    }

    override fun tab(
        key: Any,
        title: @Composable () -> Unit,
        icon: @Composable () -> Unit,
        onClick: () -> Unit,
        indication: (@Composable () -> Indication)?
    ) {
        tabs.add(
            FloatingTabBarTab(
                key = key,
                title = title,
                icon = icon,
                onClick = onClick,
                indication = indication
            )
        )
    }

    override fun standaloneTab(
        key: Any,
        icon: @Composable () -> Unit,
        onClick: () -> Unit,
        indication: (@Composable () -> Indication)?
    ) {
        standaloneTab = FloatingTabBarTab(
            key = key,
            title = {},
            icon = icon,
            onClick = onClick,
            indication = indication
        )
    }
}

private data class FloatingTabBarTab(
    val key: Any,
    val title: @Composable () -> Unit,
    val icon: @Composable () -> Unit,
    val onClick: () -> Unit,
    val indication: (@Composable () -> Indication)?
)


@Composable
private fun navBoundsTransform(): BoundsTransform {
    val reduced = LocalReduceMotion.current
    return BoundsTransform { _, _ -> if (reduced) snap() else spring(stiffness = Spring.StiffnessMediumLow) }
}
