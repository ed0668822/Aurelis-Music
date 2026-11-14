// Aurelis © 2026. GPL-3.0-or-later
package com.music.aurelis.ui

import androidx.activity.ComponentActivity
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import com.music.aurelis.AurelisApplication
import com.music.aurelis.data.model.LocalAlbum
import com.music.aurelis.data.model.LocalArtist
import com.music.aurelis.data.model.LocalSong
import com.music.aurelis.ui.components.home.AlbumCarousel
import com.music.aurelis.ui.components.home.ArtistCarousel
import com.music.aurelis.ui.components.home.SongCarousel
import com.music.aurelis.ui.theme.AurelisTheme
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33], qualifiers = "w360dp-h800dp-mdpi", application = AurelisApplication::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class HomeShelfLayoutTest {
    @get:Rule(order = 0) val registerActivity = RobolectricActivityRule()
    @get:Rule(order = 1) val compose = createAndroidComposeRule<ComponentActivity>()
    private val songs = (1..2).map { id ->
        LocalSong("$id", "Canción $id", "Artista $id", "Álbum $id",
            "content://media/external/audio/media/$id", null, 60000, null, id, 2026)
    }

    @Test fun songAndAlbumCardsShareTheReferenceGutterAndKeepIndependentActions() {
        val albums = songs.map { LocalAlbum(requireNotNull(it.album), it.artist, null, 1, listOf(it)) }
        var selectedSong = -1
        var selectedAlbum: LocalAlbum? = null
        compose.setContent { AurelisTheme {
            Column(Modifier.width(360.dp)) {
                SongCarousel("Canciones locales", songs, null, false, onSongClick = { selectedSong = it })
                AlbumCarousel("Álbumes locales", albums, onAlbumClick = { selectedAlbum = it })
            }
        } }
        for (titles in listOf(songs.map { it.title }, albums.map { it.name })) {
            val first = compose.onNode(hasText(titles[0]) and hasClickAction()).fetchSemanticsNode().boundsInRoot
            val second = compose.onNode(hasText(titles[1]) and hasClickAction()).fetchSemanticsNode().boundsInRoot
            // SDK33 mdpi: pixels equal dp. Check rendered positions, not helper constants.
            assertEquals(16f, first.left, 1f)
            assertEquals(150f, first.width, 1f)
            assertEquals(14f, second.left - first.right, 1f)
        }
        compose.onNode(hasText(songs[1].title) and hasClickAction()).performClick()
        compose.onNode(hasText(albums[0].name) and hasClickAction()).performClick()
        compose.runOnIdle {
            assertEquals(1, selectedSong)
            assertEquals(albums[0], selectedAlbum)
        }
    }

    @Test fun longLocalNamesRemainActionableWithLargeTextInBothThemes() {
        val album = LocalAlbum("Álbum con un nombre local muy extenso", "Intérprete", null, 1, songs.take(1))
        val artist = LocalArtist("Artista con un nombre local muy extenso", null, 1, 1, songs.take(1))
        var openedAlbum: LocalAlbum? = null
        var openedArtist: LocalArtist? = null
        var dark by androidx.compose.runtime.mutableStateOf(false)
        compose.setContent { AurelisTheme(darkTheme = dark) {
            val density = LocalDensity.current
            CompositionLocalProvider(LocalDensity provides Density(density.density, 1.5f)) {
                Column(Modifier.width(360.dp)) {
                    AlbumCarousel("Álbumes de la biblioteca local", listOf(album), onAlbumClick = { openedAlbum = it })
                    ArtistCarousel("Artistas de la biblioteca local", listOf(artist), onArtistClick = { openedArtist = it })
                }
            }
        } }
        for (night in listOf(false, true)) {
            compose.runOnIdle { dark = night; openedAlbum = null; openedArtist = null }
            compose.onNode(hasText(album.name) and hasClickAction()).assertIsDisplayed().performClick()
            compose.onNode(hasText(artist.name) and hasClickAction()).assertIsDisplayed().performClick()
            compose.runOnIdle { assertEquals(album, openedAlbum); assertEquals(artist, openedArtist) }
        }
    }
}
