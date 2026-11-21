package com.music.aurelis.data.discord

import android.app.Activity
import android.content.Context
import android.content.pm.PackageManager
import com.music.aurelis.BuildConfig
import kotlinx.coroutines.flow.MutableStateFlow
import com.music.aurelis.data.model.Song

/**
 * Optional official Discord Social SDK 1.10+ Android RPC bridge.
 * The SDK's proprietary AAR is not included in the public repository.
 */
class DiscordSocialPresence(private val context: Context) {
    companion object {
        /** Discord needs an Activity for native Android RPC, not an account token. */
        val engineReady = MutableStateFlow(false)

        val sdkBundled: Boolean by lazy {
            BuildConfig.DISCORD_SDK_BUNDLED &&
                runCatching { System.loadLibrary("aurelis_discord") }.isSuccess
        }

        /** Called from MainActivity.onCreate, before UI composition starts. */
        fun registerEngineActivity(activity: Activity) {
            if (!BuildConfig.DISCORD_SDK_BUNDLED) return
            engineReady.value = runCatching {
                val initializer = Class.forName("com.discord.socialsdk.DiscordSocialSdkInit")
                initializer.getMethod("setEngineActivity", Activity::class.java)
                    .invoke(null, activity)
                true
            }.getOrDefault(false)
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
        if (!sdkBundled || !engineReady.value || !isDiscordInstalled(context)) return false
        if (!started) {
            started = runCatching { nativeInitialize(DiscordApplication.ID) }.getOrDefault(false)
        }
        return started
    }

    fun updateSong(song: Song, positionMs: Long, durationMs: Long, artworkUrl: String? = null): Boolean {
        if (!started) return false
        return runCatching {
            nativeUpdate(song.title.take(128), song.artist.take(128),
                positionMs.coerceAtLeast(0), durationMs.coerceAtLeast(0),
                DiscordArtwork.publicUrl(artworkUrl))
            true
        }.getOrDefault(false)
    }

    fun pumpCallbacks() {
        if (started) runCatching { nativeRunCallbacks() }
    }

    /** Clear only the activity when paused, keeping the SDK connected for resume. */
    fun clearActivity() {
        if (started) {
            runCatching {
                nativeClear()
                nativeRunCallbacks()
            }
        }
    }

    /** Fully stop the transport when sharing is disabled or service exits. */
    fun clear() {
        clearActivity()
        started = false
    }

    private external fun nativeInitialize(applicationId: String): Boolean
    private external fun nativeUpdate(title: String, artist: String, positionMs: Long, durationMs: Long, artworkUrl: String?)
    private external fun nativeRunCallbacks()
    private external fun nativeClear()
}
