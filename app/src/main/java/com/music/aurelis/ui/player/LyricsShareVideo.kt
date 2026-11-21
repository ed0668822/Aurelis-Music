// SPDX-FileCopyrightText: 2026 Aurelis
// SPDX-License-Identifier: LGPL-3.0-or-later
package com.music.aurelis.ui.player

import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.os.Handler
import android.os.Looper
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
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import java.io.File
import java.util.Locale
import kotlin.coroutines.resume

/**
 * Local-only version of BitChord's lyrics video export.
 * The existing rendered lyrics card forms the background; a 30 fps overlay
 * highlights each selected line on its actual time axis. Audio is only read
 * from an accessible local content/file URI. Missing audio produces a silent
 * MP4, never a remote search or duplicate playback service.
 */
@androidx.annotation.OptIn(UnstableApi::class)
internal suspend fun exportLocalLyricsVideo(
    context: Context,
    request: LyricsShareRequest,
    poster: Bitmap,
): File? {
    val timeline = lyricsShareTimeline(request.lines) ?: return null
    val folder = File(context.cacheDir, "shared").apply { mkdirs() }
    // Every share has a private backing image: simultaneous exports must not
    // overwrite the PNG while Media3 is still decoding it.
    val exportId = System.nanoTime()
    val image = File(folder, "lyrics-background-$exportId.png")
    val output = File(folder, "lyrics-$exportId.mp4")
    val width = 720
    val height = 1280 // Story videos are always 9:16, regardless of source-card size.
    val resized = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
    val scale = maxOf(width.toFloat() / poster.width, height.toFloat() / poster.height)
    val drawnWidth = poster.width * scale
    val drawnHeight = poster.height * scale
    Canvas(resized).drawBitmap(
        poster,
        null,
        RectF((width - drawnWidth) / 2f, (height - drawnHeight) / 2f,
            (width + drawnWidth) / 2f, (height + drawnHeight) / 2f),
        Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG),
    )
    var exportedOk = false
    try {
        val saved = withContext(Dispatchers.IO) {
            image.outputStream().use { resized.compress(Bitmap.CompressFormat.PNG, 100, it) }
        }
        if (!saved) return null
        val uri = request.song.localUri?.takeIf {
            it.startsWith("content://") || it.startsWith("file://")
        }
        val withAudio = uri != null && transformLyricsVideo(
            context, image, output, width, height, timeline, request.lines, uri,
        )
        val succeeded = withAudio || transformLyricsVideo(
            context, image, output, width, height, timeline, request.lines, null,
        )
        exportedOk = succeeded && output.exists() && output.length() > 0L
        return output.takeIf { exportedOk }
    } finally {
        resized.recycle()
        image.delete()
        if (!exportedOk) output.delete()
    }
}

@androidx.annotation.OptIn(UnstableApi::class)
private suspend fun transformLyricsVideo(
    context: Context,
    image: File,
    output: File,
    width: Int,
    height: Int,
    timeline: LyricsShareTimeline,
    lines: List<LyricsShareLine>,
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
            width, height, Presentation.LAYOUT_SCALE_TO_FIT_WITH_CROP,
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
    val overlay = SyncedLyricsVideoOverlay(lines, timeline, width, height)
    val composition = Composition.Builder(sequences)
        .setEffects(Effects(emptyList(), listOf(OverlayEffect(listOf(overlay)))))
        .build()
    suspendCancellableCoroutine { continuation ->
        val transformer = Transformer.Builder(context)
            .setVideoMimeType(MimeTypes.VIDEO_H264)
            .setAudioMimeType(MimeTypes.AUDIO_AAC)
            .addListener(object : Transformer.Listener {
                override fun onCompleted(composition: Composition, exportResult: ExportResult) {
                    if (continuation.isActive) continuation.resume(true)
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
        output.delete()
        try {
            transformer.start(composition, output.absolutePath)
        } catch (_: Exception) {
            if (continuation.isActive) continuation.resume(false)
        }
        continuation.invokeOnCancellation {
            Handler(Looper.getMainLooper()).post { transformer.cancel() }
        }
    }
}

/** Transparent per-line overlay advanced only by the export timestamp. */
@androidx.annotation.OptIn(UnstableApi::class)
private class SyncedLyricsVideoOverlay(
    private val lines: List<LyricsShareLine>,
    private val timeline: LyricsShareTimeline,
    private val width: Int,
    private val height: Int,
) : BitmapOverlay() {
    private val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
    private val fill = Paint(Paint.ANTI_ALIAS_FLAG)
    private val type = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.WHITE
        textSize = width * 0.042f
        isFakeBoldText = true
    }

    override fun getBitmap(presentationTimeUs: Long): Bitmap {
        bitmap.eraseColor(Color.TRANSPARENT)
        val canvas = Canvas(bitmap)
        val active = timeline.windows.firstOrNull {
            presentationTimeUs >= it.fromUs && presentationTimeUs < it.toUs
        }
        val lyric = active?.let { lines.getOrNull(it.lineIndex)?.text }?.takeIf { it.isNotBlank() }
        if (lyric != null) {
            val margin = width * 0.045f
            val maxWidth = width - margin * 3f
            val displayed = lyric.take(110)
            val fit = type.breakText(displayed, true, maxWidth, null).coerceAtLeast(1)
            val baseline = height - height * 0.185f
            fill.color = Color.argb(208, 12, 12, 16)
            canvas.drawRoundRect(
                margin, baseline - width * 0.068f,
                width - margin, baseline + width * 0.035f,
                width * 0.025f, width * 0.025f, fill,
            )
            canvas.drawText(displayed.take(fit), margin * 1.5f, baseline, type)
        }
        fill.color = Color.argb(95, 255, 255, 255)
        val y = height - width * 0.025f
        canvas.drawRect(0f, y, width.toFloat(), height.toFloat(), fill)
        fill.color = Color.WHITE
        val fraction = (presentationTimeUs / (timeline.durationMs * 1000.0))
            .coerceIn(0.0, 1.0).toFloat()
        canvas.drawRect(0f, y, width * fraction, height.toFloat(), fill)
        return bitmap
    }
}

internal fun shareLocalLyricsVideoIntent(context: Context, file: File): Intent? = runCatching {
    val uri = FileProvider.getUriForFile(context, context.packageName + ".fileprovider", file)
    Intent(Intent.ACTION_SEND).apply {
        type = "video/mp4"
        putExtra(Intent.EXTRA_STREAM, uri)
        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
    }
}.getOrNull()

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
