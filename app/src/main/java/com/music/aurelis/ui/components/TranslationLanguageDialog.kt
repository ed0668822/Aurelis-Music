// SPDX-FileCopyrightText: 2026 Aurelis
// SPDX-License-Identifier: LGPL-3.0-or-later
package com.music.aurelis.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.DialogProperties
import com.music.aurelis.data.SettingsRepository
import com.music.aurelis.data.lyrics.LyricsTranslation
import com.music.aurelis.data.lyrics.TranslationLanguages

/** A public provider without URL configuration; a personal server is optional. */
@Composable
fun TranslationLanguageDialog(onDismiss: () -> Unit, onTranslate: (String) -> Unit, sendOnConfirm: Boolean = true) {
    var provider by rememberSaveable { mutableStateOf(SettingsRepository.translationProvider.value) }
    var endpoint by rememberSaveable { mutableStateOf(SettingsRepository.translationEndpoint.value) }
    var source by rememberSaveable { mutableStateOf(SettingsRepository.translationSourceLanguage.value) }
    var target by rememberSaveable {
        mutableStateOf(SettingsRepository.translationLanguage.value
            .takeIf { it in TranslationLanguages.supportedLanguages } ?: "es")
    }
    val valid = provider == "public" || LyricsTranslation.normaliseEndpoint(endpoint) != null
    AlertDialog(
        modifier = Modifier.width(320.dp).testTag("lyrics-translation-dialog"),
        properties = DialogProperties(usePlatformDefaultWidth = false),
        onDismissRequest = onDismiss,
        title = { Text("Traducir letras") },
        text = {
            Column(Modifier.heightIn(max = 400.dp).verticalScroll(rememberScrollState())) {
                Text("Al confirmar la traducción, el texto de la canción se enviará al proveedor elegido. Los tiempos originales se conservan.")
                Text("Proveedor")
                LanguageChoice("public", "Público (predeterminado; sin URL)", provider) { provider = it }
                LanguageChoice("custom", "Servidor propio (opcional)", provider) { provider = it }
                if (provider == "custom") {
                    OutlinedTextField(endpoint, { endpoint = it },
                        label = { Text("Servidor LibreTranslate HTTPS") },
                        modifier = Modifier.testTag("translation-custom-endpoint"), singleLine = true)
                }
                Text("Idioma original")
                LanguageChoice("auto", "Detectar automáticamente", source) { source = it }
                TranslationLanguages.supportedLanguages.forEach { (id, name) ->
                    LanguageChoice(id, name, source) { source = it }
                }
                Text("Traducir al idioma")
                TranslationLanguages.supportedLanguages.forEach { (id, name) ->
                    LanguageChoice(id, name, target) { target = it }
                }
            }
        },
        confirmButton = {
            TextButton(enabled = valid, onClick = {
                if (provider == "custom") SettingsRepository.setTranslationService(endpoint, source)
                else SettingsRepository.setTranslationSourceLanguage(source)
                SettingsRepository.setTranslationProvider(provider)
                SettingsRepository.setTranslationLanguage(target)
                if (sendOnConfirm) onTranslate(target) else onDismiss()
            }) { Text(if (sendOnConfirm) "Traducir" else "Guardar") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancelar") },
    )
}

@Composable
private fun LanguageChoice(id: String, name: String, current: String, onSelect: (String) -> Unit) {
    TextButton(onClick = { onSelect(id) }, modifier = Modifier.fillMaxWidth()) {
        Text((if (id == current) "✓ " else "") + name)
    }
}
