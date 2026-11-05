package com.music.aurelis.playback

import android.content.Context
import androidx.media3.common.Player
import com.music.aurelis.AurelisApplication
import com.music.aurelis.data.*
import com.music.aurelis.data.model.LocalSong
import com.music.aurelis.data.model.ProfileId
import kotlinx.coroutines.*

/** One recorder per active profile; the player and MediaSession remain the same. */
internal class ProfilePlaybackBinding(
    private val context: Context,
    private val player: Player,
    private val scope: CoroutineScope,
    private val library: () -> LocalUserLibrary = { (context.applicationContext as AurelisApplication).userLibrary },
    private val songs: suspend () -> List<LocalSong> = {
        if (AudioPermission.isGranted(context)) LocalMediaRepository.getAllSongs(context, SettingsRepository.filterNonMusicAudio.value)
        else emptyList()
    },
) : ProfileSessionListener {
    private var recorder: ServicePlaybackRecorder? = null
    private var restoreJob: Job? = null
    init { ProfileRepository.addListener(this); afterProfileChange(ProfileRepository.profileId) }
    override fun beforeProfileChange(previous: ProfileId, next: ProfileId) {
        restoreJob?.cancel()
        recorder?.close(); recorder = null
        player.pause()
        player.clearMediaItems()
        player.repeatMode = Player.REPEAT_MODE_OFF
        player.shuffleModeEnabled = false
    }
    override fun afterProfileChange(active: ProfileId) {
        restoreJob?.cancel(); recorder?.close()
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
                player.setMediaItems(available.map { it.toMediaItem() }, index, position)
                player.repeatMode = snapshot.repeat
                player.shuffleModeEnabled = snapshot.shuffle
                // Restored queues remain paused until an explicit playback command.
            } catch (cancelled: CancellationException) { throw cancelled }
            catch (_: Exception) { /* Keep the snapshot when permissions/provider are unavailable. */ }
        }
    }
    fun close() {
        ProfileRepository.removeListener(this)
        restoreJob?.cancel(); recorder?.close(); recorder = null
    }
}
