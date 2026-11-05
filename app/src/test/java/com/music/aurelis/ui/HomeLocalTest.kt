package com.music.aurelis.ui

import android.graphics.Bitmap
import android.graphics.Canvas
import androidx.activity.ComponentActivity
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.*
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.unit.dp
import androidx.compose.material3.Surface
import androidx.compose.material3.MaterialTheme
import com.music.aurelis.AurelisApplication
import com.music.aurelis.data.*
import com.music.aurelis.data.model.LocalSong
import com.music.aurelis.data.model.LocalAlbum
import com.music.aurelis.data.model.LocalArtist
import com.music.aurelis.ui.screens.HomeScreen
import com.music.aurelis.ui.theme.AurelisTheme
import java.io.File
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
class HomeLocalTest {
    @get:Rule val compose = createAndroidComposeRule<ComponentActivity>()
    private val song = LocalSong("1", "Canción local", "Artista local", "Álbum local",
        "content://media/external/audio/media/1", null, 60_000, null, 1, 2026, 1000)

    @Test fun loadingPermissionErrorAndEmptyLibraryNeverShowInventedContent() {
        var state: LibraryState by mutableStateOf(LibraryState.Loading)
        var permissions = 0; var retries = 0
        compose.setContent { AurelisTheme { Surface(color = MaterialTheme.colorScheme.background) { HomeScreen(
            emptyList(), emptyList(), emptyList(), emptyList(), emptyList(), rememberLazyListState(),
            {}, {}, {}, {}, null, false, contentPadding = PaddingValues(),
            libraryState = state, onRetry = { retries++ }, onRequestPermission = { permissions++ },
        ) } } }
        compose.onNodeWithText("Cargando tu biblioteca…").assertIsDisplayed()
        compose.runOnIdle { state = LibraryState.Error("Permiso requerido", true) }
        compose.onNodeWithText("Conceder acceso a música").performClick()
        compose.runOnIdle { assertEquals(1, permissions); state = LibraryState.Error("No se pudo consultar") }
        compose.onNodeWithText("Reintentar").performClick()
        compose.runOnIdle { assertEquals(1, retries); state = LibraryState.Success(emptyList()) }
        compose.onNodeWithText("Actualizar biblioteca").performClick()
        compose.onNodeWithText("AutoMix · Selección local").assertDoesNotExist()
        compose.runOnIdle { assertEquals(2, retries) }
    }

    @Test fun populatedHomeContinuesThePlayerAndUsesLocalCollectionsAndMix() {
        var dark by mutableStateOf(false)
        var openedPlayer = 0; var restarted = 0; var mixes = 0; var favorites = 0
        val album = LocalAlbum("Álbum local", "Artista local", null, 1, listOf(song))
        val artist = LocalArtist("Artista local", null, 1, 1, listOf(song))
        compose.setContent { AurelisTheme(darkTheme = dark) { Surface(color = MaterialTheme.colorScheme.background) { HomeScreen(
            listOf(song), listOf(song), listOf(song), listOf(album), listOf(artist), rememberLazyListState(),
            { restarted++ }, {}, {}, {}, song, false, contentPadding = PaddingValues(bottom = 100.dp),
            libraryState = LibraryState.Success(listOf(song)), onOpenPlayer = { openedPlayer++ },
            onAutoMix = { mixes++ }, onOpenFavorites = { favorites++ },
        ) } } }
        compose.onNodeWithText("CONTINUAR ESCUCHANDO").assertIsDisplayed()
        compose.onNodeWithContentDescription("Reproducir").performClick()
        compose.onNode(hasText("Favoritos") and hasClickAction()).performClick()
        compose.onNodeWithText("AutoMix · Selección local").performClick()
        compose.runOnIdle { assertEquals(1, openedPlayer); assertEquals(0, restarted); assertEquals(1, mixes); assertEquals(1, favorites) }
        for (night in listOf(false, true)) {
            compose.runOnIdle { dark = night }
            val bitmap = compose.runOnIdle {
                val view = compose.activity.window.decorView
                Bitmap.createBitmap(view.width, view.height, Bitmap.Config.ARGB_8888).also { view.draw(Canvas(it)) }
            }
            val file = File("build/reports/b05-visual/home-${if (night) "dark" else "light"}.png")
            requireNotNull(file.parentFile).mkdirs()
            file.outputStream().use { assertTrue(bitmap.compress(Bitmap.CompressFormat.PNG, 100, it)) }
        }
    }
}
