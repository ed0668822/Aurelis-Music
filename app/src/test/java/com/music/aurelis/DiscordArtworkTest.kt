// SPDX-FileCopyrightText: 2026 Aurelis
// SPDX-License-Identifier: LGPL-3.0-or-later
package com.music.aurelis

import com.music.aurelis.data.discord.DiscordArtwork
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class DiscordArtworkTest {
    @Test fun onlyPublicMusicCdnHttpsCanBeSentToDiscord() {
        assertEquals(
            "https://is1-ssl.mzstatic.com/image/thumb/x/512x512bb.jpg",
            DiscordArtwork.publicUrl("https://is1-ssl.mzstatic.com/image/thumb/x/512x512bb.jpg"),
        )
        assertEquals(
            "https://i.ytimg.com/vi/music/maxresdefault.jpg",
            DiscordArtwork.publicUrl("https://i.ytimg.com/vi/music/maxresdefault.jpg"),
        )
        assertNull(DiscordArtwork.publicUrl("content://media/external/audio/albumart/42"))
        assertNull(DiscordArtwork.publicUrl("file:///storage/emulated/0/Music/cover.jpg"))
        assertNull(DiscordArtwork.publicUrl("smb://nas.local/private/album.jpg"))
        assertNull(DiscordArtwork.publicUrl("https://my-nas.example/private/cover.jpg"))
        assertNull(DiscordArtwork.publicUrl("https://is1-ssl.mzstatic.com.evil.invalid/cover.jpg"))
        assertNull(DiscordArtwork.publicUrl("http://i.ytimg.com/vi/music/cover.jpg"))
        assertNull(DiscordArtwork.publicUrl("https://user:pass@i.ytimg.com/cover.jpg"))
        assertNull(DiscordArtwork.publicUrl("https://i.ytimg.com/" + "x".repeat(300)))
    }

    @Test fun artworkMatchingRequiresCorrectArtistAndSong() {
        assertTrue(DiscordArtwork.sameName("Smells Like Teen Spirit", "Smells Like Teen Spirit"))
        assertTrue(DiscordArtwork.sameName("Beyoncé", "Beyonce"))
        assertTrue(DiscordArtwork.sameName("D'Angello & Francis", "D’Angello and Francis") == false)
        assertFalse(DiscordArtwork.sameName("Nirvana", "Kurt Cobain"))
        assertFalse(DiscordArtwork.sameName("Blade", "Blade Runner"))
    }
}
