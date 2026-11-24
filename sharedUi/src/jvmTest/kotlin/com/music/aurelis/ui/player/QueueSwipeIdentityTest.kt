// SPDX-FileCopyrightText: © 2026 Aurelis

package com.music.aurelis.ui.player

import com.music.aurelis.data.model.Song
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class QueueSwipeIdentityTest {
    private fun song(id: String) = Song(
        videoId = id,
        title = id,
        artist = "Artist",
        thumbnailUrl = null,
    )

    @Test
    fun `stable unique entry survives a refreshed native snapshot`() {
        val before = QueueTrack(2, song("a"), "context_a", uniqueEntryId = "entry-a")
        val after = QueueTrack(1, song("a"), "context_a", uniqueEntryId = "entry-a")
        assertTrue(before.sameEntryAs(after))
    }

    @Test
    fun `a song id alone must not authorize removal of another occurrence`() {
        val first = QueueTrack(2, song("same"), "context_first", uniqueEntryId = "entry-1")
        val second = QueueTrack(2, song("same"), "context_second", uniqueEntryId = "entry-2")
        assertFalse(first.sameEntryAs(second))
    }

    @Test
    fun `legacy entries require the same unique song instance`() {
        val item = song("legacy")
        val original = QueueTrack(3, item, "context_legacy", uniqueSongReference = true)
        val moved = QueueTrack(4, item, "context_legacy", uniqueSongReference = true)
        val other = QueueTrack(3, song("legacy"), "context_legacy", uniqueSongReference = true)
        val ambiguous = QueueTrack(3, item, "context_legacy", uniqueSongReference = false)
        assertTrue(original.sameEntryAs(moved))
        assertFalse(original.sameEntryAs(other))
        assertFalse(original.sameEntryAs(ambiguous))
    }
}
