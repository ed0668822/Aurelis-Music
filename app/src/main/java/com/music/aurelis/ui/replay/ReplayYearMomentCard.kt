// SPDX-FileCopyrightText: 2026 Aurelis
// SPDX-License-Identifier: LGPL-3.0-or-later
package com.music.aurelis.ui.replay

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.IosShare
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import com.music.aurelis.R
import com.music.aurelis.data.model.CARD_ART_PX
import com.music.aurelis.data.model.artworkAt
import com.music.aurelis.data.stats.ReplaySummary
import kotlinx.coroutines.delay
import java.time.LocalDate

/** A unique page per annual fact; backed only by observed local listening data. */
internal fun ReplayYearMoment.storyPage(): ReplayStoryPage = when (this) {
    ReplayYearMoment.OPENER -> ReplayStoryPage.INTRO
    ReplayYearMoment.MINUTES -> ReplayStoryPage.MINUTES
    ReplayYearMoment.BUSIEST_DAY, ReplayYearMoment.LISTENING_CLOCK -> ReplayStoryPage.HABITS
    ReplayYearMoment.GENRES -> ReplayStoryPage.GENRES
    ReplayYearMoment.ARTIST_REVEAL, ReplayYearMoment.TOP_ARTISTS -> ReplayStoryPage.ARTISTS
    ReplayYearMoment.SONG_REVEAL, ReplayYearMoment.TOP_SONGS -> ReplayStoryPage.SONGS
    ReplayYearMoment.ALBUMS -> ReplayStoryPage.ALBUMS
    ReplayYearMoment.SUMMARY -> ReplayStoryPage.SUMMARY
}

@Composable
internal fun ReplayYearMomentCard(
    moment: ReplayYearMoment,
    summary: ReplaySummary,
    visible: Boolean,
    onShare: () -> Unit,
) {
    val annual = remember(summary) { summary.annualHighlights(LocalDate.now().year) }
    val personality = annual.personality?.let {
        when (it) {
            AnnualListeningPersonality.NIGHT_OWL -> R.string.replay_personality_night
            AnnualListeningPersonality.EARLY_BIRD -> R.string.replay_personality_early
            AnnualListeningPersonality.GOLDEN_HOUR -> R.string.replay_personality_evening
            AnnualListeningPersonality.DAYTIME -> R.string.replay_personality_day
            AnnualListeningPersonality.ALL_DAY -> R.string.replay_personality_all
        }
    }
    val heading = when (moment) {
        ReplayYearMoment.OPENER, ReplayYearMoment.BUSIEST_DAY, ReplayYearMoment.SUMMARY -> R.string.this_year
        ReplayYearMoment.MINUTES -> R.string.minutes_listened
        ReplayYearMoment.LISTENING_CLOCK -> R.string.replay_annual_personality
        ReplayYearMoment.GENRES -> R.string.top_genres
        ReplayYearMoment.ARTIST_REVEAL -> R.string.top_artist
        ReplayYearMoment.TOP_ARTISTS -> R.string.top_artists
        ReplayYearMoment.SONG_REVEAL -> R.string.top_song
        ReplayYearMoment.TOP_SONGS -> R.string.top_songs
        ReplayYearMoment.ALBUMS -> R.string.top_albums
    }
    val highlight = when (moment) {
        ReplayYearMoment.OPENER -> "Replay " + annual.year
        ReplayYearMoment.MINUTES -> annual.minutes.toString()
        ReplayYearMoment.BUSIEST_DAY -> annual.busiestDay.orEmpty()
        ReplayYearMoment.LISTENING_CLOCK -> personality?.let { stringResource(it) }.orEmpty()
        ReplayYearMoment.GENRES -> summary.genres.take(3).joinToString("\n") { it.title }
        ReplayYearMoment.ARTIST_REVEAL -> annual.topArtist.orEmpty()
        ReplayYearMoment.TOP_ARTISTS -> summary.artists.take(3).joinToString("\n") { it.title }
        ReplayYearMoment.SONG_REVEAL -> annual.topSong.orEmpty()
        ReplayYearMoment.TOP_SONGS -> summary.songs.take(3).joinToString("\n") { it.song.title }
        ReplayYearMoment.ALBUMS -> summary.albums.take(3).joinToString("\n") { it.title }
        ReplayYearMoment.SUMMARY -> stringResource(R.string.replay_annual_unique_songs, annual.uniqueSongs)
    }
    val coverUrl = when (moment) {
        ReplayYearMoment.SONG_REVEAL, ReplayYearMoment.TOP_SONGS ->
            summary.songs.firstOrNull()?.song?.thumbnailUrl
        ReplayYearMoment.ARTIST_REVEAL, ReplayYearMoment.TOP_ARTISTS ->
            summary.artists.firstOrNull()?.artworkUrl
        ReplayYearMoment.ALBUMS -> summary.albums.firstOrNull()?.artworkUrl
        else -> null
    }
    var revealed by remember(moment, visible) { mutableStateOf(false) }
    LaunchedEffect(moment, visible) {
        if (visible) {
            delay(moment.revealAtMs)
            revealed = true
        }
    }
    Column(
        Modifier.fillMaxSize().padding(start = 22.dp, end = 22.dp, top = 82.dp, bottom = 18.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(
            text = stringResource(heading),
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            color = Color.White.copy(alpha = 0.78f),
            textAlign = TextAlign.Center,
        )
        AnimatedVisibility(
            visible = revealed,
            enter = fadeIn(tween(500)) + slideInVertically(tween(540)) { it / 7 },
            modifier = Modifier.weight(1f),
        ) {
            Column(
                Modifier.fillMaxSize().padding(vertical = 28.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
            ) {
                if (!coverUrl.isNullOrBlank()) {
                    AsyncImage(
                        model = coverUrl.artworkAt(CARD_ART_PX),
                        contentDescription = null,
                        modifier = Modifier.fillMaxWidth(0.78f).weight(1f, fill = false),
                        contentScale = ContentScale.Fit,
                    )
                    Spacer(Modifier.size(20.dp))
                }
                Text(
                    text = highlight,
                    color = Color.White,
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = if (highlight.length > 65) 25.sp else 37.sp,
                    lineHeight = if (highlight.length > 65) 32.sp else 44.sp,
                    maxLines = 6,
                    overflow = TextOverflow.Ellipsis,
                    textAlign = TextAlign.Center,
                )
            }
        }
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
            Box(
                Modifier.size(44.dp).clip(CircleShape)
                    .background(Color.White.copy(alpha = 0.18f))
                    .clickable(onClick = onShare),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = Icons.Rounded.IosShare,
                    contentDescription = stringResource(R.string.share_my_replay),
                    tint = Color.White,
                    modifier = Modifier.size(20.dp),
                )
            }
        }
    }
}
