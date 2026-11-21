// SPDX-FileCopyrightText: 2026 Aurelis
// SPDX-License-Identifier: LGPL-3.0-or-later
package com.music.aurelis.playback

import android.app.PendingIntent
import android.content.ComponentName
import android.content.Intent
import android.graphics.drawable.Icon
import android.os.Build
import android.service.quicksettings.Tile
import android.service.quicksettings.TileService
import androidx.media3.common.util.UnstableApi
import androidx.media3.session.MediaController
import androidx.media3.session.SessionToken
import com.music.aurelis.MainActivity
import com.music.aurelis.R
import com.google.common.util.concurrent.ListenableFuture
import java.util.concurrent.Executor

/**
 * A system Quick Settings toggle; never spins up its own player.
 * It connects to Aurelis' sole existing MediaLibrarySession.
 */
@UnstableApi
class AurelisQuickSettingsTile : TileService() {
    private var controllerFuture: ListenableFuture<MediaController>? = null
    private var controller: MediaController? = null
    private val onMain = Executor { command -> mainExecutor.execute(command) }

    override fun onStartListening() {
        super.onStartListening()
        connect()
    }

    override fun onClick() {
        super.onClick()
        val player = controller
        if (player == null || player.mediaItemCount == 0) {
            launchAurelis()
            return
        }
        if (player.isPlaying) player.pause() else player.play()
        refresh()
    }

    private fun connect() {
        if (controllerFuture != null) {
            refresh()
            return
        }
        val future = MediaController.Builder(
            this,
            SessionToken(this, ComponentName(this, PlaybackService::class.java)),
        ).buildAsync()
        controllerFuture = future
        future.addListener({
            if (controllerFuture !== future) return@addListener
            controller = runCatching { future.get() }.getOrNull()
            refresh()
        }, onMain)
    }

    private fun refresh() {
        val tile = qsTile ?: return
        val isPlaying = controller?.isPlaying == true
        tile.state = if (isPlaying) Tile.STATE_ACTIVE else Tile.STATE_INACTIVE
        tile.label = getString(R.string.app_name)
        tile.icon = Icon.createWithResource(
            this,
            if (isPlaying) android.R.drawable.ic_media_pause else android.R.drawable.ic_media_play,
        )
        tile.updateTile()
    }

    private fun launchAurelis() {
        val intent = Intent(this, MainActivity::class.java).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
        }
        if (Build.VERSION.SDK_INT >= 34) {
            val pending = PendingIntent.getActivity(
                this, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
            )
            startActivityAndCollapse(pending)
        } else {
            @Suppress("DEPRECATION")
            startActivityAndCollapse(intent)
        }
    }

    override fun onStopListening() {
        release()
        super.onStopListening()
    }

    override fun onDestroy() {
        release()
        super.onDestroy()
    }

    private fun release() {
        controller = null
        controllerFuture?.let(MediaController::releaseFuture)
        controllerFuture = null
    }
}
