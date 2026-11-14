// Aurelis © 2026. GPL-3.0-or-later
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
import com.music.aurelis.ui.theme.LocalReduceMotion
import com.music.aurelis.ui.player.InlineQueue
import androidx.compose.ui.platform.testTag

@Composable
fun QueueScreen(
    playbackViewModel: PlaybackViewModel,
    isVisible: Boolean,
    onDismiss: () -> Unit,
    embedded: Boolean = false,
    listState: LazyListState? = null,
) {
    if (embedded) {
        if (isVisible) Surface(Modifier.fillMaxSize(), color = Color(0xFF141414)) {
            QueuePanelContent(playbackViewModel, onDismiss, embedded = true, listState = listState)
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
private fun QueuePanelContent(playbackViewModel: PlaybackViewModel, onDismiss: () -> Unit, embedded: Boolean = false, listState: LazyListState? = null) {
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
                text = "Cola de reproducción",
                style = MaterialTheme.typography.titleLarge,
                color = Color.White,
                modifier = Modifier.weight(1f)
            )
            IconButton(onClick = onDismiss) {
                Icon(Icons.Rounded.Close, contentDescription = "Cerrar", tint = Color.White)
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        if (state.queue.isEmpty()) {
            Text("La cola está vacía.", color = Color.White)
        } else {
            InlineQueue(
                queue = state.queue,
                currentIndex = state.currentIndex,
                autoplayEnabled = false,
                controlsLocked = !state.isConnected || !state.canEditQueue,
                onJumpTo = playbackViewModel::seekToIndex,
                onRemove = playbackViewModel::removeFromQueue,
                onMove = playbackViewModel::moveQueueItem,
                onClear = playbackViewModel::clearUserQueue,
                modifier = Modifier.weight(1f),
                showHeader = false,
                groupByTier = false, // Display the actual Media3 playback order.
                listState = queueListState,
                scrollToCurrentOnAttach = listState == null,
            )
        }
    }
}
