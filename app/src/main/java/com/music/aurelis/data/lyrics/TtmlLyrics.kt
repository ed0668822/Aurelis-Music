// Ported from BitChord. © 2026 Kushagra Singh (BitChord/Aurelis). GPL-3.0-or-later.
package com.music.aurelis.data.lyrics

import org.w3c.dom.Element
import org.w3c.dom.Node
import java.io.StringReader
import javax.xml.XMLConstants
import javax.xml.parsers.DocumentBuilderFactory
import org.xml.sax.InputSource
import org.xml.sax.SAXException
import org.xml.sax.SAXParseException
import org.xml.sax.helpers.DefaultHandler

object TtmlLyrics {

    private val SKIPPED_ROLES = setOf("x-translation", "x-roman")
    private const val BACKGROUND_ROLE = "x-bg"

    fun parse(ttml: String): List<LyricLine> = runCatching {
        require(ttml.isNotBlank() && ttml.length <= 1_048_576 && '\u0000' !in ttml)
        // Also reject declarations when a platform parser does not implement every feature flag.
        require(!Regex("""<!\s*(?:DOCTYPE|ENTITY)\b""", RegexOption.IGNORE_CASE).containsMatchIn(ttml))
        val factory = DocumentBuilderFactory.newInstance().apply {
            isNamespaceAware = true
            harden("http://apache.org/xml/features/disallow-doctype-decl")
            harden("http://xml.org/sax/features/external-general-entities", false)
            harden("http://xml.org/sax/features/external-parameter-entities", false)
            harden(XMLConstants.FEATURE_SECURE_PROCESSING)
            runCatching { isExpandEntityReferences = false }
        }
        val builder = factory.newDocumentBuilder().apply {
            setEntityResolver { _, _ -> throw SAXException("External XML entities are not supported") }
            setErrorHandler(object : DefaultHandler() {
                override fun error(error: SAXParseException) { throw error }
                override fun fatalError(error: SAXParseException) { throw error }
            })
        }
        val document = builder.parse(InputSource(StringReader(ttml)))
        require(document.documentElement.localName == "tt")
        require(document.documentElement.namespaceURI.orEmpty() in setOf("", "http://www.w3.org/ns/ttml", "http://www.w3.org/2006/10/ttaf1"))
        val pending = java.util.ArrayDeque<Pair<Node, Int>>()
        pending.addLast(document.documentElement to 0)
        var nodes = 0
        while (pending.isNotEmpty()) {
            val (node, depth) = pending.removeLast()
            require(++nodes <= 50_000 && depth <= 64)
            val children = node.childNodes
            for (index in 0 until children.length) pending.addLast(children.item(index) to depth + 1)
        }
        val paragraphs = document.getElementsByTagNameNS("*", "p")
        require(paragraphs.length <= 10_000)

        val sung = ArrayList<Pair<LyricLine, String?>>(paragraphs.length)
        for (i in 0 until paragraphs.length) {
            val paragraph = paragraphs.item(i) as? Element ?: continue
            val line = lineFrom(paragraph) ?: continue
            sung += line to paragraph.qualified("ttm:agent").takeIf { it.isNotEmpty() }
        }
        sung.sortBy { it.first.timeMs }

        val sides = lineAlignments(sung.map { it.second }, agentTypes(document))
        sung.mapIndexed { index, (line, _) -> line.copy(alignment = sides[index]) }
            .withInstrumentalGaps()
    }.getOrDefault(emptyList())

    private fun DocumentBuilderFactory.harden(feature: String, value: Boolean = true) {
        runCatching { setFeature(feature, value) }
    }

    private fun agentTypes(document: org.w3c.dom.Document): Map<String, String> {
        val agents = document.getElementsByTagNameNS("*", "agent")
        val types = HashMap<String, String>(agents.length)
        for (i in 0 until agents.length) {
            val agent = agents.item(i) as? Element ?: continue
            val id = agent.qualified("xml:id")
            val type = agent.getAttribute("type")
            if (id.isNotEmpty() && type.isNotEmpty()) types[id] = type
        }
        return types
    }

    private fun Element.qualified(name: String): String {
        getAttribute(name).takeIf { it.isNotEmpty() }?.let { return it }
        val local = name.substringAfter(':')
        val found = attributes ?: return ""
        for (i in 0 until found.length) {
            val attribute = found.item(i) ?: continue
            if (attribute.nodeName == name ||
                attribute.nodeName == local ||
                attribute.localName == local
            ) {
                return attribute.nodeValue.orEmpty()
            }
        }
        return ""
    }

