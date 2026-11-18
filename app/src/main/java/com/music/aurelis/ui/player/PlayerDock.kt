// Aurelis © 2026. GPL-3.0-or-later
// Adapted from BitChord b690dc146009d4815825fa4a20815ef2e4ee8898; original contributors, GPLv3.
package com.music.aurelis.ui.player

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.derivedStateOf
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp

@Stable
class PlayerDock {
    var sheetTravel: Float by mutableFloatStateOf(0f)
    internal var motion by mutableStateOf<PlayerSheetMotion?>(null)
    internal var attached by mutableStateOf(false)
    private var host by mutableStateOf<LayoutCoordinates?>(null)
    private var panel by mutableStateOf<LayoutCoordinates?>(null)
    private var mini by mutableStateOf<LayoutCoordinates?>(null)
    private var expanded by mutableStateOf<LayoutCoordinates?>(null)
    private var miniId by mutableStateOf<String?>(null)
    private var expandedId by mutableStateOf<String?>(null)
    private var miniCorner by mutableStateOf(8.dp)
    private var expandedCorner by mutableStateOf(0.dp)

    internal fun reportHost(coordinates: LayoutCoordinates) { host = coordinates }
    internal fun reportPanel(coordinates: LayoutCoordinates) { panel = coordinates }
    internal fun releasePanel() { panel = null }
    internal fun reportMini(id: String, coordinates: LayoutCoordinates, corner: Dp = 8.dp) {
        miniId = id; mini = coordinates; miniCorner = corner
    }
    /** A retiring layout must not discard the new copy of this song's cover. */
    internal fun releaseMini(id: String, coordinates: LayoutCoordinates?) {
        if (isCurrentDockOwner(miniId, mini, id, coordinates)) {
            mini = null; miniId = null
        }
    }
    internal fun reportExpanded(id: String, coordinates: LayoutCoordinates, corner: Dp = 0.dp) {
        expandedId = id; expanded = coordinates; expandedCorner = corner
    }
    internal fun releaseExpanded(id: String, coordinates: LayoutCoordinates?) {
        if (isCurrentDockOwner(expandedId, expanded, id, coordinates)) {
            expanded = null; expandedId = null
        }
    }
    private fun bounds(coordinates: LayoutCoordinates?, relative: LayoutCoordinates?): Rect? {
        val c = coordinates?.takeIf { it.isAttached } ?: return null
        val origin = relative?.takeIf { it.isAttached } ?: return null
        return runCatching { origin.localBoundingBoxOf(c, clipBounds = false) }
            .getOrNull()?.takeIf(::usableArtworkBounds)
    }
    internal fun openFraction(): Float = if (sheetTravel.isFinite() && sheetTravel > 0f) {
        (1f - (motion?.position() ?: sheetTravel) / sheetTravel).coerceIn(0f, 1f)
    } else 0f
    private val covers by derivedStateOf {
        attached && motion?.holding == true && openFraction() > 0f &&
            miniId != null && miniId == expandedId && mini?.isAttached == true && expanded?.isAttached == true &&
            bounds(mini, host) != null && bounds(expanded, panel) != null
    }
    internal fun coversArtwork(id: String): Boolean = covers && miniId == id
    internal fun transitionPose(id: String): ArtworkDockPose? {
        if (!coversArtwork(id)) return null
        val from = bounds(mini, host) ?: return null
        // Panel-local coordinates exclude the translating ancestor and remain stable.
        val to = bounds(expanded, panel) ?: return null
        return artworkDockPose(from, to, miniCorner, expandedCorner, openFraction())
    }

    internal fun transitionBounds(id: String): Rect? = transitionPose(id)?.bounds
}

internal data class ArtworkDockPose(val bounds: Rect, val corner: Dp)

/** Identity-based disposal ensures a stale cover never erases its replacement. */
internal fun isCurrentDockOwner(currentId: String?, currentOwner: Any?, oldId: String, oldOwner: Any?): Boolean =
    currentId == oldId && currentOwner != null && currentOwner === oldOwner

/** Interpolate frame and the actual mini/expanded artwork corner as one pose. */
internal fun artworkDockPose(
    from: Rect, to: Rect, miniCorner: Dp, expandedCorner: Dp, fraction: Float,
): ArtworkDockPose? {
    if (!fraction.isFinite() || !miniCorner.value.isFinite() || !expandedCorner.value.isFinite() ||
        miniCorner.value < 0f || expandedCorner.value < 0f) return null
    val bounds = artworkBoundsBetween(from, to, fraction) ?: return null
    val t = fraction.coerceIn(0f, 1f)
    val radius = miniCorner.value + (expandedCorner.value - miniCorner.value) * t
    return ArtworkDockPose(bounds, radius.dp)
}

internal fun usableArtworkBounds(rect: Rect): Boolean = rect.left.isFinite() && rect.top.isFinite() &&
    rect.right.isFinite() && rect.bottom.isFinite() && rect.width.isFinite() && rect.height.isFinite() &&
    rect.width > 0f && rect.height > 0f

internal fun artworkBoundsBetween(from: Rect, to: Rect, fraction: Float): Rect? {
    if (!usableArtworkBounds(from) || !usableArtworkBounds(to) || !fraction.isFinite()) return null
    val f = fraction.coerceIn(0f, 1f)
    fun between(a: Float, b: Float) = a + (b - a) * f
    return Rect(between(from.left, to.left), between(from.top, to.top),
        between(from.right, to.right), between(from.bottom, to.bottom)).takeIf(::usableArtworkBounds)
}

@Composable
fun PlayerDockBar(
    title: String,
    artist: String,
    isPlaying: Boolean,
    onPlayPauseClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .height(72.dp)
            .padding(horizontal = 8.dp, vertical = 4.dp)
            .clip(RoundedCornerShape(20.dp)),
        color = MaterialTheme.colorScheme.surfaceContainerHighest,
        tonalElevation = 6.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1
                )
                Text(
                    text = artist,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1
                )
            }
            IconButton(onClick = onPlayPauseClick) {
                Icon(
                    imageVector = if (isPlaying) Icons.Filled.Pause else Icons.Filled.PlayArrow,
                    contentDescription = if (isPlaying) "Pause" else "Play",
                    tint = MaterialTheme.colorScheme.primary
                )
            }
        }
    }
}
