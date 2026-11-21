package com.music.aurelis.data.discord

import android.content.Context
import android.content.pm.PackageManager
import com.music.aurelis.data.model.Song

/**
 * Optional official Discord Social SDK 1.10+ Android RPC bridge.
 * The SDK's proprietary AAR is not included in the public repository.
 */
class DiscordSocialPresence(private val context: Context) {
    companion object {
        val sdkBundled: Boolean by lazy {
            runCatching { System.loadLibrary("aurelis_discord") }.isSuccess
        }

        fun isDiscordInstalled(context: Context): Boolean = try {
            @Suppress("DEPRECATION")
            context.packageManager.getPackageInfo("com.discord", 0)
            true
        } catch (_: PackageManager.NameNotFoundException) {
            false
        }
    }

    private var started = false
    fun connect(): Boolean {
        if (!sdkBundled || !isDiscordInstalled(context)) return false
        if (!started) {
            started = runCatching { nativeInitialize(DiscordApplication.ID) }.getOrDefault(false)
        }
        return started
    }

    fun updateSong(song: Song, positionMs: Long, durationMs: Long): Boolean {
        if (!started) return false
        return runCatching {
            nativeUpdate(song.title.take(128), song.artist.take(128),
                positionMs.coerceAtLeast(0), durationMs.coerceAtLeast(0))
            true
        }.getOrDefault(false)
    }

    fun pumpCallbacks() {
        if (started) runCatching { nativeRunCallbacks() }
    }

    fun clear() {
        if (started) {
            runCatching {
                nativeClear()
                nativeRunCallbacks()
            }
            started = false
        }
    }

    private external fun nativeInitialize(applicationId: String): Boolean
    private external fun nativeUpdate(title: String, artist: String, positionMs: Long, durationMs: Long)
    private external fun nativeRunCallbacks()
    private external fun nativeClear()
}
