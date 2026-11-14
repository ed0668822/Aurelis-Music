package com.music.aurelis.playback

import android.content.Context
import androidx.media3.common.Player
import com.music.aurelis.AurelisApplication
import com.music.aurelis.data.*
import com.music.aurelis.data.model.LocalSong
import kotlinx.coroutines.*

/** Restores and records the sole local profile. Owned by the existing service. */
internal class ProfilePlaybackBinding(
    private val context: Context,
    private val player: Player,
    private val scope: CoroutineScope,
    private val library: () -> LocalUserLibrary = { (context.applicationContext as AurelisApplication).userLibrary },
    private val songs: suspend () -> List<LocalSong> = {
        if (AudioPermission.isGranted(context)) LocalMediaRepository.getLibrarySongs(context, SettingsRepository.filterNonMusicAudio.value)
        else emptyList()
    },
) {
    private var recorder: ServicePlaybackRecorder? = null
    private var restoreJob: Job? = null
    private var closed = false
    init { restore() }
    private fun restore() {
        val active = ProfileRepository.profileId
        val store = LocalQueueStore(context, active)
        val snapshot = store.read()
        recorder = ServicePlaybackRecorder(player, library(), store, scope)
        restoreJob = scope.launch {
            if (snapshot == null) return@launch
            try {
                val (available, index) = snapshot.resolve(songs())
                ensureActive()
                if (active != ProfileRepository.profileId || player.mediaItemCount != 0 || available.isEmpty()) return@launch
                val position = if (available[index].id == snapshot.ids.getOrNull(snapshot.index))
                    snapshot.positionMs.coerceAtMost(available[index].durationMs.coerceAtLeast(0)) else 0L
                player.setMediaItems(available.map { it.toQueueMediaItem() }, index, position)
                player.repeatMode = snapshot.repeat
                player.shuffleModeEnabled = snapshot.shuffle
                // Restored queues remain paused until an explicit playback command.
            } catch (cancelled: CancellationException) { throw cancelled }
            catch (_: Exception) { /* Keep the snapshot when permissions/provider are unavailable. */ }
        }
    }
    fun close() {
        if (closed) return
        closed = true
        restoreJob?.cancel(); recorder?.close(); recorder = null
    }
}
