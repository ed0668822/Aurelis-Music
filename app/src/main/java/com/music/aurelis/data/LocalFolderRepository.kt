package com.music.aurelis.data

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.DocumentsContract
import com.music.aurelis.data.model.LocalSong
import java.io.IOException
import java.util.Locale

/** SAF selects a local scope; MediaStore remains the single indexed audio catalogue. */
object LocalFolderRepository {
    data class Scope(val uri: Uri, val volume: String, val path: String) {
        val label get() = path.trimEnd('/').ifEmpty { volume }
        fun contains(volumeName: String?, relativePath: String?): Boolean {
            val candidate = normalizePath(relativePath) ?: return false
            return volume == volumeName?.lowercase(Locale.ROOT) && candidate.startsWith(path)
        }
    }
    data class Folder(val key: String, val label: String, val songs: List<LocalSong>)

    fun scope(uri: Uri): Scope {
        require(uri.scheme == "content" && uri.authority == "com.android.externalstorage.documents" && DocumentsContract.isTreeUri(uri)) {
            "Elige una carpeta local del dispositivo o de una tarjeta SD."
        }
        val id = DocumentsContract.getTreeDocumentId(uri)
        require(':' in id)
        val rawVolume = id.substringBefore(':').lowercase(Locale.ROOT)
        require(rawVolume.matches(Regex("[a-z0-9_-]{1,64}")))
        val relative = normalizePath(id.substringAfter(':')) ?: error("Ruta de carpeta no válida.")
        return Scope(uri, if (rawVolume == "primary" || rawVolume == "home") "external_primary" else rawVolume,
            if (rawVolume == "home") "Documents/$relative" else relative)
    }
    internal fun normalizePath(path: String?): String? {
        if (path == null || path.startsWith('/')) return null
        val parts = path.split('/').filter { it.isNotEmpty() }
        if (parts.any { it == "." || it == ".." }) return null
        return if (parts.isEmpty()) "" else parts.joinToString("/") + "/"
    }
    fun label(value: String) = runCatching { scope(Uri.parse(value)).label }.getOrDefault("Carpeta no disponible")

    fun add(context: Context, uri: Uri) {
        scope(uri)
        val resolver = context.contentResolver
        val priorGrant = resolver.persistedUriPermissions.any { it.uri == uri && it.isReadPermission }
        try {
            resolver.takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION)
            checkAccess(context, uri)
            SettingsRepository.setLocalFolderSelection(SettingsRepository.localMusicFolders.value + uri.toString(), false)
        } catch (failure: Exception) {
            if (!priorGrant) runCatching { resolver.releasePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION) }
            throw failure
        }
    }
    fun remove(context: Context, value: String) {
        SettingsRepository.setLocalFolderSelection(SettingsRepository.localMusicFolders.value - value, SettingsRepository.useAllAudioFolders.value)
        runCatching { context.contentResolver.releasePersistableUriPermission(Uri.parse(value), Intent.FLAG_GRANT_READ_URI_PERMISSION) }
    }
    private fun checkAccess(context: Context, uri: Uri) {
        val resolver = context.contentResolver
        if (resolver.persistedUriPermissions.none { it.uri == uri && it.isReadPermission })
            throw IOException("Vuelve a seleccionar la carpeta en Ajustes para conceder acceso.")
        try {
            val document = DocumentsContract.buildDocumentUriUsingTree(uri, DocumentsContract.getTreeDocumentId(uri))
            val cursor = resolver.query(document, arrayOf(DocumentsContract.Document.COLUMN_MIME_TYPE), null, null, null)
                ?: throw IOException("La carpeta no está disponible. Comprueba el almacenamiento en Ajustes.")
            cursor.use { require(it.moveToFirst() && it.getString(0) == DocumentsContract.Document.MIME_TYPE_DIR) { "La carpeta seleccionada ya no está disponible." } }
        } catch (_: SecurityException) {
            throw IOException("Se revocó el acceso a la carpeta. Selecciónala de nuevo en Ajustes.")
        }
    }
    suspend fun songs(context: Context, folders: Set<String>, filterNonMusic: Boolean): List<LocalSong> {
        val scopes = folders.map { scope(Uri.parse(it)) }
        scopes.forEach { checkAccess(context, it.uri) }
        if (scopes.isEmpty()) return emptyList()
        return LocalMediaRepository.getAllSongs(context, filterNonMusic, LocalMediaRepository.metadataReader(context), requireFolderMetadata = true) { volume, path ->
            scopes.any { it.contains(volume, path) }
        }
    }
    fun groups(songs: List<LocalSong>): List<Folder> = songs.groupBy { song ->
        val path = normalizePath(song.relativePath)
        if (song.volumeName == null || path == null) "unknown" else "${song.volumeName.length}:${song.volumeName}:$path"
    }.map { (key, rows) ->
        Folder(key, if (key == "unknown") "Carpeta sin información" else normalizePath(rows.first().relativePath)!!.trimEnd('/').ifEmpty { rows.first().volumeName!! }, rows)
    }.sortedBy { it.label.lowercase(Locale.ROOT) }
}
