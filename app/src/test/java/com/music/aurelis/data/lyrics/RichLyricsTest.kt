// SPDX-FileCopyrightText: 2026 Aurelis
// SPDX-License-Identifier: LGPL-3.0-or-later
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

    @Test fun localEmbeddedLrcVoiceLabelsDoNotPolluteTheVisibleLyrics() {
        val parsed = Lyrics.parse(
            "[00:01.00]v1:Just stuck, hollow and alone\n" +
                "[00:03.00]V1:And the fault is my own\n" +
                "[00:05.00]<R>v2:Answering voice"
        )!!
        val sung = parsed.lines.filterNot { it.isGap }
        assertEquals(
            listOf("Just stuck, hollow and alone", "And the fault is my own", "Answering voice"),
            sung.map { it.text },
        )
        assertEquals(listOf(1000L, 3000L, 5000L), sung.map { it.timeMs })
        assertEquals(LyricAlignment.End, sung.last().alignment)
    }

    @Test fun voiceLabelBeforeOrAfterFirstWordStampKeepsItsActualWordTiming() {
        val first = Lyrics.parse("[00:01.00]v1:<00:01.00>First <00:02.00>word")!!
        val second = Lyrics.parse("[00:01.00]<00:01.00>v1:First <00:02.00>word")!!
        for (lyrics in listOf(first, second)) {
            val line = lyrics.lines.first { !it.isGap }
            assertEquals("First word", line.text)
            assertEquals(listOf("First", "word"), line.words.map { it.text })
            assertEquals(listOf(1000L, 2000L), line.words.map { it.startMs })
        }
        assertEquals("a v1: is an actual lyric", Lyrics.parse("a v1: is an actual lyric")!!.lines.single().text)
    }

    @Test fun ordinaryLrcKeepsRightAlignedRowsAndIntroducesRealLongLeadIn() {
        val lines = Lyrics.parse("[ar:Fixture]\n[00:05.00][00:07.00]<R>Derecha\n[00:09.00]Izquierda")!!
        assertTrue(lines.isSynced)
        assertTrue(lines.lines.first().isGap)
        assertEquals(0L, lines.lines.first().timeMs)
        assertEquals(0, lines.activeIndexAt(2_000))
        assertEquals(listOf(5000L, 7000L, 9000L), lines.lines.filterNot { it.isGap }.map { it.timeMs })
        assertEquals(listOf("Derecha", "Derecha", "Izquierda"), lines.lines.filterNot { it.isGap }.map { it.text })
        assertEquals(listOf(LyricAlignment.End, LyricAlignment.End, LyricAlignment.Start),
            lines.lines.filterNot { it.isGap }.map { it.alignment })
    }

    @Test fun mixedEnhancedAndOrdinaryLrcPreservesWordTimesAndRtlAlignment() {
        // The space between 'na' and 'clara' is an explicit A2 word boundary.
        // With no whitespace anywhere, neither Aurelis nor BitChord invents syllables.
        val parsed = Lyrics.parse("[00:01.00]<R><00:01.00>Lu<00:01.50>na <00:02.00>clara\n[00:03.00]Siguiente")!!
        val first = parsed.lines.first()
        assertEquals(LyricAlignment.End, first.alignment)
        assertEquals("Luna clara", first.text)
        assertTrue(first.isWordSynced)
        assertEquals(listOf(1000L, 1500L), first.words.first().syllables.map { it.startMs })
        assertEquals("Siguiente", parsed.lines.last().text)
        assertEquals(LyricAlignment.Start, parsed.lines.last().alignment)
    }

    @Test fun a2WithoutWhitespaceKeepsSeparateWordsInsteadOfGuessingSyllables() {
        val lines = Lyrics.parse("[00:01.00]<00:01.00>Lu<00:01.50>na")!!.lines
        assertEquals(listOf("Lu", "na"), lines.flatMap { it.words }.map { it.text })
        assertTrue(lines.flatMap { it.words }.all { it.syllables.isEmpty() })
    }

    @Test fun malformedA2StampFallsBackToLineTimingRatherThanPartialWordSweep() {
        val lines = Lyrics.parse("[00:01.00]<00:01.00>Bien <00:99.00>mal")!!.lines
        assertEquals("Bien mal", lines.first().text)
        assertTrue(lines.all { it.words.isEmpty() })
        assertTrue(lines.first().timeMs == 1000L)
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

    @Test fun invalidTtmlIntervalsNeverMergeIntoPreviousWordOrCreateBackwardsLine() {
        val text = """<tt><body>
            <p begin="1s" end="4s"><span begin="1s" end="2s">Bien </span><span begin="5s" end="4s">Mal</span></p>
            <p begin="6s" end="5s">Descartar</p>
        </body></tt>"""
        val lyrics = Lyrics.parse(text)!!
        assertEquals(1, lyrics.lines.count { !it.isGap })
        assertEquals("Bien", lyrics.lines.first { !it.isGap }.text)
        assertFalse(lyrics.lines.any { "Mal" in it.text || "Descartar" in it.text })
    }

    @Test fun nestedTtmlBackgroundAndTranslationAreKeptSeparateFromLead() {
        val text = """<tt xmlns:ttm="http://www.w3.org/ns/ttml#metadata"><body>
            <p begin="1s" end="4s">
                <span begin="1s" end="2s">Principal </span>
                <span ttm:role="x-bg"><span begin="1.5s" end="2.5s">Eco</span></span>
                <span ttm:role="x-translation" begin="1s" end="3s">Excluir</span>
                <span begin="2s" end="3s">final</span>
            </p>
        </body></tt>"""
        val lyrics = Lyrics.parse(text)!!
        val line = lyrics.lines.first { !it.isGap }
        assertEquals("Principal final", line.text)
        assertEquals("Eco", line.background!!.text)
        assertEquals(1500L, line.background!!.timeMs)
        assertFalse(line.text.contains("Excluir"))
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
