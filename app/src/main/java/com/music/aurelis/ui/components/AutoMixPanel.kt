// Aurelis © 2026. GPL-3.0-or-later
package com.music.aurelis.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.music.aurelis.data.settings.MixBlend
import com.music.aurelis.ui.haptics.Haptic
import com.music.aurelis.ui.haptics.rememberHaptics

/**
 * AutoMixPanel es un panel de controles para el AutoMix de Aurelis Music.
 *
 * Permite ajustar los parámetros de mezcla (volumen, crossfade, etc.).
 */
@Composable
fun AutoMixPanel(
    mixBlend: MixBlend?,
    onMixBlendChange: (MixBlend) -> Unit,
    modifier: Modifier = Modifier,
) {
    val haptics = rememberHaptics()
    var volume by remember { mutableFloatStateOf(mixBlend?.volume ?: 0.5f) }
    var crossfade by remember { mutableFloatStateOf(mixBlend?.crossfade ?: 0.5f) }
    var intensity by remember { mutableFloatStateOf(mixBlend?.intensity ?: 0.5f) }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(MaterialTheme.colorScheme.surfaceContainerHigh)
            .padding(16.dp),
    ) {
        Text(
            text = "AutoMix",
            style = MaterialTheme.typography.titleLarge,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.padding(bottom = 16.dp),
        )

        // Volumen
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                imageVector = androidx.compose.material.icons.Icons.Default.VolumeUp,
                contentDescription = "Volumen",
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(24.dp),
            )
            Spacer(Modifier.width(12.dp))
            Text(
                text = "Volumen",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.weight(1f),
            )
            Text(
                text = "${(volume * 100).toInt()}%",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.primary,
            )
        }
        Slider(
            value = volume,
            onValueChange = {
                volume = it
                onMixBlendChange(mixBlend?.copy(volume = it) ?: MixBlend())
                haptics.play(Haptic.Select)
            },
            valueRange = 0f..1f,
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp),
            colors = SliderDefaults.colors(
                thumbColor = MaterialTheme.colorScheme.primary,
                activeTrackColor = MaterialTheme.colorScheme.primary,
                inactiveTrackColor = MaterialTheme.colorScheme.surfaceVariant,
            ),
        )

        // Crossfade
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                imageVector = androidx.compose.material.icons.Icons.Default.FastForward,
                contentDescription = "Crossfade",
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(24.dp),
            )
            Spacer(Modifier.width(12.dp))
            Text(
                text = "Crossfade",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.weight(1f),
            )
            Text(
                text = "${(crossfade * 100).toInt()}%",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.primary,
            )
        }
        Slider(
            value = crossfade,
            onValueChange = {
                crossfade = it
                onMixBlendChange(mixBlend?.copy(crossfade = it) ?: MixBlend())
                haptics.play(Haptic.Select)
            },
            valueRange = 0f..1f,
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp),
            colors = SliderDefaults.colors(
                thumbColor = MaterialTheme.colorScheme.primary,
                activeTrackColor = MaterialTheme.colorScheme.primary,
                inactiveTrackColor = MaterialTheme.colorScheme.surfaceVariant,
            ),
        )

        // Intensity
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                imageVector = androidx.compose.material.icons.Icons.Default.Favorite,
                contentDescription = "Intensidad",
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(24.dp),
            )
            Spacer(Modifier.width(12.dp))
            Text(
                text = "Intensidad",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.weight(1f),
            )
            Text(
                text = "${(intensity * 100).toInt()}%",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.primary,
            )
        }
        Slider(
            value = intensity,
            onValueChange = {
                intensity = it
                onMixBlendChange(mixBlend?.copy(intensity = it) ?: MixBlend())
                haptics.play(Haptic.Select)
            },
            valueRange = 0f..1f,
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp),
            colors = SliderDefaults.colors(
                thumbColor = MaterialTheme.colorScheme.primary,
                activeTrackColor = MaterialTheme.colorScheme.primary,
                inactiveTrackColor = MaterialTheme.colorScheme.surfaceVariant,
            ),
        )

        Spacer(Modifier.height(16.dp))

        // Botón de activar/desactivar AutoMix
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .background(
                    color = if (mixBlend?.enabled == true) {
                        MaterialTheme.colorScheme.primaryContainer
                    } else {
                        MaterialTheme.colorScheme.surfaceVariant
                    },
                )
                .padding(16.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(
                    modifier = Modifier.weight(1f),
                ) {
                    Text(
                        text = if (mixBlend?.enabled == true) "AutoMix activado" else "AutoMix desactivado",
                        style = MaterialTheme.typography.bodyLarge,
                        color = if (mixBlend?.enabled == true) {
                            MaterialTheme.colorScheme.onPrimaryContainer
                        } else {
                            MaterialTheme.colorScheme.onSurfaceVariant
                        },
                    )
                    Text(
                        text = if (mixBlend?.enabled == true) "Mezcla automática de pistas" else "Mezcla manual",
                        style = MaterialTheme.typography.bodySmall,
                        color = if (mixBlend?.enabled == true) {
                            MaterialTheme.colorScheme.onPrimaryContainer
                        } else {
                            MaterialTheme.colorScheme.onSurfaceVariant
                        },
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
                Icon(
                    imageVector = if (mixBlend?.enabled == true) {
                        androidx.compose.material.icons.Icons.Default.Check
                    } else {
                        androidx.compose.material.icons.Icons.Default.Close
                    },
                    contentDescription = null,
                    tint = if (mixBlend?.enabled == true) {
                        MaterialTheme.colorScheme.primary
                    } else {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    },
                    modifier = Modifier.size(24.dp),
                )
            }
        }
    }
}