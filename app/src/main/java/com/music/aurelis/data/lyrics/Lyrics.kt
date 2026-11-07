// Aurelis © 2026. GPL-3.0-or-later.
package com.music.aurelis.data.lyrics

data class Lyrics(val lines: List<LyricLine>, val isSynced: Boolean) {
    fun activeIndexAt(positionMs: Long): Int {
        if (!isSynced) return -1
        var low = 0; var high = lines.size
        while (low < high) { val middle = (low + high) ushr 1
            if (lines[middle].timeMs <= positionMs) low = middle + 1 else high = middle
        }
        return low - 1
    }
    companion object {
        private val timestamp = Regex("""\[(\d{1,6}):(\d{2})(?:[.:](\d{1,3}))?]""")
        private val offsetTag = Regex("""(?im)^\s*\[offset:([+-]?\d+)]\s*$""")
        private val words = Regex("""<\d+:\d{2}(?:\.\d{1,3})?>""")
        fun parse(raw: String): Lyrics? {
            if (raw.isBlank() || raw.length > 1_048_576 || raw.indexOf('\u0000') >= 0) return null
            if (raw.removePrefix("\uFEFF").trimStart().startsWith("<")) {
                val lines = TtmlLyrics.parse(raw.removePrefix("\uFEFF"))
                return lines.takeIf { it.any { line -> line.text.isNotBlank() } }?.let { Lyrics(it, true) }
            }
            val enhanced = EnhancedLrc.parse(raw)
            if (enhanced.any { it.isWordSynced }) return Lyrics(enhanced, true)
            val offset = offsetTag.findAll(raw).lastOrNull()?.groupValues?.get(1)?.toLongOrNull()?.coerceIn(-86_400_000, 86_400_000) ?: 0
            val timed = mutableListOf<LyricLine>(); val plain = mutableListOf<LyricLine>()
            raw.removePrefix("\uFEFF").lineSequence().take(10_000).forEach { value ->
                val line = value.trim()
                if (line.isEmpty()) return@forEach
                val tags = timestamp.findAll(line).toList().takeIf { it.firstOrNull()?.range?.first == 0 }.orEmpty()
                if (tags.isEmpty()) {
                    if (!line.startsWith("[")) plain.add(LyricLine(0, line))
                } else {
                    // Repeated leading timestamps apply the same text to each occurrence.
                    val leading = mutableListOf<MatchResult>(); var end = 0
                    tags.forEach { if (it.range.first == end) { leading.add(it); end = it.range.last + 1 } }
                    val text = words.replace(line.substring(end), "").trim()
                    leading.forEach timestampLoop@{ tag ->
                        val minutes = tag.groupValues[1].toLongOrNull() ?: return@timestampLoop
                        val seconds = tag.groupValues[2].toLongOrNull() ?: return@timestampLoop
                        if (seconds >= 60) return@timestampLoop
                        val fraction = tag.groupValues[3].padEnd(3, '0').toLongOrNull() ?: 0
                        timed.add(LyricLine((minutes * 60_000 + seconds * 1000 + fraction - offset).coerceAtLeast(0), text))
                    }
                }
            }
            return when {
                timed.any { it.text.isNotEmpty() } -> Lyrics(timed.sortedBy { it.timeMs }.take(10_000), true)
                plain.isNotEmpty() -> Lyrics(plain, false) // No fabricated timing for plain text.
                else -> null
            }
        }
    }
}
