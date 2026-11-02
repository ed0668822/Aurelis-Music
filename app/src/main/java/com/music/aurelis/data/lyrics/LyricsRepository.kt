// Aurelis © 2026. GPL-3.0-or-later
package com.music.aurelis.data.lyrics

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class LyricsRepository {
    private val _lyrics = MutableStateFlow<LyricsResult?>(null)
    val lyrics: StateFlow<LyricsResult?> = _lyrics.asStateFlow()

    private val _translation = MutableStateFlow<LyricsResult?>(null)
    val translation: StateFlow<LyricsResult?> = _translation.asStateFlow()

    private val _lines = MutableStateFlow<List<LyricLine>>(emptyList())
    val lines: StateFlow<List<LyricLine>> = _lines.asStateFlow()

    private val _source = MutableStateFlow<String>("")
    val source: StateFlow<String> = _source.asStateFlow()

    fun flush() {
        _lyrics.value = null
        _translation.value = null
        _lines.value = emptyList()
        _source.value = ""
    }

    fun record(lines: List<LyricLine>, source: String) {
        _lines.value = lines
        _source.value = source
    }
}