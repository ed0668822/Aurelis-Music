// Aurelis © 2026. GPL-3.0-or-later
package com.music.aurelis.playback

import android.os.Bundle
import android.os.Process
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.source.ShuffleOrder.DefaultShuffleOrder
import androidx.media3.session.MediaController
import androidx.media3.session.MediaSession
import androidx.media3.session.SessionCommand
import androidx.media3.session.SessionResult
import com.google.common.util.concurrent.Futures
import com.google.common.util.concurrent.ListenableFuture
import com.music.aurelis.data.model.LocalSong
import com.music.aurelis.data.model.QueueTier

/** Private edits of the service-owned permutation, never a second playback queue. */
@androidx.annotation.OptIn(UnstableApi::class)
internal object ShuffleQueueEdits {
    val command = SessionCommand("com.music.aurelis.EDIT_SHUFFLE_QUEUE", Bundle.EMPTY)
    private const val MAX_ITEMS = 200

    fun available(player: Player): Boolean = player is MediaController &&
        player.isSessionCommandAvailable(command) &&
        player.isCommandAvailable(Player.COMMAND_CHANGE_MEDIA_ITEMS) &&
        player.isCommandAvailable(Player.COMMAND_GET_TIMELINE)

    fun insert(player: Player, songs: List<LocalSong>, next: Boolean): ListenableFuture<SessionResult> {
        if (!available(player)) return result(SessionResult.RESULT_ERROR_NOT_SUPPORTED)
        if (songs.isEmpty() || songs.size > MAX_ITEMS) return result(SessionResult.RESULT_ERROR_BAD_VALUE)
        val args = Bundle().apply {
            putString("operation", if (next) "next" else "append")
            putParcelableArrayList("items", ArrayList(songs.map { it.toMediaItem().toBundleIncludeLocalConfiguration() }))
        }
        return (player as MediaController).sendCustomCommand(command, args)
    }

    fun clear(player: Player): ListenableFuture<SessionResult> {
        if (!available(player)) return result(SessionResult.RESULT_ERROR_NOT_SUPPORTED)
        return (player as MediaController).sendCustomCommand(command, Bundle().apply { putString("operation", "clear") })
    }

    fun move(player: Player, from: Int, to: Int): ListenableFuture<SessionResult> {
        if (!available(player)) return result(SessionResult.RESULT_ERROR_NOT_SUPPORTED)
        if (from !in 0 until player.mediaItemCount || to !in 0 until player.mediaItemCount || from == to)
            return result(SessionResult.RESULT_ERROR_BAD_VALUE)
        val args = Bundle().apply {
            putString("operation", "move")
            putString("from", player.getMediaItemAt(from).queueEntryId)
            putString("to", player.getMediaItemAt(to).queueEntryId)
            putString("current", player.currentMediaItem?.queueEntryId)
        }
        return (player as MediaController).sendCustomCommand(command, args)
    }

