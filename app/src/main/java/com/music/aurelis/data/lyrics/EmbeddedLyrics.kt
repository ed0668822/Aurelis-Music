// Aurelis © 2026. GPL-3.0-or-later
package com.music.aurelis.data.lyrics

import java.io.DataInputStream
import java.io.File
import java.io.InputStream
import java.nio.charset.Charset

/** Bounded ID3v2.3/v2.4 USLT and FLAC Vorbis LYRICS/UNSYNCEDLYRICS reader. */
class EmbeddedLyricsExtractor {
    fun extractFromAudioFile(file: File): String? = file.inputStream().use(::extract)
    fun extract(input: InputStream): String? = try { read(input) }
        catch (_: java.io.IOException) { null } catch (_: IllegalArgumentException) { null }
    private fun read(input: InputStream): String? {
        val stream = DataInputStream(input)
        val prefix = ByteArray(4); stream.readFully(prefix)
        if (prefix.copyOf(3).contentEquals("ID3".toByteArray())) {
            val major = prefix[3].toInt(); if (major !in 3..4) return null
            stream.readUnsignedByte(); val flags = stream.readUnsignedByte()
            val sizeBytes = ByteArray(4); stream.readFully(sizeBytes)
            val size = synchsafe(sizeBytes) ?: return null
            if (size !in 1..MAX || flags and 0xC0 != 0) return null // Unsync/extended headers need separate decoding.
            val tag = ByteArray(size); stream.readFully(tag)
            var index = 0
            while (index + 10 <= tag.size) {
                val id = String(tag, index, 4, Charsets.ISO_8859_1)
                val frameSize = if (major == 4) synchsafe(tag.copyOfRange(index + 4, index + 8)) ?: return null
                    else number(tag, index + 4, false)
                if (frameSize <= 0 || frameSize > tag.size - index - 10) return null
                val frameFlags = tag[index + 9].toInt() and 255
                if (id == "USLT" && frameFlags == 0) return uslt(tag.copyOfRange(index + 10, index + 10 + frameSize))
                index += 10 + frameSize
            }
        } else if (prefix.contentEquals("fLaC".toByteArray())) {
            var consumed = 4
            while (consumed < MAX) {
                val header = stream.readUnsignedByte(); val size = (stream.readUnsignedByte() shl 16) or
                    (stream.readUnsignedByte() shl 8) or stream.readUnsignedByte()
                consumed += size + 4
                if (consumed > MAX) return null
                if (header and 127 == 4) {
                    val block = ByteArray(size); stream.readFully(block)
                    var index = 0
                    fun next(): String {
                        require(index + 4 <= block.size)
                        val count = number(block, index, true); index += 4
                        require(count >= 0 && count <= block.size - index)
                        return String(block, index, count, Charsets.UTF_8).also { index += count }
                    }
                    next() // vendor
                    require(index + 4 <= block.size)
                    val count = number(block, index, true); index += 4
                    require(count in 0..10_000)
                    repeat(count) {
                        val item = next(); val key = item.substringBefore('=').uppercase(java.util.Locale.ROOT)
                        if (key == "LYRICS" || key == "UNSYNCEDLYRICS") return item.substringAfter('=', "").takeIf { it.isNotBlank() }
                    }
                } else { var remaining = size; while (remaining > 0) {
                    val skipped = stream.skipBytes(remaining); if (skipped == 0) return null; remaining -= skipped
                } }
                if (header and 128 != 0) return null
            }
        }
        return null
    }
    private fun uslt(bytes: ByteArray): String? {
        if (bytes.size < 5) return null
        val encoding = bytes[0].toInt()
        val charset: Charset = when (encoding) { 0 -> Charsets.ISO_8859_1; 1 -> Charsets.UTF_16; 2 -> Charsets.UTF_16BE; 3 -> Charsets.UTF_8; else -> return null }
        val width = if (encoding in 1..2) 2 else 1
        var index = 4 // encoding + language
        while (index + width <= bytes.size) {
            if (bytes[index] == 0.toByte() && (width == 1 || bytes[index + 1] == 0.toByte())) {
                index += width
                val hasBom = index + 1 < bytes.size &&
                    ((bytes[index] == 0xFF.toByte() && bytes[index + 1] == 0xFE.toByte()) ||
                    (bytes[index] == 0xFE.toByte() && bytes[index + 1] == 0xFF.toByte()))
                val actual = if (encoding == 1 && !hasBom && bytes.size > 5 && bytes[4] == 0xFF.toByte() && bytes[5] == 0xFE.toByte()) Charsets.UTF_16LE else charset
                return String(bytes, index, bytes.size - index, actual).removePrefix("\uFEFF").trim().takeIf { it.isNotBlank() }
            }
            index += width
        }
        return null
    }
    private fun synchsafe(bytes: ByteArray): Int? {
        if (bytes.any { it.toInt() and 128 != 0 }) return null
        return bytes.fold(0) { result, byte -> (result shl 7) or byte.toInt() }
    }
    private fun number(bytes: ByteArray, start: Int, little: Boolean): Int =
        (0..3).fold(0) { result, offset -> (result shl 8) or (bytes[start + if (little) 3 - offset else offset].toInt() and 255) }
    companion object { private const val MAX = 2 * 1024 * 1024 }
}
