// SPDX-FileCopyrightText: © 2026 Aurelis

package com.music.aurelis.playback.cast

import android.content.Context
import com.google.android.gms.cast.CastMediaControlIntent
import com.google.android.gms.cast.framework.CastOptions
import com.google.android.gms.cast.framework.OptionsProvider
import com.google.android.gms.cast.framework.SessionProvider
import com.google.android.gms.cast.framework.media.CastMediaOptions

/**
 * How the Cast framework is told this app wants to cast. Named in the manifest
 * and read by the framework itself, which is why it is a class of its own.
 *
 * Cast always uses Google's standard media receiver: the TV only plays audio
 * (no lyrics screen). The framework's own media notification and its own media
 * session are both switched off: the service already owns a session and a
 * notification, and a second pair — each showing the same track, each with its
 * own idea of whether it is playing — is what casting looks like when it is
 * bolted on rather than built in. The existing ones follow the receiver
 * instead; see [CastPlayback].
 */
class CastOptionsProvider : OptionsProvider {

    override fun getCastOptions(context: Context): CastOptions {
        val media = CastMediaOptions.Builder()
            .setNotificationOptions(null)
            .setMediaSessionEnabled(false)
            .build()
        return CastOptions.Builder()
            .setReceiverApplicationId(CastMediaControlIntent.DEFAULT_MEDIA_RECEIVER_APPLICATION_ID)
            .setCastMediaOptions(media)
            // Leaving the receiver running after the phone has let go of it
            // keeps a TV on a "casting" screen with nothing driving it.
            .setStopReceiverApplicationWhenEndingSession(true)
            .build()
    }

    override fun getAdditionalSessionProviders(context: Context): List<SessionProvider>? = null

    companion object {
        /**
         * Custom lyrics receiver removed: always null, so no caller ever treats
         * the TV as a lyrics screen. Kept only so existing callers still compile.
         */
        @Suppress("UNUSED_PARAMETER")
        fun customReceiverId(context: Context): String? = null
    }
}
