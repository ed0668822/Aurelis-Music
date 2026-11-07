// Aurelis © 2026. GPL-3.0-or-later
package com.music.aurelis.playback

import androidx.media3.common.Player
import com.music.aurelis.data.model.LocalSong
import com.music.aurelis.data.model.QueueTier

/** Edits the connected Media3 timeline in place; never owns another queue or player. */
internal class QueueCoordinator(private val connectedPlayer: () -> Player?) {
    val canEdit: Boolean
        get() = connectedPlayer()?.let {
            it.isCommandAvailable(Player.COMMAND_CHANGE_MEDIA_ITEMS) &&
                it.isCommandAvailable(Player.COMMAND_GET_TIMELINE)
        } == true

    fun moveItem(fromIndex: Int, toIndex: Int): Boolean {
        val player = editablePlayer() ?: return false
        if (fromIndex !in 0 until player.mediaItemCount ||
            toIndex !in 0 until player.mediaItemCount || fromIndex == toIndex) return false
        player.moveMediaItem(fromIndex, toIndex)
        return true
    }

    fun removeItem(index: Int): Boolean {
        val player = editablePlayer() ?: return false
        if (index !in 0 until player.mediaItemCount || index == player.currentMediaItemIndex) return false
        player.removeMediaItem(index)
        return true
    }

    fun append(songs: List<LocalSong>): Boolean = insert(songs, next = false)
    fun playNext(songs: List<LocalSong>): Boolean = insert(songs, next = true)

    private fun insert(songs: List<LocalSong>, next: Boolean): Boolean {
        if (songs.isEmpty()) return false
        val player = editablePlayer() ?: return false
        // The shuffled successor is owned by the service. Do not promise "next"
        // while a controller can only insert into the physical timeline.
        if (next && player.shuffleModeEnabled) return false
        val count = player.mediaItemCount
        val index = if (next && count > 0) player.currentMediaItemIndex.coerceIn(0, count - 1) + 1 else count
        player.addMediaItems(index, songs.map { it.copy(queueTier = QueueTier.USER_QUEUE).toQueueMediaItem() })
        // Adding to an empty queue deliberately does not start playback.
        return true
    }

    fun clearUserQueue(): Boolean {
        val player = editablePlayer() ?: return false
        var removed = false
        // Reverse indices keep earlier occurrences stable. History/current/context survive.
        for (index in player.mediaItemCount - 1 downTo player.currentMediaItemIndex + 1) {
            if (player.getMediaItemAt(index).queueTier == QueueTier.USER_QUEUE) {
                player.removeMediaItem(index)
                removed = true
            }
        }
        return removed
    }

    private fun editablePlayer(): Player? = connectedPlayer()?.takeIf { canEdit }
}
