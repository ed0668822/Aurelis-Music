// Aurelis © 2026. GPL-3.0-or-later.
// MoodGenreSleeve/duotone adapted from BitChord b690dc146009d4815825fa4a20815ef2e4ee8898 (GPLv3); original contributors retained.
package com.music.aurelis.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.ColorMatrix
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.music.aurelis.R
import com.music.aurelis.data.LibraryState
import coil3.compose.AsyncImage
import com.music.aurelis.ui.components.PAGE_GUTTER
import com.music.aurelis.ui.components.ShimmerBox
import com.music.aurelis.ui.components.MessageState

data class LocalGenre(
    val name: String,
    val songCount: Int,
    val stripeColor: Long?,
    val key: String = name,
    val artworkUri: String? = null,
)

@Composable
fun ExploreScreen(
    genres: List<LocalGenre>,
    listState: LazyListState,
    onGenreClick: (LocalGenre) -> Unit,
    contentPadding: PaddingValues,
    modifier: Modifier = Modifier,
    showTitle: Boolean = true,
    libraryState: LibraryState? = null,
    onRetry: () -> Unit = {},
    onRequestPermission: () -> Unit = {},
) {
    BoxWithConstraints(Modifier.fillMaxSize().then(modifier)) {
        val columns = moodColumns(maxWidth)
        LazyColumn(
            state = listState,
            contentPadding = contentPadding,
            modifier = Modifier.fillMaxSize().testTag("local-explore"),
        ) {
            if (showTitle) {
                item {
                    Text(
                        text = stringResource(R.string.explore),
                        style = MaterialTheme.typography.displayLarge,
                        color = MaterialTheme.colorScheme.onBackground,
                        modifier = Modifier.padding(
                            start = PAGE_GUTTER, end = PAGE_GUTTER, top = 8.dp, bottom = 14.dp,
                        ),
                    )
                }
            }
            if (libraryState == LibraryState.Loading) {
                item { ExploreSkeletonRows(columns) }
            } else if (libraryState is LibraryState.Error) {
                item { MessageState(libraryState.message,
                    actionLabel = stringResource(if (libraryState.permissionRequired) R.string.home_grant_permission else R.string.home_retry),
                    onAction = if (libraryState.permissionRequired) onRequestPermission else onRetry) }
            } else if (genres.isEmpty() || libraryState is LibraryState.Success && libraryState.songs.isEmpty()) {
                item { MessageState(stringResource(R.string.explore_local_empty),
                    actionLabel = stringResource(R.string.home_refresh), onAction = onRetry) }
            } else {
                val rows = genres.chunked(columns)
                items(rows, key = { row -> row.first().key }) { row ->
                    GenreRow(row = row, columns = columns, onGenreClick = onGenreClick)
                }
            }
        }
    }
}

@Composable
private fun GenreRow(row: List<LocalGenre>, columns: Int, onGenreClick: (LocalGenre) -> Unit) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(MOOD_SPACING),
        modifier = Modifier.padding(horizontal = PAGE_GUTTER).padding(bottom = MOOD_SPACING),
    ) {
        row.forEach { item ->
            GenreCard(item = item, onClick = { onGenreClick(item) }, modifier = Modifier.weight(1f))
        }
        repeat(columns - row.size) { Spacer(Modifier.weight(1f)) }
    }
}

@Composable
private fun GenreCard(item: LocalGenre, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val count = pluralStringResource(R.plurals.home_available_song_count, item.songCount, item.songCount)
    GenreSleeve(
        item = item,
        modifier = modifier
            .aspectRatio(MOOD_CARD_ASPECT)
            .clip(MOOD_CARD_SHAPE)
            .clickable(onClick = onClick)
            // Count remains available to accessibility without adding a visual
            // caption that is absent from the reference sleeve.
            .semantics { stateDescription = count }
            .testTag("genre:${item.key}"),
    )
}

