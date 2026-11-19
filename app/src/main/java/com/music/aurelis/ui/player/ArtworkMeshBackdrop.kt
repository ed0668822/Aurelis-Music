// SPDX-FileCopyrightText: 2026 Aurelis
// SPDX-License-Identifier: LGPL-3.0-or-later
package com.music.aurelis.ui.player

import android.net.Uri
import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.snap
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.painter.BitmapPainter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import coil3.SingletonImageLoader
import coil3.compose.LocalPlatformContext
import coil3.request.ImageRequest
import coil3.request.SuccessResult
import com.music.aurelis.ui.graphics.ColorUtils
import com.music.aurelis.ui.graphics.forPixelAccess
import com.music.aurelis.ui.graphics.imageBitmapOf
import com.music.aurelis.ui.graphics.toImageBitmap
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.withContext
import kotlin.math.floor
import kotlin.math.roundToInt
import kotlin.random.Random

/**
 * F051: the default BitChord-style artwork backdrop. Unlike the legacy
 * four-blob gradient, this preserves the COVER'S colour proportions and
 * position: bottom-row continuity, flipped/rotated lower grid, baked blur.
 * No full-screen per-frame RenderEffect is required.
 */
@Immutable
class ArtworkMesh internal constructor(internal val image: ImageBitmap)

private val meshCache = object : LinkedHashMap<String, ArtworkMesh>(0, 0.75f, true) {
    override fun removeEldestEntry(eldest: Map.Entry<String, ArtworkMesh>) = size > 64
}
private val blurCache = object : LinkedHashMap<String, ImageBitmap>(0, 0.75f, true) {
    override fun removeEldestEntry(eldest: Map.Entry<String, ImageBitmap>) = size > 8
}

/** Keep a previously shown backdrop during a pending decode, then replace it. */
@Composable
fun rememberArtworkMesh(artworkUri: Uri?): ArtworkMesh? {
    val context = LocalPlatformContext.current
    val transition = remember(context) { ArtworkTransition<ArtworkMesh?>(null) }
    LaunchedEffect(artworkUri, context) {
        val generation = transition.begin()
        if (artworkUri == null) {
            transition.complete(generation, null)
            return@LaunchedEffect
        }
        val key = artworkUri.toString()
        synchronized(meshCache) { meshCache[key] }?.let {
            transition.complete(generation, it)
            return@LaunchedEffect
        }
        val bitmap = loadSmallArtwork(context, artworkUri, MESH_PX)
        currentCoroutineContext().ensureActive()
        val next = bitmap?.let { image ->
            withContext(Dispatchers.Default) { meshOf(image, key.hashCode()) }
        }
        currentCoroutineContext().ensureActive()
        if (next != null) synchronized(meshCache) { meshCache[key] = next }
        transition.complete(generation, next)
    }
    return transition.value
}

/** A blurred 128px copy, decoded and blurred once, not per GPU frame. */
@Composable
fun rememberFullArtworkBlurImage(artworkUri: Uri?): ImageBitmap? {
    val context = LocalPlatformContext.current
    val transition = remember(context) { ArtworkTransition<ImageBitmap?>(null) }
    LaunchedEffect(artworkUri, context) {
        val generation = transition.begin()
        if (artworkUri == null) {
            transition.complete(generation, null)
            return@LaunchedEffect
        }
        val key = artworkUri.toString()
        synchronized(blurCache) { blurCache[key] }?.let {
            transition.complete(generation, it)
            return@LaunchedEffect
        }
        val bitmap = loadSmallArtwork(context, artworkUri, FULL_BLUR_SOURCE_PX)
        currentCoroutineContext().ensureActive()
        val blurred = bitmap?.let {
            withContext(Dispatchers.Default) { it.boxBlurred(FULL_BLUR_PASSES) }
        }
        currentCoroutineContext().ensureActive()
        if (blurred != null) synchronized(blurCache) { blurCache[key] = blurred }
        transition.complete(generation, blurred)
    }
    return transition.value
}

