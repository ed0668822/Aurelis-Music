// Aurelis © 2026. GPL-3.0-or-later
// Adapted from BitChord b690dc146009d4815825fa4a20815ef2e4ee8898 (GPLv3).
// Original BitChord contributors and terms retained; integration changes by Aurelis.
package com.music.aurelis.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.snap
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.CornerBasedShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.layout.layout
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil3.compose.AsyncImage
import com.music.aurelis.data.ProfileRepository
import com.music.aurelis.data.SettingsRepository
import com.music.aurelis.ui.icons.AurelisIcons
import com.music.aurelis.ui.theme.LocalReduceMotion
import com.music.aurelis.ui.theme.stableStatusBarInsets
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.materials.ExperimentalHazeMaterialsApi
import dev.chrisbanes.haze.materials.HazeMaterials
import kotlin.math.roundToInt

val TopBarContentHeight = 52.dp
val TopBarContentGap = 12.dp
private val ACCESSORY_GAP = 8.dp
private val TOP_CONTROL_SIZE = 44.dp
private val ACTION_GAP = 10.dp
private val ACTION_EDGE_INSET = 1.5.dp
private val AVATAR_SIZE = 28.dp

@Composable
fun topBarHeight() = stableStatusBarInsets().asPaddingValues().calculateTopPadding() + TopBarContentHeight

@Composable
fun topBarContentPadding() = topBarHeight() + TopBarContentGap

/** The source account photo adapted to the existing single local profile. */
@Composable
fun LocalProfileButton(onClick: () -> Unit) {
    val profile by ProfileRepository.state.collectAsStateWithLifecycle()
    val profileLabel = stringResource(com.music.aurelis.R.string.settings_profile_action)
    IconButton(onClick = onClick, modifier = Modifier.size(TOP_CONTROL_SIZE)
        .semantics { contentDescription = profileLabel }) {
        val avatar = profile.profile.avatar
        val avatarModifier = Modifier.size(AVATAR_SIZE).clip(CircleShape)
            .border(1.dp, MaterialTheme.colorScheme.onSurface.copy(alpha = 0.15f), CircleShape)
            .testTag("top-bar-avatar")
        if (avatar != null) {
            AsyncImage(avatar, null, avatarModifier, contentScale = ContentScale.Crop)
        } else {
            Box(avatarModifier.background(MaterialTheme.colorScheme.surfaceVariant), contentAlignment = Alignment.Center) {
                Icon(AurelisIcons.Artist, contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(18.dp))
            }
        }
    }
}

/** Floating page chrome from the pinned source. Title parameters remain for
 * caller compatibility; that source intentionally hides its small title.
 * Refresh gestures are owned by the feed host; its pull state also drives this bar. */
