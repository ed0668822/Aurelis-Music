// SPDX-FileCopyrightText: 2026 Aurelis
// SPDX-License-Identifier: LGPL-3.0-or-later
package com.music.aurelis.data

import com.music.aurelis.data.model.LocalSong
import org.junit.Assert.assertEquals
import org.junit.Test

class LocalGroupsSortingTest {
    private fun song(id: String, title: String, added: Long = 0, modified: Long = 0,
        disc: Int? = null, track: Int? = null) = LocalSong(
        id = id, title = title, artist = "Artista", album = "Álbum",
        contentUri = "content://media/external/audio/media/$id", artworkUri = null,
        durationMs = 60_000L, filePath = null, trackNumber = track, year = null,
        dateAddedMs = added, dateModifiedMs = modified, discNumber = disc,
    )

    @Test fun modificationDateIsIndependentFromDateAdded() {
        val rows = listOf(song("one", "Uno", added = 3000, modified = 1000),
            song("two", "Dos", added = 1000, modified = 5000))
        assertEquals(listOf("one", "two"), rows.sortedForLibrary(LocalMusicSort.DATE_ADDED).map { it.id })
        assertEquals(listOf("two", "one"), rows.sortedForLibrary(LocalMusicSort.DATE_MODIFIED).map { it.id })
    }


    @Test fun trackNumbersNeedARealAlbumName() {
        val untagged = song("untagged", "Desconocida", track = 2).copy(album = null)
        val blank = song("blank", "Sin nombre", track = 1).copy(album = " ")
        assertEquals(false, listOf(untagged).useTrackTags())
        assertEquals(false, listOf(blank).useTrackTags())
        assertEquals(true, listOf(song("known", "Con álbum", track = 1)).useTrackTags())
    }

    @Test fun albumOrderRespectsDiscBeforeTrackNumber() {
        val rows = listOf(song("disc2", "A", disc = 2, track = 1),
            song("disc1later", "C", disc = 1, track = 3),
            song("disc1first", "B", disc = 1, track = 1))
        assertEquals(listOf("disc1first", "disc1later", "disc2"),
            rows.sortedForAlbum().map { it.id })
    }
}
