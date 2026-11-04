// Aurelis © 2026. GPL-3.0-or-later
package com.music.aurelis.ui.audit

/**
 * Auditoría de imports rotos comparando Bitchord con Aurelis-Music.
 * Los imports de Bitchord usan el paquete com.music.bitchord.* y deben mapearse a com.music.aurelis.*.
 *
 * Este archivo es estático y no se compila en el entorno de PolyDev.
 */
object ImportsAudit {

    /** Mapeo de imports de Bitchord → Aurelis. */
    val importMappings: List<ImportMapping> = listOf(
        ImportMapping("com.music.bitchord.ui.theme.Theme", "com.music.aurelis.ui.theme.Theme"),
        ImportMapping("com.music.bitchord.ui.theme.Colors", "com.music.aurelis.ui.theme.Colors"),
        ImportMapping("com.music.bitchord.ui.theme.ArtworkPalette", "com.music.aurelis.ui.theme.ArtworkPalette"),
        ImportMapping("com.music.bitchord.ui.theme.Haptics", "com.music.aurelis.ui.theme.Haptics"),
        ImportMapping("com.music.bitchord.ui.theme.MeshGradient", "com.music.aurelis.ui.theme.MeshGradient"),
        ImportMapping("com.music.bitchord.ui.theme.LiquidGlass", "com.music.aurelis.ui.theme.LiquidGlass"),
        ImportMapping("com.music.bitchord.ui.theme.Haze", "com.music.aurelis.ui.theme.Haze"),
        ImportMapping("com.music.bitchord.ui.navigation.GlassNavBar", "com.music.aurelis.ui.components.GlassNavBar"),
        ImportMapping("com.music.bitchord.ui.navigation.MiniPlayer", "com.music.aurelis.ui.components.MiniPlayer"),
        ImportMapping("com.music.bitchord.ui.navigation.NavigationController", "com.music.aurelis.ui.navigation.NavigationController"),
        ImportMapping("com.music.bitchord.ui.screens.HomeScreen", "com.music.aurelis.ui.screens.HomeScreen"),
        ImportMapping("com.music.bitchord.ui.screens.LibraryScreen", "com.music.aurelis.ui.screens.LibraryScreen"),
        ImportMapping("com.music.bitchord.ui.screens.SearchScreen", "com.music.aurelis.ui.screens.SearchScreen"),
        ImportMapping("com.music.bitchord.ui.components.SongRow", "com.music.aurelis.ui.components.SongRow"),
        ImportMapping("com.music.bitchord.ui.components.AlbumRow", "com.music.aurelis.ui.components.AlbumRow"),
        ImportMapping("com.music.bitchord.ui.screens.NowPlayingScreen", "com.music.aurelis.ui.screens.NowPlayingScreen"),
        ImportMapping("com.music.bitchord.ui.components.AutoMixPanel", "com.music.aurelis.ui.components.AutoMixPanel"),
        ImportMapping("com.music.bitchord.ui.components.SyncedLyricsPanel", "com.music.aurelis.ui.components.SyncedLyricsPanel"),
        ImportMapping("com.music.bitchord.ui.components.DecoderControls", "com.music.aurelis.ui.components.DecoderControls"),
        ImportMapping("com.music.bitchord.ui.components.ThinSlider", "com.music.aurelis.ui.components.ThinSlider"),
        ImportMapping("com.music.bitchord.data.model.Song", "com.music.aurelis.data.model.LocalSong")
    )

    /** Imports conocidos que están rotos o faltan en Aurelis-Music. */
    val brokenImports: List<String> = listOf(
        "com.music.bitchord.ui.theme.* → debe ser com.music.aurelis.ui.theme.*",
        "com.music.bitchord.ui.navigation.* → debe ser com.music.aurelis.ui.navigation.*",
        "com.music.bitchord.ui.components.* → debe ser com.music.aurelis.ui.components.*",
        "com.music.bitchord.ui.screens.* → debe ser com.music.aurelis.ui.screens.*",
        "com.music.bitchord.data.model.Song → debe ser com.music.aurelis.data.model.LocalSong"
    )

    /** Todos los imports auditados. */
    val all: List<ImportMapping> = importMappings
}

data class ImportMapping(
    val bitchordImport: String,
    val aurelisImport: String
)