@Composable
fun FrostedTopBar(
    title: String,
    // A lambda, not a value: this flips as a feed leaves its very top, and
    // reading it in the caller would recompose the whole app on that frame of
    // the scroll.
    scrolled: () -> Boolean,
    modifier: Modifier = Modifier,
    /** Leaves the whole bar transparent so the page's own top gradient is the backdrop. */
    transparentBackdrop: Boolean = false,
    /** Circular back surface and no collapsing title, for artwork-led pages. */
    artworkPageChrome: Boolean = false,
    /** Source sampled by floating top-bar surfaces when liquid glass is off. */
    backButtonHazeState: HazeState? = LocalAppHazeState.current,
    trailingTitle: String? = null,
    onBack: (() -> Unit)? = null,
    refreshing: Boolean = false,
    // A lambda, not a value: the drag changes every frame, and reading it in
    // the caller would recompose the whole app on each one.
    pullFraction: () -> Float = { 0f },
    /**
     * A page control filling the bar from the leading gutter to [actions] — the
     * Search tab's field — set on a pill of its own; see [AccessoryPill].
     * Root tabs have no back button there for it to collide with.
     */
    accessory: (@Composable () -> Unit)? = null,
    actions: @Composable () -> Unit = {},
) {
    val reduceDynamicBlur by SettingsRepository.reduceDynamicBlur.collectAsStateWithLifecycle()
    val useFloatingChrome = artworkPageChrome ||
        (LocalLiquidGlassEnabled.current && isGlassSupported())
    // Every bounded bar uses the same hairline, whether its pane is blurred or
    // solid. A transparent artwork page has no pane edge for a line to mark.
    val dividerColor by animateColorAsState(
        targetValue = MaterialTheme.colorScheme.outline.copy(
            alpha = when {
                transparentBackdrop -> 0f
                else -> 0.6f
            },
        ),
        animationSpec = if (LocalReduceMotion.current) snap() else tween(220),
        label = "topBarDivider",
    )

    Column(
        modifier = modifier
            .fillMaxWidth()
            .then(
                if (reduceDynamicBlur && !transparentBackdrop) {
                    Modifier.background(MaterialTheme.colorScheme.surface)
                } else Modifier,
            ),
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .windowInsetsPadding(stableStatusBarInsets())
                .height(TopBarContentHeight),
        ) {
            // The pinned reference keeps the small centered title hidden.

            // On a pushed page the back affordance is always visible, since
            // there is no large in-list header to fall back on.
            if (onBack != null) {
                if (useFloatingChrome) {
                    ArtworkPageBackButton(
                        onClick = onBack,
                        hazeState = backButtonHazeState,
                        // The surface edge aligns with the floating navbar. The
                        // padding belongs outside the circle; its icon remains
                        // centred in the same 44dp control in every material.
                        modifier = Modifier
                            .align(Alignment.CenterStart)
                            .padding(start = BAR_GUTTER),
                    )
                } else {
                    IconButton(
                        onClick = onBack,
                        modifier = Modifier.align(Alignment.CenterStart).padding(start = 4.dp),
                    ) {
                        Icon(
                            AurelisIcons.ArrowBack,
                            contentDescription = stringResource(com.music.aurelis.R.string.back),
                            tint = MaterialTheme.colorScheme.onSurface,
                        )
                    }
                }
            }
            // No wordmark at the leading end: the root tabs carry it above
            // their large heading instead (LargePageTitle in :sharedUi).
            // The accessory takes everything from the leading gutter up to the
            // actions, which keep their own width at the trailing end.
            val edge = if (useFloatingChrome) BAR_GUTTER else 4.dp
            Row(
                modifier = Modifier
                    .align(Alignment.CenterEnd)
                    .then(if (accessory != null) Modifier.fillMaxWidth().padding(start = edge) else Modifier)
                    // The page's gutter (the navbar keeps Apple's wider one);
                    // the pill's inset below is internal padding around the
                    // icons, not extra margin.
                    .padding(end = edge),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                if (accessory != null) {
                    Box(Modifier.weight(1f)) {
                        AccessoryPill(
                            floating = useFloatingChrome,
                            hazeState = backButtonHazeState,
                            content = accessory,
                        )
                    }
                    Spacer(Modifier.width(ACCESSORY_GAP))
                }
                if (useFloatingChrome) {
                    ArtworkPageActions(hazeState = backButtonHazeState, content = actions)
                } else {
                    actions()
                }
            }
        }
        HorizontalDivider(thickness = 0.5.dp, color = dividerColor)
        RefreshPuck(refreshing, pullFraction)
    }
}

/** Circular floating back affordance, material-matched to the navbar. */
@OptIn(ExperimentalHazeMaterialsApi::class)
@Composable
private fun ArtworkPageBackButton(
    onClick: () -> Unit,
    hazeState: HazeState?,
    modifier: Modifier = Modifier,
) {
    val shape = CircleShape
    val reduceDynamicBlur by SettingsRepository.reduceDynamicBlur.collectAsStateWithLifecycle()
    val useLiquidGlass = LocalLiquidGlassEnabled.current && isGlassSupported()
    val contentColor = if (useLiquidGlass && !reduceDynamicBlur) {
        glassContentColor()
    } else {
        MaterialTheme.colorScheme.onSurface
    }

    IconButton(
        onClick = onClick,
        modifier = modifier
            .size(TOP_CONTROL_SIZE)
            .testTag("top-bar-back")
            .then(artworkPageSurface(shape = shape, hazeState = hazeState)),
    ) {
        Icon(
            AurelisIcons.ArrowBack,
            contentDescription = stringResource(com.music.aurelis.R.string.back),
            tint = contentColor,
        )
    }
}

