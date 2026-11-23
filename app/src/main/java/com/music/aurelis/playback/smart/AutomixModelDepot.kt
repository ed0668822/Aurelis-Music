// SPDX-FileCopyrightText: 2026 Aurelis
// SPDX-License-Identifier: LGPL-3.0-or-later
package com.music.aurelis.playback.smart

import android.content.Context
import android.os.SystemClock
import android.util.Log
import com.music.aurelis.data.Http
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import okhttp3.Request
import java.io.File
import java.io.FileNotFoundException
import java.io.InputStream
import java.io.IOException
import java.security.MessageDigest
import java.util.concurrent.ConcurrentHashMap

/**
 * Verified, atomic ONNX model provisioning for Android/ARM64 AutoMix.
 *
 * The offline assets remain shipped with Aurelis: no network is needed on
 * first run. If an asset is absent in a future smaller distribution, the
 * pinned GitHub revision repairs it in the background. We never load a
 * partial model (or a matching-length, wrong-content file) into native ORT.
 *
 * The pinned release matches the two models checked into this repository.
 * Change the revision and hashes together when publishing new weights.
 */
internal class AutomixModelDepot(private val context: Context) {
    private data class Model(val name: String, val size: Long, val sha256: String)

    private val known = mapOf(
        BeatTracker.MODEL_ASSET to Model(
            BeatTracker.MODEL_ASSET, 23_138_816L,
            "7801f9c1adfd5ff193f37ececa3320bf1fda1ddee2bf7de341cea4c25ffd0acd",
        ),
        VocalTracker.MODEL_ASSET to Model(
            VocalTracker.MODEL_ASSET, 9_054_206L,
            "a2be987b55a29bc149d3a6ae99b08175d81f85ee292a8ea21f96c3a473bc94cb",
        ),
    )
    private val locks = ConcurrentHashMap<String, Any>()
    private val verified = ConcurrentHashMap<String, Pair<Long, Long>>()
    private val downloads = ConcurrentHashMap.newKeySet<String>()
    private val failedAt = ConcurrentHashMap<String, Long>()
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    fun path(name: String): String {
        val model = known[name] ?: throw FileNotFoundException("Unknown ONNX model $name")
        val target = File(context.filesDir, name)
        synchronized(locks.computeIfAbsent(name) { Any() }) {
            if (isVerified(target, model)) return target.absolutePath
            verified.remove(name)
            // Assets are preferred over the network on every install/upgrade.
            try {
                context.assets.open(name).use { install(it, target, model) }
            } catch (e: IOException) {
                Log.w(TAG, "Bundled ONNX model $name missing or invalid", e)
            }
            if (isVerified(target, model)) return target.absolutePath
        }
        downloadIfMissing(model)
        throw FileNotFoundException("AutoMix ONNX model $name is not ready; using fallback mix")
    }

    private fun isVerified(file: File, model: Model): Boolean {
        if (file.length() != model.size) return false
        val stamp = file.lastModified() to file.length()
        if (verified[model.name] == stamp) return true
        val expected = model.sha256
        val actual = runCatching {
            file.inputStream().use { digestOf(it, null, model.size) }
        }.getOrNull() ?: return false
        if (actual != expected) return false
        verified[model.name] = stamp
        return true
    }

    private fun install(input: InputStream, target: File, model: Model) {
        val tmp = File(target.parentFile, model.name + ".part")
        // Never delete the last good copy before the new one is complete.
        try {
            val hash = tmp.outputStream().use { output ->
                digestOf(input, output, model.size)
            }
            if (hash != model.sha256) throw IOException("SHA-256 mismatch for ${model.name}")
            if (!tmp.renameTo(target)) {
                if (!target.delete() || !tmp.renameTo(target)) {
                    throw IOException("Cannot replace ONNX model ${model.name}")
                }
            }
            verified[model.name] = target.lastModified() to target.length()
        } finally {
            tmp.delete()
        }
    }

    private fun digestOf(input: InputStream, output: java.io.OutputStream?, expectedBytes: Long): String {
        val md = MessageDigest.getInstance("SHA-256")
        val buffer = ByteArray(64 * 1024)
        var count = 0L
        while (true) {
            val n = input.read(buffer)
            if (n < 0) break
            count += n
            if (count > expectedBytes) throw IOException("ONNX asset larger than expected")
            output?.write(buffer, 0, n)
            md.update(buffer, 0, n)
        }
        if (count != expectedBytes) throw IOException("Incomplete ONNX asset: $count of $expectedBytes")
        return md.digest().joinToString("") { "%02x".format(it) }
    }

    private fun downloadIfMissing(model: Model) {
        val lastFailure = failedAt[model.name] ?: 0L
        if (lastFailure != 0L && SystemClock.elapsedRealtime() - lastFailure < RETRY_MS) return
        if (!downloads.add(model.name)) return
        scope.launch {
            try {
                // Pinned to the exact verified model blobs already in Aurelis.
                val url = "$PINNED_RAW_URL/${model.name}"
                val request = Request.Builder().url(url).build()
                Http.client.newCall(request).execute().use { response ->
                    if (!response.isSuccessful) throw IOException("HTTP ${response.code}")
                    val body = response.body ?: throw IOException("Empty ONNX response")
                    synchronized(locks.computeIfAbsent(model.name) { Any() }) {
                        if (!isVerified(File(context.filesDir, model.name), model)) {
                            body.byteStream().use { install(it, File(context.filesDir, model.name), model) }
                        }
                    }
                }
                failedAt.remove(model.name)
                Log.i(TAG, "Recovered ONNX model ${model.name}")
            } catch (e: Exception) {
                failedAt[model.name] = SystemClock.elapsedRealtime()
                Log.w(TAG, "ONNX recovery failed; AutoMix will use its ordinary fallback", e)
            } finally {
                downloads.remove(model.name)
            }
        }
    }

    private companion object {
        const val TAG = "AutomixModelDepot"
        const val RETRY_MS = 2 * 60_000L
        const val PINNED_RAW_URL =
            "https://raw.githubusercontent.com/ed0668822/Aurelis-Music/8b6c36872510a973d332123acffc93ff57c4f300/app/src/main/assets"
    }
}
