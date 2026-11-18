// SPDX-FileCopyrightText: 2026 Aurelis
// SPDX-License-Identifier: LGPL-3.0-or-later
package com.music.aurelis.ui.player

import com.music.aurelis.data.model.LocalAlbum
import com.music.aurelis.data.model.LocalArtist
import com.music.aurelis.data.model.LocalSong
import org.junit.Assert.*
import org.junit.Test

class LocalCreditRoutesTest {
    private val track = LocalSong("track-1", "A long local title", "Alpha feat. Beta",
        "An Album", "content://media/external/audio/media/1", null, 180000, null, null, null)
    private val album = LocalAlbum("An Album", "Alpha feat. Beta", null, 1, listOf(track), mediaId = "21")
    private val artist = LocalArtist("Alpha feat. Beta", null, 1, 1, listOf(track), mediaId = "35")

    @Test fun albumNavigationRequiresAnUnambiguousRealSongMembership() {
        assertEquals(album.key, matchedLocalAlbum(track, listOf(album))?.key)
        assertNull(matchedLocalAlbum(track.copy(id = "missing"), listOf(album)))
        val conflicting = album.copy(mediaId = "22")
        assertNull(matchedLocalAlbum(track, listOf(album, conflicting)))
    }

    @Test fun artistNavigationUsesExactCreditAndMembershipNotFuzzyNameMatches() {
        val another = artist.copy(name = "Alpha", mediaId = "36",
            songs = listOf(track.copy(id = "another-track")))
        assertEquals(artist.key, matchedLocalArtist(track, artist.name, listOf(artist, another))?.key)
        assertNull(matchedLocalArtist(track, "Alpha", listOf(artist, another)))
        assertNull(matchedLocalArtist(track.copy(id = "other"), artist.name, listOf(artist)))
    }

    @Test fun displayedSegmentsReproducePunctuationAndRepeatedCreditsExactly() {
        val original = "Alpha feat. Beta, Alpha"
        val segments = artistCreditSegments(original, artistCredits(original))
        assertEquals(original, segments.joinToString("") { it.text })
        assertEquals(listOf("Alpha", "Beta", "Alpha"),
            segments.mapNotNull { it.creditName })
        assertEquals(listOf(ArtistCreditSegment("No credits")), artistCreditSegments(
            "No credits", emptyList()))
    }
}
