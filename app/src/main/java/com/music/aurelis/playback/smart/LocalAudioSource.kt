// SPDX-FileCopyrightText: © 2026 Aurelis

package com.music.aurelis.playback.smart

import android.content.ContentResolver
import android.content.Context
import android.media.MediaDataSource
import android.net.Uri
import android.os.ParcelFileDescriptor
import android.system.Os
import android.system.OsConstants
import android.util.Log
import java.io.Closeable
import java.io.File
import java.io.IOException

/** Random-access audio for analysis of the recording already selected for playback. */
internal object LocalAudioSource {
    private const val TAG = "AurelisLocalAudio"
    private const val MAX_TEMP_BYTES = 256L * 1024 * 1024

    fun isLocal(uri: Uri): Boolean =
        uri.scheme.equals(ContentResolver.SCHEME_FILE, ignoreCase = true) ||
            uri.scheme.equals(ContentResolver.SCHEME_CONTENT, ignoreCase = true)

    /** Includes the file identity and byte range so replacement at the same URI is detected. */
    fun revision(resolver: ContentResolver, uri: Uri): String? = runCatching {
        resolver.openAssetFileDescriptor(uri, "r")?.use { asset ->
            val stat = Os.fstat(asset.fileDescriptor)
            if (!OsConstants.S_ISREG(stat.st_mode)) return@use null
            listOf(stat.st_dev, stat.st_ino, stat.st_size, stat.st_mtime, stat.st_ctime,
                asset.startOffset, asset.length).joinToString(":")
        }
    }.getOrNull()

    /** Opens a seekable file for callers that only need a single reader. */
    fun open(resolver: ContentResolver, uri: Uri): MediaDataSource? {
        val asset = runCatching { resolver.openAssetFileDescriptor(uri, "r") }.getOrNull() ?: return null
        val length = asset.length
        val seekable = length > 0 && runCatching {
            Os.pread(asset.fileDescriptor, ByteArray(1), 0, 1, asset.startOffset) == 1
        }.getOrDefault(false)
        if (seekable) return Source(asset, asset.fileDescriptor, asset.startOffset, length)
        runCatching { asset.close() }
        return null
    }

    /**
     * Owns a fallback copy for one analysis operation, reused across the
     * duration probe, structure scan and model windows. Regular files remain zero-copy.
     */
    class Recording(
        private val context: Context,
        private val uri: Uri,
        private val abort: () -> Boolean,
    ) : Closeable {
        private var temporary: File? = null
        private var triedCopy = false

        fun open(): MediaDataSource? {
            if (abort()) return null
            temporary?.let { return openTemporary(it) }
            if (triedCopy) return null

            val resolver = context.contentResolver
            LocalAudioSource.open(resolver, uri)?.let { return it }

            triedCopy = true
            if (uri.scheme != ContentResolver.SCHEME_CONTENT) return null
            return runCatching {
                resolver.openInputStream(uri)?.use { input ->
                    val file = File.createTempFile("automix-", ".audio", context.cacheDir)
                    try {
                        file.outputStream().buffered().use { output ->
                            val buffer = ByteArray(64 * 1024)
                            var copied = 0L
                            while (!abort()) {
                                val count = input.read(buffer)
                                if (count < 0) break
                                copied += count
                                if (copied > MAX_TEMP_BYTES) throw IOException("Audio exceeds the analysis copy limit")
                                output.write(buffer, 0, count)
                            }
                            if (abort() || copied == 0L) throw IOException("Audio copy incomplete")
                        }
                        temporary = file
                        openTemporary(file)
                    } finally {
                        if (temporary != file) file.delete()
                    }
                }
            }.onFailure { Log.w(TAG, "Could not stage local audio for analysis", it) }.getOrNull()
        }

        private fun openTemporary(file: File): MediaDataSource? = runCatching {
            val descriptor = ParcelFileDescriptor.open(file, ParcelFileDescriptor.MODE_READ_ONLY)
            Source(descriptor, descriptor.fileDescriptor, 0L, file.length())
        }.getOrNull()

        override fun close() {
            temporary?.delete()
        }
    }

    private class Source(
        private val owner: Closeable,
        private val descriptor: java.io.FileDescriptor,
        private val startOffset: Long,
        private val length: Long,
    ) : MediaDataSource() {
        override fun getSize(): Long = length

        override fun readAt(position: Long, buffer: ByteArray, offset: Int, size: Int): Int {
            if (size == 0) return 0
            if (position < 0 || position >= length) return -1
            val wanted = minOf(size.toLong(), length - position).toInt()
            return try {
                Os.pread(descriptor, buffer, offset, wanted, startOffset + position).takeIf { it > 0 } ?: -1
            } catch (error: android.system.ErrnoException) {
                throw IOException("Cannot read local audio at $position", error)
            }
        }

        override fun close() {
            runCatching { owner.close() }
        }
    }
}
