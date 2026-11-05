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
    override fun getState() = snapshot
    fun publish(state: State) { snapshot = state; invalidateState() }
    private fun done(state: State): com.google.common.util.concurrent.ListenableFuture<*> {
        snapshot = state
        return Futures.immediateVoidFuture()
    }
    override fun handleSetMediaItems(items: List<MediaItem>, startIndex: Int, startPositionMs: Long) = done(
        snapshot.buildUpon().setPlaylist(items.mapIndexed { index, item -> MediaItemData.Builder("$index:${item.mediaId}")
            .setMediaItem(item).setIsSeekable(true)
            .setDurationUs((item.mediaMetadata.extras?.getLong("durationMs") ?: 60_000) * 1000).build() })
            .setCurrentMediaItemIndex(startIndex).setContentPositionMs(startPositionMs.coerceAtLeast(0)).build())
    override fun handleSetPlayWhenReady(value: Boolean) = done(snapshot.buildUpon()
        .setPlayWhenReady(value, Player.PLAY_WHEN_READY_CHANGE_REASON_USER_REQUEST).build())
    override fun handlePrepare(): com.google.common.util.concurrent.ListenableFuture<*> {
        prepares++
        return done(snapshot.buildUpon().setPlayerError(null).setPlaybackState(Player.STATE_READY).build())
    }
    override fun handleSeek(index: Int, positionMs: Long, command: Int) = done(snapshot.buildUpon()
        .setCurrentMediaItemIndex(index).setContentPositionMs(positionMs.takeIf { it != C.TIME_UNSET } ?: 0)
        .setPlaybackState(if (snapshot.playbackState == Player.STATE_ENDED) Player.STATE_READY else snapshot.playbackState).build())
    override fun handleSetRepeatMode(value: Int) = done(snapshot.buildUpon().setRepeatMode(value).build())
    override fun handleSetShuffleModeEnabled(value: Boolean) = done(snapshot.buildUpon().setShuffleModeEnabled(value).build())
    override fun handleRemoveMediaItems(from: Int, to: Int): com.google.common.util.concurrent.ListenableFuture<*> {
        val remaining = snapshot.playlist.toMutableList().apply { subList(from, to).clear() }
        return done(snapshot.buildUpon().setPlaylist(remaining).setCurrentMediaItemIndex(
            if (from < snapshot.currentMediaItemIndex) snapshot.currentMediaItemIndex - (to - from) else snapshot.currentMediaItemIndex).build())
    }
    override fun handleMoveMediaItems(from: Int, to: Int, newIndex: Int): com.google.common.util.concurrent.ListenableFuture<*> {
        val current = snapshot.playlist.getOrNull(snapshot.currentMediaItemIndex)?.uid
        val rows = snapshot.playlist.toMutableList()
        val moving = rows.subList(from, to).toList(); rows.subList(from, to).clear(); rows.addAll(newIndex, moving)
        return done(snapshot.buildUpon().setPlaylist(rows).setCurrentMediaItemIndex(rows.indexOfFirst { it.uid == current }).build())
    }
    override fun handleAddMediaItems(index: Int, items: List<MediaItem>): com.google.common.util.concurrent.ListenableFuture<*> {
        val rows = snapshot.playlist.toMutableList()
        rows.addAll(index, items.mapIndexed { n, item -> MediaItemData.Builder("added:$index:$n:${item.mediaId}")
            .setMediaItem(item).setIsSeekable(true).setDurationUs(60_000_000).build() })
        return done(snapshot.buildUpon().setPlaylist(rows).setCurrentMediaItemIndex(snapshot.currentMediaItemIndex.coerceAtLeast(0)).build())
    }
    override fun handleRelease(): com.google.common.util.concurrent.ListenableFuture<*> {
        released = true
        return Futures.immediateVoidFuture()
    }
}
