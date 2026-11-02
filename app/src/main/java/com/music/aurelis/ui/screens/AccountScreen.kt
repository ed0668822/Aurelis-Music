// Aurelis © 2026. GPL-3.0-or-later
package com.music.aurelis.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun AccountScreen(
    userName: String,
    onUserNameChange: (String) -> Unit,
    onBack: () -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(text = "Local Account Settings", style = MaterialTheme.typography.titleLarge)
            Spacer(modifier = Modifier.height(16.dp))
            OutlinedTextField(
                value = userName,
                onValueChange = onUserNameChange,
                label = { Text("Local Display Name") },
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(modifier = Modifier.height(16.dp))
            Button(onClick = onBack) {
                Text("Save & Return")
            }
        }
    }
}
