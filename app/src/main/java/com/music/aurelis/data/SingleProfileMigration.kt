package com.music.aurelis.data

import android.content.Context
import android.util.AtomicFile
import com.music.aurelis.data.model.ProfileId
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.io.IOException
import java.nio.file.Files
import java.nio.file.LinkOption
import java.security.MessageDigest
import java.util.zip.ZipEntry
import java.util.zip.ZipFile
import java.util.zip.ZipOutputStream

/** One-time, non-destructive consolidation. Neither legacy preferences nor avatars are deleted. */
internal class SingleProfileMigration(
    private val context: Context,
    private val beforeReceipt: () -> Unit = {},
) {
    internal val directory = File(context.noBackupFilesDir, "single-profile-migration")
    internal val receiptFile = File(directory, "selection-v2.json")
    private val registry get() = context.getSharedPreferences("aurelis_profiles_registry", Context.MODE_PRIVATE)

    fun resolve(): ProfileId {
        val receipt = AtomicFile(receiptFile)
        if (receiptFile.exists() || File(receiptFile.path + ".bak").exists()) {
            val root = receipt.openRead().use { JSONObject(String(it.readNBytes(MAX_BYTES + 1).also { b -> require(b.size <= MAX_BYTES) }, Charsets.UTF_8)) }
            require(root.getInt("version") == 2)
            val id = ProfileId(root.getString("profileId"))
            val name = root.getString("backup")
            require(name.matches(Regex("legacy-[a-f0-9]{64}\\.zip")))
            val archive = File(directory, name)
            check(digest(archive) == root.getString("archiveSha256")) { "Migration backup integrity check failed" }
            val manifest = readManifest(archive)
            check(sha256(manifest.toByteArray(Charsets.UTF_8)) == name.removePrefix("legacy-").removeSuffix(".zip"))
            check(JSONObject(manifest).getString("active") == id.value)
            return id
        }

        val (active, ids) = legacySelection()
        val storage = ProfileStorage(context)
        val areas = JSONObject()
        ids.forEach { id ->
            val scoped = JSONObject()
            ProfileDataArea.entries.forEach { area -> scoped.put(area.key, encodePreferences(storage.open(id, area).all)) }
            areas.put(id.value, scoped)
        }
        val files = linkedMapOf<String, File>()
        val avatars = JSONArray()
        var total = 0L
        ids.forEach { id ->
            val folder = File(context.filesDir, "profiles/${id.value}")
            if (folder.exists()) Files.walk(folder.toPath()).use { paths ->
                paths.filter { Files.isRegularFile(it, LinkOption.NOFOLLOW_LINKS) }.sorted().forEach { path ->
                    val file = path.toFile()
                    require(file.canonicalFile.toPath().startsWith(folder.canonicalFile.toPath()))
                    val entry = "avatars/${id.value}/${folder.toPath().relativize(path).toString().replace(File.separatorChar, '/')}"
                    total += file.length(); require(total <= MAX_BYTES)
                    files[entry] = file
                    avatars.put(JSONObject().put("entry", entry).put("bytes", file.length()).put("sha256", digest(file)))
                }
            }
        }
        val manifest = JSONObject().put("version", 1).put("active", active.value)
            .put("ids", JSONArray(ids.map { it.value })).put("registry", encodePreferences(registry.all))
            .put("areas", areas).put("avatars", avatars).toString().toByteArray(Charsets.UTF_8)
        require(total + manifest.size <= MAX_BYTES) { "Migration backup exceeds its safe size limit" }
        check(directory.mkdirs() || directory.isDirectory)
        val archive = File(directory, "legacy-${sha256(manifest)}.zip")
        if (!archive.exists()) writeAtomic(archive) { stream ->
            val zip = ZipOutputStream(object : java.io.FilterOutputStream(stream) {
                override fun close() { flush() }
            })
            zip.use {
                zip.putNextEntry(ZipEntry("manifest.json")); zip.write(manifest); zip.closeEntry()
                files.forEach { (entry, file) ->
                    zip.putNextEntry(ZipEntry(entry)); file.inputStream().use { it.copyTo(zip) }; zip.closeEntry()
                }
            }
        }
        verifyBackup(archive, manifest)
        beforeReceipt() // Fault injection: a crash here must leave the original stores and backup intact.
        val selected = JSONObject().put("version", 2).put("profileId", active.value)
            .put("backup", archive.name).put("archiveSha256", digest(archive)).toString().toByteArray(Charsets.UTF_8)
        writeAtomic(receiptFile) { it.write(selected) }
        return active
    }

    /** Used only for read-only presentation after a failure; never authorizes writes or migration. */
    fun readableId(): ProfileId = runCatching {
        val root = AtomicFile(receiptFile).openRead().use { JSONObject(String(it.readNBytes(MAX_BYTES + 1), Charsets.UTF_8)) }
        require(root.getInt("version") == 2)
        ProfileId(root.getString("profileId"))
    }.getOrElse { runCatching { legacySelection().first }.getOrDefault(ProfileId.DEFAULT) }

    private fun legacySelection(): Pair<ProfileId, List<ProfileId>> {
        val raw = registry.getString("profiles_v1", null) ?: return ProfileId.DEFAULT to listOf(ProfileId.DEFAULT)
        val root = JSONObject(raw); require(root.getInt("version") == 1)
        fun ids(array: JSONArray) = (0 until array.length()).map { ProfileId(array.getString(it)) }
        val profiles = ids(root.getJSONArray("ids"))
        require(profiles.isNotEmpty() && profiles.distinct().size == profiles.size)
        val active = ProfileId(root.getString("active")); require(active in profiles)
        val pending = root.optJSONArray("deleting")?.let(::ids).orEmpty()
        require(pending.none { it in profiles })
        // Preserve unfinished old deletions as archived data; do not resume destructive cleanup.
        return active to (profiles + pending + ProfileId.DEFAULT).distinct()
    }

    internal fun readManifest(archive: File): String = ZipFile(archive).use { zip ->
        val entry = zip.getEntry("manifest.json") ?: throw IOException("Missing migration manifest")
        zip.getInputStream(entry).use { input ->
            String(input.readNBytes(MAX_BYTES + 1).also { require(it.size <= MAX_BYTES) }, Charsets.UTF_8)
        }
    }

    private fun verifyBackup(archive: File, expected: ByteArray) {
        ZipFile(archive).use { zip ->
            val manifest = readManifest(archive)
            check(manifest.toByteArray(Charsets.UTF_8).contentEquals(expected))
            val root = JSONObject(manifest); require(root.getInt("version") == 1)
            decodePreferences(root.getJSONObject("registry"))
            val areas = root.getJSONObject("areas")
            areas.keys().forEach { id ->
                ProfileId(id)
                ProfileDataArea.entries.forEach { decodePreferences(areas.getJSONObject(id).getJSONObject(it.key)) }
            }
            val avatars = root.getJSONArray("avatars")
            val entries = mutableSetOf("manifest.json")
            var total = expected.size.toLong()
            for (i in 0 until avatars.length()) {
                val item = avatars.getJSONObject(i); val name = item.getString("entry")
                require(entries.add(name) && name.startsWith("avatars/") && name.split('/').none { it == ".." })
                val entry = zip.getEntry(name) ?: error("Missing avatar in migration backup")
                val bytes = zip.getInputStream(entry).use { it.readNBytes(MAX_BYTES + 1) }
                total += bytes.size; require(total <= MAX_BYTES && bytes.size.toLong() == item.getLong("bytes"))
                check(sha256(bytes) == item.getString("sha256"))
            }
            check(zip.entries().asSequence().map { it.name }.toList().let { it.size == entries.size && it.toSet() == entries })
        }
    }

    private fun writeAtomic(file: File, write: (java.io.FileOutputStream) -> Unit) {
        val atomic = AtomicFile(file); val stream = atomic.startWrite()
        try { write(stream); stream.fd.sync(); atomic.finishWrite(stream); check(file.isFile && file.length() > 0) }
        catch (failure: Throwable) { atomic.failWrite(stream); throw failure }
    }

    companion object {
        private const val MAX_BYTES = 64 * 1024 * 1024
        private fun sha256(bytes: ByteArray) = MessageDigest.getInstance("SHA-256").digest(bytes).joinToString("") { "%02x".format(it) }
        private fun digest(file: File): String {
            require(file.length() <= MAX_BYTES + 1024 * 1024L)
            val hash = MessageDigest.getInstance("SHA-256")
            file.inputStream().use { input -> val buffer = ByteArray(8192); while (true) {
                val count = input.read(buffer); if (count < 0) break; hash.update(buffer, 0, count)
            } }
            return hash.digest().joinToString("") { "%02x".format(it) }
        }
        internal fun encodePreferences(values: Map<String, *>): JSONObject = JSONObject().also { result ->
            values.toSortedMap().forEach { (key, value) ->
                val (type, encoded) = when (value) {
                    is String -> "string" to value
                    is Int -> "int" to value
                    is Long -> "long" to value
                    is Float -> "floatBits" to value.toRawBits()
                    is Boolean -> "boolean" to value
                    is Set<*> -> { require(value.all { it is String }); "stringSet" to JSONArray(value.map { it as String }.sorted()) }
                    else -> error("Unsupported preference type")
                }
                result.put(key, JSONObject().put("type", type).put("value", encoded))
            }
        }
        /** Typed recovery reader for B11 import tooling; unknown keys/types are never silently dropped. */
        internal fun decodePreferences(values: JSONObject): Map<String, Any> = values.keys().asSequence().associateWith { key ->
            val item = values.getJSONObject(key)
            when (item.getString("type")) {
                "string" -> item.getString("value")
                "int" -> item.getInt("value")
                "long" -> item.getLong("value")
                "floatBits" -> Float.fromBits(item.getInt("value"))
                "boolean" -> item.getBoolean("value")
                "stringSet" -> item.getJSONArray("value").let { a -> (0 until a.length()).map { a.getString(it) }.toSet() }
                else -> error("Unknown preference type")
            }
        }
    }
}
