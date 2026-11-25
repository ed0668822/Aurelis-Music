// SPDX-FileCopyrightText: 2026 Aurelis
// SPDX-License-Identifier: LGPL-3.0-or-later
package com.music.aurelis.ui.player

import android.content.ClipData
import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import androidx.core.content.FileProvider
import androidx.media3.common.MediaItem
import androidx.media3.common.MimeTypes
import androidx.media3.common.util.UnstableApi
import androidx.media3.effect.BitmapOverlay
import androidx.media3.effect.OverlayEffect
import androidx.media3.effect.Presentation
import androidx.media3.transformer.Composition
import androidx.media3.transformer.EditedMediaItem
import androidx.media3.transformer.EditedMediaItemSequence
import androidx.media3.transformer.Effects
import androidx.media3.transformer.ExportException
import androidx.media3.transformer.ExportResult
import androidx.media3.transformer.Transformer
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.isActive
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import java.io.File
import java.util.Locale
import java.util.concurrent.TimeUnit
import kotlin.coroutines.resume

internal fun localLyricsAudioUri(request: LyricsShareRequest): String? =
    request.song.localUri?.takeIf { it.startsWith("content://") || it.startsWith("file://") }

private const val VIDEO_WIDTH = 720
private const val VIDEO_HEIGHT = 1280
private const val LYRICS_CACHE_PREFIX = "aurelis-lyrics-"
private val LEGACY_LYRICS_FILE = Regex("lyrics-(background-)?-?[0-9]+[.](mp4|png)")

private fun lyricsShareCache(context: Context): File {
    val folder = File(context.cacheDir, "shared").apply { mkdirs() }
    val expiredBefore = System.currentTimeMillis() - TimeUnit.HOURS.toMillis(24)
    folder.listFiles()?.forEach { file ->
        val name = file.name
        val isLyricsFile = name.startsWith(LYRICS_CACHE_PREFIX) || name == "lyrics.png" ||
            LEGACY_LYRICS_FILE.matches(name)
        if (isLyricsFile && file.isFile && file.lastModified() < expiredBefore) file.delete()
    }
    return folder
}

internal suspend fun cacheLyricsImage(context: Context, bitmap: Bitmap): Uri? =
    withContext(Dispatchers.IO) {
        val file = File.createTempFile(LYRICS_CACHE_PREFIX, ".png", lyricsShareCache(context))
        var success = false
        try {
            if (!file.outputStream().use { bitmap.compress(Bitmap.CompressFormat.PNG, 100, it) }) {
                return@withContext null
            }
            FileProvider.getUriForFile(context, context.packageName + ".fileprovider", file)
                .also { success = true }
        } finally {
            if (!success) file.delete()
        }
    }

/** Exports the same card and highlights used by the preview. An audio URI selects audible export. */
@androidx.annotation.OptIn(UnstableApi::class)
internal suspend fun exportLocalLyricsVideo(
    context: Context,
    poster: RenderedLyricsCard,
    timeline: LyricsShareTimeline,
    audioUri: String?,
): File? = withContext(Dispatchers.IO) {
    val image = File.createTempFile(LYRICS_CACHE_PREFIX, ".png", lyricsShareCache(context))
    var output: File? = null
    var exportedOk = false
    try {
        val resized = Bitmap.createScaledBitmap(poster.bitmap, VIDEO_WIDTH, VIDEO_HEIGHT, true)
        val saved = try {
            image.outputStream().use { resized.compress(Bitmap.CompressFormat.PNG, 100, it) }
        } finally {
            if (resized !== poster.bitmap) resized.recycle()
        }
        if (!saved) return@withContext null
        val video = File.createTempFile(LYRICS_CACHE_PREFIX, ".mp4", image.parentFile)
        output = video
        val succeeded = transformLyricsVideo(context, image, video, poster, timeline, audioUri)
        exportedOk = succeeded && video.isFile && video.length() > 0L
        video.takeIf { exportedOk }
    } finally {
        image.delete()
        if (!exportedOk) output?.delete()
    }
}

@androidx.annotation.OptIn(UnstableApi::class)
private suspend fun transformLyricsVideo(
    context: Context,
    image: File,
    output: File,
    poster: RenderedLyricsCard,
    timeline: LyricsShareTimeline,
    audioUri: String?,
): Boolean = withContext(Dispatchers.Main) {
    val still = EditedMediaItem.Builder(
        MediaItem.Builder()
            .setUri(Uri.fromFile(image))
            .setMimeType(MimeTypes.IMAGE_PNG)
            .setImageDurationMs(timeline.durationMs)
            .build(),
    )
        .setFrameRate(30)
        .setEffects(Effects(emptyList(), listOf(Presentation.createForWidthAndHeight(
            VIDEO_WIDTH, VIDEO_HEIGHT, Presentation.LAYOUT_SCALE_TO_FIT_WITH_CROP,
        ))))
        .build()
    val sequences = mutableListOf(EditedMediaItemSequence.Builder(still).build())
    if (audioUri != null) {
        val clips = timeline.segments.map { (start, end) ->
            EditedMediaItem.Builder(
                MediaItem.Builder()
                    .setUri(Uri.parse(audioUri))
                    .setClippingConfiguration(
                        MediaItem.ClippingConfiguration.Builder()
                            .setStartPositionMs(start)
                            .setEndPositionMs(end)
                            .build(),
                    ).build(),
            ).setRemoveVideo(true).build()
        }
        if (clips.isNotEmpty()) sequences += EditedMediaItemSequence.Builder(clips).build()
    }
    val overlay = SyncedLyricsVideoOverlay(poster, timeline)
    val composition = Composition.Builder(sequences)
        .setEffects(Effects(emptyList(), listOf(OverlayEffect(listOf(overlay)))))
        .build()
    var runningTransformer: Transformer? = null
    try {
        suspendCancellableCoroutine { continuation ->
            val transformer = Transformer.Builder(context)
                .setVideoMimeType(MimeTypes.VIDEO_H264)
                .setAudioMimeType(MimeTypes.AUDIO_AAC)
                .addListener(object : Transformer.Listener {
                    override fun onCompleted(composition: Composition, exportResult: ExportResult) {
                        if (continuation.isActive) {
                            continuation.resume(audioUri == null || exportResult.audioMimeType != null)
                        }
                    }
                    override fun onError(
                        composition: Composition,
                        exportResult: ExportResult,
                        exportException: ExportException,
                    ) {
                        if (continuation.isActive) continuation.resume(false)
                    }
                })
                .build()
            runningTransformer = transformer
            output.delete()
            try {
                transformer.start(composition, output.absolutePath)
            } catch (_: Exception) {
                transformer.cancel()
                if (continuation.isActive) continuation.resume(false)
            }
        }
    } finally {
        // Transformer must be cancelled on its own thread before the cache files are deleted.
        if (!currentCoroutineContext().isActive) runningTransformer?.cancel()
    }
}

