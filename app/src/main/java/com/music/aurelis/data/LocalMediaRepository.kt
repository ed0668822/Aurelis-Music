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

    private const val MIN_DURATION_MS = 500L // ignora tonos, notificaciones y clips muy cortos

    /** Columnas leídas de MediaStore.Audio.Media. */
    private val SONG_PROJECTION = arrayOf(
        MediaStore.Audio.Media._ID,
        MediaStore.Audio.Media.TITLE,
        MediaStore.Audio.Media.ARTIST,
        MediaStore.Audio.Media.ALBUM,
        MediaStore.Audio.Media.DATA,
        MediaStore.Audio.Media.ALBUM_ART,
        MediaStore.Audio.Media.DURATION,
        MediaStore.Audio.Media.TRACK,
        MediaStore.Audio.Media.YEAR,
        MediaStore.Audio.Media.DATE_ADDED,
        MediaStore.Audio.Media.SIZE,
        MediaStore.Audio.Media.IS_MUSIC,
    )

    /** Escanea carpetas locales (mantiene la firma existente; la implementación
     *  real de MediaStore no necesita carpetas explícitas). */
    fun scanLocalFolders(folders: List<File>): List<LocalSong> {
        return emptyList()
    }

    /** Devuelve todas las canciones locales indexadas por MediaStore. */
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
                    val dataIdx = c.getColumnIndex(MediaStore.Audio.Media.DATA)
                    val artworkIdx = c.getColumnIndex(MediaStore.Audio.Media.ALBUM_ART)
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
                        val artworkUri = c.getString(artworkIdx)
                        val durationMs = c.getLong(durationIdx)
                        val trackNumber = readIntOrNull(c, trackIdx)
                        val year = readIntOrNull(c, yearIdx)?.takeIf { it > 0 }
                        val dateAddedMs = c.getLong(dateAddedIdx) * 1000L
                        val sizeBytes = c.getLong(sizeIdx)

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

    private fun buildSelection(filterNonMusic: Boolean): String {
        return if (filterNonMusic) {
            "${MediaStore.Audio.Media.IS_MUSIC} = 1 AND ${MediaStore.Audio.Media.DURATION} >= ? AND ${MediaStore.Audio.Media.TITLE} IS NOT NULL"
        } else {
            "${MediaStore.Audio.Media.DURATION} >= ? AND ${MediaStore.Audio.Media.TITLE} IS NOT NULL"
        }
    }

    private fun buildSelectionArgs(filterNonMusic: Boolean): Array<String> {
        return arrayOf(MIN_DURATION_MS.toString())
    }

    /** Lee un entero de una columna de cursor; devuelve null si no está disponible o es inválido. */
    private fun readIntOrNull(c: Cursor, idx: Int): Int? {
        return if (idx >= 0 && !c.isNull(idx)) c.getInt(idx) else null
    }
}
