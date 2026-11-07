package com.music.aurelis.data

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import com.music.aurelis.data.model.ProfileId
import kotlinx.coroutines.*
import java.io.File
import java.util.UUID

/** SAF images are copied, bounded and decoded off the main thread into private storage. */
object ProfileAvatars {
    suspend fun import(context: Context, id: ProfileId, source: Uri) {
        require(source.scheme == "content") { "Elige una imagen del dispositivo." }
        val directory = ProfileRepository.avatarDirectory(id)
        var output: File? = null
        var committed = false
        try {
            output = withContext(Dispatchers.IO) {
                val bytes = context.contentResolver.openInputStream(source)?.use { stream ->
                    val data = java.io.ByteArrayOutputStream()
                    val buffer = ByteArray(8192)
                    var total = 0
                    while (true) {
                        ensureActive()
                        val count = stream.read(buffer); if (count < 0) break
                        total += count; require(total <= 2 * 1024 * 1024) { "La imagen debe ocupar como máximo 2 MB." }
                        data.write(buffer, 0, count)
                    }
                    data.toByteArray()
                } ?: error("No se pudo abrir la imagen.")
                val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
                BitmapFactory.decodeByteArray(bytes, 0, bytes.size, bounds)
                require(bounds.outWidth > 0 && bounds.outHeight > 0) { "Imagen no válida." }
                val options = BitmapFactory.Options().apply {
                    inSampleSize = 1
                    while (bounds.outWidth / inSampleSize > 512 || bounds.outHeight / inSampleSize > 512) inSampleSize *= 2
                }
                val bitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.size, options) ?: error("Imagen no válida.")
                try {
                    check(directory.mkdirs() || directory.isDirectory)
                    File(directory, "${UUID.randomUUID()}.png").also { file ->
                        // Assign before the cancellable context handoff so cancellation can clean the file.
                        output = file
                        file.outputStream().use { check(bitmap.compress(Bitmap.CompressFormat.PNG, 100, it)) }
                    }
                } finally { bitmap.recycle() }
            }
            currentCoroutineContext().ensureActive()
            val old = ProfileRepository.state.value.profile.takeIf { it.id == id }?.avatar
            ProfileRepository.setAvatar(id, Uri.fromFile(output).toString())
            committed = true
            deleteOwned(directory, old)
        } finally {
            if (!committed) withContext(NonCancellable + Dispatchers.IO) { output?.delete() }
        }
    }
    fun remove(id: ProfileId) {
        val old = ProfileRepository.state.value.profile.takeIf { it.id == id }?.avatar
        ProfileRepository.setAvatar(id, null)
        deleteOwned(ProfileRepository.avatarDirectory(id), old)
    }
    private fun deleteOwned(directory: File, uri: String?) {
        runCatching {
            val parsed = uri?.let(Uri::parse) ?: return
            if (parsed.scheme == "file") {
                val file = File(parsed.path ?: return)
                if (file.canonicalFile.parentFile == directory.canonicalFile) file.delete()
            }
        }
    }
}
