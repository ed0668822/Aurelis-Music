package com.music.aurelis.data.lyrics

import com.music.aurelis.AurelisApplication
import org.json.JSONObject
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33], manifest = Config.NONE, application = AurelisApplication::class)
class RichLyricsTest {
    private val enhanced = "[00:01.0]<00:01.0>Lu<00:01.5>na <00:02.0>clara\n[00:04]Final"
    private val ttml = """
        <t:tt xmlns:t="http://www.w3.org/ns/ttml" xmlns:m="http://www.w3.org/ns/ttml#metadata">
          <t:body><t:div><t:p begin="1s" end="3s">
            <t:span begin="1s" end="1.5s">Lu</t:span><t:span begin="1.5s" end="2s">na </t:span><t:span begin="2s" end="3s">clara</t:span>
            <t:span m:role="x-bg" begin="1s" end="3s">eco</t:span>
            <t:span m:role="x-translation">Traducción excluida</t:span>
          </t:p><t:p begin="4s" end="5s">Final</t:p></t:div></t:body>
        </t:tt>
    """.trimIndent()

    @Test fun commonEntryPointKeepsEnhancedWordsAndSyllables() {
        val lyrics = Lyrics.parse(enhanced)!!
        assertTrue(lyrics.isSynced)
        val first = lyrics.lines.first()
        assertEquals("Luna clara", first.text)
        assertEquals(listOf("Luna", "clara"), first.words.map { it.text })
        assertEquals(listOf(1000L, 2000L), first.words.map { it.startMs })
        assertEquals(listOf(0, 2), first.words.first().syllables.map { it.startChar })
        assertEquals(listOf(2, 4), first.words.first().syllables.map { it.endChar })
        assertEquals(0, lyrics.activeIndexAt(1500)); assertEquals(1, lyrics.activeIndexAt(4500))
    }

    @Test fun repeatedEnhancedTimestampsShiftEveryWordAndOffsetOnlyOnce() {
        val lyrics = Lyrics.parse("[offset:500]\n[00:01][00:05]<00:01>Uno <00:02>dos")!!
        assertEquals(listOf(500L, 4500L), lyrics.lines.map { it.timeMs })
        assertEquals(listOf(500L, 4500L), lyrics.lines.map { it.words.first().startMs })
        assertEquals(listOf(1500L, 5500L), lyrics.lines.map { it.words.last().startMs })
        assertTrue(lyrics.lines.flatMap { it.words }.all { it.endMs >= it.startMs })
    }

    @Test fun invalidAndUnorderedWordTimesCannotCreateAnInvalidWordTimeline() {
        val lyrics = Lyrics.parse("[00:01]<00:03>Tres <00:02>dos\n[00:04]Final")!!
        assertTrue(lyrics.lines.all { it.words.isEmpty() })
        assertEquals("Tres dos", lyrics.lines.first().text)
        assertTrue(EnhancedLrc.parse("[00:99]<00:99>Inválida").isEmpty())
    }

    @Test fun numericEntitiesSupportSupplementaryUnicodeAndRejectOverflowSafely() {
        assertEquals("🌙 &", EnhancedLrc.decodeEntities("&#x1F319; &amp;"))
        assertEquals("&#99999999999999999999; &#xD800; &#0;",
            EnhancedLrc.decodeEntities("&#99999999999999999999; &#xD800; &#0;"))
    }

    @Test fun prefixedTtmlPreservesWordsSyllablesAndBackgroundWithoutTranslationText() {
        val lyrics = Lyrics.parse(ttml)!!
        assertTrue(lyrics.isSynced)
        val first = lyrics.lines.first()
        assertEquals("Luna clara", first.text)
        assertEquals(2, first.words.first().syllables.size)
        assertEquals("eco", first.background!!.text)
        assertFalse(first.text.contains("Traducción"))
        assertEquals(1, lyrics.activeIndexAt(4500))
    }

    @Test fun lineOnlyTtmlKeepsItsExplicitEndAndSkipsTranslatedParagraphs() {
        val lyrics = Lyrics.parse("""<tt xmlns:m="http://www.w3.org/ns/ttml#metadata"><body><p begin="1s" end="2s">Uno</p><p m:role="x-translation" begin="3s">Oculta</p></body></tt>""")!!
        assertEquals(1, lyrics.lines.size)
        assertEquals(2000L, lyrics.lines.single().sungUntilMs)
        assertEquals("Uno", lyrics.lines.single().text)
    }

