// Aurelis © 2026. GPL-3.0-or-later
package com.music.aurelis.data

import android.content.Context
import com.music.aurelis.data.model.LocalSong
import java.io.File

object LocalMediaRepository {
    // Scans and manages local music directories and metadata for Aurelis
    fun scanLocalFolders(folders: List<File>): List<LocalSong> {
        return emptyList()
    }

    suspend fun getAllSongs(context: Context, filterNonMusic: Boolean): List<LocalSong> {
        // Implementation would scan media store and return LocalSong objects
        // This is a placeholder - real implementation queries Android MediaStore
        return emptyList()
    }
}