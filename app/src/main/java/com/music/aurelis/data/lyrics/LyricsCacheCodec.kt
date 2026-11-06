package com.music.aurelis.data.lyrics

import org.json.JSONArray
import org.json.JSONObject

/** Version2 preserves the existing lyric timing model; version1 remains readable offline. */
internal object LyricsCacheCodec {
    fun encode(result: LocalLyricsResult): String {
        require(result.lines.size in 1..10_000)
        val limits = Limits()
        return JSONObject().put("version", 2).put("source", result.source.name).put("synced", result.isSynced)
            .put("lines", JSONArray().apply { result.lines.forEach { put(writeLine(it, 0, limits)) } }).toString()
    }

    fun decode(raw: String): LocalLyricsResult {
        val root = JSONObject(raw)
        require(root.getInt("version") in 1..2)
        val values = root.getJSONArray("lines")
        require(values.length() in 1..10_000)
        val limits = Limits()
        val lines = (0 until values.length()).map { readLine(values.getJSONObject(it), 0, limits) }
        return LocalLyricsResult(lines.sortedBy { it.timeMs }, LyricsSource.valueOf(root.getString("source")),
            root.getBoolean("synced"), cached = true)
    }

    private fun writeLine(line: LyricLine, depth: Int, limits: Limits): JSONObject {
        limits.line(depth, line.text)
        validate(line)
        return JSONObject().put("time", line.timeMs).put("text", line.text).put("alignment", line.alignment.name)
            .put("end", line.sungUntilMs)
            .put("words", JSONArray().apply { line.words.forEach { word ->
                limits.item(); limits.text(word.text)
                put(JSONObject().put("start", word.startMs).put("end", word.endMs).put("text", word.text)
                    .put("syllables", JSONArray().apply { word.syllables.forEach { part ->
                        limits.item()
                        put(JSONObject().put("start", part.startMs).put("end", part.endMs)
                            .put("from", part.startChar).put("until", part.endChar))
                    } }))
            } })
            .put("background", line.background?.let { writeLine(it, depth + 1, limits) })
            .put("timingSource", line.timingSource?.let { writeLine(it, depth + 1, limits) })
    }

    private fun readLine(row: JSONObject, depth: Int, limits: Limits): LyricLine {
        val text = row.getString("text")
        limits.line(depth, text)
        val values = if (row.has("words") && !row.isNull("words")) row.getJSONArray("words") else JSONArray()
        val words = (0 until values.length()).map { index ->
            limits.item()
            val word = values.getJSONObject(index)
            val content = word.getString("text"); limits.text(content)
            val parts = if (word.has("syllables") && !word.isNull("syllables")) word.getJSONArray("syllables") else JSONArray()
            LyricWord(word.getLong("start"), word.getLong("end"), content,
                (0 until parts.length()).map { partIndex ->
                    limits.item()
                    val part = parts.getJSONObject(partIndex)
                    LyricSyllable(part.getLong("start"), part.getLong("end"), part.getInt("from"), part.getInt("until"))
                })
        }
        val line = LyricLine(row.getLong("time"), text, words,
            sungUntilMs = if (row.has("end") && !row.isNull("end")) row.getLong("end") else null,
            background = if (row.has("background") && !row.isNull("background")) readLine(row.getJSONObject("background"), depth + 1, limits) else null,
            alignment = LyricAlignment.valueOf(row.optString("alignment", LyricAlignment.Start.name)),
            timingSource = if (row.has("timingSource") && !row.isNull("timingSource")) readLine(row.getJSONObject("timingSource"), depth + 1, limits) else null)
        validate(line)
        return line
    }

    private fun validate(line: LyricLine) {
        fun time(value: Long) { require(value in 0..(Long.MAX_VALUE - 86_400_000)) }
        time(line.timeMs)
        line.sungUntilMs?.let { time(it); require(it >= line.timeMs) }
        require(line.words.zipWithNext().all { (a, b) -> a.startMs <= b.startMs })
        var offset = 0
        line.words.forEach { word ->
            time(word.startMs); time(word.endMs)
            require(word.startMs >= line.timeMs && word.endMs >= word.startMs && word.text.isNotEmpty())
            val at = line.text.indexOf(word.text, offset)
            require(at >= offset); offset = at + word.text.length
            require(word.syllables.zipWithNext().all { (a, b) -> a.startMs <= b.startMs })
            var endChar = 0
            word.syllables.forEach { part ->
                time(part.startMs); time(part.endMs)
                require(part.startMs >= word.startMs && part.endMs in part.startMs..word.endMs && part.startChar >= endChar &&
                    part.endChar > part.startChar && part.endChar <= word.text.length)
                endChar = part.endChar
            }
        }
    }

    private class Limits {
        private var items = 0
        private var characters = 0
        fun item() { require(++items <= 20_000) }
        fun text(value: String) { require(value.length <= 1_048_576 - characters); characters += value.length }
        fun line(depth: Int, value: String) { require(depth <= 4); item(); text(value) }
    }
}
