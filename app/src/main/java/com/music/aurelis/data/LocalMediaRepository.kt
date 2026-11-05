// Aurelis © 2026. GPL-3.0-or-later
package com.music.aurelis.data

import android.content.ContentUris
import android.content.Context
import android.database.Cursor
import android.net.Uri
import android.provider.MediaStore
import com.music.aurelis.R
import com.music.aurelis.data.model.LocalSong
import com.music.aurelis.data.model.MetadataOrigin
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
        MediaStore.Audio.Media.VOLUME_NAME, MediaStore.Audio.Media.RELATIVE_PATH,
        MediaStore.Audio.Media.DATE_MODIFIED, MediaStore.Audio.Media.MIME_TYPE,
        MediaStore.Audio.Media.IS_PENDING, MediaStore.Audio.Media.IS_TRASHED,
    )

    private var cachedContext: Context? = null
    private var cachedReader: LocalMetadataReader? = null
    @Synchronized internal fun metadataReader(context: Context): LocalMetadataReader {
        val app = context.applicationContext
        if (cachedContext !== app) { cachedContext = app; cachedReader = LocalMetadataReader(app) }
        return checkNotNull(cachedReader)
    }

    suspend fun getAllSongs(context: Context, filterNonMusic: Boolean): List<LocalSong> =
        getAllSongs(context, filterNonMusic, metadataReader(context))

    internal suspend fun getAllSongs(context: Context, filterNonMusic: Boolean, tags: LocalMetadataReader): List<LocalSong> = withContext(Dispatchers.IO) {
        val uri = MediaStore.Audio.Media.EXTERNAL_CONTENT_URI
        val selection = buildString {
            append("(${MediaStore.Audio.Media.DURATION} >= ? OR ${MediaStore.Audio.Media.DURATION} IS NULL) AND ${MediaStore.Audio.Media.IS_PENDING} = 0 AND ${MediaStore.Audio.Media.IS_TRASHED} = 0")
            if (filterNonMusic) append(" AND ${MediaStore.Audio.Media.IS_MUSIC} != 0")
        }
        fun query(columns: Array<String>) = context.contentResolver.query(uri, columns, selection,
            arrayOf(MIN_DURATION_MS.toString()), "${MediaStore.Audio.Media.DATE_ADDED} DESC, ${MediaStore.Audio.Media._ID} ASC")
        val cursor = try { query(projection) }
        catch (_: IllegalArgumentException) {
            // Some device providers reject optional columns; keep the minimum catalogue usable.
            query(projection.filterNot { it in setOf(MediaStore.Audio.Media.GENRE, MediaStore.Audio.Media.VOLUME_NAME,
                MediaStore.Audio.Media.RELATIVE_PATH, MediaStore.Audio.Media.DATE_MODIFIED, MediaStore.Audio.Media.MIME_TYPE) }.toTypedArray())
        } ?: throw IOException(context.getString(R.string.library_read_error))
        cursor.use { c ->
            val songs = linkedMapOf<String, LocalSong>()
            while (c.moveToNext()) {
                currentCoroutineContext().ensureActive()
                val id = c.number(MediaStore.Audio.Media._ID)?.takeIf { it > 0 } ?: continue
                val knownDuration = c.number(MediaStore.Audio.Media.DURATION)
                if ((knownDuration != null && knownDuration < MIN_DURATION_MS) || c.number(MediaStore.Audio.Media.IS_PENDING) == 1L ||
                    c.number(MediaStore.Audio.Media.IS_TRASHED) == 1L ||
                    (filterNonMusic && c.number(MediaStore.Audio.Media.IS_MUSIC) == 0L)) continue
                val volume = c.metadata(MediaStore.Audio.Media.VOLUME_NAME)?.takeIf { it.matches(Regex("[a-zA-Z0-9_-]+")) }
                val contentUri = ContentUris.withAppendedId(uri, id)
                val size = c.number(MediaStore.Audio.Media.SIZE)?.takeIf { it >= 0 }
                val modified = seconds(c.number(MediaStore.Audio.Media.DATE_MODIFIED))
                val genre = c.metadata(MediaStore.Audio.Media.GENRE)
                val title = c.metadata(MediaStore.Audio.Media.TITLE)
                val artist = c.metadata(MediaStore.Audio.Media.ARTIST)
                val album = c.metadata(MediaStore.Audio.Media.ALBUM)
                val embedded = if (genre == null || title == null || artist == null || album == null || knownDuration == null)
                    tags.read(contentUri, size ?: 0, modified) else null
                currentCoroutineContext().ensureActive()
                val duration = knownDuration ?: embedded?.durationMs ?: continue
                if (duration < MIN_DURATION_MS) continue
                val albumId = c.number(MediaStore.Audio.Media.ALBUM_ID)?.takeIf { it > 0 }
                val artistId = c.number(MediaStore.Audio.Media.ARTIST_ID)?.takeIf { it > 0 }
                val packedTrack = c.number(MediaStore.Audio.Media.TRACK)?.takeIf { it in 1..Int.MAX_VALUE.toLong() }?.toInt()
                val existing = songs[id.toString()]
                if (existing != null && existing.volumeName != null && volume != null && existing.volumeName != volume)
                    throw IOException("MediaStore devolvió identificadores ambiguos entre volúmenes.")
                songs[id.toString()] = LocalSong(
                    id = id.toString(), title = title ?: embedded?.title
                        ?: c.metadata(MediaStore.Audio.Media.DISPLAY_NAME)?.substringBeforeLast('.')?.takeIf { it.isNotBlank() }
                        ?: context.getString(R.string.library_unknown_title),
                    artist = artist ?: embedded?.artist ?: context.getString(R.string.library_unknown_artist),
                    album = album ?: embedded?.album,
                    contentUri = contentUri.toString(),
                    artworkUri = albumId?.let { ContentUris.withAppendedId(Uri.parse("content://media/external/audio/albumart"), it).toString() },
                    durationMs = duration, filePath = c.text(MediaStore.Audio.Media.DATA),
                    trackNumber = packedTrack?.rem(1000)?.takeIf { it > 0 } ?: embedded?.track,
                    discNumber = packedTrack?.div(1000)?.takeIf { it > 0 } ?: embedded?.disc,
                    year = c.number(MediaStore.Audio.Media.YEAR)?.takeIf { it in 1..Int.MAX_VALUE.toLong() }?.toInt() ?: embedded?.year,
                    dateAddedMs = seconds(c.number(MediaStore.Audio.Media.DATE_ADDED)),
                    sizeBytes = size ?: 0, sizeBytesKnown = size != null,
                    albumId = albumId?.toString(), artistId = artistId?.toString(),
                    genre = genre ?: embedded?.genre,
                    genreOrigin = if (genre != null) MetadataOrigin.MEDIA_STORE else if (embedded?.genre != null) MetadataOrigin.FILE_TAG else null,
                    displayName = c.metadata(MediaStore.Audio.Media.DISPLAY_NAME), volumeName = volume,
                    relativePath = c.metadata(MediaStore.Audio.Media.RELATIVE_PATH), dateModifiedMs = modified,
                    mimeType = c.metadata(MediaStore.Audio.Media.MIME_TYPE) ?: embedded?.mimeType,
                )
            }
            songs.values.toList()
        }
    }

    private fun seconds(value: Long?) = (value ?: 0).coerceIn(0, Long.MAX_VALUE / 1000) * 1000

    private fun Cursor.text(column: String): String? {
        val index = getColumnIndex(column)
        return if (index < 0 || isNull(index)) null else getString(index)
    }
    private fun Cursor.number(column: String): Long? = text(column)?.toLongOrNull()
    private fun Cursor.metadata(column: String): String? = LocalMetadataReader.clean(text(column))
}
