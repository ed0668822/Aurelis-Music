package com.music.aurelis.data.discord

import com.music.aurelis.data.model.Song

/** Safe metadata helpers. Legacy token-gateway functionality was removed. */
object DiscordRPC {
    const val PROJECT_URL = "https://github.com/ed0668822/Aurelis-Music"
    const val DEFAULT_BUTTON_1 = "Listen on YouTube Music"
    const val DEFAULT_BUTTON_2 = "Aurelis Music on GitHub"
    private val YOUTUBE_VIDEO_ID = Regex("^[A-Za-z0-9_-]{11}$")

    fun youtubeWatchUrlOrNull(song: Song): String? {
        if (song.localUri != null || !YOUTUBE_VIDEO_ID.matches(song.videoId)) return null
        return "https://music.youtube.com/watch?v=${song.videoId}"
    }
    fun watchUrl(song: Song): String = youtubeWatchUrlOrNull(song) ?: PROJECT_URL
    fun resolveVariables(text: String, song: Song): String =
        text.replace("{song_name}", song.title)
            .replace("{artist_name}", song.artist)
            .replace("{album_name}", song.albumName ?: "")
}
