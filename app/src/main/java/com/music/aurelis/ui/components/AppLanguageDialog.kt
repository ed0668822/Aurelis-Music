// Aurelis © 2026. GPL-3.0-or-later
package com.music.aurelis.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.res.stringResource
import com.music.aurelis.R
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.os.LocaleListCompat
import java.util.Locale

@Composable
fun AppLanguageDialog(
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.settings_app_language)) },
        text = { Column {
            val selected = AppCompatDelegate.getApplicationLocales().toLanguageTags()
            listOf("" to stringResource(R.string.settings_system), "en" to "English", "es" to "Español", "pt" to "Português",
                "fr" to "Français", "de" to "Deutsch", "ja" to "日本語").forEach { (tag, label) ->
                SettingsRow(title = label, onClick = {
                    AppCompatDelegate.setApplicationLocales(LocaleListCompat.forLanguageTags(tag))
                    onDismiss()
                }, trailing = { if (selected == tag) Text("✓") })
            }
        } },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(android.R.string.ok))
            }
        }
    )
}
