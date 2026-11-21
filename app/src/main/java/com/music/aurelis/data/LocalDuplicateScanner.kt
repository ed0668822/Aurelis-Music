// SPDX-FileCopyrightText: 2026 Aurelis
// SPDX-License-Identifier: LGPL-3.0-or-later

package com.music.aurelis.data

import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import com.music.aurelis.data.model.Song
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.withContext
import java.io.File
import java.security.MessageDigest

/**
 * Reads just the local MediaStore candidates on a bounded IO dispatcher.
 * No raw audio files are modified. Full-file SHA-256 runs only for candidates
 * whose metadata AND file byte counts agree, with a 64 KiB reusable buffer.
 *
 * This is not acoustic fingerprinting. Different encodes of the same recording
 * are shown as POSSIBLE_RECORDING and never given a delete action.
 */
object LocalDuplicateScanner {
    suspend fun scan(context: Context, songs: List<Song>): List<DuplicateGroup> =
        withContext(Dispatchers.IO) {
            if (!LocalMediaRepository.hasStoragePermission(context)) return@withContext emptyList()
            val initial = songs.mapNotNull { song ->
                val uri = song.localUri?.takeIf { it.startsWith("content://") || it.startsWith("file://") }
                    ?: return@mapNotNull null
                val duration = durationMs(song.durationText)
                if (duration <= 0L) return@mapNotNull null
                DuplicateTrack(
                    uri = uri,
                    path = song.localPath,
                    title = song.title,
                    artist = song.artist,
                    durationMs = duration,
                    format = song.localPath?.substringAfterLast('.', "")?.lowercase().orEmpty(),
                )
            }
            val metadataGroups = DuplicateMatcher.candidateGroups(initial)
            if (metadataGroups.isEmpty()) return@withContext emptyList()

            val enriched = linkedMapOf<String, DuplicateTrack>()
            for (track in metadataGroups.flatten().distinctBy { it.uri }) {
                currentCoroutineContext().ensureActive()
                val details = details(context, track.uri)
                enriched[track.uri] = track.copy(
                    bytes = details.first,
                    format = details.second.ifBlank { track.format },
                )
            }

            // Hash only same-size members of an already plausible metadata
            // group. Mixed encodes are still displayed for manual review.
            val hashCandidates = metadataGroups.flatMap { batch ->
                batch.mapNotNull { candidate -> enriched[candidate.uri] }.filter { (it.bytes ?: 0L) > 0L }
                    .groupBy { it.bytes }.values.filter { it.size >= 2 }.flatten()
            }.map { it.uri }.toSet()
            for (uri in hashCandidates) {
                currentCoroutineContext().ensureActive()
                val track = enriched[uri] ?: continue
                enriched[uri] = track.copy(sha256 = digest(context, uri))
            }
            DuplicateMatcher.detect(enriched.values.toList())
        }

    /**
     * A stale scan is not deletion permission. Before Android's system
     * confirmation, re-read both full files and verify their original hashes.
     */
    suspend fun stillIdentical(
        context: Context,
        keeper: DuplicateTrack,
        removal: DuplicateTrack,
    ): Boolean = withContext(Dispatchers.IO) {
        if (keeper.uri == removal.uri || keeper.path != null && keeper.path == removal.path) {
            return@withContext false
        }
        if (!removal.uri.startsWith("content://media/") ||
            keeper.sha256.isNullOrBlank() || removal.sha256 != keeper.sha256 ||
            keeper.bytes == null || keeper.bytes != removal.bytes
        ) return@withContext false
        val freshKeep = digest(context, keeper.uri)
        val freshRemove = digest(context, removal.uri)
        freshKeep != null && freshKeep == keeper.sha256 && freshRemove == freshKeep
    }

    private fun durationMs(text: String?): Long {
        val numbers = text?.trim()?.split(':')?.map { it.toLongOrNull() } ?: return 0L
        if (numbers.isEmpty() || numbers.size > 3 || numbers.any { it == null }) return 0L
        var seconds = 0L
        for (num in numbers) {
            val part = num ?: return 0L
            if (part < 0 || part > 59 && numbers.size > 1 && seconds > 0) return 0L
            seconds = seconds * 60 + part
        }
        return seconds * 1000
    }

    private fun details(context: Context, uriString: String): Pair<Long?, String> {
        val uri = Uri.parse(uriString)
        if (uri.scheme == "file") {
            val file = uri.path?.let(::File) ?: return null to ""
            return file.takeIf { it.isFile }?.length() to file.extension.lowercase()
        }
        return runCatching {
            context.contentResolver.query(
                uri, arrayOf(OpenableColumns.SIZE, OpenableColumns.DISPLAY_NAME),
                null, null, null,
            )?.use { cursor ->
                if (!cursor.moveToFirst()) return@use null to ""
                val sizeCol = cursor.getColumnIndex(OpenableColumns.SIZE)
                val nameCol = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                val bytes = if (sizeCol >= 0 && !cursor.isNull(sizeCol)) cursor.getLong(sizeCol) else null
                val format = if (nameCol >= 0) {
                    cursor.getString(nameCol)?.substringAfterLast('.', "")?.lowercase().orEmpty()
                } else ""
                bytes to format
            } ?: (null to "")
        }.getOrDefault(null to "")
    }

    private suspend fun digest(context: Context, uriString: String): String? =
        runCatching {
            val digest = MessageDigest.getInstance("SHA-256")
            val buffer = ByteArray(64 * 1024)
            context.contentResolver.openInputStream(Uri.parse(uriString))?.use { stream ->
                while (true) {
                    currentCoroutineContext().ensureActive()
                    val count = stream.read(buffer)
                    if (count < 0) break
                    if (count > 0) digest.update(buffer, 0, count)
                }
                digest.digest().joinToString("") { "%02x".format(it) }
            }
        }.getOrNull()
}
