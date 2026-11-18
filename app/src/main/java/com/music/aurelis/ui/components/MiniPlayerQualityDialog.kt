// SPDX-FileCopyrightText: 2026 Aurelis
// SPDX-License-Identifier: LGPL-3.0-or-later
package com.music.aurelis.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.music.aurelis.R
import com.music.aurelis.playback.AudioQualityInfo

/** F032: inspect selected input-track information without claiming bit-perfect output. */
@Composable
fun MiniPlayerQualityDialog(info: AudioQualityInfo, onDismiss: () -> Unit) {
    val unknown = stringResource(R.string.mini_quality_unknown)
    val rows = listOf(
        stringResource(R.string.mini_quality_format) to (info.compactLabel ?: unknown),
        stringResource(R.string.mini_quality_origin) to info.originLabel,
        stringResource(R.string.mini_quality_rate) to (info.sampleRateHz?.let { "$it Hz" } ?: unknown),
        stringResource(R.string.mini_quality_channels) to (info.channelCount?.toString() ?: unknown),
        stringResource(R.string.mini_quality_bitrate) to (info.bitrateBps?.let { "${it / 1000} kb/s" } ?: unknown),
        stringResource(R.string.mini_quality_depth) to (info.pcmBits?.let { "$it bits" } ?: unknown),
    )
    AlertDialog(
        onDismissRequest = onDismiss,
        icon = { Icon(Icons.Rounded.Info, contentDescription = null) },
        title = { Text(stringResource(R.string.mini_quality_title)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(stringResource(R.string.mini_quality_input), style = MaterialTheme.typography.titleSmall)
                rows.forEach { (label, value) ->
                    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                        Text(label, Modifier.weight(1f), style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Spacer(Modifier.width(8.dp))
                        Text(value, style = MaterialTheme.typography.bodySmall, maxLines = 2,
                            textAlign = TextAlign.End)
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
