// SPDX-FileCopyrightText: 2026 Aurelis
// SPDX-License-Identifier: LGPL-3.0-or-later
package com.music.aurelis.data

import android.content.Context
import android.media.MediaMetadataRetriever
import android.net.Uri
import java.util.LinkedHashMap
import java.util.concurrent.CancellationException

internal data class LocalFileMetadata(
    val title: String? = null, val artist: String? = null, val album: String? = null,
    val genre: String? = null, val track: Int? = null, val disc: Int? = null,
    val year: Int? = null, val durationMs: Long? = null, val mimeType: String? = null,
) {
    /** Empty reads do not establish that metadata will remain unavailable. */
    fun hasValues(): Boolean = title != null || artist != null || album != null || genre != null ||
        track != null || disc != null || year != null || durationMs != null || mimeType != null
}

/** Bounded tag cache. Failed reads are retried; unknown modification/size never authorize reuse. */
internal class LocalMetadataReader(private val load: (Uri) -> LocalFileMetadata?) {
    constructor(context: Context) : this({ uri ->
        val reader = MediaMetadataRetriever()
        try {
            reader.setDataSource(context.applicationContext, uri)
            fun text(key: Int) = clean(reader.extractMetadata(key))
            // Accept '3/12' and whitespace, but not implausible track/disc indices.
            fun number(key: Int) = text(key)?.substringBefore('/')?.trim()?.toIntOrNull()?.takeIf { it in 1..999 }
            LocalFileMetadata(text(MediaMetadataRetriever.METADATA_KEY_TITLE), text(MediaMetadataRetriever.METADATA_KEY_ARTIST),
                text(MediaMetadataRetriever.METADATA_KEY_ALBUM), text(MediaMetadataRetriever.METADATA_KEY_GENRE),
                number(MediaMetadataRetriever.METADATA_KEY_CD_TRACK_NUMBER), number(MediaMetadataRetriever.METADATA_KEY_DISC_NUMBER),
                text(MediaMetadataRetriever.METADATA_KEY_YEAR)?.take(4)?.toIntOrNull()?.takeIf { it > 0 },
                text(MediaMetadataRetriever.METADATA_KEY_DURATION)?.toLongOrNull()?.takeIf { it > 0 },
                text(MediaMetadataRetriever.METADATA_KEY_MIMETYPE))
        } catch (denied: SecurityException) { throw denied }
        catch (cancelled: CancellationException) { throw cancelled }
        catch (_: Exception) { null }
        finally { try { reader.release() } catch (_: Exception) { } }
    })
    private data class Key(val uri: String, val size: Long, val modified: Long)
    private val cache = object : LinkedHashMap<Key, LocalFileMetadata>(128, .75f, true) {
        override fun removeEldestEntry(eldest: MutableMap.MutableEntry<Key, LocalFileMetadata>?) = size > 2048
    }
    fun read(uri: Uri, size: Long, modified: Long): LocalFileMetadata? {
        require(uri.scheme == "content")
        val key = Key(uri.toString(), size, modified)
        val cacheable = size > 0 && modified > 0
        if (cacheable) synchronized(cache) { cache[key]?.let { return it } }
        val tags = load(uri) ?: return null
        if (cacheable && tags.hasValues()) synchronized(cache) { cache[key] = tags }
        return tags
    }
    companion object {
        internal fun clean(value: String?): String? = value?.trim()?.takeIf { it.isNotEmpty() && !it.equals("<unknown>", true) }?.take(2048)
    }
}
