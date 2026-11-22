package com.music.aurelis.playback.cast

import com.music.aurelis.data.lyrics.LyricLine
import org.json.JSONArray
import org.json.JSONObject

/**
 * Compact, bounded Cast messages carrying the lyrics the existing service has
 * already resolved. The receiver plays audio itself and reads its own clock:
 * no audio frames, duplicated lyric lookups or per-frame phone messages.
 *
 * Messages are chunked because Cast custom messages have a size limit. Each
 * chunk is tagged with mediaId/revision so an old in-flight lookup cannot
 * overwrite the next track's lyric display.
 */
internal object CastLyricsPackets {
    const val NAMESPACE = "urn:x-cast:com.music.aurelis.lyrics"
    private const val MAX_MESSAGE_BYTES = 20 * 1024
    private const val MAX_LINES = 1200
    private const val MAX_TEXT_CHARS = 600
    private const val MAX_WORDS_PER_LINE = 120
    private const val MAX_WORD_CHARS = 80

    fun build(
        mediaId: String,
        lines: List<LyricLine>?,
        offsetMs: Int,
        revision: Int,
    ): List<String> {
        val allLines = lines.orEmpty().take(MAX_LINES).map { encodeLine(it) }
        val chunks = ArrayList<JSONArray>()
        var chunk = JSONArray()
        for (line in allLines) {
            // The complete wrapper is < 1 KiB; reserve it in every chunk.
            val candidate = JSONArray()
            for (i in 0 until chunk.length()) candidate.put(chunk.get(i))
            candidate.put(line)
            if (candidate.toString().toByteArray(Charsets.UTF_8).size > MAX_MESSAGE_BYTES - 1024 &&
                chunk.length() > 0
            ) {
                chunks.add(chunk)
                chunk = JSONArray().put(line)
            } else {
                chunk = candidate
            }
        }
        chunks.add(chunk)
        return chunks.mapIndexed { index, linesChunk ->
            JSONObject()
                .put("type", "lyrics")
                .put("mediaId", mediaId)
                .put("revision", revision)
                .put("offsetMs", offsetMs)
                .put("part", index)
                .put("total", chunks.size)
                .put("lines", linesChunk)
                .toString()
        }
    }

    private fun encodeLine(line: LyricLine): JSONObject {
        val encoded = JSONObject()
            .put("t", line.timeMs)
            .put("x", line.text.take(MAX_TEXT_CHARS))
        if (line.words.isNotEmpty()) {
            val words = JSONArray()
            for (word in line.words.take(MAX_WORDS_PER_LINE)) {
                words.put(JSONArray()
                    .put(word.startMs)
                    .put(word.endMs)
                    .put(word.text.take(MAX_WORD_CHARS)))
            }
            encoded.put("w", words)
        }
        line.background?.let { backing ->
            encoded.put("b", JSONObject()
                .put("t", backing.timeMs)
                .put("x", backing.text.take(MAX_TEXT_CHARS)))
        }
        return encoded
    }
}
