// Aurelis © 2026. GPL-3.0-or-later
// Compatible category order adapted from BitChord b690dc1 SettingsSheet.kt, GPLv3.
package com.music.aurelis.ui.screens

import com.music.aurelis.data.LocalSearch
import com.music.aurelis.ui.icons.AurelisIcons
import com.music.aurelis.ui.navigation.SettingsCategory

internal val settingsCategories = listOf(SettingsCategory.AUDIO, SettingsCategory.APPEARANCE,
    SettingsCategory.PERFORMANCE, SettingsCategory.LIBRARY, SettingsCategory.STORAGE,
    SettingsCategory.YOUR_DATA, SettingsCategory.LANGUAGE, SettingsCategory.ADVANCED, SettingsCategory.ABOUT)

internal fun settingsTitle(category: SettingsCategory): String = when (category) {
    SettingsCategory.GENERAL -> "Perfil del dispositivo"
    SettingsCategory.AUDIO -> "Reproducción"
    SettingsCategory.APPEARANCE -> "Apariencia"
    SettingsCategory.PERFORMANCE -> "Rendimiento"
    SettingsCategory.LYRICS -> "Letras"
    SettingsCategory.LIBRARY -> "Música local"
    SettingsCategory.STORAGE -> "Almacenamiento"
    SettingsCategory.YOUR_DATA -> "Tus datos"
    SettingsCategory.LANGUAGE -> "Idioma"
    SettingsCategory.ADVANCED -> "Avanzado"
    SettingsCategory.ABOUT -> "Acerca de"
}

internal fun settingsKeywords(category: SettingsCategory): String = LocalSearch.normalize(settingsTitle(category) + " " + when (category) {
    SettingsCategory.GENERAL -> "nombre perfil foto"
    SettingsCategory.AUDIO -> "audio equalizer ecualizador calidad archivo automix"
    SettingsCategory.APPEARANCE -> "tipografía fuente sistema Vazirmatn tema claro oscuro liquid glass respuesta háptica vibración"
    SettingsCategory.PERFORMANCE -> "reducir animaciones desenfoque"
    SettingsCategory.LYRICS -> "sincronizadas líneas inactivas LRCLIB traducción"
    SettingsCategory.LIBRARY -> "biblioteca carpetas audio filtrar elegir carpeta local"
    SettingsCategory.YOUR_DATA -> "Replay estadísticas historial"
    else -> ""
})

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
