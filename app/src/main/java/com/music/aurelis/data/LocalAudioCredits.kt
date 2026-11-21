// SPDX-FileCopyrightText: 2026 Aurelis
// SPDX-License-Identifier: LGPL-3.0-or-later

package com.music.aurelis.data

import android.content.Context
import android.media.MediaMetadataRetriever
import android.net.Uri
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.Locale

/** B08 — Credits inferred from actual tags. Never guess a contributor's role. */
enum class LocalCreditRole { COMPOSER, LYRICIST, ALBUM_ARTIST }

data class LocalTrackCredit(val role: LocalCreditRole, val names: List<String>)

/** Preserve names with commas, but support the semicolon and NUL separators used in audio tags. */
internal fun parseLocalTrackCredits(raw: Map<LocalCreditRole, String?>): List<LocalTrackCredit> =
    LocalCreditRole.entries.mapNotNull { role ->
        val seen = HashSet<String>()
        val names = raw[role].orEmpty().split(';', '\u0000')
            .map { it.trim() }
            .filter { it.isNotEmpty() && seen.add(it.lowercase(Locale.ROOT)) }
        names.takeIf { it.isNotEmpty() }?.let { LocalTrackCredit(role, it) }
    }

/**
 * Read metadata only when requested; no network lookup and no song-list scan.
 * Android's standard metadata retriever does not expose a producer field, so
 * producer credits remain unreported until a provider states them explicitly.
 */
object LocalAudioCredits {
    suspend fun read(context: Context, uri: String): List<LocalTrackCredit> =
        withContext(Dispatchers.IO) {
            if (uri.isBlank()) return@withContext emptyList()
            val retriever = MediaMetadataRetriever()
            try {
                retriever.setDataSource(context.applicationContext, Uri.parse(uri))
                parseLocalTrackCredits(
                    mapOf(
                        LocalCreditRole.COMPOSER to retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_COMPOSER),
                        LocalCreditRole.LYRICIST to retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_WRITER),
                        LocalCreditRole.ALBUM_ARTIST to retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_ALBUMARTIST),
                    ),
                )
            } catch (_: Exception) {
                emptyList()
            } finally {
                runCatching { retriever.release() }
            }
        }
}
