// Aurelis © 2026. GPL-3.0-or-later
package com.music.aurelis.ui.screens

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.DialogProperties
import coil3.compose.AsyncImage
import com.music.aurelis.BuildConfig
import com.music.aurelis.data.ProfileAvatars
import kotlinx.coroutines.launch
import kotlinx.coroutines.CancellationException
import com.music.aurelis.data.model.ProfileId
import com.music.aurelis.data.ProfileRepository
import com.music.aurelis.data.SettingsRepository
import com.music.aurelis.data.lyrics.LyricsSource
import com.music.aurelis.ui.components.*

@Composable
fun SettingsScreen(isVisible: Boolean, onDismiss: () -> Unit, onOpenEqualizer: () -> Unit = {},
    localSongs: List<com.music.aurelis.data.model.LocalSong> = emptyList(), onOpenFolder: (String) -> Unit = {}) {
    if (!isVisible) return
    val context = LocalContext.current
    val profileState by ProfileRepository.state.collectAsState()
    val active = profileState.profile
    val name = active.name
    val photo = active.avatar
    val scope = rememberCoroutineScope()
    var editing by remember { mutableStateOf(false) }
    var translation by remember { mutableStateOf(false) }
    var profileError by remember { mutableStateOf<String?>(null) }
    var importing by remember { mutableStateOf(false) }
    var photoProfile by rememberSaveable { mutableStateOf(active.id.value) }
    fun change(action: () -> Unit) {
        try { action(); profileError = null } catch (error: Exception) { profileError = error.message ?: "No se pudo guardar el perfil." }
    }
    val picker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) scope.launch {
            importing = true
            try { ProfileAvatars.import(context, ProfileId(photoProfile), uri); profileError = null }
            catch (cancelled: CancellationException) { throw cancelled }
            catch (error: Exception) { profileError = error.message ?: "No se pudo guardar la imagen." }
            finally { importing = false }
        }
    }
    if (profileState.readOnly) {
        Column(Modifier.fillMaxSize().statusBarsPadding().padding(24.dp)) {
            Text("Perfil en modo de solo lectura", style = MaterialTheme.typography.titleLarge)
            Text(profileState.error ?: "Reinicia la app para volver a intentarlo. Los datos se han conservado.")
            TextButton(onClick = onDismiss) { Text("Atrás") }
        }
        return
    }
    var libraryError by remember { mutableStateOf<String?>(null) }
    var browsingFolders by remember { mutableStateOf(false) }
    val allFolders by SettingsRepository.useAllAudioFolders.collectAsState()
    val chosenFolders by SettingsRepository.localMusicFolders.collectAsState()
    val folderPicker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocumentTree()) { uri ->
        if (uri != null) try { com.music.aurelis.data.LocalFolderRepository.add(context, uri); libraryError = null }
        catch (error: Exception) { libraryError = error.message ?: "No se pudo seleccionar la carpeta." }
    }
    val appFont by SettingsRepository.appFont.collectAsState()
    val mode by SettingsRepository.themeMode.collectAsState()
    val glass by SettingsRepository.liquidGlass.collectAsState()
    val motion by SettingsRepository.reduceAnimation.collectAsState()
    val reduceBlur by SettingsRepository.reduceDynamicBlur.collectAsState()
    val sync by SettingsRepository.syncedLyrics.collectAsState()
    val lyricBlur by SettingsRepository.blurUnfocusedLyrics.collectAsState()
    val filter by SettingsRepository.filterNonMusicAudio.collectAsState()
    val sources by SettingsRepository.lyricsSources.collectAsState()
    Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background, contentColor = MaterialTheme.colorScheme.onBackground) {
    Column(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background).statusBarsPadding()
        .navigationBarsPadding().verticalScroll(rememberScrollState())) {
        Row(Modifier.fillMaxWidth().padding(8.dp), verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onDismiss) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Atrás") }
            Text("Ajustes", style = MaterialTheme.typography.headlineLarge, fontWeight = FontWeight.Bold)
        }
        SettingsGroup(header = "Perfil del dispositivo", topSpacing = 0.dp) {
            Row(Modifier.fillMaxWidth().clickable(enabled = !importing) { photoProfile = active.id.value; picker.launch(arrayOf("image/*")) }.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.size(60.dp).clip(CircleShape).background(MaterialTheme.colorScheme.surfaceVariant), contentAlignment = Alignment.Center) {
                    Icon(Icons.Rounded.Person, null)
                    if (photo != null) AsyncImage(photo, null, Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
                }
                Column(Modifier.padding(start = 16.dp)) { Text(name, style = MaterialTheme.typography.titleLarge); Text("Toca para cambiar foto", style = MaterialTheme.typography.bodySmall) }
            }
            if (importing) Text("Guardando imagen…", Modifier.padding(16.dp))
            if (photo != null) SettingsRow(Icons.Rounded.Delete, "Quitar foto", onClick = { change { ProfileAvatars.remove(active.id) } })
            (profileError ?: profileState.error)?.let { Text(it, Modifier.padding(16.dp), color = MaterialTheme.colorScheme.error) }
            SettingsRow(Icons.Rounded.Edit, "Nombre", value = name, onClick = { editing = true })
        }
        SettingsGroup(header = "Audio local") {
            SettingsRow(Icons.Rounded.Tune, "Equalizer", "Ecualizador, balance y amplitud estéreo", onClick = onOpenEqualizer)
            SettingsRow(Icons.Rounded.AudioFile, "Calidad del archivo", "Se reproduce el archivo disponible. Cambiar una preferencia no convierte su calidad ni garantiza Dolby Atmos.")
            SettingsRow(Icons.Rounded.Shuffle, "AutoMix local", "Crea una cola desde Inicio con tu biblioteca e historial. Crossfade y reproducción continua automática no están activados.")
        }
        SettingsGroup(header = "Apariencia") {
            Column(Modifier.padding(16.dp)) {
                Text("Tipografía", style = MaterialTheme.typography.bodyLarge)
                SegmentedControl(listOf("Sistema", "Vazirmatn"), appFont.ordinal, onSelect = { SettingsRepository.setAppFont(com.music.aurelis.data.AppFont.entries[it]) })
                Text("Tema", style = MaterialTheme.typography.bodyLarge)
                SegmentedControl(listOf("Sistema", "Claro", "Oscuro"), mode, SettingsRepository::setThemeMode)
            }
            SettingsToggle("Reducir animaciones", "Detiene los efectos continuos y elimina transiciones del reproductor.", motion, SettingsRepository::setReduceAnimation)
            SettingsToggle("Reducir desenfoque", "Usa superficies sólidas y evita desenfoques dinámicos.", reduceBlur, SettingsRepository::setReduceDynamicBlur)
            SettingsToggle("Liquid Glass", "Superficies translúcidas y reflejos ligeros; se desactiva al reducir desenfoque.", glass, SettingsRepository::setLiquidGlass)
        }
        SettingsGroup(header = "Letras") {
            SettingsToggle("Letras sincronizadas", "Resalta y sigue las líneas cuando hay marcas de tiempo.", sync, SettingsRepository::setSyncedLyrics)
            SettingsToggle("Desenfocar líneas inactivas", "Solo durante seguimiento sincronizado y sin reducción de desenfoque.", lyricBlur, SettingsRepository::setBlurUnfocusedLyrics)
            SettingsToggle("Buscar letras en LRCLIB", "La búsqueda se inicia manualmente desde el reproductor y envía título y artista.", LyricsSource.LRCLIB in sources) {
                SettingsRepository.setLyricsSources(if (it) sources + LyricsSource.LRCLIB else sources - LyricsSource.LRCLIB)
            }
            SettingsRow(Icons.Rounded.Translate, "Traducción", "Configura servidor e idiomas. Guardar aquí no envía letras.", onClick = { translation = true })
        }
        SettingsGroup(header = "Biblioteca") {
            SettingsToggle("Usar todas las carpetas de audio", "Biblioteca de audio indexada por Android.", allFolders) {
                try { SettingsRepository.setLocalFolderSelection(chosenFolders, it); libraryError = null }
                catch (error: Exception) { libraryError = error.message }
            }
            SettingsRow(Icons.Rounded.Folder, "Elegir carpeta local", "Solo música indexada por Android en el dispositivo o tarjeta SD.", onClick = { folderPicker.launch(null) })
            chosenFolders.sorted().forEach { uri ->
                val label = com.music.aurelis.data.LocalFolderRepository.label(uri)
                Row(Modifier.fillMaxWidth().padding(horizontal = 16.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text(label, Modifier.weight(1f))
                    IconButton(onClick = { try { com.music.aurelis.data.LocalFolderRepository.remove(context, uri); libraryError = null }
                        catch (error: Exception) { libraryError = error.message } }) { Icon(Icons.Rounded.Close, "Dejar de usar $label") }
                }
            }
            if (!allFolders && chosenFolders.isEmpty()) Text("No hay carpetas seleccionadas.", Modifier.padding(16.dp))
            libraryError?.let { Text(it, Modifier.padding(16.dp), color = MaterialTheme.colorScheme.error) }
            SettingsRow(Icons.Rounded.FolderOpen, "Ver carpetas de la biblioteca", onClick = { browsingFolders = true })
            SettingsToggle("Filtrar audio no musical", "Oculta clips, notas de voz y sonidos del sistema.", filter, SettingsRepository::setFilterNonMusicAudio)
        }
        SettingsGroup(header = "Acerca de") {
            Column(Modifier.padding(16.dp)) {
                Text("Aurelis Music", fontWeight = FontWeight.Bold)
                Text(BuildConfig.VERSION_NAME, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text("Aurelis © 2026. GPL-3.0-or-later", style = MaterialTheme.typography.bodySmall)
            }
        }
        Spacer(Modifier.height(24.dp))
    }
    }
    if (editing) {
        var draft by remember { mutableStateOf(name) }
        var error by remember { mutableStateOf<String?>(null) }
        AlertDialog(modifier = Modifier.width(320.dp), properties = DialogProperties(usePlatformDefaultWidth = false),
            onDismissRequest = { editing = false }, title = { Text("Editar nombre") },
            text = { Column { OutlinedTextField(draft, { draft = it.take(60) }, singleLine = true, label = { Text("Nombre del perfil") }); error?.let { Text(it, color = MaterialTheme.colorScheme.error) } } },
            confirmButton = { TextButton(enabled = draft.isNotBlank(), onClick = {
                try {
                    ProfileRepository.rename(draft)
                    editing = false
                } catch (failure: Exception) { error = failure.message ?: "No se pudo guardar." }
            }) { Text("Guardar") } },
            dismissButton = { TextButton(onClick = { editing = false }) { Text("Cancelar") } })
    }
    if (browsingFolders) AlertDialog(modifier = Modifier.width(320.dp), properties = DialogProperties(usePlatformDefaultWidth = false),
        onDismissRequest = { browsingFolders = false }, title = { Text("Carpetas de la biblioteca") },
        text = { Column(Modifier.heightIn(max = 400.dp).verticalScroll(rememberScrollState())) {
            val folders = com.music.aurelis.data.LocalFolderRepository.groups(localSongs)
            if (folders.isEmpty()) Text("No hay canciones indexadas en la biblioteca seleccionada.")
            folders.forEach { folder -> TextButton(onClick = { browsingFolders = false; onOpenFolder(folder.key) }) { Text("${folder.label} · ${folder.songs.size}") } }
        } }, confirmButton = { TextButton(onClick = { browsingFolders = false }) { Text("Cerrar") } })
    if (translation) TranslationLanguageDialog(onDismiss = { translation = false }, onTranslate = { translation = false }, sendOnConfirm = false)
}

@Composable
private fun SettingsToggle(title: String, subtitle: String, checked: Boolean, change: (Boolean) -> Unit) {
    com.music.aurelis.ui.components.SettingsSubRow(title, checked, change,
        subtitle = subtitle, modifier = Modifier.semantics { contentDescription = title })
}
