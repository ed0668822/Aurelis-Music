// Aurelis © 2026. GPL-3.0-or-later
package com.music.aurelis.data.stats

import com.music.aurelis.data.model.LocalSong
import com.music.aurelis.data.LocalUserLibrary

object ListeningStats {
    private var library: LocalUserLibrary? = null
    fun init(library: LocalUserLibrary) { this.library = library }
    fun onTrackChanged(songId: String?) { library?.startListeningSession(songId) }
    fun record(song: LocalSong, deltaMs: Long, completed: Boolean) {
        library?.recordListening(song, deltaMs, completed)
    }

    fun flush() {
        library?.flush()
    }
}
