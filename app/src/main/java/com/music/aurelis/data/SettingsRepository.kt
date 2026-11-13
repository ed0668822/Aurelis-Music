// Aurelis © 2026. GPL-3.0-or-later
// Aurelis © 2026. GPL-3.0-or-later.
package com.music.aurelis.data

import androidx.core.content.edit
import android.content.Context
import android.content.SharedPreferences
import com.music.aurelis.data.model.ProfileId
import com.music.aurelis.data.lyrics.LyricsSource
import com.music.aurelis.playback.EqualizerPreset
import kotlinx.coroutines.flow.MutableStateFlow

enum class EqualizerMode { DYNAMIC, MANUAL }

/** Calidad de audio preferida para reproducción local. */
enum class AudioQuality { STANDARD, HIGH, LOSSLESS }

enum class AppFont { SYSTEM, VAZIRMATN }

object SettingsRepository {
    private lateinit var prefs: SharedPreferences

    // ── Existing ──────────────────────────────────────────────────────────────
    val themeMode = MutableStateFlow(0)
    val dolbyAtmos = MutableStateFlow(true)
    val liquidGlass = MutableStateFlow(true)
    val reduceAnimation = MutableStateFlow(false)
    val reduceDynamicBlur = MutableStateFlow(false)
    val hapticFeedback = MutableStateFlow(true)
    val syncedLyrics = MutableStateFlow(true)
    val blurUnfocusedLyrics = MutableStateFlow(true)
    val filterNonMusicAudio = MutableStateFlow(true)
    val autoplay = MutableStateFlow(true)
    val crossfadeSeconds = MutableStateFlow(0)

    // ── Equalizer ─────────────────────────────────────────────────────────────
    val equalizerEnabled = MutableStateFlow(false)
    val equalizerMode = MutableStateFlow(EqualizerMode.DYNAMIC)
    val equalizerToneX = MutableStateFlow(0)
    val equalizerToneY = MutableStateFlow(0)
    val equalizerFocused = MutableStateFlow(false)
    val equalizerBalance = MutableStateFlow(0f)
    val equalizerBands = MutableStateFlow(List(7) { 0f })
    val equalizerPreset = MutableStateFlow(EqualizerPreset.FLAT)

    // ── Spatial / Loudness ────────────────────────────────────────────────────
    val spatialAudio = MutableStateFlow(false)
    val loudnessNormalization = MutableStateFlow(false)

    // ── Audio Quality ─────────────────────────────────────────────────────────
    val audioQuality = MutableStateFlow(AudioQuality.LOSSLESS)

    // ── Player ────────────────────────────────────────────────────────────────
    val hideVolumeBar = MutableStateFlow(false)
    val showNerdStats = MutableStateFlow(false)
    val lyricsOffsetMs = MutableStateFlow(0)

    // ── Queue ─────────────────────────────────────────────────────────────────
    val showQueueHistory = MutableStateFlow(false)

    // ── Lyrics / Translation ──────────────────────────────────────────────────
    /** Empty means "follow the app's own language", which is not the same
     *  as naming today's app language and pinning it. */
    val translationLanguage = MutableStateFlow("")
    val translationEndpoint = MutableStateFlow("")
    val translationSourceLanguage = MutableStateFlow("auto")
    val lyricsSources = MutableStateFlow(LyricsSource.offered.toSet())
    val lyricsSourceOrder = MutableStateFlow(LyricsSource.offered)
    val prioritizeSyllableSync = MutableStateFlow(false)
    val paxSenixApiKey = MutableStateFlow("")

    internal val libraryFolderRevision = MutableStateFlow(0L)
    val appFont = MutableStateFlow(AppFont.SYSTEM)
    val useAllAudioFolders = MutableStateFlow(true)
    val localMusicFolders = MutableStateFlow<Set<String>>(emptySet())

