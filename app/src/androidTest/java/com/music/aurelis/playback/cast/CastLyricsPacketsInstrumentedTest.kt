// SPDX-FileCopyrightText: © 2026 Aurelis

package com.music.aurelis.playback.cast

import androidx.test.ext.junit.runners.AndroidJUnit4
import com.music.aurelis.data.lyrics.LyricLine
import com.music.aurelis.data.lyrics.LyricWord
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class CastLyricsPacketsInstrumentedTest {

    @Test
    fun lineAndWordTimingsSurviveSerialization() {
        val lyric = LyricLine(
            timeMs = 1500,
            text = "Hello world",
            words = listOf(
                LyricWord(startMs = 1500, endMs = 1800, text = "Hello"),
                LyricWord(startMs = 1850, endMs = 2100, text = "world"),
            ),
        )
        val packets = CastLyricsPackets.build("local-song", listOf(lyric), 120, 7)
        assertEquals(1, packets.size)
        val payload = JSONObject(packets.single())
        assertEquals("lyrics", payload.getString("type"))
        assertEquals("local-song", payload.getString("mediaId"))
        assertEquals(120, payload.getInt("offsetMs"))
        assertEquals(7, payload.getInt("revision"))
        assertEquals(1, payload.getInt("total"))
        val first = payload.getJSONArray("lines").getJSONObject(0)
        assertEquals("Hello world", first.getString("x"))
        assertEquals(1500L, first.getLong("t"))
        assertEquals(2, first.getJSONArray("w").length())
        assertEquals(1850L, first.getJSONArray("w").getJSONArray(1).getLong(0))
    }

    @Test
    fun missingLyricsProduceExplicitEmptyTimeline() {
        val packets = CastLyricsPackets.build("missing-song", null, 0, 2)
        assertEquals(1, packets.size)
        val payload = JSONObject(packets.single())
        assertEquals("missing-song", payload.getString("mediaId"))
        assertEquals(0, payload.getJSONArray("lines").length())
    }

    @Test
    fun longTimelinesAreChunkedWithoutDroppingLyrics() {
        val lines = (0 until 200).map { index ->
            LyricLine(index * 1000L, "repeat-" + index + "x".repeat(350))
        }
        val packets = CastLyricsPackets.build("long-song", lines, -75, 9)
        assertTrue(packets.size > 1)
        var count = 0
        packets.forEachIndexed { index, serialized ->
            val packet = JSONObject(serialized)
            assertEquals(index, packet.getInt("part"))
            assertEquals(packets.size, packet.getInt("total"))
            assertEquals(-75, packet.getInt("offsetMs"))
            assertTrue(serialized.toByteArray(Charsets.UTF_8).size < 21 * 1024)
            count += packet.getJSONArray("lines").length()
        }
        assertEquals(lines.size, count)
    }

    @Test
    fun nonsynchronizedLinesDoNotClaimWordTimestamps() {
        val packets = CastLyricsPackets.build("line-only", listOf(LyricLine(0L, "One line")), 0, 1)
        val row = JSONObject(packets.single()).getJSONArray("lines").getJSONObject(0)
        assertFalse(row.has("w"))
    }
}
