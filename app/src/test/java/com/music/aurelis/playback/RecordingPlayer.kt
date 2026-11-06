package com.music.aurelis.playback

import android.os.Looper
import androidx.media3.common.*
import androidx.media3.common.util.UnstableApi
import com.google.common.util.concurrent.Futures

/** Media3's actual command/event machinery with deterministic audio state; tests only. */
@UnstableApi
internal class RecordingPlayer : SimpleBasePlayer(Looper.getMainLooper()) {
    var snapshot: State = State.Builder().setAvailableCommands(Player.Commands.Builder().addAllCommands().build()).build()
    var released = false
    var prepares = 0
    var replacements = 0
    var seeks = 0
    private var nextUid = 0L
    private fun entry(item: MediaItem) = MediaItemData.Builder(nextUid++)
        .setMediaItem(item).setIsSeekable(true)
        .setDurationUs((item.mediaMetadata.extras?.getLong("durationMs") ?: 60_000) * 1000).build()
    override fun getState() = snapshot
    fun publish(state: State) { snapshot = state; invalidateState() }
    private fun done(state: State): com.google.common.util.concurrent.ListenableFuture<*> {
        snapshot = state
        return Futures.immediateVoidFuture()
    }
    override fun handleSetMediaItems(items: List<MediaItem>, startIndex: Int, startPositionMs: Long): com.google.common.util.concurrent.ListenableFuture<*> {
        replacements++
        return done(snapshot.buildUpon().setPlaylist(items.map(::entry))
            .setCurrentMediaItemIndex(startIndex).setContentPositionMs(startPositionMs.coerceAtLeast(0)).build())
    }
    override fun handleSetPlayWhenReady(value: Boolean) = done(snapshot.buildUpon()
        .setPlayWhenReady(value, Player.PLAY_WHEN_READY_CHANGE_REASON_USER_REQUEST).build())
    override fun handlePrepare(): com.google.common.util.concurrent.ListenableFuture<*> {
        prepares++
        return done(snapshot.buildUpon().setPlayerError(null).setPlaybackState(Player.STATE_READY).build())
    }
    override fun handleSeek(index: Int, positionMs: Long, command: Int): com.google.common.util.concurrent.ListenableFuture<*> {
        seeks++
        return done(snapshot.buildUpon().setCurrentMediaItemIndex(index)
            .setContentPositionMs(positionMs.takeIf { it != C.TIME_UNSET } ?: 0)
            .setPlaybackState(if (snapshot.playbackState == Player.STATE_ENDED) Player.STATE_READY else snapshot.playbackState).build())
    }
    override fun handleSetRepeatMode(value: Int) = done(snapshot.buildUpon().setRepeatMode(value).build())
    override fun handleSetShuffleModeEnabled(value: Boolean) = done(snapshot.buildUpon().setShuffleModeEnabled(value).build())
    override fun handleRemoveMediaItems(from: Int, to: Int): com.google.common.util.concurrent.ListenableFuture<*> {
        val remaining = snapshot.playlist.toMutableList().apply { subList(from, to).clear() }
        val index = if (remaining.isEmpty()) C.INDEX_UNSET else
            (if (from < snapshot.currentMediaItemIndex) snapshot.currentMediaItemIndex - (to - from) else snapshot.currentMediaItemIndex).coerceIn(0, remaining.lastIndex)
        val builder = snapshot.buildUpon().setPlaylist(remaining).setCurrentMediaItemIndex(index)
        if (remaining.isEmpty()) builder.setPlaybackState(Player.STATE_IDLE).setContentPositionMs(0L)
        return done(builder.build())
    }
    override fun handleMoveMediaItems(from: Int, to: Int, newIndex: Int): com.google.common.util.concurrent.ListenableFuture<*> {
        val current = snapshot.playlist.getOrNull(snapshot.currentMediaItemIndex)?.uid
        val rows = snapshot.playlist.toMutableList()
        val moving = rows.subList(from, to).toList(); rows.subList(from, to).clear(); rows.addAll(newIndex, moving)
        return done(snapshot.buildUpon().setPlaylist(rows).setCurrentMediaItemIndex(rows.indexOfFirst { it.uid == current }).build())
    }
    override fun handleAddMediaItems(index: Int, items: List<MediaItem>): com.google.common.util.concurrent.ListenableFuture<*> {
        val currentUid = snapshot.playlist.getOrNull(snapshot.currentMediaItemIndex)?.uid
        val rows = snapshot.playlist.toMutableList()
        rows.addAll(index, items.map(::entry))
        val currentIndex = if (currentUid == null) 0 else rows.indexOfFirst { it.uid == currentUid }
        return done(snapshot.buildUpon().setPlaylist(rows).setCurrentMediaItemIndex(currentIndex).build())
    }
    override fun handleRelease(): com.google.common.util.concurrent.ListenableFuture<*> {
        released = true
        return Futures.immediateVoidFuture()
    }
}
