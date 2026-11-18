package com.music.aurelis.ui.player

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.AbsoluteAlignment
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.Dp
import coil3.compose.AsyncImage
import com.music.aurelis.data.model.LocalSong

/** The endpoints retain layout; only their cover drawing yields to the overlay. */
@Composable
internal fun Modifier.playerDockArtwork(dock: PlayerDock?, id: String, mini: Boolean, corner: Dp = 8.dp): Modifier {
    if (dock == null) return this
    DisposableEffect(dock, id, mini) {
        onDispose { if (mini) dock.releaseMini(id) else dock.releaseExpanded(id) }
    }
    return onGloballyPositioned {
        if (mini) dock.reportMini(id, it, corner) else dock.reportExpanded(id, it, corner)
    }.graphicsLayer { alpha = if (dock.coversArtwork(id)) 0f else 1f }
}

/** One cached image with a fixed decode size. Per-frame movement stays in a layer. */
@Composable
internal fun DockedArtworkOverlay(dock: PlayerDock, song: LocalSong?) {
    if (song == null || !dock.coversArtwork(song.id)) return
    val density = LocalDensity.current
    BoxWithConstraints(Modifier.fillMaxSize().clearAndSetSemantics { }, contentAlignment = AbsoluteAlignment.TopLeft) {
        val baseSize = maxWidth.coerceIn(1.dp, 320.dp)
        val basePx = with(density) { baseSize.toPx() }
        Box(Modifier.size(baseSize).graphicsLayer {
            val pose = dock.transitionPose(song.id)
            if (pose == null) alpha = 0f else {
                val rect = pose.bounds
                val xScale = rect.width / basePx
                val yScale = rect.height / basePx
                if (!xScale.isFinite() || !yScale.isFinite() || xScale <= 0f || yScale <= 0f) {
                    alpha = 0f
                } else {
                    alpha = 1f
                    transformOrigin = TransformOrigin(0f, 0f)
                    translationX = rect.left; translationY = rect.top
                    scaleX = xScale; scaleY = yScale
                    // The layer scales its clip. Undo that scale for the source radius.
                    shape = RoundedCornerShape((pose.corner.value / xScale)
                        .coerceIn(0f, baseSize.value / 2f).dp)
                    clip = true
                }
            }
        }.background(MaterialTheme.colorScheme.surfaceVariant), contentAlignment = Alignment.Center) {
            Text(song.title.firstOrNull()?.uppercaseChar()?.toString() ?: "♫")
            AsyncImage(song.artworkUri, null, Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
        }
    }
}
