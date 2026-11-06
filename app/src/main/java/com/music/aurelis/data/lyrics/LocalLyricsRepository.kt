package com.music.aurelis.data.lyrics

import android.content.Context
import android.net.Uri
import android.util.AtomicFile
import com.music.aurelis.data.model.LocalSong
import kotlinx.coroutines.*
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.io.File
import java.io.InputStream
import java.nio.ByteBuffer
import java.nio.charset.CodingErrorAction
import java.security.MessageDigest

internal data class LocalLyricsResult(val lines: List<LyricLine>, val source: LyricsSource, val isSynced: Boolean, val cached: Boolean = false)

/** Local imports and embedded data precede cache and explicitly requested remote lookup. */
internal class LocalLyricsRepository(
    context: Context,
    private val remote: suspend (LocalSong) -> LyricsRepository.Result? = { song ->
        LyricsProviderRegistry.lookup(song)
    },
) {
    private val app = context.applicationContext
    private val imports = File(app.filesDir, "lyrics/v1")
    private val cache = File(app.cacheDir, "local_lyrics/v1")
    private fun key(song: LocalSong) = MessageDigest.getInstance("SHA-256").digest(
        listOf(song.contentUri, song.sizeBytes, song.durationMs, song.title, song.artist).joinToString("\u0000").toByteArray())
        .joinToString("") { "%02x".format(it) }
    private fun parsed(raw: String, source: LyricsSource) = Lyrics.parse(raw)?.let { LocalLyricsResult(it.lines, source, it.isSynced) }
    suspend fun find(song: LocalSong, allowRemote: Boolean = false): LocalLyricsResult? = withContext(Dispatchers.IO) {
        val imported = File(imports, "${key(song)}.lrc")
        if (imported.isFile) runCatching { imported.inputStream().use { parsed(decode(readLimited(it)), LyricsSource.LOCAL_FILE) } }
            .getOrNull()?.let { return@withContext it }
        currentCoroutineContext().ensureActive()
        runCatching { app.contentResolver.openInputStream(Uri.parse(song.contentUri))?.use {
            EmbeddedLyricsExtractor().extract(it)?.let { raw -> parsed(raw, LyricsSource.EMBEDDED) }
        } }.getOrNull()?.let { return@withContext it }
        currentCoroutineContext().ensureActive()
        readCache(song)?.let { return@withContext it }
        if (!allowRemote) return@withContext null
        val result = remote(song) ?: return@withContext null
        currentCoroutineContext().ensureActive()
        if (result.lines.isEmpty()) return@withContext null
        val found = LocalLyricsResult(result.lines, result.source, result.isSynced)
        // A cache write failure must not discard a successful provider response.
        try { writeMutex.withLock { currentCoroutineContext().ensureActive(); writeCache(song, found) } }
        catch (cancelled: CancellationException) { throw cancelled }
        catch (_: Exception) { /* Cache storage is optional; keep the provider result. */ }
        found
    }
    suspend fun import(song: LocalSong, uri: Uri): LocalLyricsResult = withContext(Dispatchers.IO) {
        val bytes = app.contentResolver.openInputStream(uri)?.use(::readLimited) ?: error("No se pudo abrir el archivo de letras.")
        val text = decode(bytes)
        val found = parsed(text, LyricsSource.LOCAL_FILE) ?: error("El archivo no contiene letras LRC, TTML o texto compatibles.")
        currentCoroutineContext().ensureActive()
        writeMutex.withLock {
            currentCoroutineContext().ensureActive()
            atomicWrite(File(imports, "${key(song)}.lrc"), text.toByteArray(Charsets.UTF_8))
        }
        found
    }
    private fun readCache(song: LocalSong): LocalLyricsResult? = runCatching {
        val file = File(cache, "${key(song)}.json")
        LyricsCacheCodec.decode(file.inputStream().use { decode(readLimited(it)) })
    }.getOrNull()
    private fun writeCache(song: LocalSong, result: LocalLyricsResult) {
        val bytes = LyricsCacheCodec.encode(result).toByteArray(Charsets.UTF_8)
        require(bytes.size <= MAX_BYTES)
        atomicWrite(File(cache, "${key(song)}.json"), bytes)
        var total = 0L
        cache.listFiles().orEmpty().filter { it.extension == "json" }.sortedByDescending { it.lastModified() }.forEachIndexed { index, file ->
            total += file.length()
            if (index >= 100 || total > 32L * 1024 * 1024) file.delete()
        }
    }
    private fun atomicWrite(file: File, bytes: ByteArray) {
        check(file.parentFile!!.isDirectory || file.parentFile!!.mkdirs())
        val atomic = AtomicFile(file); val output = atomic.startWrite()
        try { output.write(bytes); atomic.finishWrite(output) } catch (error: Exception) { atomic.failWrite(output); throw error }
    }
    private fun readLimited(input: InputStream): ByteArray = input.readNBytes(MAX_BYTES + 1).also {
        require(it.size <= MAX_BYTES) { "El archivo de letras supera 1 MiB." }
    }
    private fun decode(bytes: ByteArray): String {
        val charset = when {
            bytes.size >= 2 && bytes[0] == 0xFF.toByte() && bytes[1] == 0xFE.toByte() -> Charsets.UTF_16LE
            bytes.size >= 2 && bytes[0] == 0xFE.toByte() && bytes[1] == 0xFF.toByte() -> Charsets.UTF_16BE
            else -> Charsets.UTF_8
        }
        return charset.newDecoder().onMalformedInput(CodingErrorAction.REPORT).onUnmappableCharacter(CodingErrorAction.REPORT)
            .decode(ByteBuffer.wrap(bytes)).toString().removePrefix("\uFEFF")
    }
    companion object { private const val MAX_BYTES = 1_048_576; private val writeMutex = Mutex() }
}
