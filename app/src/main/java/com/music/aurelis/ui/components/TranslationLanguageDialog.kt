// Aurelis © 2026. GPL-3.0-or-later
package com.music.aurelis.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.DialogProperties
import com.music.aurelis.data.SettingsRepository
import com.music.aurelis.data.lyrics.LyricsTranslation
import com.music.aurelis.data.lyrics.TranslationLanguages

@Composable
fun TranslationLanguageDialog(onDismiss: () -> Unit, onTranslate: (String) -> Unit) {
    var endpoint by rememberSaveable { mutableStateOf(SettingsRepository.translationEndpoint.value) }
    var source by rememberSaveable { mutableStateOf(SettingsRepository.translationSourceLanguage.value) }
    var target by rememberSaveable { mutableStateOf(SettingsRepository.translationLanguage.value.takeIf { it in TranslationLanguages.supportedLanguages } ?: "es") }
    AlertDialog(modifier = Modifier.width(320.dp), properties = DialogProperties(usePlatformDefaultWidth = false),
        onDismissRequest = onDismiss, title = { Text("Traducir letras") }, text = {
            Column(Modifier.heightIn(max = 400.dp).verticalScroll(rememberScrollState())) {
                Text("Las letras se enviarán al servidor que elijas al pulsar Traducir. Usa un servidor de confianza compatible con LibreTranslate, sin clave API.")
                OutlinedTextField(endpoint, { endpoint = it }, label = { Text("Servidor HTTPS") }, singleLine = true)
                Text("Idioma original")
                LanguageChoice("auto", "Detectar automáticamente", source) { source = it }
                TranslationLanguages.supportedLanguages.forEach { (id, name) -> LanguageChoice(id, name, source) { source = it } }
                Text("Traducir al idioma")
                TranslationLanguages.supportedLanguages.forEach { (id, name) -> LanguageChoice(id, name, target) { target = it } }
            }
        }, confirmButton = { TextButton(enabled = LyricsTranslation.normaliseEndpoint(endpoint) != null,
            onClick = { SettingsRepository.setTranslationService(endpoint, source); SettingsRepository.setTranslationLanguage(target); onTranslate(target) }) { Text("Traducir") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancelar") } })
}

@Composable
private fun LanguageChoice(id: String, name: String, current: String, onSelect: (String) -> Unit) {
    TextButton(onClick = { onSelect(id) }, modifier = Modifier.fillMaxWidth()) {
        Text((if (id == current) "✓ " else "") + name)
    }
}