    fun init(context: Context, profileId: ProfileId = ProfileRepository.profileId) {
        prefs = ProfileStorage(context).open(profileId, ProfileDataArea.PREFERENCES)

        appFont.value = AppFont.entries.firstOrNull { it.name == readString("app_font", "SYSTEM") } ?: AppFont.SYSTEM
        useAllAudioFolders.value = readBoolean("all_audio_folders", true)
        localMusicFolders.value = readStringSet("local_music_folders", emptySet()).orEmpty().toSet()
        themeMode.value = readInt("theme_mode", 0).takeIf { it in 0..2 } ?: 0
        dolbyAtmos.value = readBoolean("dolby_atmos", true)
        liquidGlass.value = readBoolean("liquid_glass", true)
        reduceAnimation.value = readBoolean("reduce_animation", false)
        reduceDynamicBlur.value = readBoolean("reduce_dynamic_blur", false)
        hapticFeedback.value = readBoolean("haptic_feedback", true)
        syncedLyrics.value = readBoolean("synced_lyrics", true)
        blurUnfocusedLyrics.value = readBoolean("blur_lyrics", true)
        filterNonMusicAudio.value = readBoolean("filter_non_music", true)
        autoplay.value = readBoolean("autoplay", true)
        crossfadeSeconds.value = readInt("crossfade", 0)

        equalizerEnabled.value = readBoolean("eq_enabled", false)
        equalizerMode.value = EqualizerMode.entries.getOrElse(readInt("eq_mode", 0)) { EqualizerMode.DYNAMIC }
        equalizerToneX.value = readInt("eq_tone_x", 0)
        equalizerToneY.value = readInt("eq_tone_y", 0)
        equalizerFocused.value = readBoolean("eq_focused", false)
        equalizerBalance.value = safeFloat(readFloat("eq_balance", 0f), -1f, 1f)
        equalizerBands.value = (0 until 7).map { safeFloat(readFloat("eq_band_$it", 0f), -12f, 12f) }
        equalizerPreset.value = runCatching {
            EqualizerPreset.valueOf(readString("eq_preset", "FLAT") ?: "FLAT")
        }.getOrDefault(EqualizerPreset.FLAT)

        spatialAudio.value = readBoolean("spatial_audio", false)
        loudnessNormalization.value = readBoolean("loudness_norm", false)
        audioQuality.value = AudioQuality.entries.getOrElse(readInt("audio_quality", 2)) { AudioQuality.LOSSLESS }
        hideVolumeBar.value = readBoolean("hide_volume_bar", false)
        showNerdStats.value = readBoolean("show_nerd_stats", false)
        lyricsOffsetMs.value = readInt("lyrics_offset_ms", 0)
        showQueueHistory.value = readBoolean("show_queue_history", false)

        // Lyrics / translation: the two collections need rebuilding from names
        // so a build that renames or removes a source does not deserialize into
        // an unreachable one.
        translationLanguage.value = readString("translation_language", "") ?: ""
        translationEndpoint.value = readString("translation_endpoint", "") ?: ""
        translationSourceLanguage.value = readString("translation_source", "auto") ?: "auto"
        paxSenixApiKey.value = readString("paxsenix_api_key", "") ?: ""
        val savedSources = readStringSet("lyrics_sources", null)
        lyricsSources.value = if (savedSources != null) {
            LyricsSource.entries
                .filter { it.name in savedSources && it in LyricsSource.offered }
                .toSet()
        } else {
            LyricsSource.offered.toSet()
        }
        val savedOrder = readString("lyrics_source_order", null)
        lyricsSourceOrder.value = if (savedOrder != null) {
            LyricsSource.ordered(
                savedOrder.split(',').mapNotNull { name ->
                    LyricsSource.entries.firstOrNull { it.name == name }
                },
            )
        } else {
            LyricsSource.offered
        }
        prioritizeSyllableSync.value = readBoolean("prioritize_syllable_sync", false)
    }

    fun setAppFont(value: AppFont) {
        writable()
        check(prefs.edit().putString("app_font", value.name).commit()) { "No se pudo guardar la tipografía." }
        appFont.value = value
    }

    fun setLocalFolderSelection(folders: Set<String>, allFolders: Boolean) {
        writable(); require(folders.size <= 16) { "Puedes seleccionar hasta 16 carpetas." }
        (folders - localMusicFolders.value).forEach { LocalFolderRepository.scope(android.net.Uri.parse(it)) }
        check(prefs.edit().putStringSet("local_music_folders", folders.toSet()).putBoolean("all_audio_folders", allFolders).commit()) {
            "No se pudo guardar la selección de carpetas."
        }
        localMusicFolders.value = folders.toSet(); useAllAudioFolders.value = allFolders
        libraryFolderRevision.value += 1
    }

