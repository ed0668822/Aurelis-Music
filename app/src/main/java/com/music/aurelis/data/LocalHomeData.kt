package com.music.aurelis.data

import com.music.aurelis.data.model.LocalSong
import com.music.aurelis.playback.QueueBuilder

data class LocalHomeData(
    val recent: List<LocalSong>,
    val mostPlayed: List<LocalSong>,
    val favorites: List<LocalSong>,
    val recommendations: List<LocalSong>,
    val mix: List<LocalSong>,
) {
    companion object {
        fun build(songs: List<LocalSong>, user: LocalUserLibraryState, currentId: String?): LocalHomeData {
            val byId = songs.associateBy { it.id }
            val recent = user.listening.entries.sortedByDescending { it.value.lastPlayedAt }
                .mapNotNull { byId[it.key] }.take(20)
            val most = user.listening.entries.filter { it.value.playCount > 0 }
                .sortedWith(compareByDescending<Map.Entry<String, LocalListening>> { it.value.playCount }
                    .thenByDescending { it.value.listenedMs }.thenBy { it.key })
                .mapNotNull { byId[it.key] }.take(20)
            val favorites = songs.filter { it.id in user.favorites }
            val seed = byId[currentId] ?: recent.firstOrNull() ?: favorites.firstOrNull() ?: songs.firstOrNull()
            val candidates = songs.sortedWith(compareByDescending<LocalSong> { it.id in user.favorites }
                .thenBy { user.listening[it.id]?.lastPlayedAt ?: 0L }.thenBy { it.id })
            val suggestions = QueueBuilder.extend(listOfNotNull(seed), candidates, 20)
            return LocalHomeData(recent, most, favorites, suggestions,
                listOfNotNull(seed) + suggestions)
        }
    }
}
