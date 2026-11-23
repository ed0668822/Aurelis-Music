package com.music.aurelis.data

import android.content.Context
import android.media.MediaMetadataRetriever
import android.net.Uri
import com.music.aurelis.data.model.Song
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.security.MessageDigest

/**
 * Embedded picture fallback for local audio with no indexed MediaStore album artwork.
 * Runs only for an uncached visible song; never reads images on the UI thread.
 */
object LocalEmbeddedArtwork {
    suspend fun resolve(context: Context, song: Song): String? = withContext(Dispatchers.IO) {
        val localUri = song.localUri?.takeIf { it.isNotBlank() } ?: return@withContext null
        val key = "$localUri:${song.localDateModifiedSeconds ?: 0L}"
        val name = MessageDigest.getInstance("SHA-256")
            .digest(key.toByteArray())
            .joinToString("") { "%02x".format(it) }
        val directory = File(context.cacheDir, "embedded-album-art")
        val cover = File(directory, "$name.jpg")
        if (cover.isFile && cover.length() > 0L) {
            cover.setLastModified(System.currentTimeMillis())
            return@withContext Uri.fromFile(cover).toString()
        }
        if (cover.isFile) cover.delete()
        val bytes = runCatching {
            val reader = MediaMetadataRetriever()
            try {
                reader.setDataSource(context, Uri.parse(localUri))
                reader.embeddedPicture
            } finally {
                reader.release()
            }
        }.getOrNull() ?: return@withContext null
        if (bytes.isEmpty() || bytes.size > 12 * 1024 * 1024) return@withContext null
        runCatching {
            directory.mkdirs()
            val temporary = File.createTempFile("cover-", ".tmp", directory)
            try {
                temporary.writeBytes(bytes)
                if (!temporary.renameTo(cover)) {
                    temporary.copyTo(cover, overwrite = true)
                }
            } finally {
                temporary.delete()
            }
            pruneEmbeddedArtworkCache(directory, MAX_CACHE_BYTES, cover)
            Uri.fromFile(cover).toString()
        }.getOrNull()
    }

    private const val MAX_CACHE_BYTES = 64L * 1024 * 1024
}

/** Evict the oldest complete cover files, preserving the active art and temporary writes. */
internal fun pruneEmbeddedArtworkCache(directory: File, maxBytes: Long, protectedFile: File? = null) {
    val covers = directory.listFiles()?.filter { it.isFile && it.extension == "jpg" } ?: return
    var total = covers.sumOf { it.length().coerceAtLeast(0L) }
    if (total <= maxBytes) return
    for (candidate in covers.sortedBy(File::lastModified)) {
        if (total <= maxBytes) break
        if (candidate == protectedFile) continue
        val size = candidate.length().coerceAtLeast(0L)
        if (candidate.delete()) total -= size
    }
}
