// Aurelis © 2026. GPL-3.0-or-later
/*
 * The glass rendering itself is Kyant0/backdrop (Apache-2.0), vendored at
 * [com.music.aurelis.ui.components.backdrop] — see that package for the
 * upstream attribution. This file is the integration glue, adapted from
 * BitChord's LiquidGlass.kt, cut down to the single on/off switch Aurelis
 * exposes in Settings. It is used by the floating nav bar and, while enabled,
 * the app-wide floating top controls.
 */
package com.music.aurelis.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.shape.CornerBasedShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.platform.LocalDensity
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.music.aurelis.data.SettingsRepository
import com.music.aurelis.ui.components.backdrop.Backdrop
import com.music.aurelis.ui.components.backdrop.backdrops.LayerBackdrop
import com.music.aurelis.ui.components.backdrop.drawBackdrop
import com.music.aurelis.ui.components.backdrop.effects.blur
import com.music.aurelis.ui.components.backdrop.effects.colorControls
import com.music.aurelis.ui.components.backdrop.effects.lens
import com.music.aurelis.ui.components.backdrop.highlight.Highlight
import com.music.aurelis.ui.components.backdrop.highlight.HighlightElement
import com.music.aurelis.ui.components.backdrop.internal.ShapeProvider
import com.music.aurelis.ui.components.backdrop.shadow.Shadow
import androidx.compose.ui.unit.dp
import dev.chrisbanes.haze.hazeEffect
import dev.chrisbanes.haze.materials.HazeMaterials

/** Whether the liquid glass nav bar is turned on — see [SettingsRepository.liquidGlass]. */
val LocalLiquidGlassEnabled = staticCompositionLocalOf { false }

/** The backdrop content (app UI) that a liquid glass surface samples from. */
val LocalAppBackdrop = staticCompositionLocalOf<Backdrop?> { null }

/**
 * Where a [liquidGlass] surface composed under it also records the glass it
 * draws — the sampled, blurred and tinted backdrop, without its rim or shadow —
 * so a lens above it can refract that surface rather than the page behind it.
 * The tab bar's travelling selection pill is the one reader; see
 * [com.music.aurelis.ui.components.floatingtabbar.GlassSelectionPill].
 */
internal val LocalGlassExport = androidx.compose.runtime.compositionLocalOf<LayerBackdrop?> { null }

/**
 * The backdrop blur pipeline requires [android.graphics.RenderEffect] on a
 * [android.graphics.RenderNode], available from Android 12 (API 31).
 * Since minSdk is 33, this is always supported.
 */
fun isGlassSupported(): Boolean = true

/** Apple-matched defaults (Echo's GlassEffectConfig()), fixed rather than user sliders. */
private const val VIBRANCY = 1f
private const val BLUR_RADIUS_DP = 8f
private const val LENS_HEIGHT = 0.5f
private const val LENS_AMOUNT = 0.5f
private const val LENS_MAX_DP = 48f
private const val SURFACE_OPACITY = 0.4f

/**
 * Resolution fraction the glass surface records and processes its backdrop at.
 *
 * A third, which is what Echo's `glassResolutionScale` comes to at this blur
 * radius. The blur is what hides the upscale.
 */
private const val GLASS_RESOLUTION_SCALE = 0.33f

/**
 * The hairline along a bar's edge, and what stands in for the glass rim
 * wherever the glass itself is not drawn.
 */
internal val GLASS_EDGE_WIDTH = 0.5.dp
internal val GLASS_EDGE_COLOR = Color.White.copy(alpha = 0.10f)

/**
 * Icon and label colour for content sitting on a glass surface.
 *
 * Glass shows whatever is behind it rather than the theme's surface colour, so
 * the usual onSurface greys have nothing dependable to sit against. Pure black
 * or white off the theme's luminance is the only tint that holds against
 * arbitrary artwork, and it is what Echo's own glass nav bar uses.
 */
@Composable
fun glassContentColor(): Color =
    if (MaterialTheme.colorScheme.surface.luminance() > 0.5f) Color.Black else Color.White

/**
 * Selected-tab indicator colour for a glass surface: white in both themes.
 */
@Composable
fun glassIndicatorColor(): Color =
    if (MaterialTheme.colorScheme.surface.luminance() > 0.5f) {
        Color.White.copy(alpha = 0.5f)
    } else {
        Color.White.copy(alpha = DARK_INDICATOR_OPACITY)
    }

/** The selected tab's pill in dark theme, a step whiter than the bar. */
private const val DARK_INDICATOR_OPACITY = 0.14f

