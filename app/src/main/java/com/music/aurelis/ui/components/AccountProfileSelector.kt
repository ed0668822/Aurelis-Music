// Aurelis © 2026. GPL-3.0-or-later
package com.music.aurelis.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun AccountProfileSelector(
    currentName: String,
    currentPhotoUrl: String?,
    onNameChange: (String) -> Unit,
    onPhotoSelected: () -> Unit,
    onDismiss: () -> Unit
) {
    var nameText by remember { mutableStateOf(currentName) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Perfil de Cuenta Local") },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                OutlinedTextField(
                    value = nameText,
                    onValueChange = { nameText = it },
                    label = { Text("Nombre de usuario local") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Button(
                    onClick = onPhotoSelected,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Seleccionar Foto Local")
                }
            }
        },
        confirmButton = {
            Button(onClick = {
                onNameChange(nameText)
                onDismiss()
            }) {
                Text("Guardar")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancelar")
            }
        }
    )
}
