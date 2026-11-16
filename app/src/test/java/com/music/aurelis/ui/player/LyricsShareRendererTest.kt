// Aurelis © 2026. GPL-3.0-or-later
package com.music.aurelis.ui.player

import android.graphics.BitmapFactory
import com.music.aurelis.AurelisApplication
import com.music.aurelis.data.model.LocalSong
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33], application = AurelisApplication::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class LyricsShareRendererTest {
    @Test fun wrappedCardAndStoryExportARealPngThroughTheConfiguredProvider() = runBlocking {
        val song = LocalSong("one", "Título 100% local", "Artista", null, "content://media/1", null, 60_000, null, null, null)
        val request = LyricsShareRequest(song, null, listOf(LyricsShareLine("Una frase con varias palabras que debe ajustarse al ancho de la tarjeta", "A translated line")))
        val card = renderLyricsShare(request, false)
        val story = renderLyricsShare(request, true)
        assertEquals(1080, card.width); assertEquals(1350, card.height)
        assertEquals(1080, story.width); assertEquals(1920, story.height)
        val changed = renderLyricsShare(request.copy(lines = listOf(LyricsShareLine("Otra letra", null))), false)
        assertFalse(card.sameAs(changed))
        val context = RuntimeEnvironment.getApplication() as AurelisApplication
        val uri = cacheForSharing(context, card, "lyrics-test.png") ?: error("Sharing URI unavailable")
        assertNotNull(uri); assertEquals("content", uri.scheme)
        val decoded = context.contentResolver.openInputStream(uri)!!.use { BitmapFactory.decodeStream(it) } ?: error("Invalid PNG")
        assertNotNull(decoded); assertEquals(1080, decoded.width); assertEquals(1350, decoded.height)
        assertEquals(android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION,
            sendIntent(uri).flags and android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION)
        card.recycle(); story.recycle(); changed.recycle(); decoded.recycle()
    }
}
