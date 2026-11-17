// Aurelis © 2026. GPL-3.0-or-later
// Scrolling overload adapted from BitChord b690dc146009d4815825fa4a20815ef2e4ee8898 (GPLv3).
// Original BitChord contributors and terms retained; existing Aurelis overloads preserved.
package com.music.aurelis.ui.components

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.gestures.snapping.rememberSnapFlingBehavior
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.rememberLazyListState

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

@Composable
fun ShelfRow(
    title: String,
    modifier: Modifier = Modifier,
    actionText: String? = null,
    onActionClick: (() -> Unit)? = null,
    content: @Composable RowScope.() -> Unit
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 12.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 4.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onBackground
            )
            if (actionText != null && onActionClick != null) {
                TextButton(onClick = onActionClick) {
                    Text(text = actionText, color = MaterialTheme.colorScheme.primary)
                }
            }
        }
        Spacer(modifier = Modifier.height(8.dp))
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            content = content
        )
    }
}

@Composable
fun GlassNavBar(
    modifier: Modifier = Modifier,
    content: @Composable RowScope.() -> Unit
) {
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .height(64.dp)
            .padding(horizontal = 16.dp, vertical = 8.dp)
            .clip(RoundedCornerShape(32.dp)),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.8f),
        tonalElevation = 8.dp
    ) {
        Row(
            modifier = Modifier.fillMaxSize(),
            horizontalArrangement = Arrangement.SpaceAround,
            verticalAlignment = Alignment.CenterVertically,
            content = content
        )
    }
}

/**
 * What a sideways shelf is wrapped in, on top of the row itself.
 *
 * Nothing on the phone, where a finger drags the row. A window's mouse can't
 * swipe sideways, so the desktop wraps each row in the arrows it pages with,
 * the drag that pulls it, and the horizontal wheel.
 */
interface ShelfRowChrome {
    @Composable
    fun Wrap(state: LazyListState, row: @Composable () -> Unit)
}

private object NoShelfRowChrome : ShelfRowChrome {
    @Composable
    override fun Wrap(state: LazyListState, row: @Composable () -> Unit) = row()
}

val LocalShelfRowChrome = staticCompositionLocalOf<ShelfRowChrome> { NoShelfRowChrome }

/** A sideways-scrolling shelf, in whatever the platform wraps one in — see [ShelfRowChrome].
 *
 * Snaps to each cell rather than stopping mid-card: a fling always settles with
 * a column aligned to the leading edge instead of leaving items cut in half.
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun ShelfRow(
    contentPadding: PaddingValues,
    horizontalArrangement: Arrangement.Horizontal,
    modifier: Modifier = Modifier,
    state: LazyListState = rememberLazyListState(),
    content: LazyListScope.() -> Unit,
) {
    LocalShelfRowChrome.current.Wrap(state) {
        LazyRow(
            modifier = modifier,
            state = state,
            contentPadding = contentPadding,
            horizontalArrangement = horizontalArrangement,
            flingBehavior = rememberSnapFlingBehavior(lazyListState = state),
            content = content,
        )
    }
}