private suspend fun loadSmallArtwork(
    context: android.content.Context,
    artworkUri: Uri,
    size: Int,
): ImageBitmap? {
    val request = ImageRequest.Builder(context)
        .data(artworkUri)
        .size(size)
        .forPixelAccess()
        .build()
    return try {
        (SingletonImageLoader.get(context).execute(request) as? SuccessResult)?.image?.toImageBitmap()
    } catch (cancelled: CancellationException) {
        throw cancelled
    } catch (_: Exception) {
        null
    }
}

/** The lyrics/queue/landscape background, retaining the previous cover during a skip. */
@Composable
fun FullArtworkBlurBackdrop(
    image: ImageBitmap?,
    modifier: Modifier = Modifier,
    reduceAnimation: Boolean = false,
) {
    Box(modifier.fillMaxSize().background(FallbackBackdrop)) {
        Crossfade(
            targetState = image,
            animationSpec = if (reduceAnimation) snap() else tween(320, easing = FastOutSlowInEasing),
            label = "preparedBackdropImage",
        ) { bitmap ->
            if (bitmap != null) {
                Image(
                    painter = remember(bitmap) { BitmapPainter(bitmap) },
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize(),
                )
            }
        }
        Canvas(Modifier.fillMaxSize()) {
            drawRect(
                Brush.verticalGradient(
                    0f to Color.Black.copy(alpha = 0.34f),
                    0.55f to Color.Black.copy(alpha = 0.48f),
                    1f to Color.Black.copy(alpha = 0.64f),
                ),
            )
        }
    }
}

/**
 * Paint a 32-texel, pre-blurred mesh behind the player. The first row is
 * precisely the cover's bottom row (before downsampling); stretch it above
 * [seam] and draw the remaining grid from the sleeve's edge downward.
 *
 * Legacy blurRadius/reduceDynamicBlur arguments remain source-compatible,
 * but this implementation never does a live screen-sized blur.
 */
@Suppress("UNUSED_PARAMETER")
@Composable
fun ArtworkMeshBackdrop(
    mesh: ArtworkMesh?,
    modifier: Modifier = Modifier,
    seam: Dp = 0.dp,
    fallback: Color = FallbackBackdrop,
    blurRadius: Dp = 32.dp,
    reduceAnimation: Boolean = false,
    reduceDynamicBlur: Boolean = false,
) {
    var shown by remember { mutableStateOf(mesh) }
    var incoming by remember { mutableStateOf<ArtworkMesh?>(null) }
    val fade = remember { Animatable(0f) }

    LaunchedEffect(mesh, reduceAnimation) {
        val next = mesh
        if (next == null) {
            incoming = null
            shown = null
            fade.snapTo(0f)
            return@LaunchedEffect
        }
        incoming?.let { shown = it }
        incoming = null
        if (next === shown) return@LaunchedEffect

        if (shown == null || reduceAnimation) {
            shown = next
            fade.snapTo(0f)
            return@LaunchedEffect
        }
        incoming = next
        fade.snapTo(0f)
        fade.animateTo(1f, tween(MESH_FADE_MS, easing = FastOutSlowInEasing))
        shown = next
        incoming = null
    }
    Canvas(modifier.fillMaxSize().background(fallback)) {
        val seamY = seam.toPx().coerceIn(0f, size.height)
        shown?.let { drawMesh(it, seamY, 1f) }
        incoming?.let { drawMesh(it, seamY, fade.value) }
        drawRect(
            brush = Brush.verticalGradient(
                colors = listOf(
                    Color.Black.copy(alpha = 0.06f),
                    Color.Black.copy(alpha = 0.30f),
                ),
            ),
        )
    }
}

private fun DrawScope.drawMesh(mesh: ArtworkMesh, seamY: Float, alpha: Float) {
    if (alpha <= 0.001f) return
    val bitmap = mesh.image
    val width = size.width.roundToInt().coerceAtLeast(1)
    val lowerHeight = (size.height - seamY).roundToInt().coerceAtLeast(0)
    if (seamY > 0.5f) {
        drawImage(
            image = bitmap, srcOffset = IntOffset.Zero,
            srcSize = IntSize(bitmap.width, 1),
            dstOffset = IntOffset.Zero,
            dstSize = IntSize(width, seamY.roundToInt()),
            alpha = alpha, filterQuality = FilterQuality.Low,
        )
    }
    if (lowerHeight > 0) {
        drawImage(
            image = bitmap, srcOffset = IntOffset.Zero,
            srcSize = IntSize(bitmap.width, bitmap.height),
            dstOffset = IntOffset(0, seamY.roundToInt()),
            dstSize = IntSize(width, lowerHeight),
            alpha = alpha, filterQuality = FilterQuality.Low,
        )
    }
}

