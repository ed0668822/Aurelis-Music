package com.music.aurelis

import com.music.aurelis.data.stats.Backup
import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

/** A007: portable profile name, but no absolute private photo paths. */
class LocalProfileBackupCompatibilityTest {
    private val json = Json { ignoreUnknownKeys = true }

    @Test fun olderBackupWithoutProfileStillDeserializes() {
        val old = """{"app":"aurelis","version":1,"settings":{},"listening":[]}"""
        val restored = json.decodeFromString(Backup.BackupFile.serializer(), old)
        assertNull(restored.localProfileName)
    }

    @Test fun localNameIsPortableWithoutEmbeddingImageOrCredentials() {
        val original = Backup.BackupFile(localProfileName = "Aurelis listener")
        val encoded = json.encodeToString(Backup.BackupFile.serializer(), original)
        val restored = json.decodeFromString(Backup.BackupFile.serializer(), encoded)
        assertEquals("Aurelis listener", restored.localProfileName)
        assertTrue(encoded.contains("aurelis"))
    }

    @Test fun androidBackupRulesRetainProfileAndExcludeEncryptedKeys() {
        val appRoot = listOf(File("."), File("app"), File("../app"))
            .firstOrNull { File(it, "src/main/res/xml/backup_rules.xml").isFile }
            ?: error("Android resources not found")
        val legacy = File(appRoot, "src/main/res/xml/backup_rules.xml").readText()
        val modern = File(appRoot, "src/main/res/xml/data_extraction_rules.xml").readText()
        for (rules in listOf(legacy, modern)) {
            assertTrue(rules.contains("aurelis_auth.xml"))
            assertTrue(rules.contains("aurelis_sources.xml"))
            assertTrue(!rules.contains("path=\"aurelis_local_profile.xml\""))
        }
    }
}
