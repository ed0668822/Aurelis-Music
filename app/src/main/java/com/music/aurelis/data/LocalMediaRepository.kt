// Aurelis © 2026. GPL-3.0-or-later
package com.music.aurelis.data

import android.content.ContentUris
import android.content.Context
import android.database.Cursor
import android.net.Uri
import android.provider.MediaStore
import com.music.aurelis.data.model.LocalSong
import java.io.File
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Repositorio de música local. Toda la lectura proviene de Android MediaStore;
 * no hay peticiones de red en este archivo.
 */
object LocalMediaRepository {

    private const val MIN_DURATION_MS = 500L

    private val SONG_PROJECTION = arrayOf(
        MediaStore.Audio.Media._ID,
        MediaStore.Audio.Media.TITLE,
        MediaStore.Audio.Media.ARTIST,
        MediaStore.Audio.Media.ALBUM,
        MediaStore.Audio.Media.ALBUM_ID,
        MediaStore.Audio.Media.DATA,
        MediaStore.Audio.Media.DURATION,
        MediaStore.Audio.Media.TRACK,
        MediaStore.Audio.Media.YEAR,
        MediaStore.Audio.Media.DATE_ADDED,
        MediaStore.Audio.Media.SIZE,
        MediaStore.Audio.Media.IS_MUSIC,
    )

    fun scanLocalFolders(folders: List<File>): List<LocalSong> = emptyList()

    suspend fun getAllSongs(context: Context, filterNonMusic: Boolean): List<LocalSong> {
        return withContext(Dispatchers.Default) {
            val resolver = context.contentResolver
            val uri: Uri = MediaStore.Audio.Media.EXTERNAL_CONTENT_URI

            val selection = buildSelection(filterNonMusic)
            val selectionArgs = buildSelectionArgs(filterNonMusic)

            val cursor: Cursor? = resolver.query(
                uri,
                SONG_PROJECTION,
                selection,
                selectionArgs,
                "${MediaStore.Audio.Media.DATE_ADDED} DESC",
            )

            cursor?.use { c ->
                if (c.count == 0) {
                    emptyList()
                } else {
                    val idIdx = c.getColumnIndex(MediaStore.Audio.Media._ID)
                    val titleIdx = c.getColumnIndex(MediaStore.Audio.Media.TITLE)
                    val artistIdx = c.getColumnIndex(MediaStore.Audio.Media.ARTIST)
                    val albumIdx = c.getColumnIndex(MediaStore.Audio.Media.ALBUM)
                    val albumIdIdx = c.getColumnIndex(MediaStore.Audio.Media.ALBUM_ID)
                    val dataIdx = c.getColumnIndex(MediaStore.Audio.Media.DATA)
                    val durationIdx = c.getColumnIndex(MediaStore.Audio.Media.DURATION)
                    val trackIdx = c.getColumnIndex(MediaStore.Audio.Media.TRACK)
                    val yearIdx = c.getColumnIndex(MediaStore.Audio.Media.YEAR)
                    val dateAddedIdx = c.getColumnIndex(MediaStore.Audio.Media.DATE_ADDED)
                    val sizeIdx = c.getColumnIndex(MediaStore.Audio.Media.SIZE)

                    val songs = mutableListOf<LocalSong>()
                    while (c.moveToNext()) {
                        val id = c.getString(idIdx)
                        val title = c.getString(titleIdx)
                        val artist = c.getString(artistIdx)
                        val album = c.getString(albumIdx)
                        val filePath = c.getString(dataIdx)
                        val durationMs = c.getLong(durationIdx)
                        val trackNumber = readIntOrNull(c, trackIdx)
                        val year = readIntOrNull(c, yearIdx)?.takeIf { it > 0 }
                        val dateAddedMs = c.getLong(dateAddedIdx) * 1000L
                        val sizeBytes = c.getLong(sizeIdx)

                        val albumId = if (albumIdIdx >= 0 && !c.isNull(albumIdIdx)) {
                            c.getLong(albumIdIdx)
                        } else {
                            0L
                        }
                        // Álbum artwork URI: content://media/external/audio/albumart/<albumId>
                        val artworkUri: String? = if (albumId > 0L) {
                            ContentUris.withAppendedId(
                                Uri.parse("content://media/external/audio/albumart"),
                                albumId,
                            ).toString()
                        } else null

                        val contentUri = ContentUris.withAppendedId(
                            MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
                            id.toLongOrNull() ?: 0L,
                        ).toString()

                        songs.add(
                            LocalSong(
                                id = id,
                                title = title,
                                artist = artist,
                                album = if (album.isNullOrBlank()) null else album,
                                contentUri = contentUri,
                                artworkUri = artworkUri,
                                durationMs = durationMs,
                                filePath = filePath,
                                trackNumber = trackNumber,
                                year = year,
                                dateAddedMs = dateAddedMs,
                                sizeBytes = sizeBytes,
                            ),
                        )
                    }
                    songs.toList()
                }
            } ?: emptyList()
        }
    }

    private fun buildSelection(filterNonMusic: Boolean): String =
        if (filterNonMusic) {
            "${MediaStore.Audio.Media.IS_MUSIC} = 1 AND ${MediaStore.Audio.Media.DURATION} >= ? AND ${MediaStore.Audio.Media.TITLE} IS NOT NULL"
        } else {
            "${MediaStore.Audio.Media.DURATION} >= ? AND ${MediaStore.Audio.Media.TITLE} IS NOT NULL"
        }

    private fun buildSelectionArgs(filterNonMusic: Boolean): Array<String> =
        arrayOf(MIN_DURATION_MS.toString())

    private fun readIntOrNull(c: Cursor, idx: Int): Int? =
        if (idx >= 0 && !c.isNull(idx)) c.getInt(idx) else null
}
