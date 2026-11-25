// SPDX-FileCopyrightText: © 2026 Aurelis

package com.music.aurelis

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Color
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.music.aurelis.data.model.Song
import com.music.aurelis.ui.player.LyricsImageFormat
import com.music.aurelis.ui.player.LyricsShareLine
import com.music.aurelis.ui.player.LyricsShareRequest
import com.music.aurelis.ui.player.cacheLyricsImage
import com.music.aurelis.ui.player.renderLyricsShareCard
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test
import org.junit.runner.RunWith

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
}
