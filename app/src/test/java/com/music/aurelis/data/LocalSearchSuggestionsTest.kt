// Aurelis © 2026. GPL-3.0-or-later
package com.music.aurelis.data

import com.music.aurelis.data.model.LocalSong
import org.junit.Assert.*
import org.junit.Test

class LocalSearchSuggestionsTest {
    private fun song(id: String, title: String, artist: String = "Other", album: String? = null) =
        LocalSong(id, title, artist, album, "content://media/$id", null, 60_000, null, null, null)

    @Test fun blankExactAndMissingQueriesDoNotInventCompletions() {
        val songs = listOf(song("1", "Café"))
        for (query in listOf("", " \t ", "CAFE", "Missing")) {
            assertTrue(LocalSearchSuggestions.build(query, songs, emptyList(), emptyList()).isEmpty())
        }
    }

    @Test fun localFieldsCollectionsAndHistoryUseTheExistingAccentRulesAndCap() {
        val suggestions = LocalSearchSuggestions.build("cafe",
            listOf(song("1", "Café noir", "CAFÉ", "Cafe set")),
            listOf("CAFE NOIR", "Café live"), listOf("Cafe history"))
        assertEquals(listOf("cafe", "Café noir", "Cafe set", "Café live"), suggestions)
    }

    @Test fun currentSongOrderIsPreservedAndNoInputCollectionIsMutated() {
        val songs = listOf(song("1", "Theme B"), song("2", "Theme A"))
        val history = listOf("Theme history")
        val result = LocalSearchSuggestions.build("Theme", songs, emptyList(), history)
        assertEquals(listOf("Theme", "Theme B", "Theme A", "Theme history"), result)
        assertEquals(listOf("1", "2"), songs.map { it.id })
        assertEquals(listOf("Theme history"), history)
        assertEquals(listOf("Theme", "Theme A", "Theme B", "Theme history"),
            LocalSearchSuggestions.build("Theme", songs.reversed(), emptyList(), history))
    }

    @Test fun rebuildingForAnotherQueryCannotReturnPreviousMatches() {
        val songs = listOf(song("1", "Alpha live"), song("2", "Beta live"))
        assertEquals(listOf("Alpha", "Alpha live"), LocalSearchSuggestions.build("Alpha", songs, emptyList(), emptyList()))
        assertEquals(listOf("Beta", "Beta live"), LocalSearchSuggestions.build("Beta", songs, emptyList(), emptyList()))
        assertEquals(listOf("special", "Special % 🎵"),
            LocalSearchSuggestions.build("special", emptyList(), emptyList(), listOf("Special % 🎵")))
    }
}
