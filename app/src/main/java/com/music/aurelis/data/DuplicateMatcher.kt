// SPDX-FileCopyrightText: 2026 Aurelis
// SPDX-License-Identifier: LGPL-3.0-or-later

package com.music.aurelis.data

import java.text.Normalizer
import java.util.Locale
import kotlin.math.abs

/**
 * A physical local audio file. Two rows sharing the same physical path must
 * not be presented as two files: deleting the "duplicate" could delete the
 * only copy. Hashes are the SHA-256 of every byte, never a filename checksum.
 */
data class DuplicateTrack(
    val uri: String,
    val path: String?,
    val title: String,
    val artist: String,
    val durationMs: Long,
    val bytes: Long? = null,
    val format: String = "",
    val sha256: String? = null,
)

enum class DuplicateConfidence { EXACT_BYTES, POSSIBLE_RECORDING }

data class DuplicateGroup(
    val confidence: DuplicateConfidence,
    val tracks: List<DuplicateTrack>,
    val keep: DuplicateTrack,
)

/** Pure policy used by the scanner and unit tests; this code never deletes a file. */
object DuplicateMatcher {
    const val MAX_DURATION_DIFFERENCE_MS = 2_000L

    fun candidateGroups(tracks: List<DuplicateTrack>): List<List<DuplicateTrack>> {
        val distinct = tracks.filter {
            it.uri.isNotBlank() && it.durationMs > 0L && normalize(it.title).isNotBlank()
        }.distinctBy { track ->
            track.path?.trim()?.replace('\\', '/')?.lowercase(Locale.ROOT)
                ?.takeIf { it.isNotBlank() } ?: track.uri
        }
        return distinct.groupBy { normalize(it.title) to normalize(it.artist) }
            .values.flatMap { songs ->
                val sorted = songs.sortedWith(compareBy<DuplicateTrack> { it.durationMs }.thenBy { it.uri })
                val batches = mutableListOf<List<DuplicateTrack>>()
                var current = mutableListOf<DuplicateTrack>()
                for (song in sorted) {
                    if (current.isNotEmpty() &&
                        song.durationMs - current.first().durationMs > MAX_DURATION_DIFFERENCE_MS
                    ) {
                        if (current.size > 1) batches += current.toList()
                        current = mutableListOf()
                    }
                    current += song
                }
                if (current.size > 1) batches += current.toList()
                batches
            }
    }

    fun detect(tracks: List<DuplicateTrack>): List<DuplicateGroup> {
        val result = mutableListOf<DuplicateGroup>()
        for (batch in candidateGroups(tracks)) {
            val exact = batch.filter { !it.sha256.isNullOrBlank() && it.bytes != null && it.bytes > 0L }
                .groupBy { it.sha256 to it.bytes }
                .values.filter { it.size > 1 }
            for (sameBytes in exact) {
                val ordered = rank(sameBytes)
                result += DuplicateGroup(DuplicateConfidence.EXACT_BYTES, ordered, ordered.first())
            }
            // A different encode or an unreadable file is only a suggestion.
            // Even equal metadata and file size NEVER authorize deletion.
            if (exact.isEmpty() || exact.sumOf { it.size } < batch.size) {
                val ordered = rank(batch)
                result += DuplicateGroup(DuplicateConfidence.POSSIBLE_RECORDING, ordered, ordered.first())
            }
        }
        return result.sortedWith(
            compareBy<DuplicateGroup> { it.confidence != DuplicateConfidence.EXACT_BYTES }
                .thenBy { it.keep.title.lowercase(Locale.ROOT) },
        )
    }

    private fun rank(tracks: List<DuplicateTrack>): List<DuplicateTrack> =
        tracks.sortedWith(
            compareByDescending<DuplicateTrack> { quality(it.format) }
                .thenByDescending { it.bytes ?: 0L }
                .thenBy { it.uri },
        )

    private fun quality(extension: String): Int = when (extension.lowercase(Locale.ROOT)) {
        "flac", "wav", "aiff", "aif", "alac", "dsf", "dff", "ape", "wv" -> 3
        "m4a", "mp4", "ogg", "opus" -> 2
        "mp3", "aac", "webm" -> 1
        else -> 0
    }

    private fun normalize(raw: String): String =
        Normalizer.normalize(raw.trim(), Normalizer.Form.NFKD)
            .replace(Regex("\\p{M}+"), "")
            .lowercase(Locale.ROOT)
            .replace(Regex("[^\\p{L}\\p{N}]+"), " ")
            .trim()
}
