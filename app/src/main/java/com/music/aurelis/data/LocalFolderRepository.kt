// SPDX-FileCopyrightText: 2026 Aurelis
// SPDX-License-Identifier: LGPL-3.0-or-later
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
        // Only revoke grants that this profile actually selected. An unrelated SAF
        // grant may belong to another feature, and a stale removal must be inert.
        val selected = SettingsRepository.localMusicFolders.value
        if (value !in selected) return
        SettingsRepository.setLocalFolderSelection(selected - value, SettingsRepository.useAllAudioFolders.value)
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
    /**
     * One directory level from the current MediaStore catalogue.
     * Paths and volume-aware keys remain stable; SAF scopes are unchanged.
     */
    fun browse(songs: List<LocalSong>, parentKey: String? = null): List<Folder> {
        if (parentKey == "unknown") return emptyList()
        val parent = parentKey?.let(::parseFolderKey)
        if (parentKey != null && parent == null) return emptyList()
        val candidates = linkedMapOf<String, MutableList<LocalSong>>()
        songs.forEach { song ->
            val path = normalizePath(song.relativePath)
            val volume = song.volumeName?.lowercase(Locale.ROOT)
            if (path == null || volume == null) {
                if (parentKey == null) candidates.getOrPut("unknown") { mutableListOf() }.add(song)
                return@forEach
            }
            if (parent != null && (volume != parent.first || !path.startsWith(parent.second))) return@forEach
            val suffix = if (parent == null) path else path.removePrefix(parent.second)
            if (suffix.isEmpty()) return@forEach
            val next = suffix.substringBefore('/') + "/"
            val childPath = parent?.second.orEmpty() + next
            val key = volume.length.toString() + ":" + volume + ":" + childPath
            candidates.getOrPut(key) { mutableListOf() }.add(song)
        }
        return candidates.map { (key, rows) ->
            Folder(key, folderLabel(key), rows)
        }.sortedWith(compareBy<Folder> { it.label.lowercase(Locale.ROOT) }.thenBy { it.key })
    }

    fun folderLabel(key: String): String = if (key == "unknown") "Carpeta sin información"
        else parseFolderKey(key)?.second?.trimEnd('/')?.substringAfterLast('/').orEmpty()

    /** Includes indexed songs from nested directories, not stale filesystem entries. */
    fun songsIn(songs: List<LocalSong>, folderKey: String): List<LocalSong> {
        if (folderKey == "unknown")
            return groups(songs).firstOrNull { it.key == "unknown" }?.songs.orEmpty()
        val (volume, path) = parseFolderKey(folderKey) ?: return emptyList()
        return songs.filter { song ->
            song.volumeName?.lowercase(Locale.ROOT) == volume &&
                normalizePath(song.relativePath)?.startsWith(path) == true
        }
    }

    /** Invalid path keys must never be interpreted as SAF access grants. */
    private fun parseFolderKey(key: String): Pair<String, String>? {
        val firstColon = key.indexOf(':')
        if (firstColon <= 0) return null
        val size = key.substring(0, firstColon).toIntOrNull()?.takeIf { it in 1..64 } ?: return null
        val start = firstColon + 1
        val end = start + size
        if (end >= key.length || key[end] != ':') return null
        val volume = key.substring(start, end)
        if (!volume.matches(Regex("[a-z0-9_-]{1,64}"))) return null
        val path = key.substring(end + 1)
        if (path.isEmpty() || normalizePath(path) != path) return null
        return volume to path
    }

}
