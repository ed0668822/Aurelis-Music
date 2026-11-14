// Aurelis © 2026. GPL-3.0-or-later
// Aurelis © 2026. GPL-3.0-or-later.
package com.music.aurelis.playback

import android.content.ComponentName
import android.content.Context
import androidx.media3.session.MediaController
import androidx.media3.session.SessionToken
import com.music.aurelis.data.model.LocalSong
import kotlinx.coroutines.guava.await

object PlayerConnection {

    suspend fun connect(context: Context, onDisconnected: () -> Unit = {}): MediaController {
        val sessionToken = SessionToken(context, ComponentName(context, AurelisPlaybackService::class.java))
        val future = MediaController.Builder(context, sessionToken)
            .setListener(object : MediaController.Listener {
                override fun onDisconnected(controller: MediaController) = onDisconnected()
            }).buildAsync()
        return try { future.await() } catch (error: kotlinx.coroutines.CancellationException) {
            MediaController.releaseFuture(future)
            throw error
        }
    }

    fun playSongs(controller: androidx.media3.common.Player, songs: List<LocalSong>, startIndex: Int = 0) {
        if (songs.isEmpty()) return
        val mediaItems = songs.map { it.toQueueMediaItem() }
        controller.setMediaItems(mediaItems, startIndex.coerceIn(0, mediaItems.size - 1), 0L)
        controller.prepare()
        controller.play()
    }

}
