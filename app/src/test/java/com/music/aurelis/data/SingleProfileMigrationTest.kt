package com.music.aurelis.data

import android.app.Application
import android.content.Context
import com.music.aurelis.data.model.ProfileId
import org.json.JSONObject
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config
import java.io.File
import java.io.IOException
import java.util.zip.ZipFile

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33], manifest = Config.NONE, application = Application::class)
class SingleProfileMigrationTest {
    private val app get() = RuntimeEnvironment.getApplication()
    private val registry get() = app.getSharedPreferences("aurelis_profiles_registry", Context.MODE_PRIVATE)
    private val storage get() = ProfileStorage(app)
    private val alice = ProfileId("alice")
    private val bob = ProfileId("bob")
    private fun legacy(active: String = "alice", pending: String = "") =
        """{"version":1,"ids":["default","alice","bob"],"active":"$active","deleting":[$pending]}"""
    private fun seed(raw: String = legacy()) { registry.edit().putString("profiles_v1", raw).putString("unknown", "keep").commit() }
    private fun backup(migration: SingleProfileMigration) = migration.directory.listFiles()!!.single { it.extension == "zip" }

    @Test fun freshInstallSelectsOneDefaultAndDoesNotCreateALegacyRegistry() {
        val migration = SingleProfileMigration(app)
        assertEquals(ProfileId.DEFAULT, migration.resolve())
        assertTrue(registry.all.isEmpty())
        assertTrue(migration.receiptFile.exists())
        val bytes = backup(migration).readBytes()
        repeat(2) { assertEquals(ProfileId.DEFAULT, migration.resolve()) }
        assertArrayEquals(bytes, backup(migration).readBytes())
    }
    @Test fun archivesEveryAreaTypeAndAvatarWithoutTouchingOriginalsOrMusic() {
        seed()
        val beforeRegistry = registry.all.toMap()
        val before = mutableMapOf<Pair<ProfileId, ProfileDataArea>, Map<String, *>>()
        for (id in listOf(ProfileId.DEFAULT, alice, bob)) for (area in ProfileDataArea.entries) {
            val prefs = storage.open(id, area)
            prefs.edit().putString("text", "${id.value}:${area.name}").putInt("int", 123)
                .putLong("long", Long.MAX_VALUE).putBoolean("bool", true).putFloat("float", .125f)
                .putStringSet("set", setOf("two", "one")).commit()
            before[id to area] = prefs.all.toMap()
        }
        val photo = File(app.filesDir, "profiles/alice/private.png").apply { parentFile!!.mkdirs(); writeBytes(byteArrayOf(1, 2, 3)) }
        val song = File(app.filesDir, "music.mp3").apply { writeText("keep music") }
        val lyrics = File(app.cacheDir, "lyrics.json").apply { writeText("keep cache") }
        val migration = SingleProfileMigration(app)
        assertEquals(alice, migration.resolve())
        val zip = backup(migration)
        assertTrue(zip.canonicalPath.startsWith(app.noBackupFilesDir.canonicalPath + File.separator))
        val manifest = JSONObject(migration.readManifest(zip))
        assertEquals(beforeRegistry, SingleProfileMigration.decodePreferences(manifest.getJSONObject("registry")))
        for ((pair, values) in before) {
            val (id, area) = pair
            assertEquals(values, storage.open(id, area).all)
            assertEquals(values, SingleProfileMigration.decodePreferences(manifest.getJSONObject("areas").getJSONObject(id.value).getJSONObject(area.key)))
        }
        ZipFile(zip).use { z -> assertArrayEquals(photo.readBytes(), z.getInputStream(z.getEntry("avatars/alice/private.png")).readBytes()) }
        assertEquals(beforeRegistry, registry.all)
        assertEquals("keep music", song.readText()); assertEquals("keep cache", lyrics.readText())
    }
    @Test fun failedBackupNeverCommitsSelectionOrChangesPreferences() {
        seed(); val before = registry.all.toMap()
        val migration = SingleProfileMigration(app)
        migration.directory.writeText("not a directory")
        assertThrows(IllegalStateException::class.java) { migration.resolve() }
        assertFalse(migration.receiptFile.exists()); assertEquals(before, registry.all)
        migration.directory.delete()
        assertEquals(alice, migration.resolve())
    }
    @Test fun interruptedAfterVerifiedBackupRetriesWithoutDuplicatingOrOverwritingArchive() {
        seed(); val before = registry.all.toMap()
        val interrupted = SingleProfileMigration(app) { throw IOException("simulated process interruption") }
        assertThrows(IOException::class.java) { interrupted.resolve() }
        assertFalse(interrupted.receiptFile.exists()); assertEquals(before, registry.all)
        val archiveBytes = backup(interrupted).readBytes()
        // A torn AtomicFile new receipt is not a committed selection.
        File(interrupted.receiptFile.path + ".new").writeText("partial")
        assertEquals(alice, SingleProfileMigration(app).resolve())
        assertArrayEquals(archiveBytes, backup(interrupted).readBytes())
        assertEquals(1, interrupted.directory.listFiles()!!.count { it.extension == "zip" })
    }
    @Test fun priorDeletionTombstonesAreBackedUpWithoutResumingDeletion() {
        seed(legacy(pending = "\"removed\""))
        val removed = ProfileId("removed")
        storage.open(removed, ProfileDataArea.HISTORY).edit().putString("pending", "recover me").commit()
        val migration = SingleProfileMigration(app); assertEquals(alice, migration.resolve())
        assertEquals("recover me", storage.open(removed, ProfileDataArea.HISTORY).getString("pending", null))
        val root = JSONObject(migration.readManifest(backup(migration)))
        assertTrue(root.getJSONObject("areas").has(removed.value))
    }
    @Test fun invalidOrUnknownLegacyRegistryPreservesBytesAndBlocksAllWrites() {
        for (raw in listOf("broken", "{\"version\":99}", legacy(active = "missing"))) {
            registry.edit().putString("profiles_v1", raw).commit()
            ProfileRepository.init(app)
            assertTrue(ProfileRepository.state.value.readOnly)
            assertEquals(raw, registry.getString("profiles_v1", null))
            assertFalse(SingleProfileMigration(app).receiptFile.exists())
            assertThrows(IllegalStateException::class.java) { ProfileRepository.rename("Blocked") }
            assertThrows(IllegalStateException::class.java) { LocalUserLibrary(app).toggleFavorite("x") }
        }
    }
    @Test fun corruptCommittedBackupFailsClosedWithoutOverwritingAnything() {
        seed(); val migration = SingleProfileMigration(app); migration.resolve()
        val archive = backup(migration); archive.appendBytes(byteArrayOf(0))
        val corrupted = archive.readBytes(); val receipt = migration.receiptFile.readBytes()
        ProfileRepository.init(app)
        assertTrue(ProfileRepository.state.value.readOnly)
        assertEquals(alice, ProfileRepository.profileId)
        assertThrows(IllegalStateException::class.java) { ProfileRepository.userPhotoUri = "content://wrong" }
        assertArrayEquals(corrupted, archive.readBytes()); assertArrayEquals(receipt, migration.receiptFile.readBytes())
    }
    @Test fun unknownReceiptVersionAndActiveMismatchArePreserved() {
        seed(); val migration = SingleProfileMigration(app); migration.resolve()
        val original = JSONObject(migration.receiptFile.readText())
        for (raw in listOf(JSONObject(original.toString()).put("version", 99).toString(), JSONObject(original.toString()).put("profileId", "bob").toString())) {
            migration.receiptFile.writeText(raw)
            assertThrows(Exception::class.java) { migration.resolve() }
            assertEquals(raw, migration.receiptFile.readText())
        }
    }
    @Test fun selectionRemainsStableAfterRegistryOrMetadataEditsAndRestart() {
        seed(); val migration = SingleProfileMigration(app); assertEquals(alice, migration.resolve())
        val archive = backup(migration).readBytes()
        storage.open(alice, ProfileDataArea.PROFILE).edit().putString("user_name", "Changed later").commit()
        registry.edit().putString("profiles_v1", legacy(active = "bob")).commit()
        assertEquals(alice, SingleProfileMigration(app).resolve())
        assertArrayEquals(archive, backup(migration).readBytes())
        ProfileRepository.init(app); assertEquals("Changed later", ProfileRepository.userName)
    }
    @Test fun oldSingleProfileWithoutDefaultKeepsItsInheritedStorageId() {
        registry.edit().putString("profiles_v1", """{"version":1,"ids":["alice"],"active":"alice"}""").commit()
        storage.open(alice, ProfileDataArea.PROFILE).edit().putString("user_name", "Único anterior").commit()
        ProfileRepository.init(app)
        assertFalse(ProfileRepository.state.value.readOnly)
        assertEquals(alice, ProfileRepository.profileId)
        assertEquals("Único anterior", ProfileRepository.state.value.profile.name)
    }
    @Test fun oversizedAvatarCannotConfirmAnIncompleteBackup() {
        seed()
        val photo = File(app.filesDir, "profiles/alice/large.png").apply { parentFile!!.mkdirs() }
        java.io.RandomAccessFile(photo, "rw").use { it.setLength(64L * 1024 * 1024 + 1) }
        val migration = SingleProfileMigration(app)
        assertThrows(IllegalArgumentException::class.java) { migration.resolve() }
        assertFalse(migration.receiptFile.exists())
        assertEquals(64L * 1024 * 1024 + 1, photo.length())
    }

    @Test fun preferenceRecoveryPreservesNonFiniteFloatBits() {
        for (value in listOf(Float.NaN, Float.POSITIVE_INFINITY, Float.NEGATIVE_INFINITY, -0.0f)) {
            val decoded = SingleProfileMigration.decodePreferences(SingleProfileMigration.encodePreferences(mapOf("value" to value)))
            assertEquals(value.toRawBits(), (decoded["value"] as Float).toRawBits())
        }
    }
}
