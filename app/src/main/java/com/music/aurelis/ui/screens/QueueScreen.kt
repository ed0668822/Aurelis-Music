// SPDX-FileCopyrightText: 2026 Aurelis
// SPDX-License-Identifier: LGPL-3.0-or-later
package com.music.aurelis.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.music.aurelis.playback.PlaybackViewModel
import com.music.aurelis.data.model.LocalSong
import com.music.aurelis.data.model.QueueTier
import com.music.aurelis.R
import com.music.aurelis.ui.theme.LocalReduceMotion
import com.music.aurelis.ui.player.InlineQueue
import androidx.compose.ui.platform.testTag

/**
 * Group in BitChord's USER → CONTEXT → AUTOPLAY sections only when the actual
 * Media3 playback order already has that sequence. Never visually reorder tracks.
 */
internal fun queueSectionsPreservePlaybackOrder(
    queue: List<LocalSong>,
    playbackOrder: List<Int>,
    currentIndex: Int,
): Boolean {
    if (currentIndex !in queue.indices || playbackOrder.size != queue.size ||
        playbackOrder.toSet() != queue.indices.toSet()) return false
    val cursor = playbackOrder.indexOf(currentIndex)
    if (cursor < 0) return false
    var priorTier = -1
    for (index in playbackOrder.drop(cursor + 1)) {
        val tier = when (queue[index].queueTier) {
            QueueTier.USER_QUEUE -> 0
            QueueTier.CONTEXT -> 1
            QueueTier.AUTOPLAY -> 2
        }
        if (tier < priorTier) return false
        priorTier = tier
    }
    return true
}

@Composable
fun QueueScreen(
    playbackViewModel: PlaybackViewModel,
    isVisible: Boolean,
    onDismiss: () -> Unit,
    embedded: Boolean = false,
    listState: LazyListState? = null,
    compactPlayerHeader: Boolean = false,
) {
    if (embedded) {
        // F062: the owner player provides the prepared artwork backdrop.
        // A black nested surface was covering it with a stark rectangle.
        if (isVisible) Surface(Modifier.fillMaxSize(), color = Color.Transparent) {
            QueuePanelContent(playbackViewModel, onDismiss, embedded = true,
                listState = listState, compactPlayerHeader = compactPlayerHeader)
        }
        return
    }
    val reduceMotion = LocalReduceMotion.current

    AnimatedVisibility(
        visible = isVisible,
        enter = if (reduceMotion) androidx.compose.animation.EnterTransition.None else slideInVertically(initialOffsetY = { it }) + fadeIn(tween(300)),
        exit = if (reduceMotion) androidx.compose.animation.ExitTransition.None else slideOutVertically(targetOffsetY = { it }) + fadeOut(tween(200))
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.6f))
                .pointerInput(Unit) { detectTapGestures { } }
        ) {
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .fillMaxHeight(0.85f)
                    .align(Alignment.BottomCenter),
                shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
                color = Color(0xFF141414)
            ) {
                QueuePanelContent(playbackViewModel, onDismiss, listState = listState)
            }
        }
    }
}


@Composable
private fun QueuePanelContent(
    playbackViewModel: PlaybackViewModel,
    onDismiss: () -> Unit,
    embedded: Boolean = false,
    listState: LazyListState? = null,
    compactPlayerHeader: Boolean = false,
) {
    val queueListState = listState ?: rememberLazyListState()
    val state by playbackViewModel.state.collectAsState()
    Column(
        modifier = Modifier
            .fillMaxSize().testTag("playback-queue")
            .then(if (embedded) Modifier else Modifier.navigationBarsPadding())
            .padding(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = if (compactPlayerHeader) androidx.compose.ui.res.stringResource(R.string.queue) else "Cola de reproducción",
                style = MaterialTheme.typography.titleLarge,
                color = Color.White,
                modifier = Modifier.weight(1f)
            )
            if (!compactPlayerHeader) IconButton(onClick = onDismiss) {
                Icon(Icons.Rounded.Close, contentDescription = "Cerrar", tint = Color.White)
            }
        }

        val order = state.queuePlaybackOrder ?: if (!state.shuffleEnabled) state.queue.indices.toList() else emptyList()
        val cursor = order.indexOf(state.currentIndex)
        val groupedSections = compactPlayerHeader && !state.shuffleEnabled &&
            queueSectionsPreservePlaybackOrder(state.queue, order, state.currentIndex)
        val hasQueued = cursor >= 0 && order.drop(cursor + 1).any {
            state.queue.getOrNull(it)?.queueTier == com.music.aurelis.data.model.QueueTier.USER_QUEUE
        }
        if (hasQueued && !groupedSections) TextButton(onClick = playbackViewModel::clearUserQueue,
            enabled = state.isConnected && state.canEditQueue && (!state.shuffleEnabled || state.canEditShuffleQueue)) {
            Text(androidx.compose.ui.res.stringResource(com.music.aurelis.R.string.queue_clear_user))
        }

        Spacer(modifier = Modifier.height(8.dp))
        // Empty upcoming queues should communicate their state without
        // inventing placeholder tracks or changing Media3's playback order.
        val remaining = if (cursor >= 0) order.size - cursor - 1 else 0
        if (!compactPlayerHeader && state.queue.isNotEmpty() && remaining == 0 && !state.shuffleEnabled) {
            Text(
                text = "No hay más canciones en espera",
                style = MaterialTheme.typography.bodySmall,
                color = Color.White.copy(alpha = 0.62f),
                modifier = Modifier.padding(start = 8.dp, bottom = 6.dp),
            )
        }

        if (state.queue.isEmpty()) {
            Text("La cola está vacía.", color = Color.White)
        } else if (state.shuffleEnabled && state.queuePlaybackOrder == null) {
            Text(androidx.compose.ui.res.stringResource(com.music.aurelis.R.string.queue_shuffle_unavailable), color = Color.White)
        } else {
            if (state.shuffleEnabled && !state.canEditShuffleQueue) Text(androidx.compose.ui.res.stringResource(com.music.aurelis.R.string.queue_shuffle_reorder), color = Color.White)
            InlineQueue(
                queue = state.queue,
                currentIndex = state.currentIndex,
                autoplayEnabled = state.autoplayEnabled,
                controlsLocked = !state.isConnected || !state.canEditQueue,
                onJumpTo = playbackViewModel::seekToIndex,
                onRemove = playbackViewModel::removeFromQueue,
                onMove = playbackViewModel::moveQueueItem,
                onClear = playbackViewModel::clearUserQueue,
                modifier = Modifier.weight(1f),
                showHeader = false,
                groupByTier = groupedSections, // Only group when Media3 order stays unchanged.
                listState = queueListState,
                scrollToCurrentOnAttach = listState == null,
                playbackOrder = state.queuePlaybackOrder,
                canReorder = !state.shuffleEnabled || state.canEditShuffleQueue,
            )
        }
    }
}
