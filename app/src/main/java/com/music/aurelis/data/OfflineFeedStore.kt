// SPDX-FileCopyrightText: © 2026 Aurelis

package com.music.aurelis.data

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.util.AtomicFile
import androidx.annotation.WorkerThread
import java.io.File
import java.security.MessageDigest
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import kotlinx.serialization.KSerializer
import kotlinx.serialization.json.Json

/** Last usable screens, partitioned by the active YouTube listener. */
internal class OfflineFeedStore(context: Context) {
    private val directory = File(context.filesDir, "offline-feeds-v1")
    private val json = Json { ignoreUnknownKeys = true }
    private val disk = Mutex()

    suspend fun <T> read(listener: String?, screen: String, serializer: KSerializer<T>): T? {
        if (listener == null) return null
        return withContext(Dispatchers.IO) {
            disk.withLock {
                runCatching {
                    json.decodeFromString(serializer, entry(listener, screen).readFully().decodeToString())
                }.getOrNull()
            }
        }
    }

    suspend fun <T> write(listener: String?, screen: String, serializer: KSerializer<T>, value: T) {
        if (listener == null) return
        withContext(Dispatchers.IO) {
            disk.withLock {
                runCatching {
                    val bytes = json.encodeToString(serializer, value).encodeToByteArray()
                    val file = entry(listener, screen)
                    val stream = file.startWrite()
                    try {
                        stream.write(bytes)
                        file.finishWrite(stream)
                    } catch (error: Exception) {
                        file.failWrite(stream)
                        throw error
                    }
                    if (screen.startsWith("detail:")) pruneDetails(listener)
                }
            }
        }
    }

    @WorkerThread
    private fun entry(listener: String, screen: String): AtomicFile =
        AtomicFile(File(File(directory, digest(listener)), digest(screen) + if (screen.startsWith("detail:")) ".detail" else ".json"))

    private fun pruneDetails(listener: String) {
        val files = File(directory, digest(listener)).listFiles { file -> file.extension == "detail" }.orEmpty()
        if (files.size > 40) files.sortedByDescending(File::lastModified).drop(40).forEach { AtomicFile(it).delete() }
    }

    private fun digest(value: String): String =
        MessageDigest.getInstance("SHA-256").digest(value.encodeToByteArray()).joinToString("") { "%02x".format(it) }
}

internal fun ConnectivityManager.hasValidatedInternet(): Boolean =
    getNetworkCapabilities(activeNetwork)?.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED) == true

internal fun ConnectivityManager.internetAvailability() = callbackFlow {
    val callback = object : ConnectivityManager.NetworkCallback() {
        override fun onCapabilitiesChanged(network: Network, capabilities: NetworkCapabilities) {
            trySend(capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED))
        }

        override fun onLost(network: Network) { trySend(false) }
    }
    registerDefaultNetworkCallback(callback)
    trySend(hasValidatedInternet())
    awaitClose { unregisterNetworkCallback(callback) }
}.distinctUntilChanged()