/**
 * The first row is the source BOTTOM edge. Use means, not a vibrant
 * quantizer, so a mostly black cover does not turn the whole screen red.
 * Exposed as pure Kotlin for the F051 regression test.
 */
internal fun meshCellAverages(
    pixels: IntArray,
    width: Int,
    height: Int,
    cols: Int,
    rows: Int,
): IntArray {
    require(width > 0 && height > 0 && cols in 1..width && rows in 1..height)
    require(pixels.size == width * height)
    val cells = cols * rows
    val reds = LongArray(cells)
    val greens = LongArray(cells)
    val blues = LongArray(cells)
    val counts = IntArray(cells)
    val rowStride = (height / MESH_SAMPLE).coerceAtLeast(1)
    val columnStride = (width / MESH_SAMPLE).coerceAtLeast(1)
    var y = 0
    while (y < height) {
        val base = ((height - 1 - y) * rows / height) * cols
        var x = 0
        while (x < width) {
            val idx = base + x * cols / width
            val pixel = pixels[y * width + x]
            reds[idx] += ((pixel shr 16) and 0xFF)
            greens[idx] += ((pixel shr 8) and 0xFF)
            blues[idx] += (pixel and 0xFF)
            counts[idx]++
            x += columnStride
        }
        y += rowStride
    }
    return IntArray(cells) { n ->
        val count = counts[n].coerceAtLeast(1)
        argb((reds[n] / count).toInt(), (greens[n] / count).toInt(), (blues[n] / count).toInt())
    }
}

private fun meshOf(source: ImageBitmap, seed: Int): ArtworkMesh? {
    val width = source.width
    val height = source.height
    if (width < 1 || height < 1) return null
    val cols = MESH_GRID.coerceAtMost(width)
    val rows = MESH_GRID.coerceAtMost(height)
    val pixels = IntArray(width * height)
    source.readPixels(pixels)
    val grid = meshCellAverages(pixels, width, height, cols, rows).map { it.lifted() }.toIntArray()
    val texels = grid.rotatedBelowSeam(cols, rows, seed).resampled(cols, rows, MESH_TEX)
    val temp = IntArray(texels.size)
    repeat(MESH_BLUR_PASSES) {
        boxBlurPass(texels, temp, MESH_TEX, MESH_TEX, MESH_BLUR_RADIUS, horizontal = true)
        boxBlurPass(temp, texels, MESH_TEX, MESH_TEX, MESH_BLUR_RADIUS, horizontal = false)
    }
    return ArtworkMesh(imageBitmapOf(texels, MESH_TEX, MESH_TEX))
}

/** Preserve neighbours across all shifted rows but the literal seam. */
private fun IntArray.rotatedBelowSeam(cols: Int, rows: Int, seed: Int): IntArray {
    if (rows <= 1) return this
    val random = Random(seed)
    val mirror = random.nextBoolean()
    val shift = random.nextInt(cols)
    val out = copyOf()
    for (row in 1 until rows) {
        val base = row * cols
        for (x in 0 until cols) {
            val sourceIndex = if (mirror) cols - 1 - x else x
            out[base + x] = this[base + (sourceIndex + shift) % cols]
        }
    }
    return out
}

/** Smoothstep interpolation removes visible creases from 6x6-to-32x32 scaling. */
private fun IntArray.resampled(cols: Int, rows: Int, outSize: Int): IntArray {
    val out = IntArray(outSize * outSize)
    for (ty in 0 until outSize) {
        val fy = (ty + 0.5f) / outSize * rows - 0.5f
        val y0 = floor(fy).toInt().coerceIn(0, rows - 1)
        val y1 = (y0 + 1).coerceAtMost(rows - 1)
        val wy = smoothstep(fy - y0)
        for (tx in 0 until outSize) {
            val fx = (tx + 0.5f) / outSize * cols - 0.5f
            val x0 = floor(fx).toInt().coerceIn(0, cols - 1)
            val x1 = (x0 + 1).coerceAtMost(cols - 1)
            val wx = smoothstep(fx - x0)
            val top = lerpArgb(this[y0 * cols + x0], this[y0 * cols + x1], wx)
            val bottom = lerpArgb(this[y1 * cols + x0], this[y1 * cols + x1], wx)
            out[ty * outSize + tx] = lerpArgb(top, bottom, wy)
        }
    }
    return out
}

