// SPDX-FileCopyrightText: 2026 Aurelis
// SPDX-License-Identifier: LGPL-3.0-or-later

package com.music.aurelis

import com.music.aurelis.data.DuplicateConfidence
import com.music.aurelis.data.DuplicateMatcher
import com.music.aurelis.data.DuplicateTrack
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class DuplicateMatcherTest {
    private fun track(
        name: String,
        uri: String,
        title: String = "Numb",
        artist: String = "Linkin Park",
        ms: Long = 187_000,
        hash: String? = null,
        size: Long? = null,
        path: String? = null,
    ) = DuplicateTrack(
        uri = uri, path = path, title = title, artist = artist,
        durationMs = ms, bytes = size,
        format = name.substringAfterLast('.', ""),
        sha256 = hash,
    )

    @Test fun identicalFilesAreExactOnlyAfterFullHashes() {
        val items = listOf(
            track("one.mp3", "content://media/1", hash = "abc", size = 123),
            track("two.mp3", "content://media/2", hash = "abc", size = 123),
        )
        val groups = DuplicateMatcher.detect(items)
        assertEquals(1, groups.size)
        assertEquals(DuplicateConfidence.EXACT_BYTES, groups.single().confidence)
        assertEquals(2, groups.single().tracks.size)
    }

    @Test fun sameTitleDifferentAudioIsOnlyPossible() {
        val groups = DuplicateMatcher.detect(listOf(
            track("a.mp3", "uri:1", hash = "aaa", size = 128),
            track("b.flac", "uri:2", hash = "bbb", size = 1250),
        ))
        assertEquals(1, groups.size)
        assertEquals(DuplicateConfidence.POSSIBLE_RECORDING, groups.single().confidence)
        assertEquals("uri:2", groups.single().keep.uri)
    }

    @Test fun doesNotMatchDifferentDurationsOrArtists() {
        val groups = DuplicateMatcher.detect(listOf(
            track("a.mp3", "uri:1"),
            track("b.mp3", "uri:2", ms = 196_000),
            track("c.mp3", "uri:3", artist = "Other"),
        ))
        assertTrue(groups.isEmpty())
    }

    @Test fun preventsPhysicalPathAliasesFromBecomingDeletable() {
        val groups = DuplicateMatcher.detect(listOf(
            track("a.mp3", "uri:1", path = "/Music/One.mp3", hash = "abc", size = 123),
            track("a.mp3", "uri:2", path = "/Music/One.mp3", hash = "abc", size = 123),
        ))
        assertTrue(groups.isEmpty())
    }

    @Test fun similarAccentsAndCaseAreGroupedButNotAutomaticallyVerified() {
        val groups = DuplicateMatcher.detect(listOf(
            track("a.mp3", "uri:1", title = "Café", artist = "Élan"),
            track("b.mp3", "uri:2", title = "CAFE", artist = "elan", ms = 187_800),
        ))
        assertEquals(DuplicateConfidence.POSSIBLE_RECORDING, groups.single().confidence)
        assertFalse(groups.single().tracks.any { it.sha256 != null })
    }

    @Test fun sameHashWithDifferentLengthIsNotExact() {
        val groups = DuplicateMatcher.detect(listOf(
            track("a.mp3", "uri:1", hash = "abc", size = 10),
            track("b.mp3", "uri:2", hash = "abc", size = 11),
        ))
        assertEquals(DuplicateConfidence.POSSIBLE_RECORDING, groups.single().confidence)
    }
}
