package com.music.aurelis.data

import android.content.ContentProvider
import android.content.ContentValues
import android.content.pm.ProviderInfo
import android.database.Cursor
import android.database.MatrixCursor
import android.net.Uri
import android.os.Looper
import android.provider.MediaStore
import com.music.aurelis.AurelisApplication
import java.io.IOException
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.collect
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config
import org.robolectric.shadows.ShadowContentResolver

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33], manifest = Config.NONE, application = AurelisApplication::class)
class MediaStoreLibraryTest {
    private val context get() = RuntimeEnvironment.getApplication()
    private lateinit var provider: CatalogueProvider

    @Before fun setup() {
        provider = CatalogueProvider()
        provider.attachInfo(context, ProviderInfo().apply { authority = "media" })
        ShadowContentResolver.registerProviderInternal("media", provider)
    }

    private fun row(id: Any?, extra: Map<String, Any?> = emptyMap()): Map<String, Any?> = mapOf(
        MediaStore.Audio.Media._ID to id, MediaStore.Audio.Media.TITLE to "Song $id",
        MediaStore.Audio.Media.ARTIST to "Artist", MediaStore.Audio.Media.ALBUM to "Album",
        MediaStore.Audio.Media.ALBUM_ID to 10L, MediaStore.Audio.Media.ARTIST_ID to 20L,
        MediaStore.Audio.Media.DISPLAY_NAME to "file-$id.mp3", MediaStore.Audio.Media.DURATION to 60_000L,
        MediaStore.Audio.Media.IS_MUSIC to 1, MediaStore.Audio.Media.IS_PENDING to 0,
        MediaStore.Audio.Media.IS_TRASHED to 0, MediaStore.Audio.Media.DATE_ADDED to 123L,
    ) + extra

    @Test fun metadataFallbacksFlagsAndUrisAreReadWithoutPathsOrInventedIds() = runBlocking {
        provider.rows = listOf(row(1L, mapOf(MediaStore.Audio.Media.TRACK to 1002, MediaStore.Audio.Media.GENRE to "Pop")), row(2L, mapOf(
            MediaStore.Audio.Media.TITLE to "", MediaStore.Audio.Media.ARTIST to "<unknown>",
            MediaStore.Audio.Media.ALBUM to null, MediaStore.Audio.Media.ALBUM_ID to null,
        )), row("invalid"), row(3, mapOf(MediaStore.Audio.Media.DURATION to 0)),
            row(4, mapOf(MediaStore.Audio.Media.IS_PENDING to 1)),
            row(5, mapOf(MediaStore.Audio.Media.IS_TRASHED to 1)),
            row(6, mapOf(MediaStore.Audio.Media.IS_MUSIC to 0)))
        val songs = LocalMediaRepository.getAllSongs(context, true)
        assertEquals(listOf("1", "2"), songs.map { it.id })
        assertEquals("content://media/external/audio/media/1", songs[0].contentUri)
        assertEquals("10", songs[0].albumId)
        assertEquals("20", songs[0].artistId)
        assertEquals(2, songs[0].trackNumber)
        assertEquals(1, songs[0].discNumber)
        assertEquals("content://media/external/audio/albumart/10", songs[0].artworkUri)
        assertEquals(123_000, songs[0].dateAddedMs)
        assertEquals("Pop", songs[0].genre)
        assertEquals("file-2", songs[1].title)
        assertEquals("Artista desconocido", songs[1].artist)
        assertNull(songs[1].filePath)
        assertNull(songs[1].artworkUri)
        assertTrue(provider.cursors.all { it.isClosed })
        assertTrue(provider.selection.orEmpty().contains("is_pending = 0"))
        assertTrue(provider.selection.orEmpty().contains("is_trashed = 0"))
        assertEquals(listOf("500"), provider.arguments?.toList())
    }

    @Test fun nonMusicFilterIsOptionalButPendingAndTrashedAreAlwaysExcluded() = runBlocking {
        provider.rows = listOf(row(1, mapOf(MediaStore.Audio.Media.IS_MUSIC to 0)))
        assertTrue(LocalMediaRepository.getAllSongs(context, true).isEmpty())
        assertEquals(1, LocalMediaRepository.getAllSongs(context, false).size)
        assertFalse(provider.selection.orEmpty().contains("is_music"))
        assertTrue(provider.cursors.all { it.isClosed })
    }

