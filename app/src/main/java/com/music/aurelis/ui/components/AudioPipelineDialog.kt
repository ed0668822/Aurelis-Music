// SPDX-FileCopyrightText: 2026 Aurelis
// SPDX-License-Identifier: LGPL-3.0-or-later
package com.music.aurelis.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.InsertDriveFile
import androidx.compose.material.icons.rounded.GraphicEq
import androidx.compose.material.icons.rounded.Headphones
import androidx.compose.material.icons.rounded.Memory
import androidx.compose.material.icons.rounded.Tune
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.music.aurelis.R
import com.music.aurelis.data.SettingsRepository
import com.music.aurelis.playback.AudioQualityInfo
import com.music.aurelis.ui.player.AudioOutputDevice
import com.music.aurelis.ui.player.outputDeviceLabel

/**
 * Five-stage local audio pipeline, adapted to what Aurelis can actually
 * inspect. Input track metadata and Android's reported route are the only
 * measured facts here; decoder PCM, resampling and DAC bit-exactness are not.
 *
 * Visual stages match the BitChord hierarchy without fabricating its
 * proprietary AudioOutputStatus/NerdStats telemetry.
 */
@Composable
fun AudioPipelineDialog(
    onDismiss: () -> Unit,
    info: AudioQualityInfo = AudioQualityInfo(),
    fileName: String? = null,
    fileMimeType: String? = null,
    routedOutput: AudioOutputDevice? = null,
    onOpenOutput: (() -> Unit)? = null,
    isPlaying: Boolean = false,
) {
    val unknown = stringResource(R.string.mini_quality_unknown)
    val eqEnabled by SettingsRepository.equalizerEnabled.collectAsState()
    val loudnessEnabled by SettingsRepository.loudnessNormalization.collectAsState()
    val spatialEnabled by SettingsRepository.spatialAudio.collectAsState()
    val reduceMotion by SettingsRepository.reduceAnimation.collectAsState()
    val configuredEffects = listOfNotNull(
        if (eqEnabled) "Ecualizador" else null,
        if (loudnessEnabled) "Normalización" else null,
        if (spatialEnabled) "Audio espacial" else null,
    ).joinToString(" · ").ifBlank { "Ninguno activado en ajustes" }
    val maxDialogHeight = (LocalConfiguration.current.screenHeightDp - 48).coerceAtLeast(240).dp

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false),
    ) {
        Surface(
            modifier = Modifier.padding(horizontal = 20.dp)
                .widthIn(max = 520.dp).fillMaxWidth()
                .heightIn(max = maxDialogHeight)
                .testTag("audio-pipeline-dialog"),
            shape = RoundedCornerShape(28.dp),
            color = Color(0xFF1C1C1E),
            shadowElevation = 18.dp,
            border = androidx.compose.foundation.BorderStroke(
                0.5.dp, Color.White.copy(alpha = 0.12f)),
        ) {
            Column {
                Row(
                    Modifier.fillMaxWidth().padding(start = 20.dp, end = 18.dp, top = 18.dp, bottom = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(
                        Icons.Rounded.GraphicEq, contentDescription = null,
                        tint = Color.White.copy(alpha = 0.75f),
                        modifier = Modifier.size(25.dp),
                    )
                    Spacer(Modifier.width(12.dp))
                    Column(Modifier.weight(1f)) {
                        Text(stringResource(R.string.audio_pipeline),
                            color = Color.White, fontSize = 20.sp,
                            fontWeight = FontWeight.SemiBold)
                        Text(if (isPlaying && !reduceMotion) "Reproduciendo" else if (isPlaying) "Activo" else "En pausa",
                            color = Color.White.copy(alpha = 0.55f), fontSize = 12.sp)
                    }
                }
                Text(
                    stringResource(R.string.mini_quality_output_unmeasured),
                    modifier = Modifier.padding(horizontal = 20.dp, vertical = 9.dp),
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.White.copy(alpha = 0.66f),
                )
                HorizontalDivider(color = Color.White.copy(alpha = 0.11f))
                Column(
                    modifier = Modifier.weight(1f, fill = false)
                        .verticalScroll(rememberScrollState())
                        .padding(horizontal = 18.dp, vertical = 12.dp),
                    verticalArrangement = Arrangement.spacedBy(2.dp),
                ) {
                    PipelineStage(0, Icons.AutoMirrored.Rounded.InsertDriveFile,
                        stringResource(R.string.pipeline_stage_track)) {
                        PipelineFact(stringResource(R.string.pipeline_file_name),
                            fileName?.takeIf(String::isNotBlank) ?: unknown)
                        PipelineFact(stringResource(R.string.pipeline_file_mime),
                            fileMimeType?.takeIf(String::isNotBlank) ?: unknown)
                        PipelineFact(stringResource(R.string.mini_quality_format), info.formatLabel)
                        PipelineFact("Calidad de origen",
                            info.playbackBadgeLabel ?: info.compactLabel ?: unknown)
                        PipelineFact(stringResource(R.string.mini_quality_origin), info.originLabel)
                        PipelineFact(stringResource(R.string.mini_quality_depth),
                            info.pcmBits?.let { "$it bits" } ?: unknown)
                        PipelineFact(stringResource(R.string.mini_quality_rate),
                            info.sampleRateHz?.let { "$it Hz" } ?: unknown)
                        PipelineFact(stringResource(R.string.mini_quality_bitrate),
                            info.bitrateBps?.let { "${it / 1000} kb/s" } ?: unknown)
                        PipelineFact(stringResource(R.string.mini_quality_channels),
                            info.channelCount?.toString() ?: unknown)
                    }
                    PipelineStage(1, Icons.Rounded.Memory,
                        stringResource(R.string.pipeline_stage_decoder)) {
                        PipelineFact(stringResource(R.string.pipeline_decoder_format), unknown)
                        PipelineNote(stringResource(R.string.pipeline_decoder_note))
                    }
                    PipelineStage(2, Icons.Rounded.Tune, "Remuestreo") {
                        PipelineFact("Frecuencia de entrada",
                            info.sampleRateHz?.let { "$it Hz" } ?: unknown)
                        PipelineFact("Frecuencia de salida", unknown)
                        PipelineNote("No hay telemetría que confirme si Android remuestrea el audio.")
                    }
                    PipelineStage(3, Icons.Rounded.GraphicEq,
                        stringResource(R.string.pipeline_stage_processing)) {
                        PipelineFact("Efectos configurados", configuredEffects)
                        PipelineFact(stringResource(R.string.pipeline_dsp_result), unknown)
                        PipelineNote(stringResource(R.string.pipeline_dsp_note))
                    }
                    PipelineStage(4, Icons.Rounded.Headphones,
                        stringResource(R.string.pipeline_stage_output), isLast = true) {
                        PipelineFact(stringResource(R.string.pipeline_route_expected),
                            routedOutput?.let { outputDeviceLabel(it) }
                                ?: stringResource(R.string.pipeline_route_not_confirmed))
                        PipelineNote(stringResource(R.string.pipeline_output_note))
                        if (onOpenOutput != null) {
                            TextButton(onClick = { onDismiss(); onOpenOutput() }) {
                                Text(stringResource(R.string.audio_output), color = Color.White)
                            }
                        }
                    }
                    PipelineNote(stringResource(R.string.mini_quality_note))
                }
                HorizontalDivider(color = Color.White.copy(alpha = 0.10f))
                TextButton(
                    onClick = onDismiss,
                    modifier = Modifier.fillMaxWidth().padding(vertical = 5.dp),
                ) {
                    Text(stringResource(R.string.mini_quality_close),
                        color = Color.White, fontWeight = FontWeight.Medium)
                }
            }
        }
    }
}

