// Aurelis © 2026. GPL-3.0-or-later
package com.music.aurelis.playback

import android.content.Context
import androidx.media3.common.Player
import com.music.aurelis.AurelisApplication
import com.music.aurelis.data.*
import com.music.aurelis.data.model.LocalSong
import kotlinx.coroutines.*

/** Restores and records the sole local profile. Owned by the existing service. */
@androidx.annotation.OptIn(androidx.media3.common.util.UnstableApi::class)
internal class ProfilePlaybackBinding(
    private val context: Context,
    private val player: Player,
    private val scope: CoroutineScope,
    private val library: () -> LocalUserLibrary = { (context.applicationContext as AurelisApplication).userLibrary },
    private val songs: suspend () -> List<LocalSong> = {
        if (AudioPermission.isGranted(context)) LocalMediaRepository.getLibrarySongs(context, SettingsRepository.filterNonMusicAudio.value)
        else emptyList()
    },
) : Player.Listener {
    private var recorder: ServicePlaybackRecorder? = null
    private var restoreJob: Job? = null
    private var closed = false
    private var restoreCancelled = false
    private var queueEdited = false
    private var applyingRestore = false
    init { player.addListener(this); restore() }
    fun cancelRestore() {
        if (closed || applyingRestore) return
        restoreCancelled = true
        restoreJob?.cancel()
    }
    override fun onTimelineChanged(timeline: androidx.media3.common.Timeline, reason: Int) {
        if (!closed && !applyingRestore) { queueEdited = true; cancelRestore() }
    }
    override fun onPositionDiscontinuity(oldPosition: Player.PositionInfo, newPosition: Player.PositionInfo, reason: Int) { cancelRestore() }
    override fun onPlayWhenReadyChanged(playWhenReady: Boolean, reason: Int) { if (playWhenReady) cancelRestore() }

    private fun restore() {
        val active = ProfileRepository.profileId
        val store = LocalQueueStore(context, active)
        val snapshot = store.read()
        recorder = ServicePlaybackRecorder(player, library(), store, scope, canSaveEmpty = { queueEdited })
        restoreJob = scope.launch {
            if (snapshot == null) return@launch
            try {
                val catalog = songs()
                val (available, index) = snapshot.resolve(catalog)
                ensureActive()
                if (closed || restoreCancelled || active != ProfileRepository.profileId || player.mediaItemCount != 0 || available.isEmpty()) return@launch
                val position = if (available[index].id == snapshot.ids.getOrNull(snapshot.index)) {
                    val duration = available[index].durationMs
                    if (duration > 0) snapshot.positionMs.coerceAtMost(duration) else snapshot.positionMs
                } else 0L
                applyingRestore = true
                try {
                    player.setMediaItems(available.map { if (it.queueEntryId != null) it.toMediaItem() else it.toQueueMediaItem() }, index, position)
                    player.repeatMode = snapshot.repeat
                    if (player is androidx.media3.exoplayer.ExoPlayer) snapshot.resolveShuffleOrder(catalog)?.let {
                        player.setShuffleOrder(androidx.media3.exoplayer.source.ShuffleOrder.DefaultShuffleOrder(it.toIntArray(), System.nanoTime()))
                    }
                    player.shuffleModeEnabled = snapshot.shuffle
                    player.pause()
                } finally { applyingRestore = false }
                // Restored queues remain paused until an explicit playback command.
            } catch (cancelled: CancellationException) { throw cancelled }
            catch (_: Exception) { /* Keep the snapshot when permissions/provider are unavailable. */ }
        }
    }
    fun close() {
        if (closed) return
        closed = true
        player.removeListener(this)
        restoreJob?.cancel(); recorder?.close(); recorder = null
    }
}
