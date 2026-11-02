// Aurelis © 2026. GPL-3.0-or-later
package com.music.aurelis.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun ListenTogetherSheets(
    onDismiss: () -> Unit = {}
) {
    Column(modifier = Modifier.fillMaxWidth().padding(16.dp)) {
        Text(text = "Listen Together Sheets", style = MaterialTheme.typography.titleLarge)
        Spacer(modifier = Modifier.height(8.dp))
        Text(text = "Manage local group listening session options.", style = MaterialTheme.typography.bodyMedium)
        Spacer(modifier = Modifier.height(24.dp))
        Button(onClick = onDismiss) {
            Text("Close")
        }
    }
}
