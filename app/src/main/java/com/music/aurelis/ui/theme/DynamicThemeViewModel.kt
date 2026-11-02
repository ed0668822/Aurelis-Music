// Aurelis © 2026. GPL-3.0-or-later
package com.music.aurelis.ui.theme

import android.app.Application
import androidx.compose.ui.graphics.Color
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class ArtworkColors(
    val primary: Color = Color.Transparent,
    val secondary: Color = Color.Transparent,
    val tertiary: Color = Color.Transparent,
    val background: Color = Color.Transparent,
)

object ArtworkPalette {
    fun extractColors(context: Application, artworkUri: String): ArtworkColors? = null
}

class DynamicThemeViewModel(application: Application) : AndroidViewModel(application) {
    private val _artworkColors = MutableStateFlow<ArtworkColors?>(null)
    val artworkColors: StateFlow<ArtworkColors?> = _artworkColors.asStateFlow()

    private var lastArtworkUri: String? = null

    fun extractForArtwork(artworkUri: String?) {
        if (artworkUri == lastArtworkUri) return
        lastArtworkUri = artworkUri

        if (artworkUri.isNullOrBlank()) {
            _artworkColors.value = null
            return
        }

        viewModelScope.launch {
            _artworkColors.value = ArtworkPalette.extractColors(getApplication(), artworkUri)
        }
    }

    fun clear() {
        lastArtworkUri = null
        _artworkColors.value = null
    }
}

