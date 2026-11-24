// SPDX-FileCopyrightText: © 2026 Aurelis

package com.music.aurelis.data.settings

import android.content.Context
import android.graphics.BitmapFactory
import android.net.Uri
import kotlinx.coroutines.flow.MutableStateFlow
import java.io.File

/**
 * Single, local-only Aurelis identity. Never sends a photo or a name to Google,
 * Spotify, Listen Together, or any online account.
 */
object LocalProfile {
    private const val PREFS = "aurelis_local_profile"
    private const val NAME = "display_name"
    private const val PHOTO = "photo_uri"
    private const val MAX_PHOTO_BYTES = 8L * 1024 * 1024
    private lateinit var application: Context

    val displayName = MutableStateFlow("")
    val photoUri = MutableStateFlow<String?>(null)

    fun init(context: Context) {
        application = context.applicationContext
        reload()
    }

    fun reload() {
        if (!::application.isInitialized) return
        val preferences = application.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        displayName.value = preferences.getString(NAME, "").orEmpty()
        // Older builds saved an absolute file:// URI; newer builds persist only
        // the basename so Android's device-to-device restore can relocate the
        // private app directory without losing the photo.
        val dir = File(application.filesDir, "aurelis_profile").canonicalFile
        val saved = preferences.getString(PHOTO, null)
        val resolved = saved?.let { value ->
            val candidate = if (value.startsWith("file:")) {
                Uri.parse(value).path?.let(::File)
            } else {
                File(dir, value)
            }
            candidate?.takeIf { image ->
                image.canonicalFile.parentFile == dir && image.isFile
            }
        }
        photoUri.value = resolved?.let { Uri.fromFile(it).toString() }
        if (resolved != null && saved != resolved.name) {
            preferences.edit().putString(PHOTO, resolved.name).apply()
        }
    }

    fun setName(value: String) {
        if (!::application.isInitialized) return
        val safe = value.trim().take(40)
        displayName.value = safe
        application.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .edit().putString(NAME, safe).apply()
    }

    /** Copies a photo selected with the system picker into private app storage. */
    fun setPhoto(context: Context, source: Uri): Boolean {
        if (!::application.isInitialized) init(context)
        val dir = File(application.filesDir, "aurelis_profile").apply { mkdirs() }
        val file = File.createTempFile("avatar_", ".img", dir)
        val saved = runCatching {
            val input = context.contentResolver.openInputStream(source)
                ?: error("The selected photo is not readable")
            input.use { from ->
                file.outputStream().buffered().use { to ->
                    val chunk = ByteArray(8192)
                    var copied = 0L
                    while (true) {
                        val n = from.read(chunk)
                        if (n < 0) break
                        copied += n
                        require(copied <= MAX_PHOTO_BYTES) { "Image is too large" }
                        to.write(chunk, 0, n)
                    }
                }
            }
            val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            BitmapFactory.decodeFile(file.absolutePath, bounds)
            require(bounds.outWidth > 0 && bounds.outHeight > 0) { "Invalid image" }
            val previous = photoUri.value
            val newUri = Uri.fromFile(file).toString()
            application.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
                .edit().putString(PHOTO, file.name).apply()
            photoUri.value = newUri
            previous?.let { old ->
                Uri.parse(old).path?.let { oldPath ->
                    File(oldPath).takeIf { it.parentFile == dir }?.delete()
                }
            }
        }
        if (saved.isFailure) file.delete()
        return saved.isSuccess
    }

    /** Returns only the non-sensitive local display name for portable JSON backups. */
    fun exportDisplayName(): String = displayName.value

    /** Updates the existing profile during import without touching its image. */
    fun importDisplayName(name: String) = setName(name)

    fun clearPhoto() {
        if (!::application.isInitialized) return
        val previous = photoUri.value
        application.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .edit().remove(PHOTO).apply()
        photoUri.value = null
        previous?.let { old ->
            Uri.parse(old).path?.let { path ->
                val f = File(path)
                if (f.parentFile == File(application.filesDir, "aurelis_profile")) f.delete()
            }
        }
    }
}
