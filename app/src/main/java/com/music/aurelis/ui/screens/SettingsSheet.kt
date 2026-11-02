// Aurelis © 2026. GPL-3.0-or-later
package com.music.aurelis.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun SettingsSheet(
    onDismiss: () -> Unit = {}
) {
    Column(modifier = Modifier.fillMaxWidth().padding(16.dp)) {
        Text(text = "Aurelis Settings", style = MaterialTheme.typography.titleLarge)
        Spacer(modifier = Modifier.height(8.dp))
        Text(text = "Configure local playback, storage, and audio preferences.", style = MaterialTheme.typography.bodyMedium)
        Spacer(modifier = Modifier.height(24.dp))
        Button(onClick = onDismiss) {
            Text("Close")
        }
    }
}
