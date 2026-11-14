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
    @Test fun literalUnknownLabelDoesNotCollideWithMissingTag() {
        val groups = LocalGenres.groups(listOf(song("1", "Sin clasificar"), song("2", null)))
        assertEquals(2, groups.map { it.key }.distinct().size)
    }
}