    // ── Existing setters ──────────────────────────────────────────────────────
    fun setThemeMode(value: Int) { writable(); themeMode.value = value.takeIf { it in 0..2 } ?: 0; prefs.edit { putInt("theme_mode", themeMode.value) } }
    fun setDolbyAtmos(value: Boolean) { writable(); dolbyAtmos.value = value; prefs.edit { putBoolean("dolby_atmos", value) } }
    fun setLiquidGlass(value: Boolean) { writable(); liquidGlass.value = value; prefs.edit { putBoolean("liquid_glass", value) } }
    fun setReduceAnimation(value: Boolean) { writable(); reduceAnimation.value = value; prefs.edit { putBoolean("reduce_animation", value) } }
    fun setReduceDynamicBlur(value: Boolean) { writable(); reduceDynamicBlur.value = value; prefs.edit { putBoolean("reduce_dynamic_blur", value) } }
    fun setHapticFeedback(value: Boolean) {
        writable()
        check(prefs.edit().putBoolean("haptic_feedback", value).commit()) { "No se pudo guardar la respuesta háptica." }
        hapticFeedback.value = value
    }
    fun setSyncedLyrics(value: Boolean) { writable(); syncedLyrics.value = value; prefs.edit { putBoolean("synced_lyrics", value) } }
    fun setBlurUnfocusedLyrics(value: Boolean) { writable(); blurUnfocusedLyrics.value = value; prefs.edit { putBoolean("blur_lyrics", value) } }
    fun setFilterNonMusicAudio(value: Boolean) { writable(); filterNonMusicAudio.value = value; prefs.edit { putBoolean("filter_non_music", value) } }
    fun setAutoplay(value: Boolean) { writable(); autoplay.value = value; prefs.edit { putBoolean("autoplay", value) } }
    fun setCrossfadeSeconds(value: Int) { writable(); crossfadeSeconds.value = value; prefs.edit { putInt("crossfade", value) } }

    // Malformed individual preferences keep their original bytes and fall back safely.
    private fun readInt(key: String, fallback: Int) = runCatching { prefs.getInt(key, fallback) }.getOrDefault(fallback)
    private fun readBoolean(key: String, fallback: Boolean) = runCatching { prefs.getBoolean(key, fallback) }.getOrDefault(fallback)
    private fun readFloat(key: String, fallback: Float) = runCatching { prefs.getFloat(key, fallback) }.getOrDefault(fallback)
    private fun readString(key: String, fallback: String?) = runCatching { prefs.getString(key, fallback) }.getOrDefault(fallback)
    private fun readStringSet(key: String, fallback: Set<String>?) = runCatching { prefs.getStringSet(key, fallback) }.getOrDefault(fallback)

    private fun writable() { check(!ProfileRepository.state.value.readOnly) { "Los perfiles están en modo de solo lectura." } }

    private fun safeFloat(value: Float, min: Float, max: Float): Float = if (value.isFinite()) value.coerceIn(min, max) else 0f

    // ── Equalizer setters ─────────────────────────────────────────────────────
    fun setEqualizerEnabled(value: Boolean) { writable(); equalizerEnabled.value = value; prefs.edit { putBoolean("eq_enabled", value) } }
    fun setEqualizerMode(value: EqualizerMode) { writable(); equalizerMode.value = value; prefs.edit { putInt("eq_mode", value.ordinal) } }
    fun setEqualizerTone(x: Int, y: Int) { writable();
        equalizerToneX.value = x; equalizerToneY.value = y
        prefs.edit { putInt("eq_tone_x", x); putInt("eq_tone_y", y) }
    }
    fun setEqualizerFocused(value: Boolean) { writable(); equalizerFocused.value = value; prefs.edit { putBoolean("eq_focused", value) } }
    fun setEqualizerBalance(value: Float) { writable(); equalizerBalance.value = safeFloat(value, -1f, 1f); prefs.edit { putFloat("eq_balance", equalizerBalance.value) } }
    fun setEqualizerBands(bands: List<Float>) { writable();
        equalizerBands.value = List(7) { safeFloat(bands.getOrElse(it) { 0f }, -12f, 12f) }
        equalizerPreset.value = EqualizerPreset.matching(equalizerBands.value)
        prefs.edit {
            putString("eq_preset", equalizerPreset.value.name)
            equalizerBands.value.forEachIndexed { i, v -> putFloat("eq_band_$i", v) }
        }
    }
    fun setEqualizerPreset(value: EqualizerPreset) { writable();
        equalizerPreset.value = value
        prefs.edit { putString("eq_preset", value.name) }
        if (value != EqualizerPreset.CUSTOM) {
            setEqualizerBands(value.bands)
        }
    }