    private fun lineFrom(paragraph: Element): LyricLine? {
        if (paragraph.qualified("ttm:role") in SKIPPED_ROLES) return null
        val pieces = mutableListOf<Piece>()
        val backingPieces = mutableListOf<Piece>()
        collect(paragraph, pieces, backingPieces)
        val words = mergeIntoWords(pieces).takeIf(::ordered) ?: emptyList()
        val backing = mergeIntoWords(backingPieces).takeIf { it.isNotEmpty() && ordered(it) }?.let {
            LyricLine(
                timeMs = it.first().startMs,
                text = it.joinToString(" ") { word -> word.text },
                words = it,
            )
        }

        if (words.isEmpty()) {
            val text = pieces.joinToString("") { piece -> when (piece) {
                is Piece.Text -> piece.text
                is Piece.Timed -> piece.text
            } }.trim()
            val begin = time(paragraph.getAttribute("begin")) ?: return null
            if (text.isEmpty()) return null
            val end = time(paragraph.getAttribute("end"))?.takeIf { it > begin }
            return LyricLine(timeMs = begin, text = text, sungUntilMs = end)
        }

        val begin = time(paragraph.getAttribute("begin")) ?: words.first().startMs
        return LyricLine(
            timeMs = minOf(begin, words.first().startMs),
            text = words.joinToString(" ") { it.text },
            words = words,
            sungUntilMs = time(paragraph.getAttribute("end"))?.takeIf { it >= minOf(begin, words.first().startMs) },
            background = backing,
        )
    }

    private fun ordered(words: List<LyricWord>): Boolean =
        words.zipWithNext().all { (a, b) -> a.startMs <= b.startMs } && words.all { word ->
            word.endMs >= word.startMs && word.syllables.zipWithNext().all { (a, b) -> a.startMs <= b.startMs } &&
                word.syllables.all { it.startMs >= word.startMs && it.endMs in it.startMs..word.endMs }
        }

    private fun collect(node: Node, out: MutableList<Piece>, backing: MutableList<Piece>) {
        val children = node.childNodes
        for (i in 0 until children.length) {
            when (val child = children.item(i)) {
                is Element -> {
                    val role = child.qualified("ttm:role")
                    if (role in SKIPPED_ROLES) continue
                    val sink = if (role == BACKGROUND_ROLE) backing else out
                    val begin = time(child.getAttribute("begin"))
                    val end = time(child.getAttribute("end"))
                    if (begin != null && end != null && end >= begin && !hasTimedChild(child)) {
                        sink += Piece.Timed(child.textContent.orEmpty(), begin, end)
                    } else {
                        collect(child, sink, backing)
                    }
                }
                else -> if (child.nodeType == Node.TEXT_NODE) {
                    val text = child.textContent.orEmpty()
                    if (text.isNotEmpty()) out += Piece.Text(text)
                }
            }
        }
    }

    private fun hasTimedChild(element: Element): Boolean {
        val children = element.childNodes
        for (i in 0 until children.length) {
            val child = children.item(i) as? Element ?: continue
            if (child.getAttribute("begin").isNotEmpty() || hasTimedChild(child)) return true
        }
        return false
    }

    private fun mergeIntoWords(pieces: List<Piece>): List<LyricWord> {
        val words = mutableListOf<LyricWord>()
        val current = StringBuilder()
        val syllables = mutableListOf<LyricSyllable>()
        var start = 0L
        var end = 0L
        var timed = false

        fun flush() {
            val text = current.toString().trim()
            current.setLength(0)
            if (text.isNotEmpty() && timed) {
                words += LyricWord(start, end, text, LyricSyllable.normalized(syllables, text.length))
            }
            syllables.clear()
            timed = false
        }

        pieces.forEach { piece ->
            when (piece) {
                is Piece.Text -> when {
                    piece.text.isBlank() -> flush()
                    timed -> current.append(piece.text)
                    else -> Unit
                }
                is Piece.Timed -> {
                    if (piece.text.isBlank()) return@forEach
                    if (piece.text.first().isWhitespace()) flush()
                    if (current.isEmpty()) start = piece.start
                    val from = current.length
                    current.append(piece.text.trim())
                    syllables += LyricSyllable(piece.start, piece.end, from, current.length)
                    end = piece.end
                    timed = true
                    if (piece.text.last().isWhitespace()) flush()
                }
            }
        }
        flush()
        return words
    }

    fun time(value: String?): Long? {
        val raw = value?.trim()?.takeIf { it.isNotEmpty() } ?: return null
        val unit = Regex("""(\d+(?:\.\d+)?)(ms|h|m|s)?""").matchEntire(raw)
        val millis = if (unit != null) {
            val number = unit.groupValues[1].toDoubleOrNull() ?: return null
            number * when (unit.groupValues[2]) { "ms" -> 1.0; "m" -> 60_000.0; "h" -> 3_600_000.0; else -> 1000.0 }
        } else {
            val parts = raw.split(':')
            if (parts.size !in 2..3 || parts.dropLast(1).any { !it.matches(Regex("""\d+""")) } ||
                !parts.last().matches(Regex("""\d+(?:\.\d+)?"""))) return null
            val seconds = parts.last().toDoubleOrNull()?.takeIf { it < 60 } ?: return null
            val minutes = parts[parts.lastIndex - 1].toDoubleOrNull() ?: return null
            if (parts.size == 3 && minutes >= 60) return null
            val hours = if (parts.size == 3) parts[0].toDoubleOrNull() ?: return null else 0.0
            (hours * 3600 + minutes * 60 + seconds) * 1000
        }
        return millis.takeIf { it.isFinite() && it >= 0 && it < Long.MAX_VALUE.toDouble() - 86_400_000 }?.toLong()
    }

    private sealed interface Piece {
        data class Text(val text: String) : Piece
        data class Timed(val text: String, val start: Long, val end: Long) : Piece
    }
}
