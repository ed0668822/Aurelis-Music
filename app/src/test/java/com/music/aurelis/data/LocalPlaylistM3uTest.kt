// SPDX-FileCopyrightText: 2026 Aurelis
// SPDX-License-Identifier: LGPL-3.0-or-later

package com.music.aurelis.data

import com.music.aurelis.data.model.Song
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class LocalPlaylistM3uTest {
    private fun song(title: String, path: String): Song =
        Song(videoId = "content://media/" + title, title = title, artist = "Artist", thumbnailUrl = null,
            localUri = "content://media/" + title, localPath = path, durationText = "03:17")

    @Test fun parsesBomExtinfAndCommentsInOrder() {
        val entries = LocalPlaylistM3u.parse("\uFEFF#EXTM3U\n#EXTINF:197,Artist - A\nMusic/A.mp3\n# ignored\nMusic/B.flac\n")
        assertEquals(2, entries.size)
        assertEquals("Artist - A", entries[0].label)
        assertEquals("Music/A.mp3", entries[0].location)
    }

    @Test fun matchesExactPathsAndUniqueRelativeSuffixOnly() {
        val songs = listOf(song("one", "/storage/emulated/0/Music/Album/one.flac"),
            song("two", "/storage/emulated/0/Music/Other/two.mp3"))
        val out = LocalPlaylistM3u.resolve(
            listOf(LocalPlaylistM3u.Entry("Album/one.flac"),
                LocalPlaylistM3u.Entry("two.mp3")), songs)
        assertEquals(listOf("one", "two"), out.songs.map { it.title })
        assertTrue(out.missing.isEmpty())
    }

    @Test fun ambiguousAndMissingAreReportedRatherThanGuessed() {
        val songs = listOf(song("a", "/Music/A/track.flac"), song("b", "/Music/B/track.flac"))
        val out = LocalPlaylistM3u.resolve(listOf(
            LocalPlaylistM3u.Entry("track.flac"),
            LocalPlaylistM3u.Entry("missing.mp3"),
            LocalPlaylistM3u.Entry("https://example.com/stream.mp3")), songs)
        assertEquals(1, out.ambiguous.size)
        assertEquals(2, out.missing.size)
        assertTrue(out.songs.isEmpty())
    }

    @Test fun exportRoundTripsAndDoesNotDuplicateRows() {
        val tracks = listOf(song("first", "/Music/first.mp3"), song("second", "/Music/second.flac"))
        val exported = LocalPlaylistM3u.export(tracks)
        assertTrue(exported.startsWith("#EXTM3U\n"))
        val imported = LocalPlaylistM3u.resolve(LocalPlaylistM3u.parse(exported), tracks)
        assertEquals(tracks, imported.songs)
        assertEquals(0, imported.repeated)
        assertTrue(exported.contains("#EXTINF:197,Artist - first"))
    }

    @Test fun duplicateRowsAreReportedButNotAddedTwice() {
        val track = song("same", "/Music/same.mp3")
        val out = LocalPlaylistM3u.resolve(listOf(
            LocalPlaylistM3u.Entry("/Music/same.mp3"), LocalPlaylistM3u.Entry("/Music/same.mp3")),
            listOf(track))
        assertEquals(1, out.songs.size)
        assertEquals(1, out.repeated)
    }
}