@Composable
private fun PipelineStage(
    index: Int,
    icon: ImageVector,
    heading: String,
    isLast: Boolean = false,
    content: @Composable ColumnScope.() -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth().testTag("audio-pipeline-stage-$index"),
        verticalAlignment = Alignment.Top,
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Surface(
                modifier = Modifier.size(36.dp),
                shape = CircleShape,
                color = Color.White.copy(alpha = 0.09f),
                border = androidx.compose.foundation.BorderStroke(
                    0.5.dp, Color.White.copy(alpha = 0.18f)),
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(icon, contentDescription = null,
                        modifier = Modifier.size(18.dp),
                        tint = Color.White.copy(alpha = 0.77f))
                }
            }
            if (!isLast) {
                Box(Modifier.width(1.dp).height(26.dp)
                    .background(Color.White.copy(alpha = 0.24f)))
            }
        }
        Spacer(Modifier.width(13.dp))
        Column(
            modifier = Modifier.weight(1f).padding(bottom = 15.dp),
            verticalArrangement = Arrangement.spacedBy(5.dp),
        ) {
            Text(heading, color = Color.White.copy(alpha = 0.95f),
                fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
            content()
        }
    }
}

@Composable
private fun PipelineFact(label: String, value: String) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.Top) {
        Text(label, color = Color.White.copy(alpha = 0.57f),
            fontSize = 12.sp, lineHeight = 16.sp,
            modifier = Modifier.weight(1f))
        Spacer(Modifier.width(10.dp))
        Text(value, color = Color.White.copy(alpha = 0.86f),
            fontSize = 12.sp, lineHeight = 16.sp,
            textAlign = TextAlign.End, maxLines = 3,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f))
    }
}

@Composable
private fun PipelineNote(message: String) {
    Text(message, style = MaterialTheme.typography.bodySmall,
        color = Color.White.copy(alpha = 0.54f),
        modifier = Modifier.padding(top = 3.dp))
}
