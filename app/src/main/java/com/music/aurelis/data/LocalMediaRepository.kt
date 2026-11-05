// Aurelis © 2026. GPL-3.0-or-later
package com.music.aurelis.data

import android.content.ContentUris
import android.content.Context
import android.database.Cursor
import android.net.Uri
import android.provider.MediaStore
import com.music.aurelis.R
import com.music.aurelis.data.model.LocalSong
import java.io.File
import java.io.IOException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.withContext

/** MediaStore is the only catalogue; no filesystem crawling or network metadata. */
object LocalMediaRepository {
    private const val MIN_DURATION_MS = 500L
    private val projection = arrayOf(
        MediaStore.Audio.Media._ID, MediaStore.Audio.Media.TITLE,
        MediaStore.Audio.Media.DISPLAY_NAME, MediaStore.Audio.Media.ARTIST,
        MediaStore.Audio.Media.ARTIST_ID, MediaStore.Audio.Media.ALBUM,
        MediaStore.Audio.Media.ALBUM_ID, MediaStore.Audio.Media.DATA,
        MediaStore.Audio.Media.DURATION, MediaStore.Audio.Media.TRACK,
        MediaStore.Audio.Media.YEAR, MediaStore.Audio.Media.DATE_ADDED,
        MediaStore.Audio.Media.SIZE, MediaStore.Audio.Media.GENRE, MediaStore.Audio.Media.IS_MUSIC,
        MediaStore.Audio.Media.IS_PENDING, MediaStore.Audio.Media.IS_TRASHED,
    )

    // Legacy API is not used by the MediaStore catalogue; scoped storage is not bypassed.
    fun scanLocalFolders(folders: List<File>): List<LocalSong> = emptyList()

    suspend fun getAllSongs(context: Context, filterNonMusic: Boolean): List<LocalSong> = withContext(Dispatchers.IO) {
        val uri = MediaStore.Audio.Media.EXTERNAL_CONTENT_URI
        val selection = buildString {
            append("${MediaStore.Audio.Media.DURATION} >= ? AND ${MediaStore.Audio.Media.IS_PENDING} = 0 AND ${MediaStore.Audio.Media.IS_TRASHED} = 0")
            if (filterNonMusic) append(" AND ${MediaStore.Audio.Media.IS_MUSIC} != 0")
        }
        val cursor = context.contentResolver.query(uri, projection, selection,
            arrayOf(MIN_DURATION_MS.toString()), "${MediaStore.Audio.Media.DATE_ADDED} DESC, ${MediaStore.Audio.Media._ID} ASC")
            ?: throw IOException(context.getString(R.string.library_read_error))
        cursor.use { c ->
            val songs = linkedMapOf<String, LocalSong>()
            while (c.moveToNext()) {
                currentCoroutineContext().ensureActive()
                val id = c.number(MediaStore.Audio.Media._ID)?.takeIf { it > 0 } ?: continue
                val duration = c.number(MediaStore.Audio.Media.DURATION) ?: continue
                if (duration < MIN_DURATION_MS || c.number(MediaStore.Audio.Media.IS_PENDING) == 1L ||
                    c.number(MediaStore.Audio.Media.IS_TRASHED) == 1L ||
                    (filterNonMusic && c.number(MediaStore.Audio.Media.IS_MUSIC) == 0L)) continue
                val albumId = c.number(MediaStore.Audio.Media.ALBUM_ID)?.takeIf { it > 0 }
                val artistId = c.number(MediaStore.Audio.Media.ARTIST_ID)?.takeIf { it > 0 }
                val packedTrack = c.number(MediaStore.Audio.Media.TRACK)?.takeIf { it in 1..Int.MAX_VALUE.toLong() }?.toInt()
                val title = c.metadata(MediaStore.Audio.Media.TITLE)
                    ?: c.metadata(MediaStore.Audio.Media.DISPLAY_NAME)?.substringBeforeLast('.')?.takeIf { it.isNotBlank() }
                    ?: context.getString(R.string.library_unknown_title)
                songs[id.toString()] = LocalSong(
                    id = id.toString(), title = title,
                    artist = c.metadata(MediaStore.Audio.Media.ARTIST) ?: context.getString(R.string.library_unknown_artist),
                    album = c.metadata(MediaStore.Audio.Media.ALBUM),
                    contentUri = ContentUris.withAppendedId(uri, id).toString(),
                    artworkUri = albumId?.let { ContentUris.withAppendedId(Uri.parse("content://media/external/audio/albumart"), it).toString() },
                    durationMs = duration, filePath = c.text(MediaStore.Audio.Media.DATA),
                    trackNumber = packedTrack?.rem(1000)?.takeIf { it > 0 },
                    discNumber = packedTrack?.div(1000)?.takeIf { it > 0 },
                    year = c.number(MediaStore.Audio.Media.YEAR)?.takeIf { it in 1..Int.MAX_VALUE.toLong() }?.toInt(),
                    dateAddedMs = (c.number(MediaStore.Audio.Media.DATE_ADDED) ?: 0).coerceIn(0, Long.MAX_VALUE / 1000) * 1000,
                    sizeBytes = (c.number(MediaStore.Audio.Media.SIZE) ?: 0).coerceAtLeast(0),
                    albumId = albumId?.toString(), artistId = artistId?.toString(),
                    genre = c.metadata(MediaStore.Audio.Media.GENRE),
                )
            }
            songs.values.toList()
        }
    }

    private fun Cursor.text(column: String): String? {
        val index = getColumnIndex(column)
        return if (index < 0 || isNull(index)) null else getString(index)
    }
    private fun Cursor.number(column: String): Long? = text(column)?.toLongOrNull()
    private fun Cursor.metadata(column: String): String? = text(column)?.trim()?.takeIf { it.isNotEmpty() && it != "<unknown>" }
}
