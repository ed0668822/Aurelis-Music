// Aurelis © 2026. GPL-3.0-or-later
// Portrait HeroCard adapted from BitChord b690dc146009d4815825fa4a20815ef2e4ee8898 (GPLv3).
// Original BitChord contributors and terms retained; local callbacks and legacy layout preserved.
package com.music.aurelis.ui.components.home

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import com.music.aurelis.R
import com.music.aurelis.ui.components.HERO_CARD_RATIO
import com.music.aurelis.ui.components.SearchPlayingBars
import com.music.aurelis.ui.components.heldContextMenuInteractions
import coil3.compose.AsyncImage
import com.music.aurelis.data.model.LocalSong

/**
 * Tarjeta principal del Home: la canción más reciente con su artwork
 * difuminado de fondo, gradiente oscuro para legibilidad y botón de
 * reproducción circular en la variante legacy. El Home activo usa la variante
 * portrait adaptada del HeroCard original.
 */
@Composable
fun HomeHero(
    song: LocalSong?,
    onPlayClick: () -> Unit,
    modifier: Modifier = Modifier,
    eyebrow: String = stringResource(R.string.home_listen),
    portrait: Boolean = false,
    isPlaying: Boolean = false,
    onLongPress: (() -> Unit)? = null,
) {
    if (portrait) {
        val shape = RoundedCornerShape(18.dp)
        Box(modifier.fillMaxWidth().aspectRatio(HERO_CARD_RATIO).clip(shape)
            .border(1.dp, MaterialTheme.colorScheme.onSurface.copy(alpha = 0.15f), shape)
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .then(if (onLongPress != null) Modifier.heldContextMenuInteractions(onPlayClick, onLongPress)
                else Modifier.clickable(onClick = onPlayClick))
            .semantics { role = Role.Button; stateDescription = eyebrow }
            .testTag("home-leading-hero")) {
            if (song?.artworkUri != null) AsyncImage(song.artworkUri, null,
                contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize())
            if (isPlaying) SearchPlayingBars(Modifier.align(Alignment.Center))
            Column(Modifier.fillMaxWidth().align(Alignment.BottomStart)
                .background(Brush.verticalGradient(listOf(Color.Transparent, Color.Black.copy(alpha = 0.78f))))
                .padding(start = 16.dp, end = 16.dp, top = 34.dp, bottom = 14.dp)) {
                Text(song?.title ?: "Aurelis Music", style = MaterialTheme.typography.titleLarge,
                    color = Color.White, maxLines = 1, overflow = TextOverflow.Ellipsis)
                val subtitle = song?.artist.orEmpty()
                if (subtitle.isNotBlank()) Text(subtitle, style = MaterialTheme.typography.bodyMedium,
                    color = Color.White.copy(alpha = 0.72f), maxLines = 2, overflow = TextOverflow.Ellipsis)
            }
        }
        return
    }
    val shape = RoundedCornerShape(24.dp)

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(200.dp)
            .clip(shape)
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .clickable(onClick = onPlayClick),
    ) {
        if (song?.artworkUri != null) {
            AsyncImage(
                model = song.artworkUri,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize(),
            )
        }

        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            Color.Black.copy(alpha = 0.20f),
                            Color.Black.copy(alpha = 0.85f),
                        ),
                    ),
                ),
        )

        Column(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .padding(20.dp)
                .fillMaxWidth(0.75f),
        ) {
            Text(
                text = eyebrow,
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Bold,
                color = Color.White.copy(alpha = 0.75f),
            )
            Text(
                text = song?.title ?: "Aurelis Music",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                color = Color.White,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.padding(top = 4.dp),
            )
            Text(
                text = song?.artist ?: stringResource(R.string.home_local_music),
                style = MaterialTheme.typography.bodyMedium,
                color = Color.White.copy(alpha = 0.70f),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.padding(top = 2.dp),
            )
        }

        Box(
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(20.dp)
                .size(56.dp)
                .clip(CircleShape)
                .background(Color.White)
                .clickable(onClick = onPlayClick),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = Icons.Filled.PlayArrow,
                contentDescription = stringResource(R.string.player_play),
                tint = Color.Black,
                modifier = Modifier.size(28.dp),
            )
        }
    }
}
