package com.music.aurelis.playback

import com.music.aurelis.data.model.Song
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class YouTubeRadioSeedTest {
    private fun song(videoId: String, localUri: String? = null) = Song(
        videoId = videoId, title = "Local Track", artist = "Artist",
        thumbnailUrl = null, localUri = localUri,
    )

    @Test fun realYouTubeIdDoesNotRequireMatching() {
        assertFalse(radioNeedsYouTubeMatch(song("dQw4w9WgXcQ")))
        assertFalse(radioNeedsYouTubeMatch(song("dQw4w9WgXcQ", "content://media/audio/42")))
    }

    @Test fun localFileAndMediaStoreIdsRequireYouTubeMatching() {
        assertTrue(radioNeedsYouTubeMatch(song("content://media/external/audio/42", "content://media/external/audio/42")))
        assertTrue(radioNeedsYouTubeMatch(song("file:///music/song.flac", "file:///music/song.flac")))
        assertTrue(radioNeedsYouTubeMatch(song("local:audio/42", "content://media/audio/42")))
    }

    @Test fun unrecognizedIdsCannotBeSentDirectlyToRadio() {
        assertTrue(radioNeedsYouTubeMatch(song("")))
        assertTrue(radioNeedsYouTubeMatch(song("external_track_identifier")))
    }
}
