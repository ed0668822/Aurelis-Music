// Aurelis © 2026. GPL-3.0-or-later
package com.music.aurelis.ui.theme

import androidx.compose.foundation.shape.CornerBasedShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import com.music.aurelis.ui.components.liquidGlass as componentGlass
import com.music.aurelis.ui.components.liquidGlassRim as componentRim
import com.music.aurelis.ui.components.lightweightLiquidGlass as componentSurface

/** Compatibility imports share the active renderer/state; no second visual implementation. */
val LocalLiquidGlassEnabled = com.music.aurelis.ui.components.LocalLiquidGlassEnabled
val LocalAppBackdrop = com.music.aurelis.ui.components.LocalAppBackdrop
internal val LocalGlassExport = com.music.aurelis.ui.components.LocalGlassExport
internal val GLASS_EDGE_WIDTH = com.music.aurelis.ui.components.GLASS_EDGE_WIDTH
internal val GLASS_EDGE_COLOR = com.music.aurelis.ui.components.GLASS_EDGE_COLOR
fun isGlassSupported() = com.music.aurelis.ui.components.isGlassSupported()
@Composable fun glassContentColor() = com.music.aurelis.ui.components.glassContentColor()
@Composable fun glassIndicatorColor() = com.music.aurelis.ui.components.glassIndicatorColor()
@Composable fun Modifier.lightweightLiquidGlass(shape: CornerBasedShape, fallbackColor: Color): Modifier = componentSurface(shape, fallbackColor)
@Composable fun Modifier.liquidGlassRim(shape: CornerBasedShape): Modifier = componentRim(shape)
@Composable fun Modifier.liquidGlass(shape: CornerBasedShape, surfaceTint: Color? = null): Modifier = componentGlass(shape, surfaceTint)
