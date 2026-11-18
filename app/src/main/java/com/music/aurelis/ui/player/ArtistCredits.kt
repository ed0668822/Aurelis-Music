// SPDX-FileCopyrightText: 2026 Aurelis
// SPDX-License-Identifier: LGPL-3.0-or-later
package com.music.aurelis.ui.player

import com.music.aurelis.data.model.LocalAlbum
import com.music.aurelis.data.model.LocalArtist
import com.music.aurelis.data.model.LocalSong

/**
 * One credit in an artist line.
 */
data class ArtistCredit(
    val name: String,
    /** Null for local music: no page to navigate to. */
    val browseId: String? = null,
)

/**
 * The credits to draw for a track.
 *
 * For local music the artist field is a plain string. Split on common
 * separators so each name can be displayed individually in the marquee.
 */
fun artistCredits(artist: String): List<ArtistCredit> {
    val text = artist.trim()
    if (text.isEmpty()) return emptyList()

    val cuts = CREDIT_SEPARATOR.findAll(text).toList()
    if (cuts.isEmpty()) {
        return listOf(ArtistCredit(name = text))
    }

    val out = mutableListOf<ArtistCredit>()
    var start = 0
    for (cut in cuts) {
        val name = text.substring(start, cut.range.first).trim()
        if (name.isNotEmpty()) {
            out += ArtistCredit(name = name)
        }
        start = cut.range.last + 1
    }
    val tail = text.substring(start).trim()
    if (tail.isNotEmpty()) out += ArtistCredit(name = tail)
    if (out.isEmpty()) out += ArtistCredit(name = text)
    return out
}

/** A displayed portion of the original byline, with a navigation identity only for a real credit. */
data class ArtistCreditSegment(val text: String, val creditName: String? = null)

/**
 * Preserve punctuation and spacing exactly. An unrelated artist matching only
 * a fragment of a name must never receive an accidental tap target.
 */
fun artistCreditSegments(text: String, credits: List<ArtistCredit>): List<ArtistCreditSegment> {
    if (text.isEmpty()) return emptyList()
    val spans = mutableListOf<ArtistCreditSegment>()
    var offset = 0
    credits.forEach { credit ->
        val index = text.indexOf(credit.name, startIndex = offset, ignoreCase = true)
        if (index < 0) return@forEach
        if (index > offset) spans += ArtistCreditSegment(text.substring(offset, index))
        val end = index + credit.name.length
        spans += ArtistCreditSegment(text.substring(index, end), credit.name)
        offset = end
    }
    if (offset < text.length) spans += ArtistCreditSegment(text.substring(offset))
    return spans.ifEmpty { listOf(ArtistCreditSegment(text)) }
}

/** A song ID is authoritative: identical album titles cannot identify a collection. */
fun matchedLocalAlbum(song: LocalSong, albums: List<LocalAlbum>): LocalAlbum? =
    albums.singleOrNull { album -> album.songs.any { it.id == song.id } }

/** An artist link requires both an exact credit name and ownership of the current song. */
fun matchedLocalArtist(song: LocalSong, credit: String, artists: List<LocalArtist>): LocalArtist? =
    artists.singleOrNull { artist ->
        artist.name.equals(credit, ignoreCase = true) && artist.songs.any { it.id == song.id }
    }

/** Common separators in artist bylines: comma, ampersand, "feat.", "ft.", "with", " x ". */
private val CREDIT_SEPARATOR = Regex(
    """\s*(?:,|&|\bfeat\b\.?|\bft\b\.?|\bwith\b|\s+x\s+)\s*""",
    RegexOption.IGNORE_CASE,
)
