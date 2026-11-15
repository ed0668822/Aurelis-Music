// Aurelis © 2026. GPL-3.0-or-later
// Compatible category order adapted from BitChord b690dc1 SettingsSheet.kt, GPLv3.
package com.music.aurelis.ui.screens

import com.music.aurelis.R
import com.music.aurelis.data.LocalSearch
import com.music.aurelis.ui.icons.AurelisIcons
import com.music.aurelis.ui.navigation.SettingsCategory

internal val settingsCategories = listOf(SettingsCategory.AUDIO, SettingsCategory.APPEARANCE,
    SettingsCategory.PERFORMANCE, SettingsCategory.LIBRARY, SettingsCategory.STORAGE,
    SettingsCategory.YOUR_DATA, SettingsCategory.LANGUAGE, SettingsCategory.ADVANCED, SettingsCategory.ABOUT)

internal fun settingsTitle(category: SettingsCategory, resources: android.content.res.Resources): String = resources.getString(when (category) {
    SettingsCategory.GENERAL -> R.string.settings_profile_group
    SettingsCategory.AUDIO -> R.string.settings_playback
    SettingsCategory.APPEARANCE -> R.string.settings_appearance
    SettingsCategory.PERFORMANCE -> R.string.settings_performance
    SettingsCategory.LYRICS -> R.string.settings_lyrics
    SettingsCategory.LIBRARY -> R.string.settings_local_music
    SettingsCategory.STORAGE -> R.string.settings_storage
    SettingsCategory.YOUR_DATA -> R.string.settings_your_data
    SettingsCategory.LANGUAGE -> R.string.settings_language
    SettingsCategory.ADVANCED -> R.string.settings_advanced
    SettingsCategory.ABOUT -> R.string.settings_about
})

/** Search the individual visible control, never substitute a category match. */
internal enum class LocalSetting(val category: SettingsCategory, val title: Int, val subtitle: Int = 0) {
    PHOTO(SettingsCategory.GENERAL, R.string.settings_photo, R.string.settings_photo_hint),
    REMOVE_PHOTO(SettingsCategory.GENERAL, R.string.settings_photo_remove),
    NAME(SettingsCategory.GENERAL, R.string.settings_name),
    AUDIO_LYRICS(SettingsCategory.AUDIO, R.string.settings_lyrics),
    EQUALIZER(SettingsCategory.AUDIO, R.string.settings_equalizer, R.string.settings_equalizer_hint),
    FILE_QUALITY(SettingsCategory.AUDIO, R.string.settings_file_quality, R.string.settings_file_quality_hint),
    AUTOMIX(SettingsCategory.AUDIO, R.string.settings_automix, R.string.settings_automix_hint),
    FONT(SettingsCategory.APPEARANCE, R.string.settings_font),
    THEME(SettingsCategory.APPEARANCE, R.string.settings_theme),
    HAPTICS(SettingsCategory.APPEARANCE, R.string.settings_haptics, R.string.settings_haptics_hint),
    GLASS(SettingsCategory.APPEARANCE, R.string.settings_glass, R.string.settings_glass_hint),
    MOTION(SettingsCategory.PERFORMANCE, R.string.settings_reduce_motion, R.string.settings_reduce_motion_hint),
    BLUR(SettingsCategory.PERFORMANCE, R.string.settings_reduce_blur, R.string.settings_reduce_blur_hint),
    SYNC(SettingsCategory.LYRICS, R.string.settings_synced_lyrics, R.string.settings_synced_lyrics_hint),
    LYRIC_BLUR(SettingsCategory.LYRICS, R.string.settings_lyric_blur, R.string.settings_lyric_blur_hint),
    LRCLIB(SettingsCategory.LYRICS, R.string.settings_lrclib, R.string.settings_lrclib_hint),
    TRANSLATION(SettingsCategory.LYRICS, R.string.settings_translation, R.string.settings_translation_hint),
    ALL_FOLDERS(SettingsCategory.LIBRARY, R.string.settings_all_folders, R.string.settings_all_folders_hint),
    ADD_FOLDER(SettingsCategory.LIBRARY, R.string.settings_add_folder, R.string.settings_add_folder_hint),
    SELECTED_FOLDERS(SettingsCategory.LIBRARY, R.string.settings_selected_folders),
    BROWSE_FOLDERS(SettingsCategory.LIBRARY, R.string.settings_browse_folders),
    FILTER_AUDIO(SettingsCategory.LIBRARY, R.string.settings_filter_audio, R.string.settings_filter_audio_hint),
    STORAGE(SettingsCategory.STORAGE, R.string.settings_storage),
    REPLAY(SettingsCategory.YOUR_DATA, R.string.settings_replay),
    LANGUAGE(SettingsCategory.LANGUAGE, R.string.settings_app_language),
    ADVANCED(SettingsCategory.ADVANCED, R.string.settings_local_music, R.string.settings_library_hint),
    ABOUT(SettingsCategory.ABOUT, R.string.settings_about);

    fun matches(resources: android.content.res.Resources, query: String): Boolean =
        LocalSearch.normalize(resources.getString(title) + " " + if (subtitle == 0) "" else resources.getString(subtitle))
            .contains(LocalSearch.normalize(query))
}

internal fun settingsIcon(category: SettingsCategory) = when (category) {
    SettingsCategory.AUDIO -> AurelisIcons.Play
    SettingsCategory.APPEARANCE -> AurelisIcons.Appearance
    SettingsCategory.PERFORMANCE -> AurelisIcons.Performance
    SettingsCategory.LIBRARY -> AurelisIcons.Library
    SettingsCategory.LYRICS -> AurelisIcons.Lyrics
    SettingsCategory.STORAGE -> AurelisIcons.Storage
    SettingsCategory.YOUR_DATA -> AurelisIcons.Clock
    SettingsCategory.LANGUAGE -> AurelisIcons.Language
    SettingsCategory.ADVANCED -> AurelisIcons.Settings
    SettingsCategory.ABOUT -> AurelisIcons.Info
    SettingsCategory.GENERAL -> AurelisIcons.Artist
}
