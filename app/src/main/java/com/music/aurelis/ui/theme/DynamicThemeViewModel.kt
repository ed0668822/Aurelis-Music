// SPDX-FileCopyrightText: 2026 Aurelis
// SPDX-License-Identifier: LGPL-3.0-or-later
package com.music.aurelis.ui.theme

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

/**
 * The active player observes a single theme. Cancel the previous decode before
 * publishing a new album's colors, including rapid next/previous interactions.
 */
class DynamicThemeViewModel(application: Application) : AndroidViewModel(application) {
    private val _artworkColors = MutableStateFlow<ArtworkColors?>(null)
    val artworkColors: StateFlow<ArtworkColors?> = _artworkColors.asStateFlow()

    private var lastArtworkUri: String? = null
    private var decodeJob: Job? = null

    fun extractForArtwork(artworkUri: String?) {
        if (artworkUri == lastArtworkUri) return
        decodeJob?.cancel()
        decodeJob = null
        lastArtworkUri = artworkUri
        _artworkColors.value = null
        if (artworkUri.isNullOrBlank()) return

        decodeJob = viewModelScope.launch {
            val colors = try {
                extractArtworkThemeColors(getApplication(), artworkUri)
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (_: Exception) {
                null
            }
            if (isActive && lastArtworkUri == artworkUri) {
                _artworkColors.value = colors
            }
        }
    }

    fun clear() {
        decodeJob?.cancel()
        decodeJob = null
        lastArtworkUri = null
        _artworkColors.value = null
    }
}