internal class LyricsVideoHighlight(
    private val card: RenderedLyricsCard,
    private val timeline: LyricsShareTimeline,
) {
    private val paint = Paint(Paint.ANTI_ALIAS_FLAG)

    fun draw(canvas: Canvas, width: Float, height: Float, positionUs: Long) {
        canvas.save()
        canvas.scale(width / card.bitmap.width, height / card.bitmap.height)
        val active = timeline.windows.firstOrNull { positionUs >= it.fromUs && positionUs < it.toUs }
        val bounds = active?.let { card.lineBounds[it.lineIndex] }
        if (bounds != null) {
            paint.color = Color.argb(36, 255, 255, 255)
            canvas.drawRoundRect(bounds, 16f, 16f, paint)
            paint.color = Color.WHITE
            canvas.drawRoundRect(bounds.left - 13f, bounds.top + 4f, bounds.left - 7f,
                bounds.bottom - 4f, 3f, 3f, paint)
        }
        val bottom = card.bitmap.height.toFloat()
        val right = card.bitmap.width.toFloat()
        val top = bottom - 12f
        paint.color = Color.argb(95, 255, 255, 255)
        canvas.drawRect(0f, top, right, bottom, paint)
        paint.color = Color.WHITE
        val fraction = (positionUs / (timeline.durationMs * 1000.0)).coerceIn(0.0, 1.0).toFloat()
        canvas.drawRect(0f, top, right * fraction, bottom, paint)
        canvas.restore()
    }
}

@androidx.annotation.OptIn(UnstableApi::class)
private class SyncedLyricsVideoOverlay(
    card: RenderedLyricsCard,
    timeline: LyricsShareTimeline,
) : BitmapOverlay() {
    private val bitmap = Bitmap.createBitmap(VIDEO_WIDTH, VIDEO_HEIGHT, Bitmap.Config.ARGB_8888)
    private val canvas = Canvas(bitmap)
    private val highlight = LyricsVideoHighlight(card, timeline)

    override fun getBitmap(presentationTimeUs: Long): Bitmap {
        bitmap.eraseColor(Color.TRANSPARENT)
        highlight.draw(canvas, VIDEO_WIDTH.toFloat(), VIDEO_HEIGHT.toFloat(), presentationTimeUs)
        return bitmap
    }
}

internal fun shareLocalLyricsVideoIntent(context: Context, file: File): Intent =
    shareLyricsMediaIntent(
        FileProvider.getUriForFile(context, context.packageName + ".fileprovider", file),
        "video/mp4",
    )

internal fun shareLyricsMediaIntent(uri: Uri, mimeType: String): Intent =
    Intent(Intent.ACTION_SEND).apply {
        type = mimeType
        putExtra(Intent.EXTRA_STREAM, uri)
        clipData = ClipData.newRawUri(null, uri)
        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
    }

internal suspend fun saveLocalLyricsVideo(context: Context, video: File, title: String): Boolean =
    withContext(Dispatchers.IO) {
        runCatching {
            val cleaned = title.replace(Regex("[^\\p{L}\\p{N}-]+"), "-")
                .trim('-').take(50).lowercase(Locale.ROOT)
            val name = "aurelis-lyrics-" + cleaned + ".mp4"
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                val resolver = context.contentResolver
                val row = ContentValues().apply {
                    put(MediaStore.Video.Media.DISPLAY_NAME, name)
                    put(MediaStore.Video.Media.MIME_TYPE, "video/mp4")
                    put(MediaStore.Video.Media.RELATIVE_PATH, Environment.DIRECTORY_MOVIES + "/Aurelis Music")
                    put(MediaStore.Video.Media.IS_PENDING, 1)
                }
                val uri = resolver.insert(MediaStore.Video.Media.EXTERNAL_CONTENT_URI, row)
                    ?: error("Unable to reserve video")
                try {
                    resolver.openOutputStream(uri)?.use { output ->
                        video.inputStream().use { it.copyTo(output) }
                    } ?: error("Unable to write video")
                    resolver.update(uri, ContentValues().apply {
                        put(MediaStore.Video.Media.IS_PENDING, 0)
                    }, null, null)
                } catch (error: Exception) {
                    resolver.delete(uri, null, null)
                    throw error
                }
            } else {
                @Suppress("DEPRECATION")
                val folder = File(
                    Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_MOVIES),
                    "Aurelis Music",
                ).apply { mkdirs() }
                video.copyTo(File(folder, name), overwrite = true)
            }
            true
        }.getOrDefault(false)
    }
