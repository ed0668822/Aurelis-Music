package com.music.aurelis.playback

import androidx.media3.common.*
import com.music.aurelis.data.model.LocalSong
import org.junit.Assert.*
import org.junit.Test

@org.junit.runner.RunWith(org.robolectric.RobolectricTestRunner::class)
@org.robolectric.annotation.Config(sdk = [33], application = com.music.aurelis.AurelisApplication::class)
@androidx.annotation.OptIn(androidx.media3.common.util.UnstableApi::class)
class AudioQualityInfoTest {
    private val song = LocalSong("1", "Track", "Artist", null, "content://media/1", null,
        1000, null, null, null, mimeType = "audio/flac", sourceQuality = "DOLBY", sourceHiRes = true)
    @Test fun fileMetadataDoesNotInventTechnicalValuesOrHonorLegacyMarketingLabels() {
        val info = AudioQualityInfo.from(song)
        assertEquals("FLAC", info.formatLabel)
        assertEquals(AudioQualityOrigin.FILE_METADATA, info.origin)
        assertNull(info.sampleRateHz); assertNull(info.bitrateBps); assertNull(info.pcmBits)
        assertEquals("Formato desconocido", AudioQualityInfo.from(song.copy(mimeType = null)).formatLabel)
        assertFalse(info.details.contains("Dolby"))
    }
    @Test fun selectedInputTrackReplacesMetadataWithoutClaimingAnOutputFormat() {
        val selected = Format.Builder().setSampleMimeType("audio/mpeg").setSampleRate(44100)
            .setChannelCount(2).setAverageBitrate(192000).build()
        val group = Tracks.Group(TrackGroup(selected), false, intArrayOf(C.FORMAT_HANDLED), booleanArrayOf(true))
        val info = AudioQualityInfo.selected(song, Tracks(listOf(group)))
        assertEquals("MP3", info.formatLabel)
        assertEquals(44100, info.sampleRateHz)
        assertEquals(192000, info.bitrateBps)
        assertEquals(AudioQualityOrigin.SELECTED_TRACK, info.origin)
        assertNull(info.pcmBits)
        assertEquals(AudioQualityInfo.from(song), AudioQualityInfo.selected(song, Tracks.EMPTY))
    }
    @Test fun missingTrackAndUnknownFormatClearPreviousQuality() {
        assertEquals(AudioQualityInfo(), AudioQualityInfo.from(null))
        val info = AudioQualityInfo.from(song, Format.Builder().setSampleMimeType("audio/raw")
            .setPcmEncoding(C.ENCODING_PCM_24BIT).build())
        assertEquals(24, info.pcmBits)
        assertNull(info.sampleRateHz)
        assertNull(AudioQualityInfo.from(song, Format.Builder().setSampleMimeType("audio/flac")
            .setPcmEncoding(C.ENCODING_PCM_24BIT).build()).pcmBits)
    }
    @Test fun multipleAdaptiveCandidatesDoNotBecomeMeasuredDecoderValues() {
        val a = Format.Builder().setSampleMimeType("audio/aac").setSampleRate(44100).build()
        val b = a.buildUpon().setSampleRate(48000).build()
        val group = Tracks.Group(TrackGroup(a, b), true,
            intArrayOf(C.FORMAT_HANDLED, C.FORMAT_HANDLED), booleanArrayOf(true, true))
        assertEquals(AudioQualityInfo.from(song), AudioQualityInfo.selected(song, Tracks(listOf(group))))
    }
}
