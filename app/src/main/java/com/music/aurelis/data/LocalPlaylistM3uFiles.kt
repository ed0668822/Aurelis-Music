// SPDX-FileCopyrightText: 2026 Aurelis
// SPDX-License-Identifier: LGPL-3.0-or-later

package com.music.aurelis.data

import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import com.music.aurelis.data.model.Song
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream

/** SAF-based playlist I/O. It never deletes or modifies audio files. */
internal object LocalPlaylistM3uFiles {
    private const val MAX_BYTES = 2 * 1024 * 1024

    data class Preview(val title: String, val result: LocalPlaylistM3u.Resolved)

    suspend fun read(context: Context, uri: Uri): Preview = withContext(Dispatchers.IO) {
        val bytes = ByteArrayOutputStream()
        val buffer = ByteArray(8 * 1024)
        context.contentResolver.openInputStream(uri)?.use { source ->
            while (true) {
                val n = source.read(buffer)
                if (n < 0) break
                require(bytes.size() + n <= MAX_BYTES) { "Playlist is larger than 2 MB" }
                bytes.write(buffer, 0, n)
            }
        } ?: error("Cannot open playlist")
        val title = displayName(context, uri)
            .substringBeforeLast('.', "Imported playlist").ifBlank { "Imported playlist" }
            .take(120)
        val entries = LocalPlaylistM3u.parse(bytes.toString(Charsets.UTF_8.name()))
        val songs = LocalMediaRepository.getLocalMusic(context)
        Preview(title, LocalPlaylistM3u.resolve(entries, songs))
    }

    suspend fun write(context: Context, uri: Uri, songs: List<Song>) = withContext(Dispatchers.IO) {
        val content = LocalPlaylistM3u.export(songs)
        context.contentResolver.openOutputStream(uri, "wt")?.use { stream ->
            stream.write(content.toByteArray(Charsets.UTF_8))
            stream.flush()
        } ?: error("Cannot write playlist")
    }

    private fun displayName(context: Context, uri: Uri): String {
        return runCatching {
            context.contentResolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)
                ?.use { cursor ->
                    val column = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                    if (column >= 0 && cursor.moveToFirst()) cursor.getString(column) else null
                }
        }.getOrNull().orEmpty().ifBlank { "Imported playlist.m3u8" }
    }
}
