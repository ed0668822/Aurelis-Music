// SPDX-FileCopyrightText: 2026 Aurelis
// SPDX-License-Identifier: LGPL-3.0-or-later
package com.music.aurelis.data.lyrics

import java.io.InputStream
import java.nio.ByteBuffer
import java.nio.charset.CodingErrorAction

/**
 * Reads MP4/M4A and Matroska/WebM embedded lyrics without touching the audio decoder.
 * Only the first 8 MiB is inspected; large trailing metadata can remain unavailable.
 * Parsing is constrained to metadata boxes or EBML SimpleTags, not arbitrary audio bytes.
 */
internal object EmbeddedContainerLyrics {
    private const val MAX_PREFIX = 8 * 1024 * 1024
    private const val MAX_TEXT = 1_048_576
    private const val WORD_FIELD = "BITCHORD_LYRICS" // Existing audio-file metadata compatibility.
    private val matroskaMagic = byteArrayOf(0x1a, 0x45, 0xdf.toByte(), 0xa3.toByte())

    fun extract(prefix: ByteArray, input: InputStream): String? {
        val sniff = prefix + input.readNBytes(8)
        val isMp4 = sniff.size >= 12 && String(sniff, 4, 4, Charsets.ISO_8859_1) == "ftyp"
        val isMatroska = sniff.size >= 4 && matroskaMagic.indices.all { sniff[it] == matroskaMagic[it] }
        if (!isMp4 && !isMatroska) return null
        val bytes = sniff + input.readNBytes((MAX_PREFIX - sniff.size).coerceAtLeast(0))
        return if (isMp4) mp4(bytes) else matroska(bytes)
    }

    private data class Box(val type: String, val payload: Int, val end: Int)

    private fun box(bytes: ByteArray, from: Int, limit: Int): Box? {
        if (from < 0 || limit > bytes.size || from > limit - 8) return null
        val declared = u32(bytes, from)
        val header = if (declared == 1L) 16 else 8
        if (from > limit - header) return null
        val length = when (declared) {
            0L -> (limit - from).toLong()
            1L -> u64(bytes, from + 8)
            else -> declared
        }
        if (length < header || length > (limit - from).toLong()) return null
        return Box(String(bytes, from + 4, 4, Charsets.ISO_8859_1), from + header, from + length.toInt())
    }

    private inline fun walk(bytes: ByteArray, from: Int, limit: Int, visit: (Box) -> Unit) {
        var offset = from
        while (offset <= limit - 8) {
            val next = box(bytes, offset, limit) ?: break
            visit(next)
            if (next.end <= offset) break
            offset = next.end
        }
    }

    private fun mp4(bytes: ByteArray): String? {
        var answer: String? = null
        walk(bytes, 0, bytes.size) { top ->
            if (top.type == "moov" && answer == null) answer = metadata(bytes, top, 0)
        }
        return answer
    }

    private fun metadata(bytes: ByteArray, parent: Box, depth: Int): String? {
        if (depth > 5) return null
        var result: String? = null
        val from = parent.payload + if (parent.type == "meta") 4 else 0
        if (from > parent.end) return null
        walk(bytes, from, parent.end) { child ->
            if (result == null) result = when (child.type) {
                "udta", "meta" -> metadata(bytes, child, depth + 1)
                "ilst" -> ilst(bytes, child)
                else -> null
            }
        }
        return result
    }

    private fun ilst(bytes: ByteArray, parent: Box): String? {
        var ordinary: String? = null
        var enhanced: String? = null
        walk(bytes, parent.payload, parent.end) { item ->
            if (item.type != "©lyr" && item.type != "----") return@walk
            var name: String? = null
            var value: String? = null
            walk(bytes, item.payload, item.end) { child ->
                when (child.type) {
                    "name" -> if (child.payload + 4 <= child.end) {
                        name = utf8(bytes, child.payload + 4, child.end)
                    }
                    "data" -> if (child.payload + 8 <= child.end &&
                        u32(bytes, child.payload) == 1L) {
                        value = utf8(bytes, child.payload + 8, child.end)
                    }
                }
            }
            val text = value?.takeIf { it.isNotBlank() } ?: return@walk
            if (item.type == "©lyr" || name == "LYRICS") ordinary = ordinary ?: text
            if (name == WORD_FIELD || name == "UNSYNCEDLYRICS") enhanced = enhanced ?: text
        }
        return enhanced ?: ordinary
    }

    private data class Vint(val value: Long, val width: Int)

    private fun vint(bytes: ByteArray, at: Int, limit: Int): Vint? {
        if (at >= limit) return null
        val first = bytes[at].toInt() and 255
        if (first == 0) return null
        var width = 1
        var mask = 0x80
        while (first and mask == 0) { mask = mask shr 1; width++ }
        if (width > 8 || at + width > limit) return null
        var value = (first and (mask - 1)).toLong()
        for (i in 1 until width) value = (value shl 8) or (bytes[at + i].toLong() and 255)
        return Vint(value, width)
    }

    private fun matroska(bytes: ByteArray): String? {
        var normal: String? = null
        var enhanced: String? = null
        var at = 4
        while (at < bytes.size - 3) {
            if (bytes[at] != 0x67.toByte() || bytes[at + 1] != 0xc8.toByte()) { at++; continue }
            val length = vint(bytes, at + 2, bytes.size)
            if (length == null || length.value > MAX_PREFIX || length.value < 1) { at++; continue }
            val begin = at + 2 + length.width
            val end = begin.toLong() + length.value
            if (end > bytes.size) { at++; continue }
            val pair = simpleTag(bytes, begin, end.toInt())
            if (pair != null) {
                if (pair.first == WORD_FIELD || pair.first == "UNSYNCEDLYRICS") enhanced = enhanced ?: pair.second
                if (pair.first == "LYRICS") normal = normal ?: pair.second
            }
            at = end.toInt()
        }
        return enhanced ?: normal
    }

    private fun simpleTag(bytes: ByteArray, from: Int, end: Int): Pair<String, String>? {
        var name: String? = null
        var value: String? = null
        var pos = from
        while (pos < end - 2) {
            val id = (bytes[pos].toInt() and 255) shl 8 or (bytes[pos + 1].toInt() and 255)
            val length = vint(bytes, pos + 2, end) ?: break
            val start = pos + 2 + length.width
            val last = start.toLong() + length.value
            if (last > end || last < start) break
            when (id) {
                0x45a3 -> name = utf8(bytes, start, last.toInt())
                0x4487 -> value = utf8(bytes, start, last.toInt())
            }
            pos = last.toInt()
        }
        val key = name ?: return null
        val text = value?.takeIf { it.isNotBlank() } ?: return null
        return key to text
    }

    private fun utf8(bytes: ByteArray, from: Int, until: Int): String? {
        if (until < from || until - from !in 1..MAX_TEXT) return null
        return runCatching {
            Charsets.UTF_8.newDecoder().onMalformedInput(CodingErrorAction.REPORT)
                .onUnmappableCharacter(CodingErrorAction.REPORT)
                .decode(ByteBuffer.wrap(bytes, from, until - from)).toString()
        }.getOrNull()
    }

    private fun u32(bytes: ByteArray, index: Int): Long =
        ((bytes[index].toLong() and 255) shl 24) or
        ((bytes[index + 1].toLong() and 255) shl 16) or
        ((bytes[index + 2].toLong() and 255) shl 8) or
        (bytes[index + 3].toLong() and 255)

    private fun u64(bytes: ByteArray, index: Int): Long {
        var value = 0L
        repeat(8) { value = (value shl 8) or (bytes[index + it].toLong() and 255) }
        return value
    }
}