    @Test fun ttmlInvalidWordOrderKeepsOnlyTheDeclaredLineTiming() {
        val lyrics = Lyrics.parse("""<tt><p begin="1s"><span begin="3s" end="4s">Tres </span><span begin="2s" end="3s">dos</span></p></tt>""")!!
        assertEquals(1000L, lyrics.lines.single().timeMs)
        assertEquals("Tres dos", lyrics.lines.single().text)
        assertTrue(lyrics.lines.single().words.isEmpty())
    }

    @Test fun ttmlTimesAcceptClockAndUnitsButRejectInvalidOrNonFiniteValues() {
        assertEquals(3723500L, TtmlLyrics.time("01:02:03.5"))
        assertEquals(1500L, TtmlLyrics.time("1500ms"))
        assertEquals(90_000L, TtmlLyrics.time("1.5m"))
        assertEquals(3_600_000L, TtmlLyrics.time("1h"))
        for (raw in listOf("-1s", "NaNs", "Infinitys", "1e300s", "00:60", "1:99:00", "12f", "10t"))
            assertNull(raw, TtmlLyrics.time(raw))
    }

    @Test fun nonTtmlXmlMalformedXmlAndExternalEntitiesAreRejected() {
        assertNull(Lyrics.parse("<html><p>Contenido ajeno</p></html>"))
        assertNull(Lyrics.parse("<tt><p begin='1s'>Sin cierre"))
        val secret = java.io.File.createTempFile("lyrics-entity-fixture", ".txt").apply { writeText("Must not appear as lyrics") }
        try {
            assertNull(Lyrics.parse("""<!DOCTYPE tt [<!ENTITY leak SYSTEM "${secret.toURI()}">]><tt><p begin="1s">&leak;</p></tt>"""))
        } finally { secret.delete() }
    }

    @Test fun deeplyNestedTtmlIsRejectedBeforeRecursiveCollection() {
        val nested = "<tt>" + "<span>".repeat(70) + "<p begin='1s'>Profunda</p>" + "</span>".repeat(70) + "</tt>"
        assertNull(Lyrics.parse(nested))
    }

    @Test fun richCacheRoundTripKeepsAllTimingAndAlignmentData() {
        val first = Lyrics.parse(ttml)!!.lines.first().copy(alignment = LyricAlignment.End)
        val line = first.copy(timingSource = first.copy(background = null))
        val original = LocalLyricsResult(listOf(line), LyricsSource.LRCLIB, true)
        val encoded = LyricsCacheCodec.encode(original)
        assertEquals(2, JSONObject(encoded).getInt("version"))
        val decoded = LyricsCacheCodec.decode(encoded)
        assertEquals(original.copy(cached = true), decoded)
        assertEquals(first.words.first().charsSungAt(1750), decoded.lines.first().words.first().charsSungAt(1750), 0f)
    }

    @Test fun legacyCacheRemainsReadableAndFutureVersionsAreRejected() {
        val raw = """{"version":1,"source":"LRCLIB","synced":true,"lines":[{"time":1000,"text":"Anterior"}]}"""
        val result = LyricsCacheCodec.decode(raw)
        assertTrue(result.cached); assertEquals(listOf(LyricLine(1000, "Anterior")), result.lines)
        try { LyricsCacheCodec.decode(raw.replace("\"version\":1", "\"version\":99")); fail("Future version must be rejected") }
        catch (_: IllegalArgumentException) { }
    }

    @Test fun corruptWordIntervalsAndCharacterRangesCannotBecomeCachedLyrics() {
        val encoded = LyricsCacheCodec.encode(LocalLyricsResult(Lyrics.parse(enhanced)!!.lines, LyricsSource.LRCLIB, true))
        for (field in listOf("end", "until")) {
            val root = JSONObject(encoded)
            val word = root.getJSONArray("lines").getJSONObject(0).getJSONArray("words").getJSONObject(0)
            if (field == "end") word.put("end", -1) else word.getJSONArray("syllables").getJSONObject(0).put("until", 500)
            try { LyricsCacheCodec.decode(root.toString()); fail("Invalid timing/range must be rejected") }
            catch (_: IllegalArgumentException) { }
        }
    }
}
