package com.music.aurelis.data.lyrics

import android.net.Uri
import com.music.aurelis.AurelisApplication
import com.music.aurelis.data.model.LocalSong
import kotlinx.coroutines.*
import kotlinx.coroutines.test.runTest
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config
import java.io.ByteArrayOutputStream
import java.io.File

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33], manifest = Config.NONE, application = AurelisApplication::class)
class LocalLyricsTest {
    private val app get() = RuntimeEnvironment.getApplication() as AurelisApplication
    private fun song(id: String = "1") = LocalSong(id, "Prueba & canción", "Artista", "Álbum", "content://media/external/audio/media/$id",
        null, 60_000, null, 1, 2026, sizeBytes = 1000)
    private fun file(name: String, bytes: ByteArray): File = File(app.cacheDir, name).also { it.writeBytes(bytes) }

    @Test fun lrcSupportsRepeatedTimestampsOffsetsOrderFractionsAndGaps() {
        val lyrics = Lyrics.parse("[ar:Fixture]\n[offset:500]\n[00:02.5][00:04.050]Línea repetida\n[00:01.00]Inicio\n[00:03.00]\n[00:99.00]Inválida")!!
        assertTrue(lyrics.isSynced)
        assertEquals(listOf(500L, 2000L, 2500L, 3550L), lyrics.lines.map { it.timeMs })
        assertEquals("Línea repetida", lyrics.lines.last().text)
        assertEquals(-1, lyrics.activeIndexAt(499)); assertEquals(0, lyrics.activeIndexAt(500))
        assertTrue(lyrics.lines[lyrics.activeIndexAt(3000)].isGap)
    }
    @Test fun plainTextNeverReceivesFabricatedTimesOrAnActiveLine() {
        val lyrics = Lyrics.parse("Primera línea\nSegunda línea")!!
        assertFalse(lyrics.isSynced); assertEquals(listOf(0L, 0L), lyrics.lines.map { it.timeMs })
        assertEquals(-1, lyrics.activeIndexAt(100_000))
        assertNull(Lyrics.parse("[ar:Solo metadatos]")); assertNull(Lyrics.parse("\u0000binario"))
        assertNull(Lyrics.parse("x".repeat(1_048_577)))
    }
    @Test fun id3UsltAndFlacCommentsAreReadWithoutScanningAudio() {
        val raw = "[00:01.000]Letra propia de prueba"
        assertEquals(raw, EmbeddedLyricsExtractor().extract(id3(raw).inputStream()))
        val comment = "LYRICS=$raw".toByteArray()
        val block = le(4) + "test".toByteArray() + le(1) + le(comment.size) + comment
        val bytes = "fLaC".toByteArray() + byteArrayOf(0x84.toByte(), 0, 0, block.size.toByte()) + block
        assertEquals(raw, EmbeddedLyricsExtractor().extract(bytes.inputStream()))
        assertNull(EmbeddedLyricsExtractor().extract(byteArrayOf(1, 2).inputStream()))
        assertNull(EmbeddedLyricsExtractor().extract(("ID3".toByteArray() + byteArrayOf(3,0,0,127,127,127,127)).inputStream()))
    }
    @Test fun localImportWinsOverEmbeddedCacheAndNetworkAndSurvivesRepositoryReopen() = runTest {
        var requests = 0
        val audio = file("audio.mp3", id3("[00:01.00]Incrustada de prueba"))
        val song = song().copy(contentUri = Uri.fromFile(audio).toString())
        val repo = LocalLyricsRepository(app) { requests++; error("Must not request network") }
        assertEquals(LyricsSource.EMBEDDED, repo.find(song, true)!!.source)
        val lrc = file("sidecar.lrc", "[00:02.000]Importada de prueba".toByteArray())
        repo.import(song, Uri.fromFile(lrc))
        assertEquals("Importada de prueba", repo.find(song, true)!!.lines.single().text)
        assertEquals(LyricsSource.LOCAL_FILE, LocalLyricsRepository(app).find(song)!!.source)
        assertEquals(0, requests)
    }
    @Test fun cacheWorksOfflineAndChangedMetadataDoesNotReuseWrongRecording() = runTest {
        var requests = 0
        val repo = LocalLyricsRepository(app) { requests++; LyricsRepository.Result(LyricsSource.LRCLIB, listOf(LyricLine(1000, "Respuesta ficticia de prueba"))) }
        assertNull(repo.find(song())); assertEquals(0, requests)
        assertFalse(repo.find(song(), true)!!.cached); assertEquals(1, requests)
        val offline = LocalLyricsRepository(app) { error("No remote access permitted") }
        assertTrue(offline.find(song(), true)!!.cached)
        assertNull(offline.find(song().copy(durationMs = 70_000)))
    }
    @Test fun utf16ImportAndFailedReplacementPreserveThePreviousFile() = runTest {
        val repo = LocalLyricsRepository(app)
        val lrc = file("utf16.lrc", "[00:01.0]Árbol propio".toByteArray(Charsets.UTF_16))
        repo.import(song(), Uri.fromFile(lrc))
        assertEquals("Árbol propio", repo.find(song())!!.lines.single().text)
        val broken = file("broken.lrc", byteArrayOf(0, 1, 2))
        try { repo.import(song(), Uri.fromFile(broken)); fail("Expected invalid input") } catch (_: IllegalStateException) { }
        assertEquals("Árbol propio", repo.find(song())!!.lines.single().text)
    }
    @Test fun cancellationIsNotConvertedToMissingLyricsOrCached() = runTest {
        val repo = LocalLyricsRepository(app) { throw CancellationException("fixture") }
        try { repo.find(song(), true); fail("Cancellation must propagate") } catch (_: CancellationException) { }
        assertNull(repo.find(song()))
    }
    @Test fun lrclibEncodesMetadataWithoutTurningCharactersIntoQueryParameters() {
        val url = LrcLib.searchUrl("Árbol & Sol? #1", "A+B / 日本語")
        assertEquals("Árbol & Sol? #1", url.queryParameter("track_name"))
        assertEquals("A+B / 日本語", url.queryParameter("artist_name"))
        assertEquals(setOf("track_name", "artist_name"), url.queryParameterNames)
    }
    @Test fun lrclibRejectsWrongArtistsAndVersionsWithDifferentDurations() {
        val rows = org.json.JSONArray().put(org.json.JSONObject().put("id", 1).put("trackName", "Árbol").put("artistName", "Otro").put("duration", 60))
            .put(org.json.JSONObject().put("id", 2).put("trackName", "Árbol").put("artistName", "Artista").put("duration", 90))
            .put(org.json.JSONObject().put("id", 3).put("trackName", "Árbol").put("artistName", "Artista").put("duration", 61))
        assertEquals(3, LrcLib.selectTrack(rows, "arbol", "ARTISTA", 60)!!.getInt("id"))
        assertNull(LrcLib.selectTrack(rows, "Canción diferente", "ARTISTA", 60))
    }
    @Test fun importedTtmlKeepsWordTimingAfterRepositoryReopenWithoutNetwork() = runTest {
        val raw = """<tt><body><p begin="1s"><span begin="1s" end="2s">Luna</span></p></body></tt>"""
        val local = file("fixture.ttml", raw.toByteArray())
        val repo = LocalLyricsRepository(app) { error("No network permitted") }
        val imported = repo.import(song(), Uri.fromFile(local))
        assertTrue(imported.isSynced); assertTrue(imported.lines.first().isWordSynced)
        val reopened = LocalLyricsRepository(app) { error("No network permitted") }.find(song(), true)!!
        assertEquals(imported.lines, reopened.lines)
        assertEquals(LyricsSource.LOCAL_FILE, reopened.source)
    }

