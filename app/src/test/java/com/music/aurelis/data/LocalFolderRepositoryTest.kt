package com.music.aurelis.data

import android.content.*
import android.content.pm.ProviderInfo
import android.database.Cursor
import android.database.MatrixCursor
import android.net.Uri
import android.provider.DocumentsContract
import android.provider.MediaStore
import com.music.aurelis.AurelisApplication
import com.music.aurelis.data.model.LocalSong
import kotlinx.coroutines.runBlocking
import org.junit.*
import org.junit.Assert.*
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.shadows.ShadowContentResolver

@RunWith(RobolectricTestRunner::class)
@org.robolectric.annotation.Config(sdk = [33], manifest = org.robolectric.annotation.Config.NONE, application = AurelisApplication::class)
class LocalFolderRepositoryTest {
    private val app get() = RuntimeEnvironment.getApplication()
    private val tree = Uri.parse("content://com.android.externalstorage.documents/tree/primary%3AMusic")
    private lateinit var docs: FolderProvider
    private lateinit var media: MediaStoreLibraryTest.CatalogueProvider
    @Before fun setup() {
        docs = FolderProvider(); docs.attachInfo(app, ProviderInfo().apply { authority = "com.android.externalstorage.documents" })
        ShadowContentResolver.registerProviderInternal("com.android.externalstorage.documents", docs)
        media = MediaStoreLibraryTest.CatalogueProvider(); media.attachInfo(app, ProviderInfo().apply { authority = "media" })
        ShadowContentResolver.registerProviderInternal("media", media)
    }
    private fun row(id: Int, volume: String, path: String) = mapOf<String, Any?>(
        MediaStore.Audio.Media._ID to id, MediaStore.Audio.Media.TITLE to "Song$id", MediaStore.Audio.Media.ARTIST to "Artist",
        MediaStore.Audio.Media.ALBUM to "Album", MediaStore.Audio.Media.GENRE to "Jazz", MediaStore.Audio.Media.DURATION to 60000,
        MediaStore.Audio.Media.VOLUME_NAME to volume, MediaStore.Audio.Media.RELATIVE_PATH to path, MediaStore.Audio.Media.IS_MUSIC to 1)
    @Test fun scopedMediaStoreCatalogueKeepsIdsAndExcludesNeighboursAndOtherVolumes() = runBlocking {
        LocalFolderRepository.add(app, tree)
        media.rows = listOf(row(1,"external_primary","Music/"),row(2,"external_primary","Music/Jazz/"),
            row(3,"external_primary","Music2/"),row(4,"1234-abcd","Music/"))
        assertEquals(listOf("1","2"), LocalMediaRepository.getLibrarySongs(app,true).map { it.id })
        assertFalse(SettingsRepository.useAllAudioFolders.value)
        SettingsRepository.init(app)
        assertEquals(setOf(tree.toString()), SettingsRepository.localMusicFolders.value)
        assertFalse(SettingsRepository.useAllAudioFolders.value)
    }
    @Test fun revokedGrantFailsInsteadOfReturningAnEmptySuccessAndNeverDeletesPrivateData() = runBlocking {
        LocalFolderRepository.add(app, tree)
        LocalUserLibrary(app).toggleFavorite("1")
        app.contentResolver.releasePersistableUriPermission(tree, Intent.FLAG_GRANT_READ_URI_PERMISSION)
        assertTrue(runCatching { LocalMediaRepository.getLibrarySongs(app,true) }.exceptionOrNull() is java.io.IOException)
        assertEquals(setOf("1"),LocalUserLibrary(app).state.value.favorites)
        LocalFolderRepository.add(app, tree)
        assertTrue(LocalMediaRepository.getLibrarySongs(app,true).isEmpty())
    }
    @Test fun failedNewSelectionReleasesItsGrantAndPreservesPreviousPolicy() {
        docs.available = false
        assertThrows(IllegalArgumentException::class.java) { LocalFolderRepository.add(app, tree) }
        assertTrue(SettingsRepository.useAllAudioFolders.value)
        assertTrue(SettingsRepository.localMusicFolders.value.isEmpty())
        assertTrue(app.contentResolver.persistedUriPermissions.none { it.uri == tree })
    }
    @Test fun removeLastFolderDoesNotSilentlyExpandToTheWholeLibrary() = runBlocking {
        LocalFolderRepository.add(app, tree); LocalFolderRepository.remove(app, tree.toString())
        assertFalse(SettingsRepository.useAllAudioFolders.value)
        assertTrue(LocalMediaRepository.getLibrarySongs(app,true).isEmpty())
        assertTrue(app.contentResolver.persistedUriPermissions.none { it.uri == tree })
    }
    @Test fun providerWithoutFolderColumnsReportsUnsupportedRatherThanAnEmptyFolder() = runBlocking {
        LocalFolderRepository.add(app, tree)
        media.columns = arrayOf(MediaStore.Audio.Media._ID, MediaStore.Audio.Media.DURATION)
        assertTrue(runCatching { LocalMediaRepository.getLibrarySongs(app,true) }.exceptionOrNull() is java.io.IOException)
        assertTrue(media.cursors.all { it.isClosed })
    }
    @Test fun groupingKeepsVolumesSeparateAndUnknownPathsExplicit() {
        fun song(id:String,volume:String?,path:String?) = LocalSong(id,id,"A",null,"content://media/$id",null,60000,null,null,null,volumeName=volume,relativePath=path)
        val groups = LocalFolderRepository.groups(listOf(song("1","external_primary","Music/"),song("2","1234-abcd","Music/"),song("3",null,null)))
        assertEquals(3,groups.size); assertEquals(3,groups.map{it.key}.distinct().size)
        assertEquals("3",groups.single{it.key=="unknown"}.songs.single().id)
    }
    @Test fun onlyLocalTreesAreAcceptedAndTraversalIsRejected() {
        for (uri in listOf("https://example.com/tree/a", "content://cloud.documents/tree/a", "content://com.android.externalstorage.documents/tree/primary%3A..%2FMusic"))
            assertTrue(runCatching { LocalFolderRepository.scope(Uri.parse(uri)) }.isFailure)
        val scope = LocalFolderRepository.scope(tree)
        assertTrue(scope.contains("external_primary","Music/Jazz/")); assertFalse(scope.contains("external_primary","Music2/"))
    }
    class FolderProvider : ContentProvider() {
        var available = true
        override fun onCreate() = true
        override fun query(uri: Uri, projection:Array<out String>?,selection:String?,selectionArgs:Array<out String>?,sortOrder:String?):Cursor =
            MatrixCursor(arrayOf(DocumentsContract.Document.COLUMN_MIME_TYPE)).apply { if (available) addRow(arrayOf(DocumentsContract.Document.MIME_TYPE_DIR)) }
        override fun getType(uri: Uri) = DocumentsContract.Document.MIME_TYPE_DIR
        override fun insert(uri:Uri,values:ContentValues?):Uri? = null
        override fun update(uri:Uri,values:ContentValues?,selection:String?,selectionArgs:Array<out String>?)=0
        override fun delete(uri:Uri,selection:String?,selectionArgs:Array<out String>?)=0
    }
}
