// Ported from BitChord. © 2026 Kushagra Singh (BitChord/Aurelis). GPL-3.0-or-later.
package com.music.aurelis.data.lyrics

/**
 * Enhanced ("A2") LRC — a normal LRC line with a stamp in front of each word.
 */
object EnhancedLrc {

    private val STAMP = Regex("""\[(\d{1,6}):(\d{2})(?:[.:](\d{1,3}))?]""")
    private val WORD = Regex("""<(\d{1,6}):(\d{2})(?:[.:](\d{1,3}))?>""")
    private val OFFSET = Regex("""(?im)^\s*\[offset:([+-]?\d+)]\s*$""")

    /** Shares the existing timing model with local imports; malformed words fall back to line timing. */
    fun parse(lrc: String): List<LyricLine> {
        if (lrc.isBlank() || lrc.length > 1_048_576 || '\u0000' in lrc) return emptyList()
        val offset = OFFSET.findAll(lrc).lastOrNull()?.groupValues?.get(1)?.toLongOrNull()
            ?.coerceIn(-86_400_000, 86_400_000) ?: 0L
        val rows = ArrayList<Row>()
        var totalRuns = 0
        for (raw in lrc.removePrefix("\uFEFF").lineSequence().take(10_000)) {
            val line = raw.trim()
            val leading = ArrayList<MatchResult>(); var end = 0
            for (tag in STAMP.findAll(line)) {
                if (tag.range.first != end) break
                leading += tag; end = tag.range.last + 1
            }
            val times = leading.mapNotNull(::stamp)
            val base = times.firstOrNull() ?: continue
            val body = line.substring(end)
            val matches = WORD.findAll(body).toList()
            val ends = matches.mapIndexed { index, match -> match.range.first to
                (matches.getOrNull(index + 1)?.range?.first ?: body.length) }.toMap()
            val originalWords = matches.mapNotNull { match -> stamp(match)?.let { match to it } }
            val ordered = originalWords.zipWithNext().all { (first, second) -> first.second <= second.second }
            for (time in times) {
                if (originalWords.size > 20_000 - totalRuns) return emptyList()
                totalRuns += originalWords.size
                rows += Row((time - offset).coerceAtLeast(0), body,
                    if (ordered) originalWords.map { (match, at) ->
                        val until = checkNotNull(ends[match.range.first])
                        TimedRun((at + time - base - offset).coerceAtLeast(0), 0L,
                            decodeEntities(body.substring(match.range.last + 1, until)))
                    } else emptyList())
                if (rows.size >= 10_000) break
            }
            if (rows.size >= 10_000) break
        }
        if (rows.none { it.runs.isNotEmpty() }) return emptyList()
        val sorted = rows.sortedBy { it.timeMs }
        return sorted.mapIndexed { index, row ->
            val tail = sorted.getOrNull(index + 1)?.timeMs ?: ((row.runs.lastOrNull()?.startMs ?: row.timeMs) + TAIL_MS)
            val runs = row.runs.mapIndexed { wordIndex, run ->
                TimedRun(run.startMs, maxOf(run.startMs, row.runs.getOrNull(wordIndex + 1)?.startMs ?: tail), run.raw)
            }
            val words = wordsFromRuns(runs)
            LyricLine(minOf(row.timeMs, words.firstOrNull()?.startMs ?: row.timeMs),
                decodeEntities(WORD.replace(row.body, "")).trim(), words)
        }.withInstrumentalGaps()
    }

    private class Row(val timeMs: Long, val body: String, val runs: List<TimedRun>)

    private fun stamp(match: MatchResult): Long? {
        val minutes = match.groupValues[1].toLongOrNull() ?: return null
        val seconds = match.groupValues[2].toLongOrNull()?.takeIf { it < 60 } ?: return null
        val fraction = match.groupValues[3].padEnd(3, '0').toLongOrNull() ?: 0L
        return minutes * 60_000 + seconds * 1000 + fraction
    }

    fun decodeEntities(text: String): String {
        if ('&' !in text) return text
        fun decoded(match: MatchResult, radix: Int): String {
            val code = match.groupValues[1].toIntOrNull(radix)
            return if (code != null && code in 1..0x10FFFF && code !in 0xD800..0xDFFF)
                String(Character.toChars(code)) else match.value
        }
        return text
            .replace(Regex("&#[xX]([0-9a-fA-F]+);")) { decoded(it, 16) }
            .replace(Regex("&#(\\d+);")) { decoded(it, 10) }
            .replace("&apos;", "'").replace("&quot;", "\"").replace("&nbsp;", " ")
            .replace("&lt;", "<").replace("&gt;", ">").replace("&amp;", "&")
    }

    private const val TAIL_MS = 800L
}
