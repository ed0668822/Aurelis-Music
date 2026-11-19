// SPDX-FileCopyrightText: 2026 Aurelis
// SPDX-License-Identifier: LGPL-3.0-or-later
package com.music.aurelis.ui.theme

import android.app.Activity
import android.content.ContextWrapper
import android.view.Window
import android.view.WindowManager
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import android.os.Build
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.statusBarsIgnoringVisibility
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import android.view.View

/**
 * Reuse the actual window: a themed ContextWrapper is not itself an Activity,
 * and composables inside a Dialog can belong to a distinct window.
 */
internal fun findSystemBarWindow(view: View): Window? {
    var parent = view.parent
    while (parent != null) {
        if (parent is androidx.compose.ui.window.DialogWindowProvider) return parent.window
        parent = parent.parent
    }
    var context = view.context
    while (context is ContextWrapper) {
        if (context is Activity) return context.window
        context = context.baseContext
    }
    return (context as? Activity)?.window
}

/**
 * F063: override status glyph appearance only. The nav bar remains managed
 * by the page underneath. Restore the previous glyph mode on player close.
 */
@Composable
fun StatusBarIcons(dark: Boolean) {
    val view = LocalView.current
    if (view.isInEditMode) return
    val window = findSystemBarWindow(view) ?: return
    DisposableEffect(window, view) {
        val controller = WindowCompat.getInsetsController(window, view)
        val previous = controller.isAppearanceLightStatusBars
        onDispose { controller.isAppearanceLightStatusBars = previous }
    }
    SideEffect {
        WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = dark
    }
}

@Composable
fun HideStatusBar(hidden: Boolean) {
    val view = LocalView.current
    if (view.isInEditMode) return
    val window = findSystemBarWindow(view) ?: return
    DisposableEffect(window, view, hidden) {
        if (!hidden) return@DisposableEffect onDispose {}
        val controller = WindowCompat.getInsetsController(window, view)
        val oldBehavior = controller.systemBarsBehavior
        val previousCutout = window.attributes.layoutInDisplayCutoutMode
        val adjustCutout = Build.VERSION.SDK_INT >= Build.VERSION_CODES.P &&
            Build.VERSION.SDK_INT < Build.VERSION_CODES.VANILLA_ICE_CREAM &&
            previousCutout == WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_DEFAULT
        if (adjustCutout) window.attributes = window.attributes.apply {
            layoutInDisplayCutoutMode = WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES
        }
        controller.systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        controller.hide(WindowInsetsCompat.Type.statusBars())
        onDispose {
            controller.show(WindowInsetsCompat.Type.statusBars())
            controller.systemBarsBehavior = oldBehavior
            if (adjustCutout) window.attributes = window.attributes.apply {
                layoutInDisplayCutoutMode = previousCutout
            }
        }
    }
}

@Composable
fun HideSystemBars(hidden: Boolean) {
    val view = LocalView.current
    if (view.isInEditMode) return
    val window = findSystemBarWindow(view) ?: return
    DisposableEffect(window, view, hidden) {
        if (!hidden) return@DisposableEffect onDispose {}
        val controller = WindowCompat.getInsetsController(window, view)
        val behavior = controller.systemBarsBehavior
        controller.systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        controller.hide(WindowInsetsCompat.Type.systemBars())
        onDispose {
            controller.show(WindowInsetsCompat.Type.systemBars())
            controller.systemBarsBehavior = behavior
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun stableStatusBarInsets(): WindowInsets = WindowInsets.statusBarsIgnoringVisibility
