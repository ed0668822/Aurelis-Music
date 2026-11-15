// Aurelis © 2026. GPL-3.0-or-later
package com.music.aurelis.widget

import android.content.Context
import androidx.media3.common.Player
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.Channel

/** Observes the existing session player; widgets never own another audio engine. */
internal class MediaWidgetBinding(context: Context, private val player: Player) : AutoCloseable {
    private val app = context.applicationContext
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val updates = Channel<MediaWidgetSnapshot>(Channel.CONFLATED)
    private val listener = object : Player.Listener {
        override fun onEvents(player: Player, events: Player.Events) { updates.trySend(snapshot(player)) }
    }
    init {
        scope.launch {
            try {
                for (value in updates) {
                    runCatching {
                        MediaWidgetSnapshot.save(app, value)
                        MediaWidget.refresh(app)
                        MediaWidgetPill.refresh(app)
                    }.onFailure { android.util.Log.w("AurelisWidgets", "Widget state update failed", it) }
                }
            } finally { scope.cancel() }
        }
        player.addListener(listener)
        updates.trySend(snapshot(player))
    }
    override fun close() {
        player.removeListener(listener)
        updates.trySend(snapshot(player).copy(isPlaying = false))
        updates.close()
    }
    companion object {
        internal fun snapshot(player: Player): MediaWidgetSnapshot {
            val item = player.currentMediaItem ?: return MediaWidgetSnapshot.EMPTY
            val meta = item.mediaMetadata
            val uri = meta.artworkUri?.toString()
            return MediaWidgetSnapshot(item.mediaId, meta.title?.toString().orEmpty(),
                meta.artist?.toString().orEmpty(), "${item.mediaId}:$uri", uri, player.isPlaying,
                player.isCommandAvailable(Player.COMMAND_SEEK_TO_PREVIOUS),
                player.isCommandAvailable(Player.COMMAND_SEEK_TO_NEXT_MEDIA_ITEM) && player.hasNextMediaItem(),
                player.shuffleModeEnabled)
        }
    }
}
