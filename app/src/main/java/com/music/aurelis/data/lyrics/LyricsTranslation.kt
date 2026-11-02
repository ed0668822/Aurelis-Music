// Aurelis © 2026. GPL-3.0-or-later
package com.music.aurelis.data.lyrics

import com.music.aurelis.data.model.LocalSong

object LyricsTranslation {
    sealed interface Result {
        data class Translated(
            val lines: List<LyricLine>,
            val sourceLanguage: String,
            val fromCache: Boolean,
        ) : Result

        data class SameLanguage(val language: String) : Result
        data object Unavailable : Result
    }

    data class LyricsTranslationResult(
        val translatedLines: List<String>,
        val sourceLanguage: String?,
        val targetLanguage: String
    )

    fun translate(
        songId: String,
        originalLines: List<String>,
        targetLanguageTag: String
    ): TranslationResult {
        return try {
            val translated = originalLines.map { it }
            TranslationResult.Success(LyricsTranslationResult(
                translatedLines = translated,
                sourceLanguage = null,
                targetLanguage = targetLanguageTag
            ))
        } catch (e: Exception) {
            TranslationResult.Failure(e)
        }
    }

    fun translate(
        trackId: String,
        lines: List<LyricLine>,
        targetLanguageTag: String,
    ): Result {
        if (targetLanguageTag.isBlank() || lines.isEmpty()) return Result.Unavailable
        return Result.SameLanguage(targetLanguageTag)
    }

    sealed class TranslationResult {
        data class Success(val data: LyricsTranslationResult) : TranslationResult()
        data class Failure(val error: Throwable) : TranslationResult()
    }
}
