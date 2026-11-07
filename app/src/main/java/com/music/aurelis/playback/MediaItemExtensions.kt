// Aurelis © 2026. GPL-3.0-or-later
package com.music.aurelis.playback

import androidx.media3.common.MediaItem
import com.music.aurelis.data.model.LocalSong
import com.music.aurelis.data.model.QueueTier
import java.util.concurrent.ConcurrentHashMap

internal const val EXTRA_QUEUE_ENTRY_ID = "bitchord.queueEntryId"
internal const val EXTRA_CANONICAL_INDEX = "bitchord.canonicalIndex"
internal const val EXTRA_QUEUE_TIER = "bitchord.queueTier"

internal val mediaItemEntryIds = ConcurrentHashMap<MediaItem, String>()
internal val mediaItemCanonicalIndices = ConcurrentHashMap<MediaItem, Int>()
internal val mediaItemTiers = ConcurrentHashMap<MediaItem, QueueTier>()

val MediaItem.queueEntryId: String?
    get() = mediaItemEntryIds[this] ?: mediaMetadata.extras?.getString(EXTRA_QUEUE_ENTRY_ID)

val MediaItem.canonicalIndex: Int?
    get() = mediaItemCanonicalIndices[this] ?: mediaMetadata.extras?.getInt(EXTRA_CANONICAL_INDEX)

val MediaItem.queueTier: QueueTier
    get() = mediaItemTiers[this] ?: mediaMetadata.extras?.getString(EXTRA_QUEUE_TIER)?.let { 
        runCatching { QueueTier.valueOf(it) }.getOrNull() 
    } ?: QueueTier.CONTEXT

/** A new occurrence always gets a new identity, even when the same row is added twice. */
internal fun LocalSong.toQueueMediaItem(): MediaItem =
    copy(queueEntryId = java.util.UUID.randomUUID().toString()).toMediaItem()

fun LocalSong.toMediaItem(): MediaItem {
    return MediaItem.Builder()
        .setMediaId(id)
        .setUri(contentUri)
        .setMediaMetadata(
            androidx.media3.common.MediaMetadata.Builder()
                .setTitle(title)
                .setArtist(artist)
                .setAlbumTitle(album)
                .setIsPlayable(true)
                .setIsBrowsable(false)
                .setArtworkUri(artworkUri?.let { android.net.Uri.parse(it) })
                .setExtras(android.os.Bundle().apply {
                    putString(EXTRA_QUEUE_ENTRY_ID, queueEntryId)
                    putString(EXTRA_QUEUE_TIER, queueTier.name)
                    putLong("durationMs", durationMs)
                    putString("filePath", filePath)
                    putInt("trackNumber", trackNumber ?: -1)
                    putInt("year", year ?: -1)
                    putLong("dateAddedMs", dateAddedMs)
                    putLong("sizeBytes", sizeBytes)
                    putString("albumId", albumId)
                    putString("artistId", artistId)
                    putInt("discNumber", discNumber ?: -1)
                    putString("genre", genre)
                    putString("genreOrigin", genreOrigin?.name)
                    putString("volumeName", volumeName); putString("relativePath", relativePath)
                    putString("displayName", displayName); putString("mimeType", mimeType)
                    putLong("dateModifiedMs", dateModifiedMs); putBoolean("sizeBytesKnown", sizeBytesKnown)
                })
                .build()
        )
        .build()
}

internal fun MediaItem.toLocalSong(): LocalSong {
    val metadata = mediaMetadata
    val extras = metadata.extras
    return LocalSong(
        id = mediaId, title = metadata.title?.toString() ?: "Unknown",
        artist = metadata.artist?.toString() ?: "Unknown Artist", album = metadata.albumTitle?.toString(),
        contentUri = localConfiguration?.uri?.toString().orEmpty(), artworkUri = metadata.artworkUri?.toString(),
        durationMs = extras?.getLong("durationMs", 0) ?: 0,
        filePath = extras?.getString("filePath"),
        trackNumber = extras?.getInt("trackNumber", -1)?.takeIf { it > 0 },
        year = extras?.getInt("year", -1)?.takeIf { it > 0 },
        dateAddedMs = extras?.getLong("dateAddedMs", 0) ?: 0,
        sizeBytes = extras?.getLong("sizeBytes", 0) ?: 0,
        albumId = extras?.getString("albumId"), artistId = extras?.getString("artistId"),
        discNumber = extras?.getInt("discNumber", -1)?.takeIf { it > 0 },
        genre = extras?.getString("genre"),
        genreOrigin = extras?.getString("genreOrigin")?.let { value -> runCatching { com.music.aurelis.data.model.MetadataOrigin.valueOf(value) }.getOrNull() },
        volumeName = extras?.getString("volumeName"), relativePath = extras?.getString("relativePath"),
        displayName = extras?.getString("displayName"), mimeType = extras?.getString("mimeType"),
        dateModifiedMs = extras?.getLong("dateModifiedMs", 0) ?: 0,
        sizeBytesKnown = extras?.getBoolean("sizeBytesKnown", false) ?: false,
        queueTier = queueTier, queueEntryId = queueEntryId,
    )
}
