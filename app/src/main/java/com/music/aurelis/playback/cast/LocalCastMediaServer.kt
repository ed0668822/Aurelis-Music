package com.music.aurelis.playback.cast

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.net.Uri
import android.util.Log
import java.io.File
import java.io.FileInputStream
import java.io.InputStream
import java.net.Inet4Address

/**
 * Android-facing adapter for the temporary LAN HTTP server. Only the music
 * already selected through MediaStore/SAF or a readable local file is exposed.
 * URLs contain cryptographically random identifiers, not filesystem paths.
 */
internal class LocalCastMediaServer(private val context: Context) {
    private var address: Inet4Address? = null
    private var server: LocalCastHttpServer? = null

    @Synchronized
    fun streamFor(uri: Uri): CastStream? {
        if (uri.scheme != "content" && uri.scheme != "file") return null
        val path = uri.path?.lowercase().orEmpty()
        // Offline HLS/DASH playlists refer to other segments; serving the
        // playlist alone would generate broken relative requests on the TV.
        if (path.endsWith(".m3u8") || path.endsWith(".mpd")) return null
        val length = lengthOf(uri)?.takeIf { it > 0 } ?: return null
        val mime = mimeTypeOf(uri) ?: return null
        val lan = localIpv4() ?: return null

        if (server == null || address != lan) {
            server?.close()
            address = lan
            server = LocalCastHttpServer(lan)
        }
        val url = server?.publish(
            LocalCastHttpServer.MediaResource(length, mime) { offset -> openAt(uri, offset) },
        ) ?: return null
        return CastStream(url, mime)
    }

    private fun lengthOf(uri: Uri): Long? = try {
        when (uri.scheme) {
            "file" -> uri.path?.let(::File)?.takeIf { it.isFile && it.canRead() }?.length()
            "content" -> context.contentResolver.openFileDescriptor(uri, "r")?.use {
                it.statSize.takeIf { size -> size > 0 }
            }
            else -> null
        }
    } catch (e: Exception) {
        Log.w(TAG, "Local Cast cannot read media size", e)
        null
    }

    private fun openAt(uri: Uri, offset: Long): InputStream {
        val input: FileInputStream = when (uri.scheme) {
            "file" -> FileInputStream(File(requireNotNull(uri.path)))
            "content" -> {
                val descriptor = context.contentResolver.openFileDescriptor(uri, "r")
                    ?: throw IllegalStateException("Media provider returned no descriptor")
                android.os.ParcelFileDescriptor.AutoCloseInputStream(descriptor)
            }
            else -> throw IllegalArgumentException("Non-local source")
        }
        try {
            input.channel.position(offset)
        } catch (e: Exception) {
            input.close()
            throw e
        }
        return input
    }

    private fun mimeTypeOf(uri: Uri): String? {
        val extension = uri.lastPathSegment?.substringAfterLast('.', "")?.lowercase()
        val byExtension = when (extension) {
            "mp3" -> "audio/mpeg"
            "flac" -> "audio/flac"
            "wav", "wave" -> "audio/wav"
            "ogg", "opus" -> "audio/ogg"
            "m4a", "mp4", "alac" -> "audio/mp4"
            "aac" -> "audio/aac"
            "aiff", "aif" -> "audio/aiff"
            else -> null
        }
        val reported = runCatching { context.contentResolver.getType(uri) }.getOrNull()
        return reported?.takeIf { it.startsWith("audio/") && it.none { ch -> ch == '\r' || ch == '\n' } }
            ?: byExtension
    }

    private fun localIpv4(): Inet4Address? {
        val manager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager
            ?: return null
        val networks = (listOfNotNull(manager.activeNetwork) + manager.allNetworks).distinct()
        for (network in networks) {
            val capabilities = manager.getNetworkCapabilities(network) ?: continue
            if (!capabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) &&
                !capabilities.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET)
            ) continue
            val links = manager.getLinkProperties(network) ?: continue
            val ipv4 = links.linkAddresses.map { it.address }.filterIsInstance<Inet4Address>()
                .firstOrNull { !it.isAnyLocalAddress && !it.isLoopbackAddress && !it.isLinkLocalAddress }
            if (ipv4 != null) return ipv4
        }
        return null
    }

    @Synchronized
    fun stop() {
        server?.close()
        server = null
        address = null
    }

    private companion object {
        const val TAG = "AurelisLocalCast"
    }
}
