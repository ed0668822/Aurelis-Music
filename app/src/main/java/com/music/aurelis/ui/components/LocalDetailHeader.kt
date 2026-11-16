// Aurelis © 2026. GPL-3.0-or-later
// Responsive sleeve/header arrangement adapted from BitChord b690dc1 DetailScreen.kt (GPLv3).
package com.music.aurelis.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import com.music.aurelis.R
import com.music.aurelis.ui.haptics.Haptic
import com.music.aurelis.ui.haptics.rememberHaptics
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import com.music.aurelis.ui.icons.AurelisIcons

/** Header is an item of its page's lazy list, never a fixed sibling of the tracks. */
@Composable
internal fun LocalDetailHeader(title: String, subtitle: String, metadata: String,
    sleeveSize: Dp, artwork: String?, placeholder: ImageVector,
    onPlay: () -> Unit, onShuffle: () -> Unit, canPlay: Boolean) {
    val shape = RoundedCornerShape(12.dp)
    Column(Modifier.fillMaxWidth().padding(top = topBarContentPadding(), bottom = 18.dp),
        horizontalAlignment = Alignment.CenterHorizontally) {
        Box(Modifier.size(sleeveSize).shadow(18.dp, shape)
            .background(MaterialTheme.colorScheme.surfaceVariant, shape)
            .border(0.5.dp, MaterialTheme.colorScheme.onBackground.copy(alpha = 0.14f), shape),
            contentAlignment = Alignment.Center) {
            Icon(placeholder, null, Modifier.size(sleeveSize * 0.3f), tint = MaterialTheme.colorScheme.onSurfaceVariant)
            if (artwork != null) AsyncImage(artwork, null, Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
        }
        Spacer(Modifier.height(20.dp))
        Text(title, Modifier.padding(horizontal = 24.dp), style = MaterialTheme.typography.headlineMedium,
            textAlign = TextAlign.Center, maxLines = 2, overflow = TextOverflow.Ellipsis)
        if (subtitle.isNotBlank()) Text(subtitle, Modifier.padding(horizontal = 24.dp),
            style = MaterialTheme.typography.titleLarge.copy(fontSize = 19.sp, fontWeight = FontWeight.Normal), color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center, maxLines = 2, overflow = TextOverflow.Ellipsis)
        Text(metadata, Modifier.padding(horizontal = 24.dp, vertical = 4.dp),
            style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        if (canPlay) Row(Modifier.widthIn(max = 420.dp).fillMaxWidth().padding(horizontal = 24.dp, vertical = 18.dp),
            horizontalArrangement = Arrangement.spacedBy(14.dp)) {
            LocalActionPill(AurelisIcons.Play, stringResource(R.string.collection_play), onPlay, true, Modifier.weight(1f))
            LocalActionPill(AurelisIcons.Shuffle, stringResource(R.string.shuffle), onShuffle, false, Modifier.weight(1f))
        }
    }
}

/** BitChord DetailScreen.ActionPill geometry; safe draw-only surface inside the backdrop. */
@Composable
private fun LocalActionPill(icon: ImageVector, label: String, onClick: () -> Unit, filled: Boolean, modifier: Modifier) {
    val haptics = rememberHaptics()
    val ink = if (filled) Color.Black else MaterialTheme.colorScheme.onSurface
    Row(modifier.height(50.dp).clip(CircleShape)
        .background(if (filled) Color.White else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.07f))
        .then(if (filled) Modifier else Modifier.border(1.dp, MaterialTheme.colorScheme.onSurface.copy(alpha = 0.10f), CircleShape))
        .clickable(role = Role.Button) { haptics.play(Haptic.Resume); onClick() }
        .padding(horizontal = 16.dp), horizontalArrangement = Arrangement.Center, verticalAlignment = Alignment.CenterVertically) {
        Icon(icon, null, Modifier.size(18.dp), tint = ink)
        Spacer(Modifier.width(8.dp))
        Text(label, style = MaterialTheme.typography.titleMedium, color = ink, maxLines = 1, overflow = TextOverflow.Ellipsis)
    }
}
