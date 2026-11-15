// Aurelis © 2026. GPL-3.0-or-later
package com.music.aurelis.playback

import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer

/** Keep interruption handling inside the only ExoPlayer, not a competing focus owner. */
@androidx.annotation.OptIn(UnstableApi::class)
internal fun ExoPlayer.Builder.withLocalAudioPolicy(): ExoPlayer.Builder =
    setAudioAttributes(AudioAttributes.Builder().setUsage(C.USAGE_MEDIA)
        .setContentType(C.AUDIO_CONTENT_TYPE_MUSIC).build(), true)
        .setHandleAudioBecomingNoisy(true)
        .setWakeMode(C.WAKE_MODE_LOCAL)
