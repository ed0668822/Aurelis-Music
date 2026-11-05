package com.music.aurelis.ui

import android.Manifest
import org.robolectric.Shadows.shadowOf
import android.graphics.Bitmap
import android.graphics.Canvas
import androidx.activity.ComponentActivity
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.lifecycle.ViewModelStore
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import com.music.aurelis.*
import com.music.aurelis.data.*
import com.music.aurelis.data.model.LocalSong
import com.music.aurelis.playback.*
import com.music.aurelis.ui.navigation.*
import com.music.aurelis.ui.theme.AurelisTheme
import java.io.File
import org.junit.*
import org.junit.Assert.*
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

@UnstableApi
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33], qualifiers = "w360dp-h800dp-mdpi", application = AurelisApplication::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class PlayerIntegrationTest {
    @get:org.junit.Rule(order = 0) val registerActivity = com.music.aurelis.ui.RobolectricActivityRule()
    @get:Rule(order = 1) val compose = createAndroidComposeRule<ComponentActivity>()
    private val app get() = RuntimeEnvironment.getApplication() as AurelisApplication
    private val store = ViewModelStore()
    private lateinit var vm: PlaybackViewModel
    private lateinit var player: RecordingPlayer
    private fun song(id: String) = LocalSong(id, "Canción local $id", "Artista local", "Álbum local",
        "content://media/external/audio/media/$id", null, 60_000, null, 1, 2026)
    @After fun cleanup() { compose.runOnIdle { store.clear() } }
    private fun start() {
        SettingsRepository.setReduceAnimation(true)
        SettingsRepository.lyricsSources.value = emptySet()
        player = RecordingPlayer()
        vm = PlaybackViewModel(app) { player }; store.put("player", vm)
        shadowOf(app).grantPermissions(Manifest.permission.READ_MEDIA_AUDIO)
        val library = LibraryViewModel(app, LocalLibrarySource { listOf(song("1"), song("2"), song("3")) }); store.put("library", library)
        compose.setContent { AurelisTheme {
            AurelisNavigationContent(rememberAppNavigator(), vm, library)
        } }
        compose.runOnIdle { library.loadLibrary(); vm.playSongs(listOf(song("1"), song("2"), song("3"))) }
        compose.waitForIdle()
        compose.runOnIdle { player.pause() }
    }
    @Test fun miniPlayerOpensRealControlsAndQueueActionsStaySynchronized() {
        start()
        compose.onNodeWithTag("mini-player").assertIsDisplayed()
        compose.onNodeWithContentDescription("Abrir reproductor").assertIsDisplayed().performClick()
        compose.onNodeWithContentDescription("Cerrar reproductor").assertIsDisplayed()
        compose.onNodeWithContentDescription("Aleatorio").performScrollTo().performClick()
        compose.runOnIdle { assertTrue(player.shuffleModeEnabled) }
        compose.onNodeWithContentDescription("Repetición").performScrollTo().performClick()
        compose.runOnIdle { assertEquals(Player.REPEAT_MODE_ALL, player.repeatMode) }
        compose.onNodeWithContentDescription("Posición de reproducción").performScrollTo()
            .performSemanticsAction(androidx.compose.ui.semantics.SemanticsActions.SetProgress) { it(0.5f) }
        compose.runOnIdle { assertEquals(30_000, vm.state.value.positionMs) }
        compose.onNodeWithContentDescription("Abrir cola").performScrollTo().performClick()
        compose.onNodeWithText("Canción local 2").performClick()
        compose.runOnIdle { assertEquals("2", vm.state.value.currentSong?.id); player.pause() }
        compose.onAllNodesWithContentDescription("Quitar de la cola")[0].performClick()
        compose.runOnIdle { assertEquals(listOf("2", "3"), vm.state.value.queue.map { it.id }) }
        compose.onNodeWithContentDescription("Cerrar").performClick()
        compose.onNodeWithContentDescription("Cerrar reproductor").performScrollTo().performClick()
        compose.onNodeWithTag("mini-player").assertIsDisplayed()
        compose.onNodeWithTag("mini-player").assertTextContains("Canción local 2")
    }
    @Test fun playerHasUsableArtworkTimeControlsAndCompactBarAt360dp() {
        start()
        capture("mini-player")
        compose.onNodeWithContentDescription("Abrir reproductor").performClick()
        compose.onNodeWithContentDescription("Repetición").performScrollTo().assertIsDisplayed()
        compose.onNodeWithText("1:00").assertIsDisplayed()
        compose.onNodeWithContentDescription("Siguiente").performScrollTo().assertIsDisplayed()
        capture("now-playing")
    }
    @Test
    @Config(qualifiers = "w640dp-h360dp-mdpi")
    fun landscapeKeepsTransportAccessibleByScrolling() {
        start()
        compose.onNodeWithContentDescription("Abrir reproductor").performClick()
        compose.onNodeWithContentDescription("Repetición").performScrollTo().assertIsDisplayed()
        compose.onNodeWithContentDescription("Siguiente").performScrollTo().performClick()
        compose.runOnIdle { assertEquals("2", vm.state.value.currentSong?.id) }
        compose.onNodeWithContentDescription("Cerrar reproductor").performScrollTo().performClick()
        compose.onNodeWithTag("mini-player").assertIsDisplayed()
    }
    @Test fun songActionsPersistFavoritePlaylistAndAppendWithoutRestartingQueue() {
        start()
        compose.onNodeWithContentDescription("Abrir reproductor").performClick()
        compose.onNodeWithContentDescription("Opciones de canción").performClick()
        compose.onNodeWithText("Añadir a favoritos").performClick()
        compose.runOnIdle { assertEquals(setOf("1"), app.userLibrary.state.value.favorites) }
        compose.onNode(hasSetTextAction()).performTextInput("Mi lista")
        compose.onNodeWithText("Crear playlist con esta canción").performClick()
        compose.runOnIdle { assertEquals(listOf("1"), app.userLibrary.state.value.playlists.single().songIds) }
        compose.onNodeWithContentDescription("Opciones de canción").performClick()
        compose.onNodeWithText("Añadir a la cola").performClick()
        compose.runOnIdle { assertEquals(listOf("1", "2", "3", "1"), vm.state.value.queue.map { it.id }) }
        compose.onNodeWithContentDescription("Abrir cola").performScrollTo().performClick()
        compose.onAllNodesWithContentDescription("Bajar en la cola")[0].performClick()
        compose.runOnIdle {
            assertEquals(listOf("2", "1", "3", "1"), vm.state.value.queue.map { it.id })
            assertEquals("1", vm.state.value.currentSong?.id); assertEquals(1, vm.state.value.currentIndex)
        }
    }
    @Test fun replayShowsActualHistoryAndRequiresConfirmationToClearIt() {
        val store = app.userLibrary
        store.recordListening(song("1"), 5000, true); store.flush()
        store.toggleFavorite("1"); store.createPlaylist("Conservar", listOf("1"))
        compose.setContent { AurelisTheme { com.music.aurelis.ui.screens.ReplayScreen(
            store = store, songs = listOf(song("1")), onPlay = {}) } }
        compose.onNodeWithText("1 reproducciones · 5 s").assertIsDisplayed()
        compose.onNodeWithText("Borrar historial").performClick()
        compose.onNodeWithText("Cancelar").performClick()
        compose.runOnIdle { assertEquals(1, store.state.value.listening.size) }
        compose.onNodeWithText("Borrar historial").performClick()
        compose.onAllNodesWithText("Borrar historial").onLast().performClick()
        compose.runOnIdle {
            assertTrue(store.state.value.listening.isEmpty())
            assertEquals(setOf("1"), store.state.value.favorites)
            assertEquals(1, store.state.value.playlists.size)
        }
    }
    @Test fun importedLyricsHighlightAndSeekWithOffsetInTheRealPlayer() {
        start()
        SettingsRepository.setLyricsOffsetMs(500)
        val lrc = File(app.cacheDir, "import-test.lrc").apply { writeText("[00:00.000]Primera frase propia\n[00:01.000]Segunda frase propia") }
        compose.runOnIdle { vm.importLyrics(android.net.Uri.fromFile(lrc)) }
        compose.waitUntil(5000) { compose.runOnIdle { vm.state.value.lyrics !is LyricsState.Loading } }
        compose.runOnIdle { assertTrue("Expected imported lyrics, got ${vm.state.value.lyrics}", vm.state.value.lyrics is LyricsState.Loaded) }
        compose.onNodeWithContentDescription("Abrir reproductor").performClick()
        compose.onNodeWithText("Ver Letras").performScrollTo().performClick()
        compose.onNode(hasText("Archivo local") and hasAnyAncestor(hasTestTag("lyrics-overlay"))).assertIsDisplayed()
        compose.onNodeWithText("Primera frase propia").assertIsSelected()
        compose.onNodeWithText("Segunda frase propia").performClick()
        compose.runOnIdle { assertEquals(500L, vm.state.value.positionMs) }
        compose.onNodeWithText("Segunda frase propia").assertIsSelected()
        capture("synced-lyrics")
        compose.runOnIdle { vm.next() }
        compose.runOnIdle { vm.importLyrics(android.net.Uri.fromFile(lrc), "1") }
        compose.runOnIdle { assertFalse(vm.state.value.lyrics is LyricsState.Loaded) }
    }
    @Test fun translationDialogCancelNeverSendsAndBilingualViewKeepsOriginal() {
        start(); importTranslationFixture()
        var requests = 0
        vm.translateRequest = { _, original, _ -> requests++
            com.music.aurelis.data.lyrics.LyricsTranslation.Result.Translated(original.mapIndexed { index, line -> line.copy(text = "Traducción de prueba $index") }, "en", false) }
        SettingsRepository.setTranslationService("https://translation.example", "en")
        SettingsRepository.setTranslationLanguage("es")
        compose.onNodeWithContentDescription("Abrir reproductor").performClick()
        compose.onNodeWithText("Ver Letras").performScrollTo().performClick()
        compose.onNodeWithContentDescription("Traducir").performClick()
        compose.onNodeWithText("Cancelar").performClick()
        compose.runOnIdle { assertEquals(0, requests) }
        compose.onNodeWithContentDescription("Traducir").performClick()
        compose.onNodeWithText("Traducir").performClick()
        compose.onNodeWithText("Source line A").assertIsDisplayed()
        compose.onNodeWithText("Traducción de prueba 0").assertIsDisplayed()
        compose.runOnIdle { assertEquals(1, requests) }
        capture("bilingual-lyrics")
        compose.onNodeWithText("Ocultar traducción").performClick()
        compose.onNodeWithText("Traducción de prueba 0").assertDoesNotExist()
        compose.onNodeWithText("Source line A").assertIsDisplayed()
    }
    @Test fun aLateTranslationCannotOverwriteLyricsOfAnotherTrack() {
        start(); importTranslationFixture()
        val gate = kotlinx.coroutines.CompletableDeferred<Unit>()
        vm.translateRequest = { _, lines, _ ->
            kotlinx.coroutines.withContext(kotlinx.coroutines.NonCancellable) { gate.await() }
            com.music.aurelis.data.lyrics.LyricsTranslation.Result.Translated(lines.map { it.copy(text = "Late fixture") }, "en", false)
        }
        try {
            compose.runOnIdle { vm.translateLyrics("es") }
            compose.runOnIdle { vm.next() }
            importTranslationFixture()
            compose.runOnIdle { gate.complete(Unit) }
            compose.runOnIdle {
                assertEquals("2", vm.state.value.currentSong?.id)
                val loaded = vm.state.value.lyrics as LyricsState.Loaded
                assertEquals(TranslationState.None, loaded.translation)
                assertEquals("Source line A", loaded.lines.first().text)
            }
        } finally { gate.complete(Unit) }
    }
    private fun importTranslationFixture() {
        val file = File(app.cacheDir, "translation-source.lrc").apply { writeText("[00:00.000]Source line A\n[00:01.000]Source line B") }
        compose.runOnIdle { vm.importLyrics(android.net.Uri.fromFile(file)) }
        compose.waitUntil(5000) { compose.runOnIdle { vm.state.value.lyrics !is LyricsState.Loading } }
        compose.runOnIdle { assertTrue(vm.state.value.lyrics is LyricsState.Loaded) }
    }
    @Test fun homeAutoMixKeepsCurrentItemAndPositionWhileReplacingUpcomingSongs() {
        start()
        val original = compose.runOnIdle { player.seekTo(12_345); player.shuffleModeEnabled = true; player.currentMediaItem }
        compose.onNodeWithText("AutoMix · Selección local").performClick()
        compose.waitUntil(5000) { compose.runOnIdle { !vm.state.value.isCreatingMix } }
        compose.runOnIdle {
            assertNull(vm.state.value.mixError)
            assertSame(original, player.currentMediaItem)
            assertEquals("1", vm.state.value.currentSong?.id)
            assertEquals(12_345L, vm.state.value.positionMs)
            assertEquals(setOf("1", "2", "3"), vm.state.value.queue.map { it.id }.toSet())
            assertFalse(player.shuffleModeEnabled); assertTrue(player.playWhenReady)
        }
        compose.onNodeWithContentDescription("Abrir reproductor").performClick()
        compose.onNodeWithContentDescription("Abrir cola").performScrollTo().performClick()
        compose.onNodeWithText("Canción local 2").assertIsDisplayed()
        capture("automix-queue")
    }
    @Test fun emptyAutoMixShowsAnErrorAndDoesNotClearTheCurrentQueue() {
        start()
        compose.runOnIdle { vm.startAutoMix(emptyList(), app.userLibrary.state.value) }
        compose.waitUntil(5000) { compose.runOnIdle { !vm.state.value.isCreatingMix } }
        compose.onNodeWithText("No hay canciones locales disponibles para crear una mezcla.").performScrollTo().assertIsDisplayed()
        compose.runOnIdle { assertEquals(listOf("1", "2", "3"), vm.state.value.queue.map { it.id }) }
    }
    private fun capture(name: String) {
        val bitmap = compose.runOnIdle {
            val view = compose.activity.window.decorView
            Bitmap.createBitmap(view.width, view.height, Bitmap.Config.ARGB_8888).also { view.draw(Canvas(it)) }
        }
        val file = File("build/reports/b07-visual/$name.png")
        file.parentFile!!.mkdirs()
        file.outputStream().use { assertTrue(bitmap.compress(Bitmap.CompressFormat.PNG, 100, it)) }
    }
}
