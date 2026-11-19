// SPDX-FileCopyrightText: 2026 Aurelis
// SPDX-License-Identifier: LGPL-3.0-or-later
package com.music.aurelis.playback

import com.music.aurelis.data.model.LocalSong
import com.music.aurelis.data.model.PlaybackSourceType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33])
class PlaybackOriginMetadataTest {
    private fun song() = LocalSong(
        "42", "Local track", "Local artist", "Recorded album",
        "content://media/external/audio/media/42", null, 60_000L, null, 1, 2026,
    )

    @Test fun preservesRealSourceFieldsAcrossMedia3() {
        val source = song().copy(
            playbackSource = "Saved playlist",
            playbackSourceType = PlaybackSourceType.BROWSE,
            playbackSourceId = "playlist-key",
        )
        val read = source.toMediaItem().toLocalSong()
        assertEquals(source.playbackSource, read.playbackSource)
        assertEquals(source.playbackSourceType, read.playbackSourceType)
        assertEquals(source.playbackSourceId, read.playbackSourceId)
        assertEquals("Recorded album", read.album)
    }

    @Test fun noSourceDoesNotInventAlbumProvenance() {
        val read = song().toMediaItem().toLocalSong()
        assertNull(read.playbackSource)
        assertNull(read.playbackSourceType)
        assertNull(read.playbackSourceId)
    }
}
