// Aurelis © 2026. GPL-3.0-or-later
package com.music.aurelis.ui.theme

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

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
            // TODO: Decodificar la artwork vía Coil y construir ArtworkColors
            // a partir de su paleta. Hasta entonces, DynamicAurelisTheme cae
            // en su fallback oscuro.
            _artworkColors.value = null
        }
    }

    fun clear() {
        lastArtworkUri = null
        _artworkColors.value = null
    }
}
