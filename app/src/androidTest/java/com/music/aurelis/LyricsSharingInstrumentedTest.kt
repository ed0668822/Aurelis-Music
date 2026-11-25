// SPDX-FileCopyrightText: © 2026 Aurelis

package com.music.aurelis

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Color
import android.media.MediaExtractor
import android.media.MediaFormat
import android.net.Uri
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.music.aurelis.data.model.Song
import com.music.aurelis.ui.player.LyricsImageFormat
import com.music.aurelis.ui.player.LyricsShareLine
import com.music.aurelis.ui.player.LyricsShareRequest
import com.music.aurelis.ui.player.cacheLyricsImage
import com.music.aurelis.ui.player.exportLocalLyricsVideo
import com.music.aurelis.ui.player.localLyricsAudioDurationMs
import com.music.aurelis.ui.player.lyricsShareTimeline
import com.music.aurelis.ui.player.renderLyricsShareCard
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File
import java.nio.ByteBuffer
import java.nio.ByteOrder

@RunWith(AndroidJUnit4::class)
class LyricsSharingInstrumentedTest {
    private val context = InstrumentationRegistry.getInstrumentation().targetContext
    private val song = Song(videoId = "test", title = "Test song", artist = "Test artist", thumbnailUrl = null)

    @Test fun fixedFormatsAndHighlightBoundsIncludeAllSelectedVerses() = runBlocking {
        val lines = listOf(
            LyricsShareLine("A long first verse ".repeat(7), null),
            LyricsShareLine("", null, isGap = true),
            LyricsShareLine("Dernier vers", "Último verso"),
        )
        val request = LyricsShareRequest(song, null, lines)
        val square = requireNotNull(renderLyricsShareCard(context, request, LyricsImageFormat.SQUARE, false))
        val story = requireNotNull(renderLyricsShareCard(context, request, LyricsImageFormat.STORY, true))

        assertEquals(1080, square.bitmap.width)
        assertEquals(1080, square.bitmap.height)
        assertEquals(1080, story.bitmap.width)
        assertEquals(1920, story.bitmap.height)
        assertEquals(setOf(0, 2), story.lineBounds.keys)
    }

    @Test fun oversizedSelectionCanFitStoryWithoutForcingItIntoSquare() = runBlocking {
        val lines = List(20) { LyricsShareLine("Verse number ${it + 1}", null) }
        val request = LyricsShareRequest(song, null, lines)

        assertNull(renderLyricsShareCard(context, request, LyricsImageFormat.SQUARE, false))
        assertNotNull(renderLyricsShareCard(context, request, LyricsImageFormat.STORY, false))
    }

    @Test fun secondImageShareDoesNotOverwriteFirstImage() = runBlocking {
        val bitmap = Bitmap.createBitmap(2, 2, Bitmap.Config.ARGB_8888)
        bitmap.eraseColor(Color.RED)
        val first = requireNotNull(cacheLyricsImage(context, bitmap))
        bitmap.eraseColor(Color.BLUE)
        val second = requireNotNull(cacheLyricsImage(context, bitmap))

        assertNotEquals(first, second)
        val firstPixel = context.contentResolver.openInputStream(first).use { input ->
            requireNotNull(BitmapFactory.decodeStream(input)).getPixel(0, 0)
        }
        assertEquals(Color.RED, firstPixel)
    }

    @Test(timeout = 90_000) fun mp4RetainsTheFullAudioTrackWithSparseLyrics() = runBlocking {
        val audio = File.createTempFile("lyrics-test", ".wav", context.cacheDir)
        val background = Bitmap.createBitmap(720, 1280, Bitmap.Config.ARGB_8888)
        var video: File? = null
        val extractor = MediaExtractor()
        try {
            audio.writeBytes(testTone())
            val uri = Uri.fromFile(audio).toString()
            val durationMs = requireNotNull(localLyricsAudioDurationMs(context, uri))
            val lines = listOf(
                LyricsShareLine("First", null, startMs = 750L, endMs = 1_250L),
                LyricsShareLine("Second", null, startMs = 2_500L, endMs = 3_000L),
            )
            val timeline = requireNotNull(lyricsShareTimeline(lines, durationMs))
            val result = requireNotNull(exportLocalLyricsVideo(context, background, timeline, lines, false, uri))
            video = result
            extractor.setDataSource(result.absolutePath)
            val formats = (0 until extractor.trackCount).map { extractor.getTrackFormat(it) }
            assertEquals(2, formats.size)
            val audioTrack = formats.single { it.getString(MediaFormat.KEY_MIME)?.startsWith("audio/") == true }
            val videoTrack = formats.single { it.getString(MediaFormat.KEY_MIME)?.startsWith("video/") == true }
            val expectedUs = durationMs * 1000L
            assertTrue(audioTrack.getLong(MediaFormat.KEY_DURATION) in (expectedUs - 100_000L)..(expectedUs + 100_000L))
            assertTrue(videoTrack.getLong(MediaFormat.KEY_DURATION) in (expectedUs - 100_000L)..(expectedUs + 100_000L))
        } finally {
            extractor.release()
            background.recycle()
            audio.delete()
            video?.delete()
        }
    }

    private fun testTone(): ByteArray {
        val sampleRate = 44_100
        val sampleCount = 4 * sampleRate
        val dataSize = sampleCount * Short.SIZE_BYTES
        return ByteBuffer.allocate(44 + dataSize).order(ByteOrder.LITTLE_ENDIAN).apply {
            put("RIFF".toByteArray())
            putInt(36 + dataSize)
            put("WAVEfmt ".toByteArray())
            putInt(16)
            putShort(1)
            putShort(1)
            putInt(sampleRate)
            putInt(sampleRate * Short.SIZE_BYTES)
            putShort(2)
            putShort(16)
            put("data".toByteArray())
            putInt(dataSize)
            repeat(sampleCount) { sample -> putShort(if (sample % 100 < 50) 1_000 else -1_000) }
        }.array()
    }
}
