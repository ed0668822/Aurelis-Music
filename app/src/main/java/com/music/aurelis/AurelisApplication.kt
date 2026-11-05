// Aurelis © 2026. GPL-3.0-or-later
package com.music.aurelis

import androidx.core.content.edit
import android.app.Application
import coil3.ImageLoader
import coil3.PlatformContext
import coil3.SingletonImageLoader
import coil3.disk.DiskCache
import coil3.disk.directory
import coil3.memory.MemoryCache
import coil3.request.crossfade
import com.music.aurelis.data.SettingsRepository
import com.music.aurelis.data.ProfileRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import com.music.aurelis.data.LocalUserLibrary
import com.music.aurelis.data.stats.ListeningStats

class AurelisApplication : Application(), SingletonImageLoader.Factory {
    private lateinit var mutableLibraries: MutableStateFlow<LocalUserLibrary>
    val libraries: StateFlow<LocalUserLibrary> get() = mutableLibraries.asStateFlow()
    val userLibrary: LocalUserLibrary get() = mutableLibraries.value

    override fun onCreate() {
        super.onCreate()
        ProfileRepository.init(this)
        SettingsRepository.init(this)
        mutableLibraries = MutableStateFlow(LocalUserLibrary(this, ProfileRepository.profileId))
        com.music.aurelis.data.lyrics.LyricsTranslation.cacheDir = cacheDir
        ListeningStats.init(userLibrary)

        val meta = getSharedPreferences("app_meta", MODE_PRIVATE)
        val lastVersion = meta.getInt("last_version_code", 0)
        if (lastVersion != BuildConfig.VERSION_CODE) {
            runCatching { cacheDir.resolve("image_cache").deleteRecursively() }
            meta.edit { putInt("last_version_code", BuildConfig.VERSION_CODE) }
        }
    }

    override fun newImageLoader(context: PlatformContext): ImageLoader =
        ImageLoader.Builder(context)
            .memoryCache {
                MemoryCache.Builder()
                    .maxSizePercent(context, 0.20)
                    .build()
            }
            .diskCache {
                DiskCache.Builder()
                    .directory(cacheDir.resolve("image_cache"))
                    .maxSizeBytes(100L * 1024 * 1024)
                    .build()
            }
            .crossfade(200)
            .build()
}