@Composable
fun GenreSleeve(
    item: LocalGenre,
    modifier: Modifier = Modifier,
) {
    val tone = remember(item.stripeColor, item.name) { genreTone(item) }
    Box(modifier = modifier.background(tone.stripe)) {
        Box(
            Modifier
                .align(Alignment.CenterEnd)
                .fillMaxHeight()
                .fillMaxWidth(1f - MOOD_STRIPE_FRACTION)
                .background(tone.placeholder)
                .testTag("genre-cover:${item.key}"),
        ) {
            item.artworkUri?.let { artwork ->
                AsyncImage(artwork, null, contentScale = ContentScale.Crop,
                    colorFilter = tone.duotone, modifier = Modifier.fillMaxSize())
            }
        }
        Box(
            Modifier.matchParentSize()
                .background(Brush.verticalGradient(0f to Color.Black.copy(alpha = .24f), .62f to Color.Transparent)),
        )
        Text(
            text = item.name,
            style = MaterialTheme.typography.titleMedium.copy(
                shadow = Shadow(Color.Black.copy(alpha = .35f), offset = Offset(0f, 1f), blurRadius = 6f),
            ),
            fontWeight = FontWeight.Bold,
            color = Color.White,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.align(Alignment.TopStart).padding(horizontal = 14.dp, vertical = 12.dp),
        )
    }
}

private class GenreTone(val stripe: Color, val placeholder: Color, val duotone: ColorFilter)

private fun genreTone(item: LocalGenre): GenreTone {
    val base = item.stripeColor?.let { Color(it.toInt()) } ?: fallbackGenreColor(item.name)
    val stripe = lerp(base, Color.Black, .08f)
    val shadow = lerp(base, Color.Black, .68f)
    val highlight = lerp(base, Color.White, .32f)
    return GenreTone(stripe = stripe, placeholder = lerp(shadow, highlight, .45f), duotone = genreDuotone(shadow, highlight))
}

/** Luminance mapped onto the original shadow/highlight ramp; offsets are 0..255. */
private fun genreDuotone(shadow: Color, highlight: Color): ColorFilter {
    fun channel(from: Float, to: Float): FloatArray {
        val span = to - from
        return floatArrayOf(span * .299f, span * .587f, span * .114f, 0f, from * 255f)
    }
    return ColorFilter.colorMatrix(ColorMatrix(
        channel(shadow.red, highlight.red) + channel(shadow.green, highlight.green) +
            channel(shadow.blue, highlight.blue) + floatArrayOf(0f, 0f, 0f, 1f, 0f)))
}

private fun fallbackGenreColor(title: String): Color = when ((title.hashCode() and Int.MAX_VALUE) % 8) {
    0 -> Color(0xFFCC6A55); 1 -> Color(0xFFC07A92); 2 -> Color(0xFF9C8AC0); 3 -> Color(0xFF8090C8)
    4 -> Color(0xFFD0A060); 5 -> Color(0xFF6A88B0); 6 -> Color(0xFF7AAED0); else -> Color(0xFF86B890)
}

private val MOOD_SPACING = 12.dp
private val MOOD_CARD_SHAPE = RoundedCornerShape(18.dp)
const val MOOD_CARD_ASPECT = 1.72f
private const val MOOD_STRIPE_FRACTION = .28f
private val MOOD_MIN_CARD_WIDTH = 220.dp
private const val MOOD_MAX_COLUMNS = 6

private fun moodColumns(available: Dp): Int {
    val row = available - PAGE_GUTTER * 2
    return ((row + MOOD_SPACING) / (MOOD_MIN_CARD_WIDTH + MOOD_SPACING)).toInt().coerceIn(2, MOOD_MAX_COLUMNS)
}

@Composable
private fun ExploreSkeletonRows(columns: Int) {
    Column(Modifier.padding(horizontal = PAGE_GUTTER).testTag("explore-loading")) {
        repeat(6) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(MOOD_SPACING),
                modifier = Modifier.padding(bottom = MOOD_SPACING),
            ) {
                repeat(columns) { ShimmerBox(Modifier.weight(1f).aspectRatio(MOOD_CARD_ASPECT), MOOD_CARD_SHAPE) }
            }
        }
    }
}
