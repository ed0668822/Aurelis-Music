// Aurelis © 2026. GPL-3.0-or-later
package com.music.aurelis.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.music.aurelis.R
import com.music.aurelis.data.SettingsRepository
import com.music.aurelis.data.lyrics.LyricsProviderRegistry
import com.music.aurelis.data.lyrics.LyricsSource
import com.music.aurelis.playback.LyricsLookupState

@Composable
fun LyricsSourcesDialog(
    onDismiss: () -> Unit,
    currentSource: LyricsSource? = null,
    lookupState: LyricsLookupState = LyricsLookupState.Idle,
    onSearch: (() -> Unit)? = null,
) {
    val selected by SettingsRepository.lyricsSources.collectAsState()
    val sourceOrder by SettingsRepository.lyricsSourceOrder.collectAsState()
    val ordered = (sourceOrder + LyricsProviderRegistry.providers.map { it.source }).distinct()
    AlertDialog(
        modifier = Modifier.testTag("lyrics-sources-dialog"),
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.lyrics_sources_title)) },
        text = {
            Column(Modifier.heightIn(max = 400.dp).verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(stringResource(R.string.lyrics_sources_privacy))
                currentSource?.let { Text(stringResource(R.string.lyrics_source_current, lyricsSourceTitle(it))) }
                Text("Orden de búsqueda: usa ↑ y ↓ para cambiar prioridad. Los proveedores sin verificar permanecen inactivos.",
                    style = MaterialTheme.typography.bodySmall)
                TextButton(onClick = SettingsRepository::resetLyricsSourceSettings) {
                    Text("Restablecer proveedores")
                }
                ordered.forEach { source ->
                    val provider = LyricsProviderRegistry.providers.firstOrNull { it.source == source }
                        ?: return@forEach
                    if (provider.availability == LyricsProviderRegistry.Availability.AVAILABLE) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Column(Modifier.weight(1f)) {
                                Text(provider.source.label, style = MaterialTheme.typography.titleSmall)
                                Text(stringResource(R.string.lyrics_lrclib_detail), style = MaterialTheme.typography.bodySmall)
                            }
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                TextButton(
                                    enabled = ordered.indexOf(source) > 0,
                                    modifier = Modifier.testTag("lyrics-priority-up-${source.name}"),
                                    onClick = {
                                        val rank = LyricsSource.ordered(sourceOrder).toMutableList()
                                        val index = rank.indexOf(source)
                                        if (index > 0) {
                                            java.util.Collections.swap(rank, index, index - 1)
                                            SettingsRepository.setLyricsSourceOrder(rank)
                                        }
                                    },
                                ) { Text("↑") }
                                TextButton(
                                    enabled = ordered.indexOf(source) < ordered.lastIndex,
                                    modifier = Modifier.testTag("lyrics-priority-down-${source.name}"),
                                    onClick = {
                                        val rank = LyricsSource.ordered(sourceOrder).toMutableList()
                                        val index = rank.indexOf(source)
                                        if (index >= 0 && index < rank.lastIndex) {
                                            java.util.Collections.swap(rank, index, index + 1)
                                            SettingsRepository.setLyricsSourceOrder(rank)
                                        }
                                    },
                                ) { Text("↓") }
                                Switch(checked = source in selected,
                                    modifier = Modifier.testTag("lyrics-provider-${source.name}")
                                        .semantics { contentDescription = source.label },
                                    onCheckedChange = { enabled ->
                                        SettingsRepository.setLyricsSources(
                                            if (enabled) selected + source else selected - source)
                                    })
                            }
                        }
                    } else {
                        Column(Modifier.testTag("lyrics-provider-${provider.source.name}-disabled")) {
                            Text(provider.source.label, style = MaterialTheme.typography.titleSmall)
                            Text(stringResource(R.string.lyrics_provider_unverified), style = MaterialTheme.typography.bodySmall)
                        }
                    }
                }
                when (lookupState) {
                    LyricsLookupState.Loading -> Text(stringResource(R.string.lyrics_lookup_loading))
                    LyricsLookupState.NotFound -> Text(stringResource(R.string.lyrics_lookup_not_found))
                    is LyricsLookupState.Failed -> Text(lyricsFailureText(lookupState.failure))
                    is LyricsLookupState.Complete -> Text(stringResource(R.string.lyrics_source_current, lookupState.source.label))
                    else -> Unit
                }
            }
        },
        confirmButton = {
            if (onSearch != null) TextButton(onClick = onSearch,
                enabled = selected.any(LyricsProviderRegistry::isAvailable) &&
                    lookupState !is LyricsLookupState.Loading) {
                Text(stringResource(R.string.lyrics_search_lrclib))
            }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.lyrics_sources_close)) } },
    )
}