    internal fun execute(player: ExoPlayer, args: Bundle): @SessionResult.Code Int {
        if (!player.shuffleModeEnabled) return SessionResult.RESULT_ERROR_INVALID_STATE
        val order = queuePlaybackOrder(player) ?: return SessionResult.RESULT_ERROR_INVALID_STATE
        val current = player.currentMediaItemIndex
        val cursor = order.indexOf(current)
        return when (args.getString("operation")) {
            "clear" -> {
                if (cursor < 0) return SessionResult.RESULT_ERROR_INVALID_STATE
                val upcoming = order.drop(cursor + 1).filter {
                    player.getMediaItemAt(it).queueTier == QueueTier.USER_QUEUE
                }
                upcoming.sortedDescending().forEach { player.removeMediaItem(it) }
                SessionResult.RESULT_SUCCESS
            }
            "move" -> {
                // Resolve occurrence IDs again at execution; never move an index reused by a removal.
                if (args.getString("current") != player.currentMediaItem?.queueEntryId || cursor < 0)
                    return SessionResult.RESULT_ERROR_INVALID_STATE
                val fromId = args.getString("from") ?: return SessionResult.RESULT_ERROR_BAD_VALUE
                val toId = args.getString("to") ?: return SessionResult.RESULT_ERROR_BAD_VALUE
                val from = order.indexOfFirst { player.getMediaItemAt(it).queueEntryId == fromId }
                val to = order.indexOfFirst { player.getMediaItemAt(it).queueEntryId == toId }
                if (from < 0 || to < 0 || from == to) return SessionResult.RESULT_ERROR_BAD_VALUE
                val changed = order.toMutableList().apply { add(to, removeAt(from)) }
                player.setShuffleOrder(DefaultShuffleOrder(changed.toIntArray(), System.nanoTime()))
                SessionResult.RESULT_SUCCESS
            }
            "next", "append" -> {
                // Bundle's typed accessor requires API33; the legacy accessor supports our minSdk.
                @Suppress("DEPRECATION")
                val bundles = try { args.getParcelableArrayList<Bundle>("items") }
                    catch (_: RuntimeException) { return SessionResult.RESULT_ERROR_BAD_VALUE }
                    ?: return SessionResult.RESULT_ERROR_BAD_VALUE
                if (bundles.isEmpty() || bundles.size > MAX_ITEMS || (order.isNotEmpty() && cursor < 0))
                    return SessionResult.RESULT_ERROR_BAD_VALUE
                val items = try {
                    bundles.map { bundle ->
                        val item = MediaItem.fromBundle(bundle)
                        val scheme = item.localConfiguration?.uri?.scheme
                        require(scheme == "content" || scheme == "file")
                        item.toLocalSong().copy(queueTier = QueueTier.USER_QUEUE).toQueueMediaItem()
                    }
                } catch (_: RuntimeException) { return SessionResult.RESULT_ERROR_BAD_VALUE }
                val prefix = order.take(cursor + 1)
                val upcoming = order.drop(cursor + 1)
                val pinned = upcoming.filter { player.getMediaItemAt(it).queueTier == QueueTier.USER_QUEUE }
                val context = upcoming.filter { player.getMediaItemAt(it).queueTier != QueueTier.USER_QUEUE }
                val added = (player.mediaItemCount until player.mediaItemCount + items.size).toList()
                val future = if (args.getString("operation") == "next") added + pinned + context else pinned + added + context
                player.addMediaItems(items)
                player.setShuffleOrder(DefaultShuffleOrder((prefix + future).toIntArray(), System.nanoTime()))
                SessionResult.RESULT_SUCCESS
            }
            else -> SessionResult.RESULT_ERROR_BAD_VALUE
        }
    }

    private fun result(code: @SessionResult.Code Int) = Futures.immediateFuture(SessionResult(code))
}

@androidx.annotation.OptIn(UnstableApi::class)
internal class ShuffleQueueCallback(private val player: ExoPlayer, private val ownerPackage: String) : MediaSession.Callback {
    private fun owns(controller: MediaSession.ControllerInfo) =
        controller.uid == Process.myUid() && controller.packageName == ownerPackage

    override fun onConnect(session: MediaSession, controller: MediaSession.ControllerInfo): MediaSession.ConnectionResult {
        val commands = MediaSession.ConnectionResult.DEFAULT_SESSION_COMMANDS.buildUpon()
        if (owns(controller)) commands.add(ShuffleQueueEdits.command)
        return MediaSession.ConnectionResult.AcceptedResultBuilder(session, controller)
            .setAvailableSessionCommands(commands.build()).build()
    }

    override fun onCustomCommand(session: MediaSession, controller: MediaSession.ControllerInfo,
        customCommand: SessionCommand, args: Bundle): ListenableFuture<SessionResult> {
        val code: @SessionResult.Code Int = when {
            !owns(controller) -> SessionResult.RESULT_ERROR_PERMISSION_DENIED
            customCommand != ShuffleQueueEdits.command -> SessionResult.RESULT_ERROR_NOT_SUPPORTED
            else -> ShuffleQueueEdits.execute(player, args)
        }
        return Futures.immediateFuture(SessionResult(code))
    }
}
