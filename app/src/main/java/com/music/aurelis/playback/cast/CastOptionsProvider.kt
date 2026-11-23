package com.music.aurelis.playback.cast

import android.content.Context
import com.music.aurelis.R
import com.google.android.gms.cast.CastMediaControlIntent
import com.google.android.gms.cast.framework.CastOptions
import com.google.android.gms.cast.framework.OptionsProvider
import com.google.android.gms.cast.framework.SessionProvider
import com.google.android.gms.cast.framework.media.CastMediaOptions

/**
 * How the Cast framework is told this app wants to cast. Named in the manifest
 * and read by the framework itself, which is why it is a class of its own.
 *
 * When a registered custom receiver ID is configured, the receiver can
 * display Aurelis lyrics. With an empty ID, Cast remains on Google's standard
 * media receiver so audio casting keeps working without extra setup. The framework's own media notification and its own media session are
 * both switched off: the service already owns a session and a notification,
 * and a second pair — each showing the same track, each with its own idea of
 * whether it is playing — is what casting looks like when it is bolted on
 * rather than built in. The existing ones follow the receiver instead; see
 * [CastPlayback].
 */
class CastOptionsProvider : OptionsProvider {

    override fun getCastOptions(context: Context): CastOptions {
        val media = CastMediaOptions.Builder()
            .setNotificationOptions(null)
            .setMediaSessionEnabled(false)
            .build()
        return CastOptions.Builder()
            .setReceiverApplicationId(
                customReceiverId(context) ?: CastMediaControlIntent.DEFAULT_MEDIA_RECEIVER_APPLICATION_ID,
            )
            .setCastMediaOptions(media)
            // Leaving the receiver running after the phone has let go of it
            // keeps a TV on a "casting" screen with nothing driving it.
            .setStopReceiverApplicationWhenEndingSession(true)
            .build()
    }

    override fun getAdditionalSessionProviders(context: Context): List<SessionProvider>? = null

    companion object {
        /**
         * To enable the TV UI, host receiver/index.html over HTTPS and register
         * the URL in the Google Cast Developer Console; put its eight-character
         * application ID in res/values/cast_receiver.xml.
         */
        fun customReceiverId(context: Context): String? =
            context.getString(R.string.cast_custom_receiver_id).trim()
                .takeIf { it.matches(Regex("[a-fA-F0-9]{8}")) }
    }
}
