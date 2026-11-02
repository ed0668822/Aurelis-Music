// Aurelis © 2026. GPL-3.0-or-later
package com.music.aurelis.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun SpotifyImportAlert(
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Local Music Notice") },
        text = { Text("Aurelis Music exclusively supports local music library playback.") },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Understood")
            }
        }
    )
}
