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
        if (cover.isFile && cover.length() > 0L) return@withContext(Uri.fromFile(cover).toString())
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
            Uri.fromFile(cover).toString()
        }.getOrNull()
    }
}