    @Test fun sparseColumnsAndDuplicateRowsDoNotCrashOrCreateDuplicateLazyKeys() = runBlocking {
        provider.columns = arrayOf(MediaStore.Audio.Media._ID, MediaStore.Audio.Media.DURATION,
            MediaStore.Audio.Media.DISPLAY_NAME)
        provider.rows = listOf(row(1), row(1))
        val songs = LocalMediaRepository.getAllSongs(context, false)
        assertEquals(1, songs.size)
        assertEquals("file-1", songs.single().title)
        assertNull(songs.single().albumId)
        assertTrue(provider.cursors.all { it.isClosed })
    }

    @Test fun nullAndDeniedQueriesAreFailuresRatherThanAnEmptySuccessfulCatalogue() = runBlocking {
        provider.returnNull = true
        assertTrue(runCatching { LocalMediaRepository.getAllSongs(context, true) }.exceptionOrNull() is IOException)
        provider.returnNull = false; provider.denied = true
        assertTrue(runCatching { LocalMediaRepository.getAllSongs(context, true) }.exceptionOrNull() is SecurityException)
    }

    @Test fun movesAndRemovalsResolveCurrentMetadataWithoutDeletingSavedReferences() = runBlocking {
        val store = LocalUserLibrary(context)
        store.toggleFavorite("1"); store.createPlaylist("Local", listOf("1"))
        provider.rows = listOf(row(1, mapOf(MediaStore.Audio.Media.DATA to "/Music/old.mp3")))
        val first = LocalMediaRepository.getAllSongs(context, true).single()
        provider.rows = listOf(row(1, mapOf(MediaStore.Audio.Media.DATA to "/Music/new.mp3", MediaStore.Audio.Media.TITLE to "Renamed")))
        val moved = LocalMediaRepository.getAllSongs(context, true).single()
        assertEquals(first.contentUri, moved.contentUri)
        assertEquals("/Music/new.mp3", moved.filePath)
        assertEquals("Renamed", moved.title)
        provider.rows = emptyList()
        val removed = LocalMediaRepository.getAllSongs(context, true)
        assertTrue(LocalHomeData.build(removed, store.state.value, null).favorites.isEmpty())
        assertEquals(setOf("1"), LocalUserLibrary(context).state.value.favorites)
        assertEquals(listOf("1"), LocalUserLibrary(context).state.value.playlists.single().songIds)
    }

    @Test fun contentObserverRegistersBeforeInitialEventAndIsReleasedOnCancellation() {
        val resolver = context.contentResolver
        val uri = MediaStore.Audio.Media.EXTERNAL_CONTENT_URI
        var notifications = 0
        val scope = CoroutineScope(SupervisorJob() + Dispatchers.Unconfined)
        val job = scope.launch { MediaStoreLibrarySource(context).changes.collect { notifications++ } }
        shadowOf(Looper.getMainLooper()).idle()
        assertEquals(1, shadowOf(resolver).getContentObservers(uri).size)
        assertEquals(1, notifications)
        resolver.notifyChange(uri, null)
        shadowOf(Looper.getMainLooper()).idle()
        assertEquals(2, notifications)
        job.cancel(); scope.cancel()
        shadowOf(Looper.getMainLooper()).idle()
        assertTrue(shadowOf(resolver).getContentObservers(uri).isEmpty())
    }

    class CatalogueProvider : ContentProvider() {
        var rows: List<Map<String, Any?>> = emptyList()
        var columns: Array<String>? = null
        var returnNull = false
        var denied = false
        var selection: String? = null
        var arguments: Array<out String>? = null
        val cursors = mutableListOf<MatrixCursor>()
        override fun onCreate() = true
        override fun query(uri: Uri, projection: Array<out String>?, selection: String?, selectionArgs: Array<out String>?, sortOrder: String?): Cursor? {
            this.selection = selection; arguments = selectionArgs
            if (denied) throw SecurityException("Denied")
            if (returnNull) return null
            val names = columns ?: requireNotNull(projection).map { it }.toTypedArray()
            return MatrixCursor(names).also { cursor ->
                rows.forEach { row -> cursor.addRow(names.map { row[it] }.toTypedArray()) }
                cursors += cursor
            }
        }
        override fun getType(uri: Uri): String = "vnd.android.cursor.dir/audio"
        override fun insert(uri: Uri, values: ContentValues?): Uri? = null
        override fun delete(uri: Uri, selection: String?, selectionArgs: Array<out String>?): Int = 0
        override fun update(uri: Uri, values: ContentValues?, selection: String?, selectionArgs: Array<out String>?): Int = 0
    }
}