/** One shared pill for every action at the right of a floating top bar. */
@Composable
private fun ArtworkPageActions(
    hazeState: HazeState?,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    ActionsRow(
        modifier = modifier
            .testTag("top-bar-actions")
            .then(artworkPageSurface(shape = CircleShape, hazeState = hazeState))
            // Keep the right edge fixed while a new action opens room to its
            // left. The surface itself therefore grows instead of jumping to
            // its new width in a single frame.
            .animateContentSize(
                animationSpec = if (LocalReduceMotion.current) snap() else spring(
                    dampingRatio = Spring.DampingRatioNoBouncy,
                    stiffness = Spring.StiffnessMediumLow,
                ),
            )
            // One profile target with no inset is a true circle, the back
            // button's size. Once another action exists, Apple's inset opens at
            // both ends. This is layout padding inside the surface, not an
            // outer margin, so BAR_GUTTER remains unchanged.
            .artworkActionEdgePadding(),
    ) {
        // Apple's pill is as tall as its back button, 44pt, where the actions'
        // own 48dp touch targets stood it 4dp taller than the circle beside it.
        CompositionLocalProvider(LocalMinimumInteractiveComponentSize provides TOP_CONTROL_SIZE) {
            content()
        }
    }
}

/**
 * The actions side by side, centred on the pill's height, with Apple's gap
 * between each and the one before it.
 *
 * Each gap opens with the width of the action after it, so one sliding in from
 * nothing — the download ring — brings its gap with it rather than the row
 * jumping by a gap's width the frame it appears.
 */
@Composable
private fun ActionsRow(modifier: Modifier, content: @Composable () -> Unit) {
    Layout(content = content, modifier = modifier) { measurables, constraints ->
        val placeables = measurables.map { it.measure(constraints.copy(minWidth = 0, minHeight = 0)) }
        val target = TOP_CONTROL_SIZE.roundToPx()
        val gap = ACTION_GAP.toPx()
        val gaps = placeables.mapIndexed { index, placeable ->
            if (index == 0) 0 else (gap * (placeable.width.toFloat() / target).coerceIn(0f, 1f)).roundToInt()
        }
        val width = placeables.sumOf { it.width } + gaps.sum()
        val height = placeables.maxOfOrNull { it.height } ?: 0
        layout(width, height) {
            var x = 0
            placeables.forEachIndexed { index, placeable ->
                x += gaps[index]
                placeable.placeRelative(x, (height - placeable.height) / 2)
                x += placeable.width
            }
        }
    }
}

/**
 * The bar's accessory — the Search tab's field — as a squircle pill at the
 * height of the controls beside it. On floating chrome it is cut from the same
 * surface as those controls, its glyphs and words in the glass's content
 * colour; on the plain bar it keeps the field's own fill. Same shape and size
 * either way, so turning Liquid Glass off changes the material and nothing else.
 */
@Composable
private fun AccessoryPill(
    floating: Boolean,
    hazeState: HazeState?,
    content: @Composable () -> Unit,
) {
    val reduceDynamicBlur by SettingsRepository.reduceDynamicBlur.collectAsStateWithLifecycle()
    val glass = floating && LocalLiquidGlassEnabled.current && isGlassSupported() && !reduceDynamicBlur
    val shape = remember { SquirclePillShape() }
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(TOP_CONTROL_SIZE)
            .testTag("top-bar-accessory")
            .then(
                if (floating) {
                    artworkPageSurface(shape = shape, hazeState = hazeState)
                } else {
                    Modifier
                        .clip(shape)
                        .background(MaterialTheme.colorScheme.surfaceVariant)
                },
            ),
    ) {
        CompositionLocalProvider(LocalSearchFieldOnSurface provides true) {
            if (glass) {
                val glassContent = glassContentColor()
                MaterialTheme(
                    colorScheme = MaterialTheme.colorScheme.copy(
                        onBackground = glassContent,
                        onSurfaceVariant = glassContent.copy(alpha = ACCESSORY_HINT_ALPHA),
                        primary = glassContent,
                    ),
                    content = content,
                )
            } else {
                content()
            }
        }
    }
}

/** The field's hint and glyphs on glass, against its typed text at full strength. */
private const val ACCESSORY_HINT_ALPHA = 0.6f

/**
 * Edge padding that collapses only for the profile-only state.
 *
 * Invisible/animating action slots can measure between zero and a full target,
 * so the natural content width—not the number of emitted composables—is the
 * reliable source of truth. At exactly one icon target the pill stays circular;
 * above that it gains Apple's inset while retaining fully round ends.
 */
