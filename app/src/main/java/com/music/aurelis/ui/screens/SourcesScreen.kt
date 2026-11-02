// Aurelis © 2026. GPL-3.0-or-later
package com.music.aurelis.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fn SourcesScreen(
    onSelectFolder: () -> Unit = {}
) {
    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        Text(text = "Music Sources & Folders", style = MaterialTheme.typography.titleLarge)
        Spacer(modifier = Modifier.height(8.dp))
        Text(text = "Manage local directories scanned for music files.", style = MaterialTheme.typography.bodyMedium)
        Spacer(modifier = Modifier.height(24.dp))
        Button(onClick = onSelectFolder) {
            Text("Add Local Folder")
        }
    }
}
