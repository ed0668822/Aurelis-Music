package com.music.aurelis.playback

import android.os.SystemClock
import androidx.media3.common.Player
import com.music.aurelis.data.LocalQueueStore
import com.music.aurelis.data.LocalUserLibrary
import com.music.aurelis.data.SavedLocalQueue
import kotlinx.coroutines.*

/** Owned by the service, so activity recreation cannot duplicate or stop accounting. */
internal class ServicePlaybackRecorder(
    private val player: Player,
    private val library: LocalUserLibrary,
    private val queue: LocalQueueStore,
    scope: CoroutineScope,
    private val clock: () -> Long = SystemClock::elapsedRealtime,
) : Player.Listener {
    private var lastTick = clock()
    private var lastSave = lastTick
    private var song = player.currentMediaItem?.toLocalSong()
    private var advancing = player.isPlaying
    private val job: Job
    init {
        library.startListeningSession(song?.id)
        player.addListener(this)
        job = scope.launch { while (isActive) { delay(1000); tick() } }
    }
    internal fun tick() {
        val now = clock()
        val elapsed = now - lastTick
        if (advancing && elapsed in 1..5000) song?.let { library.recordListening(it, elapsed, false) }
        lastTick = now
        if (now - lastSave >= 15_000) saveQueue()
    }
    override fun onEvents(player: Player, events: Player.Events) {
        tick()
        val current = player.currentMediaItem?.toLocalSong()
        if (current?.id != song?.id || events.contains(Player.EVENT_MEDIA_ITEM_TRANSITION)) library.startListeningSession(current?.id)
        song = current
        advancing = player.isPlaying
        if (!advancing) library.flush()
        saveQueue()
    }
    private fun saveQueue() {
        // Do not erase a saved queue while an asynchronous restore is reading MediaStore.
        if (player.mediaItemCount > 0) queue.save(SavedLocalQueue(
            (0 until player.mediaItemCount).map { player.getMediaItemAt(it).mediaId },
            player.currentMediaItemIndex.coerceAtLeast(0), player.currentPosition.coerceAtLeast(0),
            player.repeatMode, player.shuffleModeEnabled))
        lastSave = clock()
    }
    fun close() { tick(); saveQueue(); library.flush(); player.removeListener(this); job.cancel() }
}