private fun Modifier.artworkActionEdgePadding(): Modifier = layout { measurable, constraints ->
    val placeable = measurable.measure(constraints.copy(minWidth = 0))
    val oneActionWidth = TOP_CONTROL_SIZE.roundToPx()
    val extraActionFraction = ((placeable.width - oneActionWidth).toFloat() / oneActionWidth)
        .coerceIn(0f, 1f)
    val edgePadding = (ACTION_EDGE_INSET.roundToPx() * extraActionFraction).roundToInt()
    val width = (placeable.width + edgePadding * 2).coerceIn(constraints.minWidth, constraints.maxWidth)

    layout(width, placeable.height) {
        placeable.placeRelative(edgePadding, 0)
    }
}

/** The navbar's exact material choice, reusable by every floating top control. */
@OptIn(ExperimentalHazeMaterialsApi::class)
@Composable
private fun artworkPageSurface(
    shape: CornerBasedShape,
    hazeState: HazeState?,
): Modifier {
    val container = MaterialTheme.colorScheme.surface
    val reduceDynamicBlur by SettingsRepository.reduceDynamicBlur.collectAsStateWithLifecycle()
    val useLiquidGlass = LocalLiquidGlassEnabled.current && isGlassSupported()

    return Modifier
        .clip(shape)
        .then(
            when {
                reduceDynamicBlur || !LocalGlassHardware.current -> Modifier.background(container)
                useLiquidGlass -> Modifier.liquidGlass(shape)
                hazeState == null -> Modifier.background(container)
                else -> Modifier.optimizedHazeEffect(
                    state = hazeState,
                    style = HazeMaterials.regular(container),
                )
            },
        )
        // Keep the same explicit hairline as FloatingBottomBar. Liquid glass
        // also has its refractive highlight, but the navbar retains this edge
        // so these surfaces do as well.
        .border(GLASS_EDGE_WIDTH, GLASS_EDGE_COLOR, shape)
}

// Original refresh puck geometry and motion; Android Material3 compatibility adapted.
@Composable
private fun RefreshPuck(refreshing: Boolean, pullFraction: () -> Float, modifier: Modifier = Modifier) {
    val currentFraction by rememberUpdatedState(pullFraction)
    val pulling by remember { derivedStateOf { currentFraction() > 0f } }
    if (!refreshing && !pulling) return

    val travel = with(LocalDensity.current) { (PUCK_REST + PUCK_SIZE).toPx() }
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(PUCK_REST + PUCK_SIZE + PUCK_OVERSHOOT + 12.dp)
            .clipToBounds()
            // Measure the clipped puck viewport at its real size, then reserve
            // zero height in the top bar so the bar never grows while pulling.
            // Keeping the zero-height layout outside the viewport avoids
            // clipping the puck itself to an empty rectangle.
            .layout { measurable, constraints ->
                val placeable = measurable.measure(constraints.copy(minHeight = 0))
                layout(placeable.width, 0) { placeable.place(0, 0) }
            },
        contentAlignment = Alignment.TopCenter,
    ) {
        Box(
            modifier = Modifier
                .graphicsLayer {
                    val f = currentFraction().coerceIn(0f, 2f)
                    // Past the threshold the puck keeps following, but
                    // reluctantly, like the list under it.
                    val eased = if (f <= 1f) f else 1f + (f - 1f) * 0.35f
                    translationY = travel * eased - PUCK_SIZE.toPx()
                    alpha = (f * 3f).coerceAtMost(1f)
                }
                .shadow(6.dp, CircleShape)
                .size(PUCK_SIZE)
                .testTag("library-refresh-puck")
                .background(MaterialTheme.colorScheme.surfaceContainerHigh, CircleShape),
            contentAlignment = Alignment.Center,
        ) {
            val indicatorModifier = Modifier.size(22.dp)
            if (refreshing) {
                CircularProgressIndicator(
                    modifier = indicatorModifier,
                    color = MaterialTheme.colorScheme.primary,
                    strokeWidth = 2.5.dp,
                    strokeCap = StrokeCap.Round,
                )
            } else {
                CircularProgressIndicator(
                    progress = { (currentFraction() * 0.8f).coerceIn(0f, 0.8f) },
                    modifier = indicatorModifier.graphicsLayer {
                        rotationZ = currentFraction().coerceIn(0f, 2f) * 180f
                    },
                    color = MaterialTheme.colorScheme.primary,
                    trackColor = Color.Transparent,
                    strokeWidth = 2.5.dp,
                    strokeCap = StrokeCap.Round,
                )
            }
        }
    }
}

private val PUCK_SIZE = 40.dp
private val PUCK_REST = 16.dp
private val PUCK_OVERSHOOT = 20.dp
