// Aurelis © 2026. GPL-3.0-or-later
package com.music.aurelis.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.window.DialogProperties
import com.music.aurelis.R
import com.music.aurelis.data.LocalUserLibrary
import com.music.aurelis.data.model.LocalSong
import androidx.compose.foundation.clickable
import androidx.compose.foundation.background
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.Alignment
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.layout.ContentScale
import coil3.compose.AsyncImage
import com.music.aurelis.data.ProfileRepository
import com.music.aurelis.ui.components.FrostedTopBar
import com.music.aurelis.ui.components.topBarContentPadding
import com.music.aurelis.ui.replay.*
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.ui.platform.testTag
import kotlinx.coroutines.launch

@Composable
fun ReplayScreen(onBack: () -> Unit = {}, store: LocalUserLibrary, songs: List<LocalSong>, onPlay: (LocalSong) -> Unit) {
    val user by store.state.collectAsState()
    var confirm by remember { mutableStateOf(false) }
    val profile by ProfileRepository.state.collectAsState()
    val summary = remember(user.listening, songs) { localReplaySummary(user.listening, songs) }
    val list = rememberLazyListState()
    val scope = rememberCoroutineScope()
    Box(Modifier.fillMaxSize().testTag("replay-page")) {
        LazyColumn(Modifier.fillMaxSize(), state = list, contentPadding = PaddingValues(top = topBarContentPadding(), bottom = 160.dp)) {
            item {
                Column(Modifier.padding(horizontal = 20.dp, vertical = 16.dp)) {
                    Text(stringResource(R.string.replay), style = MaterialTheme.typography.displayLarge, fontWeight = FontWeight.ExtraBold)
                    Text(stringResource(R.string.all_time), style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
            if (user.readOnly) item { Text(stringResource(R.string.replay_history_unavailable), Modifier.padding(20.dp)) }
            if (user.listening.isEmpty()) item { Text(stringResource(R.string.navigation_replay_empty), Modifier.padding(20.dp)) }
            else {
                item {
                    LazyRow(contentPadding = PaddingValues(horizontal = 20.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        item { ReplayCreditCard(stringResource(R.string.minutes_listened), formatMinutes(summary.totalMs),
                            stringResource(R.string.replay_plays_total, summary.totalPlays), profile.profile.name, null,
                            { scope.launch { list.animateScrollToItem(if (user.readOnly) 4 else 3) } }, Modifier.width(300.dp)) }
                        summary.rows.firstOrNull()?.let { top -> item {
                            ReplayCreditCard(stringResource(R.string.top_songs), top.title,
                                top.subtitle, profile.profile.name, null, { songs.firstOrNull { it.id == top.key }?.let(onPlay) }, Modifier.width(300.dp))
                        } }
                    }
                }
                item {
                    TextButton(onClick = { summary.rows.firstNotNullOfOrNull { row -> songs.firstOrNull { it.id == row.key } }?.let(onPlay) },
                        enabled = summary.rows.any { row -> songs.any { it.id == row.key } }, modifier = Modifier.padding(horizontal = 20.dp, vertical = 12.dp).fillMaxWidth()) {
                        Text(stringResource(R.string.replay_play_top))
                    }
                }
                item { Text(stringResource(R.string.top_songs), Modifier.padding(20.dp), style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold) }
                items(summary.rows, key = { it.key }) { row ->
                    val song = songs.firstOrNull { it.id == row.key }
                    Row(Modifier.fillMaxWidth().clickable(enabled = song != null) { song?.let(onPlay) }.padding(horizontal = 20.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                        Text(row.rank.toString(), Modifier.width(28.dp), color = MaterialTheme.colorScheme.primary)
                        AsyncImage(row.artworkUri, null, Modifier.size(48.dp).clip(RoundedCornerShape(8.dp)).background(MaterialTheme.colorScheme.surfaceVariant), contentScale = ContentScale.Crop)
                        Column(Modifier.weight(1f).padding(horizontal = 12.dp)) {
                            Text(if (song == null) stringResource(R.string.replay_song_unavailable) else row.title, maxLines = 1, overflow = TextOverflow.Ellipsis, fontWeight = FontWeight.SemiBold)
                            Text(stringResource(R.string.replay_row_minutes, row.subtitle.orEmpty(), row.ms / 60_000), maxLines = 1, overflow = TextOverflow.Ellipsis, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        Text(row.plays.toString(), style = MaterialTheme.typography.labelMedium)
                    }
                }
                item { TextButton(onClick = { confirm = true }, enabled = !user.readOnly, modifier = Modifier.padding(20.dp)) { Text(stringResource(R.string.replay_clear)) } }
            }
        }
        FrostedTopBar(stringResource(R.string.replay), { false }, onBack = onBack, transparentBackdrop = true)
    }
    if (confirm) AlertDialog(modifier = Modifier.width(320.dp), properties = DialogProperties(usePlatformDefaultWidth = false),
        onDismissRequest = { confirm = false }, title = { Text(stringResource(R.string.replay_clear_confirm)) },
        text = { Text(stringResource(R.string.replay_clear_hint)) },
        confirmButton = { TextButton(onClick = { store.clearHistory(); confirm = false }) { Text(stringResource(R.string.replay_clear)) } },
        dismissButton = { TextButton(onClick = { confirm = false }) { Text(stringResource(R.string.cancel)) } })
}
