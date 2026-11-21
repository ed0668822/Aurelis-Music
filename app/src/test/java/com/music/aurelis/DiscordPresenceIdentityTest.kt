package com.music.aurelis

import com.music.aurelis.data.discord.DiscordApplication
import com.music.aurelis.data.discord.DiscordRPC
import com.music.aurelis.data.model.Song
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/** Guards the Discord application registration and local-music privacy. */
class DiscordPresenceIdentityTest {
    private fun song(id: String, localUri: String? = null) = Song(
        videoId = id,
        title = "Numb",
        artist = "Linkin Park",
        thumbnailUrl = null,
        localUri = localUri,
    )

    @Test fun officialDiscordApplicationIdIsUsed() {
        assertEquals("1573536052935794802", DiscordApplication.ID)
        assertEquals(
            "https://github.com/ed0668822/Aurelis-Music",
            DiscordRPC.PROJECT_URL,
        )
    }

    @Test fun localMusicNeverCreatesFakeYouTubeLinks() {
        assertNull(
            DiscordRPC.youtubeWatchUrlOrNull(
                song("local:media:42", "content://media/external/audio/media/42"),
            ),
        )
        assertNull(DiscordRPC.youtubeWatchUrlOrNull(song("spotify:other-source")))
    }

    @Test fun validYouTubeTracksKeepTheirRealLink() {
        assertEquals(
            "https://music.youtube.com/watch?v=kXYiU_JCYtU",
            DiscordRPC.youtubeWatchUrlOrNull(song("kXYiU_JCYtU")),
        )
    }
}
