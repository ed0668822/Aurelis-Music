package com.music.aurelis.data

import android.content.ContextWrapper
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.music.aurelis.data.model.HomeShelf
import com.music.aurelis.data.model.LibraryPage
import com.music.aurelis.data.model.MoodGenre
import com.music.aurelis.data.model.MoodGenreSection
import com.music.aurelis.data.model.ShelfItem
import com.music.aurelis.data.model.Song
import java.nio.file.Files
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.builtins.ListSerializer
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class OfflineFeedStoreTest {
    @Test fun restoresLastSuccessfulFeedsAndKeepsListenersSeparate() = runBlocking {
        val app = InstrumentationRegistry.getInstrumentation().targetContext
        val root = Files.createTempDirectory(app.cacheDir.toPath(), "offline-feed-test").toFile()
        val context = object : ContextWrapper(app) {
            override fun getFilesDir() = root
        }
        try {
            val store = OfflineFeedStore(context)
            val shelves = ListSerializer(HomeShelf.serializer())
            val genres = ListSerializer(MoodGenreSection.serializer())
            val songs = ListSerializer(Song.serializer())
            val first = listOf(HomeShelf("Home", listOf(ShelfItem("Song", "Artist", "https://example.com/art", "a", null))))
            val second = listOf(HomeShelf("New", first.first().items))
            val liked = Song("a", "Song", "Artist", null)
            val library = LibraryPage(listOf(liked), emptyList(), listOf(HomeShelf("Playlists", first.first().items)))
            val explore = listOf(MoodGenreSection("Genres", listOf(MoodGenre("Rock", "genre", "params"))))

            store.write("account:one", "home", shelves, first)
            store.write("account:one", "home", shelves, second)
            store.write("account:one", "explore", genres, explore)
            store.write("account:one", "library", LibraryPage.serializer(), library)
            store.write("account:one", "detail:playlist", songs, listOf(liked))
            store.write("account:two", "home", shelves, first)

            val reopened = OfflineFeedStore(context)
            assertEquals(second, reopened.read("account:one", "home", shelves))
            assertEquals(first, reopened.read("account:two", "home", shelves))
            assertEquals(explore, reopened.read("account:one", "explore", genres))
            assertEquals(library, reopened.read("account:one", "library", LibraryPage.serializer()))
            assertEquals(listOf(liked), reopened.read("account:one", "detail:playlist", songs))
            assertNull(reopened.read("account:two", "library", LibraryPage.serializer()))
            assertNull(reopened.read("anonymous", "home", shelves))
            assertNull(reopened.read(null, "home", shelves))
        } finally {
            root.deleteRecursively()
        }
    }
}