    // ── Spatial / Loudness setters ────────────────────────────────────────────
    fun setSpatialAudio(value: Boolean) { writable(); spatialAudio.value = value; prefs.edit { putBoolean("spatial_audio", value) } }
    fun setLoudnessNormalization(value: Boolean) { writable(); loudnessNormalization.value = value; prefs.edit { putBoolean("loudness_norm", value) } }

    fun setAudioQuality(value: AudioQuality) { writable();
        audioQuality.value = value
        prefs.edit { putInt("audio_quality", value.ordinal) }
    }

    // ── Player setters ────────────────────────────────────────────────────────
    fun setHideVolumeBar(value: Boolean) { writable(); hideVolumeBar.value = value; prefs.edit { putBoolean("hide_volume_bar", value) } }
    fun setShowNerdStats(value: Boolean) { writable(); showNerdStats.value = value; prefs.edit { putBoolean("show_nerd_stats", value) } }
    fun setLyricsOffsetMs(value: Int) { writable(); lyricsOffsetMs.value = value; prefs.edit { putInt("lyrics_offset_ms", value) } }
    fun setShowQueueHistory(value: Boolean) { writable();
        showQueueHistory.value = value
        prefs.edit { putBoolean("show_queue_history", value) }
    }

    // ── Lyrics / translation setters ──────────────────────────────────────────
    fun setTranslationService(endpoint: String, source: String) { writable();
        val normalised = com.music.aurelis.data.lyrics.LyricsTranslation.normaliseEndpoint(endpoint)
        require(normalised != null)
        require(source == "auto" || source in com.music.aurelis.data.lyrics.TranslationLanguages.supportedLanguages)
        translationEndpoint.value = normalised; translationSourceLanguage.value = source
        prefs.edit { putString("translation_endpoint", normalised); putString("translation_source", source) }
    }

    fun setTranslationLanguage(value: String) { writable();
        translationLanguage.value = value
        prefs.edit { putString("translation_language", value) }
    }

    fun setLyricsSources(value: Set<LyricsSource>) { writable();
        val allowed = value.intersect(LyricsSource.offered.toSet())
        lyricsSources.value = allowed
        prefs.edit { putStringSet("lyrics_sources", allowed.map { it.name }.toSet()) }
    }

    fun setLyricsSourceOrder(value: List<LyricsSource>) { writable();
        val allowed = LyricsSource.ordered(value)
        lyricsSourceOrder.value = allowed
        prefs.edit { putString("lyrics_source_order", allowed.joinToString(",") { it.name }) }
    }

    fun setPrioritizeSyllableSync(value: Boolean) { writable();
        prioritizeSyllableSync.value = value
        prefs.edit { putBoolean("prioritize_syllable_sync", value) }
    }

    fun setPaxSenixApiKey(value: String) { writable();
        paxSenixApiKey.value = value
        prefs.edit { putString("paxsenix_api_key", value) }
    }

    /** Back to the enum's own default order and to "every visible source on". */
    fun resetLyricsSourceSettings() {
        writable()
        lyricsSources.value = LyricsSource.offered.toSet()
        lyricsSourceOrder.value = LyricsSource.offered
        prioritizeSyllableSync.value = false
        prefs.edit {
            remove("lyrics_sources")
            remove("lyrics_source_order")
            remove("prioritize_syllable_sync")
        }
    }
}
