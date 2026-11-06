package com.music.aurelis.playback

import androidx.media3.common.C
import androidx.media3.common.Format
import androidx.media3.common.Tracks
import com.music.aurelis.data.model.LocalSong
import java.util.Locale

enum class AudioQualityOrigin { UNKNOWN, FILE_METADATA, SELECTED_TRACK }

/** Input track information only. Never treated as the DAC/output format. */
@androidx.annotation.OptIn(androidx.media3.common.util.UnstableApi::class)
data class AudioQualityInfo(
    val mimeType: String? = null,
    val sampleRateHz: Int? = null,
    val channelCount: Int? = null,
    val bitrateBps: Int? = null,
    val pcmBits: Int? = null,
    val origin: AudioQualityOrigin = AudioQualityOrigin.UNKNOWN,
) {
    val originLabel: String get() = when (origin) {
        AudioQualityOrigin.UNKNOWN -> "No disponibles"
        AudioQualityOrigin.FILE_METADATA -> "Metadatos del archivo"
        AudioQualityOrigin.SELECTED_TRACK -> "Pista seleccionada"
    }
    val formatLabel: String get() = when (mimeType?.lowercase(Locale.ROOT)) {
        "audio/flac", "audio/x-flac" -> "FLAC"
        "audio/mpeg" -> "MP3"
        "audio/mp4a-latm", "audio/aac" -> "AAC"
        "audio/opus" -> "Opus"
        "audio/vorbis" -> "Vorbis"
        "audio/raw" -> "PCM"
        "audio/wav", "audio/x-wav" -> "WAV"
        "audio/alac" -> "ALAC"
        else -> "Formato desconocido"
    }
    val details: String get() = listOfNotNull(
        formatLabel,
        sampleRateHz?.let { "$it Hz" }, channelCount?.let { "$it canales" },
        bitrateBps?.let { "${it / 1000} kb/s" }, pcmBits?.let { "$it bits PCM" },
    ).joinToString(" · ")

    companion object {
        fun from(song: LocalSong?, format: Format? = null): AudioQualityInfo {
            if (song == null) return AudioQualityInfo()
            val trackMime = format?.sampleMimeType?.takeIf { it.startsWith("audio/") }
            val mime = trackMime ?: song.mimeType?.takeIf { it.startsWith("audio/") }
            val input = format.takeIf { trackMime != null }
            return AudioQualityInfo(mime, input?.sampleRate?.takeIf { it > 0 },
                input?.channelCount?.takeIf { it > 0 }, input?.averageBitrate?.takeIf { it > 0 },
                if (trackMime == "audio/raw") when (format?.pcmEncoding) {
                    C.ENCODING_PCM_8BIT -> 8
                    C.ENCODING_PCM_16BIT -> 16
                    C.ENCODING_PCM_24BIT -> 24
                    C.ENCODING_PCM_32BIT, C.ENCODING_PCM_FLOAT -> 32
                    else -> null
                } else null,
                if (trackMime != null) AudioQualityOrigin.SELECTED_TRACK else if (mime != null)
                    AudioQualityOrigin.FILE_METADATA else AudioQualityOrigin.UNKNOWN)
        }
        fun selected(song: LocalSong?, tracks: Tracks): AudioQualityInfo {
            val formats = tracks.groups.filter { it.type == C.TRACK_TYPE_AUDIO && it.isSelected }.flatMap { group ->
                (0 until group.length).filter(group::isTrackSelected).map(group::getTrackFormat)
            }
            // Adaptive candidates are not proof of which format the decoder is using.
            return from(song, formats.singleOrNull())
        }
    }
}