    @Test fun richRemoteCacheRetainsTimingOfflineInsteadOfFlatteningWords() = runTest {
        val lines = Lyrics.parse("[00:01]<00:01>Luna <00:02>clara")!!.lines
        val repo = LocalLyricsRepository(app) { LyricsRepository.Result(LyricsSource.LRCLIB, lines) }
        assertEquals(lines, repo.find(song(), true)!!.lines)
        val offline = LocalLyricsRepository(app) { error("No remote access permitted") }.find(song(), true)!!
        assertTrue(offline.cached); assertEquals(lines, offline.lines)
        assertTrue(offline.lines.first().isWordSynced)
    }

    private fun le(value: Int) = byteArrayOf(value.toByte(), (value ushr 8).toByte(), (value ushr 16).toByte(), (value ushr 24).toByte())
    private fun id3(text: String): ByteArray {
        val payload = byteArrayOf(3, 'e'.code.toByte(), 'n'.code.toByte(), 'g'.code.toByte(), 0) + text.toByteArray()
        val size = payload.size
        val frame = "USLT".toByteArray() + byteArrayOf((size ushr 24).toByte(), (size ushr 16).toByte(), (size ushr 8).toByte(), size.toByte(), 0, 0) + payload
        return "ID3".toByteArray() + byteArrayOf(3, 0, 0, (frame.size ushr 21).toByte(), ((frame.size ushr 14) and 127).toByte(),
            ((frame.size ushr 7) and 127).toByte(), (frame.size and 127).toByte()) + frame
    }
}