private fun smoothstep(t: Float): Float {
    val x = t.coerceIn(0f, 1f)
    return x * x * (3f - 2f * x)
}
private fun lerpArgb(from: Int, to: Int, fraction: Float): Int {
    if (fraction <= 0f) return from
    if (fraction >= 1f) return to
    fun channel(shift: Int): Int {
        val a = (from shr shift) and 0xFF
        val b = (to shr shift) and 0xFF
        return (a + ((b - a) * fraction)).roundToInt().coerceIn(0, 255)
    }
    return argb(channel(16), channel(8), channel(0))
}
private fun argb(red: Int, green: Int, blue: Int): Int =
    (0xFF shl 24) or (red shl 16) or (green shl 8) or blue

private fun Int.lifted(): Int {
    val hsl = FloatArray(3).also { ColorUtils.colorToHSL(this, it) }
    hsl[1] = (hsl[1] * MESH_VIBRANCE).coerceAtMost(1f)
    hsl[2] = hsl[2].coerceAtLeast(MESH_FLOOR)
    return ColorUtils.HSLToColor(hsl)
}

/** Three inexpensive horizontal + vertical box-blur passes on a thumbnail. */
private fun ImageBitmap.boxBlurred(passes: Int): ImageBitmap {
    val width = width
    val height = height
    if (width < 2 || height < 2) return this
    var current = IntArray(width * height)
    readPixels(current)
    val scratch = IntArray(current.size)
    val radius = (minOf(width, height) / 12).coerceAtLeast(2)
    repeat(passes) {
        boxBlurPass(current, scratch, width, height, radius, horizontal = true)
        boxBlurPass(scratch, current, width, height, radius, horizontal = false)
    }
    return imageBitmapOf(current, width, height)
}
private fun boxBlurPass(
    source: IntArray, target: IntArray, width: Int, height: Int,
    radius: Int, horizontal: Boolean,
) {
    val major = if (horizontal) width else height
    val minor = if (horizontal) height else width
    val window = radius * 2 + 1
    repeat(minor) { fixed ->
        var red = 0
        var green = 0
        var blue = 0
        fun pixel(at: Int): Int {
            val position = at.coerceIn(0, major - 1)
            return if (horizontal) source[fixed * width + position]
            else source[position * width + fixed]
        }
        for (offset in -radius..radius) {
            val color = pixel(offset)
            red += color shr 16 and 0xFF
            green += color shr 8 and 0xFF
            blue += color and 0xFF
        }
        repeat(major) { moving ->
            val idx = if (horizontal) fixed * width + moving else moving * width + fixed
            target[idx] = argb((red + window / 2) / window, (green + window / 2) / window, (blue + window / 2) / window)
            val leaving = pixel(moving - radius)
            val entering = pixel(moving + radius + 1)
            red += (entering shr 16 and 0xFF) - (leaving shr 16 and 0xFF)
            green += (entering shr 8 and 0xFF) - (leaving shr 8 and 0xFF)
            blue += (entering and 0xFF) - (leaving and 0xFF)
        }
    }
}

private val FallbackBackdrop = Color(0xFF121212)
private const val MESH_PX = 120
private const val MESH_GRID = 6
private const val MESH_TEX = 32
private const val MESH_SAMPLE = 128
private const val MESH_BLUR_RADIUS = 1
private const val MESH_BLUR_PASSES = 3
private const val MESH_FADE_MS = 900
private const val FULL_BLUR_SOURCE_PX = 128
private const val FULL_BLUR_PASSES = 3
private const val MESH_VIBRANCE = 1.12f
private const val MESH_FLOOR = 0.045f
