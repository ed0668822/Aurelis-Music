// SPDX-FileCopyrightText: 2026 Aurelis
// SPDX-License-Identifier: LGPL-3.0-or-later
package com.music.aurelis.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.GraphicEq
import androidx.compose.material.icons.rounded.Headphones
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.Memory
import androidx.compose.material.icons.rounded.MusicNote
import androidx.compose.material.icons.rounded.Tune
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.music.aurelis.R
import com.music.aurelis.playback.AudioQualityInfo
import com.music.aurelis.ui.player.AudioOutputDevice
import com.music.aurelis.ui.player.outputDeviceLabel

/**
 * F048: real selected-input data, with honest unknown stages.
 *
 * BitChord's stage order is retained (track → decoder → resampler/DSP → output)
 * but its proprietary sink/decoder telemetry does not exist in Aurelis.
 * Neither an input MIME type nor an Android media route proves bit-perfect audio.
 */
@Composable
fun AudioPipelineDialog(
    onDismiss: () -> Unit,
    info: AudioQualityInfo = AudioQualityInfo(),
    fileName: String? = null,
    fileMimeType: String? = null,
    routedOutput: AudioOutputDevice? = null,
    onOpenOutput: (() -> Unit)? = null,
) {
    val unknown = stringResource(R.string.mini_quality_unknown)
    AlertDialog(
        onDismissRequest = onDismiss,
        icon = { Icon(Icons.Rounded.GraphicEq, contentDescription = null) },
        title = { Text(stringResource(R.string.audio_pipeline)) },
        text = {
            Column(Modifier.fillMaxWidth().verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(9.dp)) {
                // Keep this warning upfront, even on small landscape windows.
                Text(stringResource(R.string.mini_quality_output_unmeasured),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
                PipelineStage(Icons.Rounded.MusicNote, stringResource(R.string.pipeline_stage_file)) {
                    PipelineFact(stringResource(R.string.pipeline_file_name), fileName?.takeIf(String::isNotBlank) ?: unknown)
                    PipelineFact(stringResource(R.string.pipeline_file_mime), fileMimeType?.takeIf(String::isNotBlank) ?: unknown)
                }
                PipelineStage(Icons.Rounded.Info, stringResource(R.string.pipeline_stage_track)) {
                    PipelineFact(stringResource(R.string.mini_quality_format), info.formatLabel)
                    PipelineFact(stringResource(R.string.mini_quality_origin), info.originLabel)
                    PipelineFact(stringResource(R.string.mini_quality_rate),
                        info.sampleRateHz?.let { "$"+"it Hz" } ?: unknown)
                    PipelineFact(stringResource(R.string.mini_quality_channels),
                        info.channelCount?.toString() ?: unknown)
                    PipelineFact(stringResource(R.string.mini_quality_bitrate),
                        info.bitrateBps?.let { "$"+"{it / 1_000} kb/s" } ?: unknown)
                    PipelineFact(stringResource(R.string.mini_quality_depth),
                        info.pcmBits?.let { "$"+"it bits" } ?: unknown)
                }
                PipelineStage(Icons.Rounded.Memory, stringResource(R.string.pipeline_stage_decoder)) {
                    PipelineFact(stringResource(R.string.pipeline_decoder_format), unknown)
                    Text(stringResource(R.string.pipeline_decoder_note),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                PipelineStage(Icons.Rounded.Tune, stringResource(R.string.pipeline_stage_processing)) {
                    PipelineFact(stringResource(R.string.pipeline_dsp_result), unknown)
                    Text(stringResource(R.string.pipeline_dsp_note),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                PipelineStage(Icons.Rounded.Headphones, stringResource(R.string.pipeline_stage_output)) {
                    PipelineFact(stringResource(R.string.pipeline_route_expected),
                        routedOutput?.let { outputDeviceLabel(it) } ?: unknown)
                    Text(stringResource(R.string.pipeline_output_note),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                    if (onOpenOutput != null) {
                        TextButton(onClick = { onDismiss(); onOpenOutput() }) {
                            Text(stringResource(R.string.audio_output))
                        }
                    }
                }
                Text(stringResource(R.string.mini_quality_note),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.mini_quality_close)) }
        },
    )
}

@Composable
private fun PipelineStage(
    icon: ImageVector,
    heading: String,
    content: @Composable ColumnScope.() -> Unit,
) {
    Column(Modifier.fillMaxWidth().background(
        MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
        RoundedCornerShape(14.dp)).padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(icon, contentDescription = null, modifier = Modifier.size(18.dp),
                tint = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(Modifier.width(8.dp))
            Text(heading, style = MaterialTheme.typography.titleSmall)
        }
        content()
    }
}

@Composable
private fun PipelineFact(label: String, value: String) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.Top) {
        Text(label, style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.weight(1f))
        Spacer(Modifier.width(10.dp))
        Text(value, style = MaterialTheme.typography.bodySmall,
            textAlign = TextAlign.End, maxLines = 2, overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f))
    }
}
