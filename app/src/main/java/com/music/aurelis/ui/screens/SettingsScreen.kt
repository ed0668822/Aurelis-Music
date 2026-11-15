// Aurelis © 2026. GPL-3.0-or-later
package com.music.aurelis.ui.screens

import androidx.activity.compose.BackHandler
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
import com.music.aurelis.R
import com.music.aurelis.BuildConfig
import com.music.aurelis.data.ProfileAvatars
import kotlinx.coroutines.launch
import kotlinx.coroutines.CancellationException
import com.music.aurelis.data.model.ProfileId
import com.music.aurelis.data.ProfileRepository
import com.music.aurelis.data.SettingsRepository
import com.music.aurelis.data.lyrics.LyricsSource
import com.music.aurelis.ui.components.*
import com.music.aurelis.ui.navigation.SettingsCategory
import com.music.aurelis.ui.icons.AurelisIcons
import androidx.compose.ui.semantics.clearAndSetSemantics

@Composable
fun SettingsScreen(isVisible: Boolean, onDismiss: () -> Unit, onOpenEqualizer: () -> Unit = {},
    localSongs: List<com.music.aurelis.data.model.LocalSong> = emptyList(), onOpenFolder: (String) -> Unit = {},
    section: SettingsCategory? = null, onOpenCategory: ((SettingsCategory) -> Unit)? = null,
    onOpenReplay: (() -> Unit)? = null) {
    if (!isVisible) return
    var localSection by rememberSaveable { mutableStateOf<SettingsCategory?>(null) }
    var query by rememberSaveable { mutableStateOf("") }
    var language by remember { mutableStateOf(false) }
    val category = section ?: localSection
    fun openCategory(next: SettingsCategory?) {
        if (next != null && onOpenCategory != null) onOpenCategory(next)
        else if (section != null) onDismiss()
        else { localSection = next; query = "" }
    }
    BackHandler(section == null && localSection != null) { localSection = null }
    val context = LocalContext.current
    val profileState by ProfileRepository.state.collectAsState()
    val active = profileState.profile
    val name = active.name
    val photo = active.avatar
    val matchingRows = LocalSetting.entries.filter {
        (it != LocalSetting.REMOVE_PHOTO || photo != null) && it.matches(context.resources, query)
    }.toSet()
    fun shows(value: SettingsCategory): Boolean = category == value ||
        (category == null && query.isNotBlank() && matchingRows.any { it.category == value })
    fun showRow(value: LocalSetting): Boolean = category != null || query.isBlank() || value in matchingRows
    val scrollState = rememberScrollState()
    var lastQuery by rememberSaveable { mutableStateOf(query) }
    LaunchedEffect(query) {
        if (lastQuery != query) { scrollState.scrollTo(0); lastQuery = query }
    }
    val scope = rememberCoroutineScope()
    var editing by remember { mutableStateOf(false) }
    var translation by remember { mutableStateOf(false) }
    var profileError by remember { mutableStateOf<String?>(null) }
    var importing by remember { mutableStateOf(false) }
    var photoProfile by rememberSaveable { mutableStateOf(active.id.value) }
    fun change(action: () -> Unit) {
        try { action(); profileError = null } catch (error: Exception) { profileError = error.message ?: context.getString(R.string.settings_profile_save_error) }
    }
    val picker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) scope.launch {
            importing = true
            try { ProfileAvatars.import(context, ProfileId(photoProfile), uri); profileError = null }
            catch (cancelled: CancellationException) { throw cancelled }
            catch (error: Exception) { profileError = error.message ?: context.getString(R.string.settings_photo_save_error) }
            finally { importing = false }
        }
    }
    if (profileState.readOnly) {
        Column(Modifier.fillMaxSize().statusBarsPadding().padding(24.dp)) {
            Text(context.getString(R.string.settings_profile_read_only), style = MaterialTheme.typography.titleLarge)
            Text(profileState.error ?: context.getString(R.string.settings_profile_read_only_hint))
            TextButton(onClick = onDismiss) { Text(context.getString(R.string.settings_back)) }
        }
        return
    }
    var libraryError by remember { mutableStateOf<String?>(null) }
    var browsingFolders by remember { mutableStateOf(false) }
    val allFolders by SettingsRepository.useAllAudioFolders.collectAsState()
    val chosenFolders by SettingsRepository.localMusicFolders.collectAsState()
    val folderPicker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocumentTree()) { uri ->
        if (uri != null) try { com.music.aurelis.data.LocalFolderRepository.add(context, uri); libraryError = null }
        catch (error: Exception) { libraryError = error.message ?: context.getString(R.string.settings_folder_error) }
    }
    val appFont by SettingsRepository.appFont.collectAsState()
    val mode by SettingsRepository.themeMode.collectAsState()
    val glass by SettingsRepository.liquidGlass.collectAsState()
    val motion by SettingsRepository.reduceAnimation.collectAsState()
    val reduceBlur by SettingsRepository.reduceDynamicBlur.collectAsState()
    val hapticFeedback by SettingsRepository.hapticFeedback.collectAsState()
    val sync by SettingsRepository.syncedLyrics.collectAsState()
    val lyricBlur by SettingsRepository.blurUnfocusedLyrics.collectAsState()
    val filter by SettingsRepository.filterNonMusicAudio.collectAsState()
    val sources by SettingsRepository.lyricsSources.collectAsState()
    Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background, contentColor = MaterialTheme.colorScheme.onBackground) {
    Box(Modifier.fillMaxSize()) {
    Column(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)
        .navigationBarsPadding().then(if (editing || translation || browsingFolders || language) Modifier.clearAndSetSemantics { } else Modifier)
        .verticalScroll(scrollState).padding(top = topBarContentPadding())) {
        Text(category?.let { settingsTitle(it, context.resources) } ?: context.getString(R.string.navigation_settings), style = MaterialTheme.typography.displayLarge,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp))
        if (category == null) SearchField(query, { query = it }, onSubmit = {},
            placeholder = context.getString(R.string.settings_search), modifier = Modifier.padding(horizontal = 20.dp))
        (profileError ?: profileState.error)?.let { Text(it, Modifier.padding(16.dp), color = MaterialTheme.colorScheme.error) }
        if (category == null && query.isBlank() || shows(SettingsCategory.GENERAL)) {
        SettingsGroup(header = context.getString(R.string.settings_profile_group), topSpacing = 0.dp) {
            if (showRow(LocalSetting.PHOTO)) Row(Modifier.fillMaxWidth().clickable(enabled = !importing) { photoProfile = active.id.value; picker.launch(arrayOf("image/*")) }.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.size(60.dp).clip(CircleShape).background(MaterialTheme.colorScheme.surfaceVariant), contentAlignment = Alignment.Center) {
                    Icon(Icons.Rounded.Person, null)
                    if (photo != null) AsyncImage(photo, null, Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
                }
                Column(Modifier.padding(start = 16.dp)) { Text(name, style = MaterialTheme.typography.titleLarge); Text(context.getString(R.string.settings_photo_hint), style = MaterialTheme.typography.bodySmall) }
            }
            if (importing) Text(context.getString(R.string.settings_photo_saving), Modifier.padding(16.dp))
            if (photo != null && showRow(LocalSetting.REMOVE_PHOTO)) SettingsRow(Icons.Rounded.Delete, context.getString(R.string.settings_photo_remove), onClick = { change { ProfileAvatars.remove(active.id) } })
            if (showRow(LocalSetting.NAME)) SettingsRow(Icons.Rounded.Edit, context.getString(R.string.settings_name), value = name, onClick = { editing = true })
        }
        }
        if (shows(SettingsCategory.AUDIO)) {
        SettingsGroup(header = context.getString(R.string.settings_playback)) {
            if (showRow(LocalSetting.AUDIO_LYRICS)) SettingsRow(AurelisIcons.Lyrics, context.getString(R.string.settings_lyrics), onClick = { openCategory(SettingsCategory.LYRICS) })
            if (showRow(LocalSetting.EQUALIZER)) SettingsRow(AurelisIcons.Equalizer, context.getString(R.string.settings_equalizer), context.getString(R.string.settings_equalizer_hint), onClick = onOpenEqualizer)
            if (showRow(LocalSetting.FILE_QUALITY)) SettingsRow(Icons.Rounded.AudioFile, context.getString(R.string.settings_file_quality), context.getString(R.string.settings_file_quality_hint))
            if (showRow(LocalSetting.AUTOMIX)) SettingsRow(Icons.Rounded.Shuffle, context.getString(R.string.settings_automix), context.getString(R.string.settings_automix_hint))
        }
        }
        if (shows(SettingsCategory.APPEARANCE)) {
        SettingsGroup(header = context.getString(R.string.settings_appearance)) {
            if (showRow(LocalSetting.FONT) || showRow(LocalSetting.THEME)) Column(Modifier.padding(16.dp)) {
                if (showRow(LocalSetting.FONT)) {
                Text(context.getString(R.string.settings_font), style = MaterialTheme.typography.bodyLarge)
                SegmentedControl(listOf(context.getString(R.string.settings_system), "Vazirmatn"), appFont.ordinal, onSelect = { SettingsRepository.setAppFont(com.music.aurelis.data.AppFont.entries[it]) })
                }
                if (showRow(LocalSetting.THEME)) {
                Text(context.getString(R.string.settings_theme), style = MaterialTheme.typography.bodyLarge)
                SegmentedControl(listOf(context.getString(R.string.settings_system), context.getString(R.string.settings_light), context.getString(R.string.settings_dark)), mode, SettingsRepository::setThemeMode)
                }
            }
            if (showRow(LocalSetting.HAPTICS)) SettingsToggle(context.getString(R.string.settings_haptics), context.getString(R.string.settings_haptics_hint), hapticFeedback) { enabled ->
                change { SettingsRepository.setHapticFeedback(enabled) }
            }
            if (showRow(LocalSetting.GLASS)) SettingsToggle(context.getString(R.string.settings_glass), context.getString(R.string.settings_glass_hint), glass, SettingsRepository::setLiquidGlass)
        }
        }
        if (shows(SettingsCategory.PERFORMANCE)) SettingsGroup(header = context.getString(R.string.settings_performance)) {
            if (showRow(LocalSetting.MOTION)) SettingsToggle(context.getString(R.string.settings_reduce_motion), context.getString(R.string.settings_reduce_motion_hint), motion, SettingsRepository::setReduceAnimation)
            if (showRow(LocalSetting.BLUR)) SettingsToggle(context.getString(R.string.settings_reduce_blur), context.getString(R.string.settings_reduce_blur_hint), reduceBlur, SettingsRepository::setReduceDynamicBlur)
        }
        if (shows(SettingsCategory.LYRICS)) {
        SettingsGroup(header = context.getString(R.string.settings_lyrics)) {
            if (showRow(LocalSetting.SYNC)) SettingsToggle(context.getString(R.string.settings_synced_lyrics), context.getString(R.string.settings_synced_lyrics_hint), sync, SettingsRepository::setSyncedLyrics)
            if (showRow(LocalSetting.LYRIC_BLUR)) SettingsToggle(context.getString(R.string.settings_lyric_blur), context.getString(R.string.settings_lyric_blur_hint), lyricBlur, SettingsRepository::setBlurUnfocusedLyrics)
            if (showRow(LocalSetting.LRCLIB)) SettingsToggle(context.getString(R.string.settings_lrclib), context.getString(R.string.settings_lrclib_hint), LyricsSource.LRCLIB in sources) {
                SettingsRepository.setLyricsSources(if (it) sources + LyricsSource.LRCLIB else sources - LyricsSource.LRCLIB)
            }
            if (showRow(LocalSetting.TRANSLATION)) SettingsRow(Icons.Rounded.Translate, context.getString(R.string.settings_translation), context.getString(R.string.settings_translation_hint), onClick = { translation = true })
        }
        }
        if (shows(SettingsCategory.LIBRARY)) {
        SettingsGroup(header = context.getString(R.string.settings_local_music)) {
            if (showRow(LocalSetting.ALL_FOLDERS)) SettingsToggle(context.getString(R.string.settings_all_folders), context.getString(R.string.settings_all_folders_hint), allFolders) {
                try { SettingsRepository.setLocalFolderSelection(chosenFolders, it); libraryError = null }
                catch (error: Exception) { libraryError = error.message }
            }
            if (showRow(LocalSetting.ADD_FOLDER)) SettingsRow(Icons.Rounded.Folder, context.getString(R.string.settings_add_folder), context.getString(R.string.settings_add_folder_hint), onClick = { folderPicker.launch(null) })
            if (showRow(LocalSetting.SELECTED_FOLDERS)) {
            chosenFolders.sorted().forEach { uri ->
                val label = com.music.aurelis.data.LocalFolderRepository.label(uri)
                Row(Modifier.fillMaxWidth().padding(horizontal = 16.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text(label, Modifier.weight(1f))
                    IconButton(onClick = { try { com.music.aurelis.data.LocalFolderRepository.remove(context, uri); libraryError = null }
                        catch (error: Exception) { libraryError = error.message } }) { Icon(Icons.Rounded.Close, context.getString(R.string.settings_folder_remove, label)) }
                }
            }
            if (!allFolders && chosenFolders.isEmpty()) Text(context.getString(R.string.settings_no_folders), Modifier.padding(16.dp))
            }
            libraryError?.let { Text(it, Modifier.padding(16.dp), color = MaterialTheme.colorScheme.error) }
            if (showRow(LocalSetting.BROWSE_FOLDERS)) SettingsRow(Icons.Rounded.FolderOpen, context.getString(R.string.settings_browse_folders), onClick = { browsingFolders = true })
            if (showRow(LocalSetting.FILTER_AUDIO)) SettingsToggle(context.getString(R.string.settings_filter_audio), context.getString(R.string.settings_filter_audio_hint), filter, SettingsRepository::setFilterNonMusicAudio)
        }
        }
        if (shows(SettingsCategory.ABOUT)) {
        SettingsGroup(header = context.getString(R.string.settings_about)) {
            Column(Modifier.padding(16.dp)) {
                Text(context.getString(R.string.app_name), fontWeight = FontWeight.Bold)
                Text(BuildConfig.VERSION_NAME, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text("Aurelis © 2026. GPL-3.0-or-later", style = MaterialTheme.typography.bodySmall)
            }
        }
        }
        if (category == null && query.isBlank()) SettingsGroup {
            settingsCategories.forEachIndexed { index, item ->
                if (index > 0) RowDivider()
                SettingsRow(settingsIcon(item), settingsTitle(item, context.resources), onClick = { openCategory(item) })
            }
        }
        if (shows(SettingsCategory.STORAGE)) SettingsGroup(header = context.getString(R.string.settings_storage)) {
            val known = localSongs.filter { it.sizeBytesKnown }
            Text(context.resources.getQuantityString(R.plurals.settings_song_count, localSongs.size, localSongs.size), Modifier.padding(16.dp))
            Text(context.resources.getQuantityString(R.plurals.settings_known_files, known.size, known.size,
                android.text.format.Formatter.formatFileSize(context, known.sumOf { it.sizeBytes.coerceAtLeast(0) })), Modifier.padding(16.dp))
        }
        if (shows(SettingsCategory.YOUR_DATA)) SettingsGroup(header = context.getString(R.string.settings_your_data)) {
            SettingsRow(AurelisIcons.Clock, context.getString(R.string.settings_replay), enabled = onOpenReplay != null, onClick = onOpenReplay)
        }
        if (shows(SettingsCategory.LANGUAGE)) SettingsGroup(header = context.getString(R.string.settings_language)) {
            SettingsRow(AurelisIcons.Language, context.getString(R.string.settings_app_language), onClick = { language = true })
        }
        if (shows(SettingsCategory.ADVANCED)) SettingsGroup(header = context.getString(R.string.settings_advanced)) {
            Text(context.getString(R.string.settings_library_hint), Modifier.padding(16.dp))
            SettingsRow(AurelisIcons.Folder, context.getString(R.string.settings_local_music), onClick = { openCategory(SettingsCategory.LIBRARY) })
        }
        if (category == null && query.isNotBlank() && matchingRows.isEmpty()) {
            Text(context.getString(R.string.settings_no_results, query), Modifier.padding(20.dp))
        }
        Spacer(Modifier.height(24.dp))
    }
    FrostedTopBar(category?.let { settingsTitle(it, context.resources) } ?: context.getString(R.string.navigation_settings), { scrollState.value > 0 }, transparentBackdrop = true, artworkPageChrome = false,
        onBack = { if (category != null) openCategory(null) else onDismiss() })
    }
    }
    if (language) AppLanguageDialog { language = false }
    if (editing) {
        var draft by remember { mutableStateOf(name) }
        var error by remember { mutableStateOf<String?>(null) }
        AlertDialog(modifier = Modifier.width(320.dp), properties = DialogProperties(usePlatformDefaultWidth = false),
            onDismissRequest = { editing = false }, title = { Text(context.getString(R.string.settings_edit_name)) },
            text = { Column { OutlinedTextField(draft, { draft = it.take(60) }, singleLine = true, label = { Text(context.getString(R.string.settings_profile_name)) }); error?.let { Text(it, color = MaterialTheme.colorScheme.error) } } },
            confirmButton = { TextButton(enabled = draft.isNotBlank(), onClick = {
                try {
                    ProfileRepository.rename(draft)
                    editing = false
                } catch (failure: Exception) { error = failure.message ?: context.getString(R.string.settings_save_error) }
            }) { Text(context.getString(R.string.settings_save)) } },
            dismissButton = { TextButton(onClick = { editing = false }) { Text(context.getString(R.string.settings_cancel)) } })
    }
    if (browsingFolders) AlertDialog(modifier = Modifier.width(320.dp), properties = DialogProperties(usePlatformDefaultWidth = false),
        onDismissRequest = { browsingFolders = false }, title = { Text(context.getString(R.string.settings_library_folders)) },
        text = { Column(Modifier.heightIn(max = 400.dp).verticalScroll(rememberScrollState())) {
            val folders = com.music.aurelis.data.LocalFolderRepository.groups(localSongs)
            if (folders.isEmpty()) Text(context.getString(R.string.settings_no_indexed))
            folders.forEach { folder -> TextButton(onClick = { browsingFolders = false; onOpenFolder(folder.key) }) { Text("${folder.label} · ${folder.songs.size}") } }
        } }, confirmButton = { TextButton(onClick = { browsingFolders = false }) { Text(context.getString(R.string.settings_close)) } })
    if (translation) TranslationLanguageDialog(onDismiss = { translation = false }, onTranslate = { translation = false }, sendOnConfirm = false)
}

@Composable
private fun SettingsToggle(title: String, subtitle: String, checked: Boolean, change: (Boolean) -> Unit) {
    com.music.aurelis.ui.components.SettingsSubRow(title, checked, change,
        subtitle = subtitle, modifier = Modifier.semantics { contentDescription = title })
}
