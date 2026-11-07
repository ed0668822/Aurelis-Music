// Aurelis © 2026. GPL-3.0-or-later
package com.music.aurelis.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.snap
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.paneTitle
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Popup
import androidx.compose.ui.window.PopupProperties
import com.music.aurelis.ui.theme.LocalReduceMotion

/** Focusable bounded action panel. Back and outside taps dismiss the real owner. */
@Composable
fun IosMenu(onDismiss: () -> Unit, content: @Composable ColumnScope.() -> Unit) {
    val reduced = LocalReduceMotion.current
    val progress = remember { Animatable(if (reduced) 1f else 0f) }
    LaunchedEffect(reduced) { progress.animateTo(1f, if (reduced) snap() else tween(180)) }
    val configuration = LocalConfiguration.current
    val width = (configuration.screenWidthDp - 32).coerceIn(1, 320).dp
    val height = (configuration.screenHeightDp - 64).coerceIn(1, 560).dp
    val shape = RoundedCornerShape(20.dp)
    Popup(alignment = Alignment.Center, onDismissRequest = onDismiss,
        properties = PopupProperties(focusable = true, dismissOnBackPress = true,
            dismissOnClickOutside = true, usePlatformDefaultWidth = false)) {
        Surface(modifier = Modifier.width(width).heightIn(max = height).semantics { paneTitle = "Acciones" }.graphicsLayer {
                alpha = progress.value
                scaleX = if (reduced) 1f else 0.94f + 0.06f * progress.value
                scaleY = scaleX
            }.liquidGlass(shape),
            shape = shape, color = Color.Transparent) {
            Column(Modifier.verticalScroll(rememberScrollState()).padding(12.dp), content = content)
        }
    }
}