/** Compatibility entry point; uses the shared backdrop pipeline when enabled. */
@Composable
fun Modifier.lightweightLiquidGlass(
    shape: CornerBasedShape,
    fallbackColor: Color,
): Modifier {
    return if (!LocalLiquidGlassEnabled.current) clip(shape).background(fallbackColor, shape)
    else liquidGlass(shape, fallbackColor = fallbackColor)

}

/**
 * Just the rim of liquid glass — the directional [Highlight] that catches the
 * light along a glass edge — for a control that keeps its own fill.
 *
 * Over a flat, stable page the blur and lens of [liquidGlass] would sample a
 * single colour and change nothing a reader could see; the rim is the part
 * that reads as glass. No backdrop capture, so it costs a stroke. Nothing at
 * all while Liquid Glass is off or unsupported.
 */
@Composable
fun Modifier.liquidGlassRim(shape: CornerBasedShape): Modifier {
    val hardware = LocalGlassHardware.current
    val shaders = remember(hardware) { hardware && glassShadersAvailable() }
    if (!LocalLiquidGlassEnabled.current || !hardware) return this
    if (!shaders) return border(GLASS_EDGE_WIDTH, GLASS_EDGE_COLOR, shape)
    val shapeProvider = remember(shape) { ShapeProvider { shape } }
    return then(
        HighlightElement(
            shapeProvider = shapeProvider,
            highlight = { Highlight.Default },
        ),
    )
}

/**
 * Samples the page backdrop with blur/refraction where supported. Reduced effects
 * and software renderers use an opaque surface; Haze is the compatible fallback
 * when the renderer supports blur but cannot compile the refraction programs.
 */
@Composable
fun Modifier.liquidGlass(
    shape: CornerBasedShape,
    /** Drawn over the glass in place of the theme's default tint, when set. */
    surfaceTint: Color? = null,
    fallbackColor: Color = MaterialTheme.colorScheme.surface,
): Modifier {
    val reduceDynamicBlur by SettingsRepository.reduceDynamicBlur.collectAsStateWithLifecycle()
    val backdrop = LocalAppBackdrop.current
    val hardware = LocalGlassHardware.current
    val shaders = remember(hardware, backdrop != null) { hardware && backdrop != null && glassShadersAvailable() }
    val haze = LocalAppHazeState.current
    val mode = glassRenderMode(LocalLiquidGlassEnabled.current, reduceDynamicBlur, hardware, backdrop != null, shaders, haze != null)
    if (mode == GlassRenderMode.SOLID) return clip(shape).background(fallbackColor, shape).border(GLASS_EDGE_WIDTH, GLASS_EDGE_COLOR, shape)
    if (mode == GlassRenderMode.BLUR) return clip(shape).then(Modifier.hazeEffect(state = checkNotNull(haze),
        style = HazeMaterials.regular(fallbackColor))).border(GLASS_EDGE_WIDTH, GLASS_EDGE_COLOR, shape)
    val exportedBackdrop = LocalGlassExport.current
    val density = LocalDensity.current
    val blurPx = with(density) { BLUR_RADIUS_DP.dp.toPx() } * GLASS_RESOLUTION_SCALE
    val lensHeightPx = with(density) { (LENS_HEIGHT * LENS_MAX_DP).dp.toPx() } * GLASS_RESOLUTION_SCALE
    val lensAmountPx = with(density) { (LENS_AMOUNT * LENS_MAX_DP).dp.toPx() } * GLASS_RESOLUTION_SCALE
    val themeTint = if (MaterialTheme.colorScheme.surface.luminance() > 0.5f) {
        Color(0xFFFAFAFA)
    } else {
        Color(0xFF121212)
    }
    val surfaceTintColor = surfaceTint ?: themeTint.copy(alpha = SURFACE_OPACITY)

    return drawBackdrop(
        backdrop = checkNotNull(backdrop),
        shape = { shape },
        effects = {
            colorControls(saturation = 1f + 0.5f * VIBRANCY)
            blur(blurPx)
            lens(
                refractionHeight = lensHeightPx,
                refractionAmount = lensAmountPx,
                depthEffect = true,
                chromaticAberration = true,
            )
        },
        highlight = { Highlight.Default },
        shadow = { Shadow.Default },
        onDrawSurface = {
            drawRect(color = surfaceTintColor, size = size)
        },
        exportedBackdrop = exportedBackdrop,
        backdropScale = GLASS_RESOLUTION_SCALE,
    )
}
