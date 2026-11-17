// Aurelis © 2026. GPL-3.0-or-later
package com.music.aurelis.ui

import android.graphics.Bitmap
import android.graphics.Color
import coil3.size.Size
import com.music.aurelis.ui.components.home.RecommendationBackdropBlur
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33, 35])
class RecommendationBackdropTest {
    @Test fun onlyTheLowerCoverStripContributesAndTheInputRemainsOwnedByCoil() = runBlocking {
        val input = Bitmap.createBitmap(100, 100, Bitmap.Config.ARGB_8888)
        input.eraseColor(Color.RED)
        for (y in 60 until 100) for (x in 0 until 100) input.setPixel(x, y, Color.BLUE)
        val result = RecommendationBackdropBlur.transform(input, Size.ORIGINAL)
        assertEquals(48, result.width)
        assertEquals(Color.BLUE, result.getPixel(24, result.height / 2))
        assertFalse(input.isRecycled)
        assertEquals(Color.RED, input.getPixel(0, 0))
    }

    @Test fun extremeArtworkAspectRatiosHaveBoundedOutputAndKeepSolidColors() = runBlocking {
        for ((width, height) in listOf(1 to 4096, 4096 to 1, 1 to 1)) {
            val input = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
            input.eraseColor(Color.GREEN)
            val result = RecommendationBackdropBlur.transform(input, Size.ORIGINAL)
            assertEquals(48, result.width)
            assertTrue(result.height in 4..96)
            assertEquals(Color.GREEN, result.getPixel(0, 0))
            assertFalse(input.isRecycled)
            result.recycle(); input.recycle()
        }
    }
}
