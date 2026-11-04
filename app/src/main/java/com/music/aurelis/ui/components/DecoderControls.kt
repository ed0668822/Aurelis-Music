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
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.music.aurelis.data.settings.DecoderFormat

/**
 * DecoderControls es un panel de controles para los decodificadores de Aurelis Music.
 *
 * Permite ajustar el formato de decodificación (DSD, FFmpeg, etc.).
 */
@Composable
fun DecoderControls(
    decoderFormat: DecoderFormat?,
    onDecoderFormatChange: (DecoderFormat) -> Unit,
    modifier: Modifier = Modifier,
) {
    var useDsd by remember { mutableStateOf(decoderFormat == DecoderFormat.DSD) }
    var useFfmpeg by remember { mutableStateOf(decoderFormat == DecoderFormat.FFMPEG) }
    var sampleRate by remember { mutableStateOf(decoderFormat?.sampleRate ?: 48000) }
    var channels by remember { mutableStateOf(decoderFormat?.channels ?: 2) }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(MaterialTheme.colorScheme.surfaceContainerHigh)
            .padding(16.dp),
    ) {
        Text(
            text = "Decodificadores",
            style = MaterialTheme.typography.titleLarge,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.padding(bottom = 16.dp),
        )

        // DSD
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                imageVector = androidx.compose.material.icons.Icons.Default.Favorite,
                contentDescription = "DSD",
                tint = if (useDsd) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(24.dp),
            )
            Spacer(Modifier.width(12.dp))
            Text(
                text = "DSD",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.weight(1f),
            )
            Switch(
                checked = useDsd,
                onCheckedChange = {
                    useDsd = it
                    onDecoderFormatChange(
                        if (it) DecoderFormat.DSD else DecoderFormat.FFMPEG,
                    )
                },
            )
        }

        // FFmpeg
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                imageVector = androidx.compose.material.icons.Icons.Default.FastForward,
                contentDescription = "FFmpeg",
                tint = if (useFfmpeg) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(24.dp),
            )
            Spacer(Modifier.width(12.dp))
            Text(
                text = "FFmpeg",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.weight(1f),
            )
            Switch(
                checked = useFfmpeg,
                onCheckedChange = {
                    useFfmpeg = it
                    onDecoderFormatChange(
                        if (it) DecoderFormat.FFMPEG else DecoderFormat.DSD,
                    )
                },
            )
        }

        Spacer(Modifier.height(16.dp))

        // Tasa de muestreo
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                imageVector = androidx.compose.material.icons.Icons.Default.Speed,
                contentDescription = "Tasa de muestreo",
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(24.dp),
            )
            Spacer(Modifier.width(12.dp))
            Text(
                text = "Tasa de muestreo",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.weight(1f),
            )
            Text(
                text = "$sampleRate Hz",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.primary,
            )
        }
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            listOf(44100, 48000, 96000, 192000).forEach { rate ->
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(
                            color = if (sampleRate == rate) {
                                MaterialTheme.colorScheme.primaryContainer
                            } else {
                                MaterialTheme.colorScheme.surfaceVariant
                            },
                        )
                        .clickable {
                            sampleRate = rate
                            onDecoderFormatChange(decoderFormat?.copy(sampleRate = rate) ?: DecoderFormat.FFMPEG)
                        }
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                ) {
                    Text(
                        text = "$rate Hz",
                        style = MaterialTheme.typography.bodySmall,
                        color = if (sampleRate == rate) {
                            MaterialTheme.colorScheme.onPrimaryContainer
                        } else {
                            MaterialTheme.colorScheme.onSurfaceVariant
                        },
                    )
                }
            }
        }

        Spacer(Modifier.height(16.dp))

        // Canales
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                imageVector = androidx.compose.material.icons.Icons.Default.SurroundSound,
                contentDescription = "Canales",
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(24.dp),
            )
            Spacer(Modifier.width(12.dp))
            Text(
                text = "Canales",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.weight(1f),
            )
            Text(
                text = "$channels",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.primary,
            )
        }
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            listOf(1, 2, 4, 6).forEach { channelCount ->
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(
                            color = if (channels == channelCount) {
                                MaterialTheme.colorScheme.primaryContainer
                            } else {
                                MaterialTheme.colorScheme.surfaceVariant
                            },
                        )
                        .clickable {
                            channels = channelCount
                            onDecoderFormatChange(decoderFormat?.copy(channels = channelCount) ?: DecoderFormat.FFMPEG)
                        }
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                ) {
                    Text(
                        text = "$channelCount",
                        style = MaterialTheme.typography.bodySmall,
                        color = if (channels == channelCount) {
                            MaterialTheme.colorScheme.onPrimaryContainer
                        } else {
                            MaterialTheme.colorScheme.onSurfaceVariant
                        },
                    )
                }
            }
        }
    }
}