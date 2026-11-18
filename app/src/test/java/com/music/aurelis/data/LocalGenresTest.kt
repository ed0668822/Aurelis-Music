// SPDX-FileCopyrightText: 2026 Aurelis
// SPDX-License-Identifier: LGPL-3.0-or-later
package com.music.aurelis.data

import com.music.aurelis.data.model.LocalSong
import org.junit.Assert.*
import org.junit.Test

class LocalGenresTest {
    private fun song(id: String, genre: String?) = LocalSong(id, "Song", "Artist", null,
        "content://media/$id", null, 1000, null, null, null, genre = genre)
    @Test fun groupsOnlyMetadataAndUnknownTracksWithoutGuessing() {
        val groups = LocalGenres.groups(listOf(song("1", " Róck "), song("2", "rock"), song("3", null), song("4", "  ")))
        assertEquals(2, groups.size)
        assertEquals(listOf("1", "2"), groups.single { it.key == "tag:rock" }.songs.map { it.id })
        assertEquals(listOf("3", "4"), groups.single { it.key == "unknown" }.songs.map { it.id })
        assertTrue(LocalGenres.groups(emptyList()).isEmpty())
    }
    @Test fun unsupportedProviderGenreMarkersRemainUnknown() {
        val groups = LocalGenres.groups(listOf(
            song("1", " <unknown> "), song("2", null), song("3", "\u0301"),
            song("4", "Jazz"),
        ))
        assertEquals(listOf("1", "2", "3"), groups.single { it.key == "unknown" }.songs.map { it.id })
        assertEquals(listOf("4"), groups.single { it.key == "tag:jazz" }.songs.map { it.id })
        assertEquals(2, groups.size)
    }

    @Test fun accentAndCaseVariantsKeepOneRealGenreIdentity() {
        val a = song("a", " Música  ").copy(genreOrigin = com.music.aurelis.data.model.MetadataOrigin.MEDIA_STORE)
        val b = song("b", "musica").copy(genreOrigin = com.music.aurelis.data.model.MetadataOrigin.FILE_TAG)
        val groups = LocalGenres.groups(listOf(a, b))
        assertEquals(1, groups.size)
        assertEquals("tag:musica", groups.single().key)
        assertEquals(listOf("a", "b"), groups.single().songs.map { it.id })
        assertEquals(setOf(com.music.aurelis.data.model.MetadataOrigin.MEDIA_STORE,
            com.music.aurelis.data.model.MetadataOrigin.FILE_TAG),
            groups.single().songs.mapNotNull { it.genreOrigin }.toSet())
    }

    @Test fun literalUnknownLabelDoesNotCollideWithMissingTag() {
        val groups = LocalGenres.groups(listOf(song("1", "Sin clasificar"), song("2", null)))
        assertEquals(2, groups.map { it.key }.distinct().size)
    }
}
