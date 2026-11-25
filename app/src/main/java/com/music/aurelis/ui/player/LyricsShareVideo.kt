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
import android.graphics.Typeface
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import android.text.Layout
import android.text.StaticLayout
import android.text.TextPaint
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
import kotlin.math.roundToInt

internal fun localLyricsAudioUri(request: LyricsShareRequest): String? =
    request.song.localUri?.takeIf { it.startsWith("content://") || it.startsWith("file://") }

internal suspend fun localLyricsAudioDurationMs(context: Context, audioUri: String): Long? =
    withContext(Dispatchers.IO) {
        val retriever = MediaMetadataRetriever()
        try {
            retriever.setDataSource(context, Uri.parse(audioUri))
            retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)
                ?.toLongOrNull()?.takeIf { it > 0L }
        } catch (_: Exception) {
            null
        } finally {
            runCatching { retriever.release() }
        }
    }

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

/** Exports the artwork and animated verses over a single, untrimmed local audio track. */
@androidx.annotation.OptIn(UnstableApi::class)
internal suspend fun exportLocalLyricsVideo(
    context: Context,
    background: Bitmap,
    timeline: LyricsShareTimeline,
    lines: List<LyricsShareLine>,
    includeSubText: Boolean,
    audioUri: String?,
): File? = withContext(Dispatchers.IO) {
    if (timeline.exceedsVideoLimit) return@withContext null
    val image = File.createTempFile(LYRICS_CACHE_PREFIX, ".png", lyricsShareCache(context))
    var output: File? = null
    var exportedOk = false
    try {
        val resized = Bitmap.createScaledBitmap(background, VIDEO_WIDTH, VIDEO_HEIGHT, true)
        val saved = try {
            image.outputStream().use { resized.compress(Bitmap.CompressFormat.PNG, 100, it) }
        } finally {
            if (resized !== background) resized.recycle()
        }
        if (!saved) return@withContext null
        val video = File.createTempFile(LYRICS_CACHE_PREFIX, ".mp4", image.parentFile)
        output = video
        val animation = LyricsVideoAnimation(timeline, lines, includeSubText)
        val succeeded = transformLyricsVideo(context, image, video, animation, timeline.durationMs, audioUri)
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
    animation: LyricsVideoAnimation,
    durationMs: Long,
    audioUri: String?,
): Boolean = withContext(Dispatchers.Main) {
    val still = EditedMediaItem.Builder(
        MediaItem.Builder()
            .setUri(Uri.fromFile(image))
            .setMimeType(MimeTypes.IMAGE_PNG)
            .setImageDurationMs(durationMs)
            .build(),
    )
        .setFrameRate(30)
        .setEffects(Effects(emptyList(), listOf(Presentation.createForWidthAndHeight(
            VIDEO_WIDTH, VIDEO_HEIGHT, Presentation.LAYOUT_SCALE_TO_FIT_WITH_CROP,
        ))))
        .build()
    val sequences = mutableListOf(EditedMediaItemSequence.withVideoFrom(listOf(still)))
    if (audioUri != null) {
        val audio = EditedMediaItem.Builder(MediaItem.fromUri(audioUri)).build()
        sequences += EditedMediaItemSequence.withAudioFrom(listOf(audio))
    }
    val overlay = SyncedLyricsVideoOverlay(animation)
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

internal class LyricsVideoAnimation(
    private val timeline: LyricsShareTimeline,
    private val lines: List<LyricsShareLine>,
    private val includeSubText: Boolean,
) {
    private val paint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val lyricPaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.WHITE
        typeface = Typeface.DEFAULT_BOLD
    }
    private val translationPaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.WHITE }
    private var laidOutLineIndex = -1
    private var lyric: StaticLayout? = null
    private var translation: StaticLayout? = null
    private val fontSizes = floatArrayOf(56f, 48f, 40f, 32f, 24f)

    fun draw(canvas: Canvas, width: Float, height: Float, positionUs: Long) {
        canvas.save()
        canvas.scale(width / VIDEO_WIDTH, height / VIDEO_HEIGHT)
        val active = timeline.windows.firstOrNull { positionUs >= it.fromUs && positionUs < it.toUs }
        if (active != null) drawVerse(canvas, active, positionUs)
        val bottom = VIDEO_HEIGHT.toFloat()
        val right = VIDEO_WIDTH.toFloat()
        val top = bottom - 8f
        paint.color = Color.argb(95, 255, 255, 255)
        canvas.drawRect(0f, top, right, bottom, paint)
        paint.color = Color.WHITE
        val fraction = (positionUs / (timeline.durationMs * 1000.0)).coerceIn(0.0, 1.0).toFloat()
        canvas.drawRect(0f, top, right * fraction, bottom, paint)
        canvas.restore()
    }

    private fun drawVerse(canvas: Canvas, window: LyricsVideoLineWindow, positionUs: Long) {
        if (laidOutLineIndex != window.lineIndex) layoutVerse(window.lineIndex)
        val main = lyric ?: return
        val sub = translation
        val transitionUs = minOf(250_000L, (window.toUs - window.fromUs) / 2)
        val reveal = (minOf(positionUs - window.fromUs, window.toUs - positionUs).toFloat() / transitionUs)
            .coerceIn(0f, 1f)
        val opacity = reveal * reveal * (3f - 2f * reveal)
        val contentHeight = main.height + (sub?.let { it.height + 16 } ?: 0)
        val scale = (VIDEO_HEIGHT * 0.55f / contentHeight.coerceAtLeast(1)).coerceAtMost(1f)
        val textLeft = (VIDEO_WIDTH - main.width * scale) / 2f
        val textTop = (VIDEO_HEIGHT - contentHeight * scale) / 2f + (1f - opacity) * 24f
        canvas.save()
        canvas.translate(textLeft, textTop)
        canvas.scale(scale, scale)
        paint.color = Color.argb((135f * opacity).roundToInt(), 0, 0, 0)
        canvas.drawRoundRect(
            -18f, -18f, main.width + 18f, contentHeight + 18f, 24f, 24f, paint,
        )
        lyricPaint.alpha = (255f * opacity).roundToInt()
        translationPaint.alpha = (205f * opacity).roundToInt()
        main.draw(canvas)
        if (sub != null) {
            canvas.translate(0f, main.height + 16f)
            sub.draw(canvas)
        }
        canvas.restore()
    }

    private fun layoutVerse(index: Int) {
        val line = lines[index]
        val subText = line.subText?.takeIf { includeSubText && it.isNotBlank() && it != line.text }
        for (size in fontSizes) {
            lyricPaint.textSize = size
            translationPaint.textSize = size * 0.67f
            val main = textLayout(line.text, lyricPaint)
            val sub = subText?.let { textLayout(it, translationPaint) }
            lyric = main
            translation = sub
            if (main.height + (sub?.height ?: 0) <= VIDEO_HEIGHT * 0.55f) break
        }
        laidOutLineIndex = index
    }

    private fun textLayout(text: String, textPaint: TextPaint): StaticLayout =
        StaticLayout.Builder.obtain(text, 0, text.length, textPaint, VIDEO_WIDTH - 112)
            .setAlignment(Layout.Alignment.ALIGN_CENTER)
            .setIncludePad(false)
            .build()
}

@androidx.annotation.OptIn(UnstableApi::class)
private class SyncedLyricsVideoOverlay(
    private val animation: LyricsVideoAnimation,
) : BitmapOverlay() {
    private val bitmap = Bitmap.createBitmap(VIDEO_WIDTH, VIDEO_HEIGHT, Bitmap.Config.ARGB_8888)
    private val canvas = Canvas(bitmap)
    override fun getBitmap(presentationTimeUs: Long): Bitmap {
        bitmap.eraseColor(Color.TRANSPARENT)
        animation.draw(canvas, VIDEO_WIDTH.toFloat(), VIDEO_HEIGHT.toFloat(), presentationTimeUs)
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
