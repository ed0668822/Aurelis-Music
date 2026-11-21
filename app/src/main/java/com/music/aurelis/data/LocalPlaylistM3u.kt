// SPDX-FileCopyrightText: 2026 Aurelis
// SPDX-License-Identifier: LGPL-3.0-or-later

package com.music.aurelis.data

import com.music.aurelis.data.model.Song
import java.net.URLDecoder

/** B05/B06: pure M3U/M3U8 parser, resolver and portable UTF-8 exporter. */
internal object LocalPlaylistM3u {
    private const val MAX_ENTRIES = 20_000
    private const val MAX_LINES = 60_000

    data class Entry(val location: String, val label: String? = null)
    data class Resolved(
        val songs: List<Song>,
        val missing: List<String>,
        val ambiguous: List<String>,
        val repeated: Int,
    )

    fun parse(content: String): List<Entry> {
        val entries = ArrayList<Entry>()
        var label: String? = null
        val lines = content.removePrefix("\uFEFF").lineSequence()
        var count = 0
        for (original in lines) {
            count++
            require(count <= MAX_LINES) { "Playlist exceeds supported length" }
            val line = original.trim().removePrefix("\uFEFF")
            if (line.startsWith("#EXTINF:", ignoreCase = true)) {
                label = line.substringAfter(',', "").trim().takeIf { it.isNotBlank() }
                continue
            }
            if (line.isEmpty() || line.startsWith('#')) continue
            require(entries.size < MAX_ENTRIES) { "Playlist has too many tracks" }
            entries += Entry(line.trim('"'), label)
            label = null
        }
        return entries
    }

    /**
     * Exact URI or path beats suffix; a basename is accepted only when unique.
     * Unresolvable and ambiguous files are reported, not guessed or downloaded.
     * Duplicate entries are skipped since OfflinePlaylists requires unique URIs.
     */
    fun resolve(entries: List<Entry>, library: List<Song>): Resolved {
        val candidates = library.filter { !it.localUri.isNullOrBlank() }
            .distinctBy { it.localUri }
        val byUri = candidates.groupBy { it.localUri.orEmpty() }
        val byPath = candidates.filter { !it.localPath.isNullOrBlank() }
            .groupBy { normalize(it.localPath.orEmpty()) }
        val byFilename = candidates.groupBy {
            normalize(it.localPath ?: it.localUri.orEmpty()).substringAfterLast('/')
        }
        val songs = ArrayList<Song>()
        val missing = ArrayList<String>()
        val ambiguous = ArrayList<String>()
        val seen = HashSet<String>()
        var repeated = 0
        for (entry in entries) {
            val normalized = normalize(entry.location)
            if (normalized.isEmpty() || normalized.startsWith("http://") ||
                normalized.startsWith("https://")) {
                missing += entry.location
                continue
            }
            val exact = byUri[entry.location] ?: byPath[normalized]
            val matches = when {
                exact != null -> exact
                else -> {
                    // Multi-segment relative paths are useful in exported files.
                    val suffix = if (normalized.contains('/')) "/" + normalized.trimStart('/') else ""
                    val pathMatches = if (suffix.isNotEmpty()) candidates.filter {
                        normalize(it.localPath.orEmpty()).endsWith(suffix)
                    } else emptyList()
                    if (pathMatches.isNotEmpty()) pathMatches
                    else byFilename[normalized.substringAfterLast('/')].orEmpty()
                }
            }
            when {
                matches.isEmpty() -> missing += entry.location
                matches.size > 1 -> ambiguous += entry.location
                !seen.add(matches[0].localUri.orEmpty()) -> repeated++
                else -> songs += matches[0]
            }
        }
        return Resolved(songs, missing, ambiguous, repeated)
    }

    fun export(songs: List<Song>): String = buildString {
        append("#EXTM3U\n")
        for (song in songs) {
            val location = song.localPath?.takeIf { it.isNotBlank() }
                ?: song.localUri?.takeIf { it.isNotBlank() }
                ?: continue
            val duration = durationSeconds(song.durationText)
            val label = (song.artist + " - " + song.title).replace('\r', ' ').replace('\n', ' ')
            append("#EXTINF:").append(duration).append(',').append(label).append('\n')
            append(location.replace('\r', ' ').replace('\n', ' ')).append('\n')
        }
    }

    private fun durationSeconds(text: String?): Int {
        val fields = text?.trim()?.split(':') ?: return -1
        if (fields.size !in 2..3) return -1
        val nums = fields.map { it.toIntOrNull() ?: return -1 }
        if (nums.any { it < 0 } || nums.drop(1).any { it > 59 }) return -1
        return nums.fold(0) { total, part -> total * 60 + part }
    }

    private fun normalize(raw: String): String {
        val slash = raw.trim().replace('\\', '/')
        val decoded = if (slash.startsWith("file://", ignoreCase = true)) {
            val path = slash.removePrefix("file://")
            runCatching { URLDecoder.decode(path.replace("+", "%2B"), "UTF-8") }
                .getOrDefault(path)
        } else slash
        val parts = ArrayList<String>()
        for (part in decoded.split('/')) {
            when (part) {
                "", "." -> Unit
                ".." -> if (parts.isNotEmpty()) parts.removeAt(parts.lastIndex)
                else -> parts += part.lowercase(java.util.Locale.ROOT)
            }
        }
        return parts.joinToString("/")
    }
}
