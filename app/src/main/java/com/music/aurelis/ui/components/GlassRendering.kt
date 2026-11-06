package com.music.aurelis.ui.components

import android.view.View
import androidx.compose.runtime.*
import androidx.compose.ui.platform.LocalView
import dev.chrisbanes.haze.HazeState
import com.music.aurelis.ui.components.backdrop.internal.*

internal enum class GlassRenderMode { SOLID, BLUR, REFRACTION }
internal val LocalGlassHardware = staticCompositionLocalOf { false }
val LocalAppHazeState = staticCompositionLocalOf<HazeState?> { null }
internal val glassShaderPrograms get() = listOf(RoundedRectRefractionShaderString,
    RoundedRectRefractionWithDispersionShaderString, DefaultHighlightShaderString, AmbientHighlightShaderString)
private val compatibleRuntimeShaders: Boolean by lazy {
    glassShaderPrograms.all { source -> runCatching { android.graphics.RuntimeShader(source) }.isSuccess }
}
internal fun glassShadersAvailable(): Boolean = compatibleRuntimeShaders
internal fun glassRenderMode(enabled: Boolean, reduceBlur: Boolean, hardware: Boolean,
    backdrop: Boolean, shaders: Boolean, haze: Boolean): GlassRenderMode = when {
    !enabled || reduceBlur || !hardware -> GlassRenderMode.SOLID
    backdrop && shaders -> GlassRenderMode.REFRACTION
    haze -> GlassRenderMode.BLUR
    else -> GlassRenderMode.SOLID
}

@Composable
internal fun rememberGlassHardware(): Boolean {
    val view = LocalView.current
    var hardware by remember(view) { mutableStateOf(view.isHardwareAccelerated) }
    LaunchedEffect(view) { withFrameNanos { }; hardware = view.isHardwareAccelerated }
    DisposableEffect(view) {
        val listener = object : View.OnAttachStateChangeListener {
            override fun onViewAttachedToWindow(v: View) { hardware = v.isHardwareAccelerated }
            override fun onViewDetachedFromWindow(v: View) { hardware = false }
        }
        view.addOnAttachStateChangeListener(listener)
        onDispose { view.removeOnAttachStateChangeListener(listener) }
    }
    return hardware
}
