// Aurelis © 2026. GPL-3.0-or-later
package com.music.aurelis.data

import android.content.Context
import java.io.File

class LocalMediaRepository {
    // Scans and manages local music directories and metadata for Aurelis
    fun scanLocalFolders(folders: List<File>): List<Map<String, String>> {
        return emptyList()
    }

    suspend fun getAllSongs(context: Context, filterNonMusic: Boolean): List<LocalSong> {
        return emptyList()
    }
}