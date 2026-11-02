// Aurelis © 2026. GPL-3.0-or-later
package com.music.aurelis.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun CoverPlaceholderWidget(
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier.size(48.dp),
        shape = MaterialTheme.shapes.small,
        color = MaterialTheme.colorScheme.surfaceVariant
    ) {
        Box(contentAlignment = androidx.compose.ui.Alignment.Center) {
            Text(text = "🎵", style = MaterialTheme.typography.bodyMedium)
        }
    }
}
