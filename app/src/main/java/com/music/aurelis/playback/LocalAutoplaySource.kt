// SPDX-FileCopyrightText: 2026 Aurelis
// SPDX-License-Identifier: LGPL-3.0-or-later

package com.music.aurelis.playback

import android.content.ContentResolver
import android.content.Context
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import com.music.aurelis.data.LocalMediaRepository
import com.music.aurelis.data.OfflinePlaylists
import com.music.aurelis.data.model.Song
import com.music.aurelis.data.stats.ListeningStats
import com.music.aurelis.data.stats.ReplayPeriod
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * B08: offline-only input for the existing AutoPlay queue.
 *
 * Queries at most two small, bounded MediaStore result windows, then merges
 * locally persisted playlists. No full-library scan and no PCM/BPM analysis.
 * Cached windows refresh at most once every 15 minutes, never on audio thread.
 */
internal object LocalAutoplaySource {
    private const val TTL_MS = 15L * 60 * 1000
    private const val MAX_MEDIA_ROWS = 320
    private const val MAX_PERSISTED = 500
    private val mutex = kotlinx.coroutines.sync.Mutex()
    private var cachedAt = 0L
    private var mediaCandidates: List<Song> = emptyList()
    private var historyCounts: Map<String, Int> = emptyMap()

    suspend fun suggestions(
        context: Context,
        seed: Song,
        existing: List<Song>,
        limit: Int,
        bpmFor: (String) -> Double?,
    ): List<Song> = withContext(Dispatchers.IO) {
        if (limit <= 0 || seed.localUri.isNullOrBlank()) return@withContext emptyList()
        refreshIfStale(context.applicationContext, seed.artist)
        // Read at most a bounded prefix of the already-in-memory local lists.
        val fromPlaylists = OfflinePlaylists.playlists.value.asSequence()
            .flatMap { it.songs.asSequence() }.take(MAX_PERSISTED).toList()
        val pool = buildList {
            addAll(fromPlaylists)
            addAll(mediaCandidates)
        }
        val chosen = LocalAutoplayRanker.suggest(
            seed = seed, candidates = pool, existing = existing,
            likedUris = OfflinePlaylists.liked.value,
            playCounts = historyCounts, limit = limit,
            bpmFor = bpmFor,
        )
        // Skip MediaStore rows that have disappeared since the cached lookup.
        LocalMediaRepository.present(context, chosen)
    }

    private suspend fun refreshIfStale(context: Context, artist: String) {
        val now = android.os.SystemClock.elapsedRealtime()
        if (now - cachedAt < TTL_MS && cachedAt != 0L) return
        mutex.lock()
        try {
            val recent = android.os.SystemClock.elapsedRealtime()
            if (recent - cachedAt < TTL_MS && cachedAt != 0L) return
            val candidates = ArrayList<Song>()
            if (artist.isNotBlank() && artist != "Unknown Artist") {
                candidates.addAll(readBounded(context, artist, 96))
            }
            candidates.addAll(readBounded(context, null, MAX_MEDIA_ROWS))
            mediaCandidates = candidates.distinctBy { it.localUri }.take(MAX_MEDIA_ROWS)
            historyCounts = runCatching {
                ListeningStats.summary(ReplayPeriod.THIS_MONTH).songs.take(200)
                    .associate { it.song.videoId to it.plays }
            }.getOrDefault(emptyMap())
            cachedAt = android.os.SystemClock.elapsedRealtime()
        } finally {
            mutex.unlock()
        }
    }

    private fun readBounded(context: Context, artist: String?, limit: Int): List<Song> {
        val projection = arrayOf(
            MediaStore.Audio.Media._ID,
            MediaStore.Audio.Media.TITLE,
            MediaStore.Audio.Media.ARTIST,
            MediaStore.Audio.Media.ALBUM,
            MediaStore.Audio.Media.DURATION,
            MediaStore.Audio.Media.DATA,
        )
        val query = Bundle().apply {
            putString(ContentResolver.QUERY_ARG_SQL_SELECTION,
                if (artist != null) "${MediaStore.Audio.Media.IS_MUSIC} != 0 AND ${MediaStore.Audio.Media.ARTIST} = ?"
                else "${MediaStore.Audio.Media.IS_MUSIC} != 0")
            if (artist != null) putStringArray(ContentResolver.QUERY_ARG_SQL_SELECTION_ARGS, arrayOf(artist))
            putString(ContentResolver.QUERY_ARG_SQL_SORT_ORDER,
                "${MediaStore.Audio.Media.DATE_ADDED} DESC")
            putString(ContentResolver.QUERY_ARG_SQL_LIMIT, limit.toString())
        }
        val result = ArrayList<Song>(limit)
        runCatching {
            context.contentResolver.query(
                MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, projection, query, null,
            )?.use { cursor ->
                val id = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media._ID)
                val title = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.TITLE)
                val artistColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ARTIST)
                val album = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ALBUM)
                val data = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DATA)
                val duration = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DURATION)
                while (result.size < limit && cursor.moveToNext()) {
                    val row = cursor.getLong(id)
                    val uri = android.content.ContentUris.withAppendedId(
                        MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, row,
                    ).toString()
                    result += Song(
                        videoId = uri,
                        title = cursor.getString(title)?.takeIf { it.isNotBlank() } ?: "Track $row",
                        artist = cursor.getString(artistColumn)?.takeIf { it.isNotBlank() } ?: "Unknown Artist",
                        thumbnailUrl = null,
                        albumName = cursor.getString(album),
                        localUri = uri,
                        localPath = cursor.getString(data),
                    )
                }
            }
        }
        return result
    }
}
