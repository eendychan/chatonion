package com.flxrs.dankchat.ui.main.stream

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animate
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import org.koin.compose.viewmodel.koinViewModel
import kotlin.math.roundToInt

private const val EDGE_THICKNESS = 2

// Full strength right at the edge of the video, then falling off towards the end of the glow
private val FADE_STOPS =
    arrayOf(
        0f to Color.Black,
        0.3f to Color.Black.copy(alpha = 0.6f),
        1f to Color.Transparent,
    )

/**
 * A soft glow in the chat background that follows the colors along the edge of the stream next to
 * the chat - the bottom edge when the chat sits under the video, the right edge when it sits beside
 * it - fading out with distance from the video. How far it reaches into the chat is a setting.
 */
@Composable
fun AmbilightBackground(
    chatBesideStream: Boolean,
    modifier: Modifier = Modifier,
) {
    val streamViewModel: StreamViewModel = koinViewModel()
    val enabled by streamViewModel.ambilightEnabled.collectAsStateWithLifecycle()
    val opacity by streamViewModel.ambilightOpacity.collectAsStateWithLifecycle()
    val spread by streamViewModel.ambilightSpread.collectAsStateWithLifecycle()
    val powerSaving by streamViewModel.ambilightPowerSaving.collectAsStateWithLifecycle()
    val frame by streamViewModel.ambilightColors.collectAsStateWithLifecycle()

    // What's drawn moves smoothly towards each newly sampled frame instead of jumping to it
    var shown by remember { mutableStateOf<ImageBitmap?>(null) }
    val current = remember { IntArray(AMBILIGHT_COLUMNS * AMBILIGHT_ROWS) }

    LaunchedEffect(frame, powerSaving) {
        val target = frame?.takeIf { it.size == current.size }?.let(::softened)
        if (target == null) {
            shown = null
            return@LaunchedEffect
        }
        if (shown == null) {
            target.copyInto(current)
            shown = current.toBitmap()
            return@LaunchedEffect
        }
        val from = current.copyOf()
        val duration = if (powerSaving) AMBILIGHT_INTERVAL_POWER_SAVING_MS else AMBILIGHT_INTERVAL_SMOOTH_MS
        animate(
            initialValue = 0f,
            targetValue = 1f,
            animationSpec = tween(durationMillis = duration.toInt(), easing = LinearEasing),
        ) { progress, _ ->
            for (index in current.indices) {
                current[index] = lerpArgb(from[index], target[index], progress)
            }
            shown = current.toBitmap()
        }
    }

    val bitmap = shown
    if (!enabled || bitmap == null) return

    // Only the strip of the frame that borders the chat is used - bilinear scaling of such a tiny
    // image is what gives the glow its blur
    val srcOffset =
        if (chatBesideStream) IntOffset(AMBILIGHT_COLUMNS - EDGE_THICKNESS, 0) else IntOffset(0, AMBILIGHT_ROWS - EDGE_THICKNESS)
    val srcSize =
        if (chatBesideStream) IntSize(EDGE_THICKNESS, AMBILIGHT_ROWS) else IntSize(AMBILIGHT_COLUMNS, EDGE_THICKNESS)

    Canvas(
        modifier =
            modifier
                .fillMaxSize()
                .graphicsLayer {
                    alpha = opacity
                    compositingStrategy = CompositingStrategy.Offscreen
                },
    ) {
        // Only this part of the chat, next to the video, gets the glow
        val glow = if (chatBesideStream) Size(size.width * spread, size.height) else Size(size.width, size.height * spread)
        drawImage(
            image = bitmap,
            srcOffset = srcOffset,
            srcSize = srcSize,
            dstSize = IntSize(glow.width.roundToInt(), glow.height.roundToInt()),
            filterQuality = FilterQuality.Low,
        )
        val fade =
            if (chatBesideStream) {
                Brush.horizontalGradient(colorStops = FADE_STOPS, startX = 0f, endX = glow.width)
            } else {
                Brush.verticalGradient(colorStops = FADE_STOPS, startY = 0f, endY = glow.height)
            }
        drawRect(brush = fade, size = glow, blendMode = BlendMode.DstIn)
    }
}

private fun IntArray.toBitmap(): ImageBitmap =
    ImageBitmap(AMBILIGHT_COLUMNS, AMBILIGHT_ROWS).also {
        it.asAndroidBitmap().setPixels(this, 0, AMBILIGHT_COLUMNS, 0, 0, AMBILIGHT_COLUMNS, AMBILIGHT_ROWS)
    }

// A small box blur, so neighbouring columns/rows of the sample don't show up as separate streaks
private fun softened(pixels: IntArray): IntArray =
    IntArray(pixels.size) { index -> averageAround(pixels, index % AMBILIGHT_COLUMNS, index / AMBILIGHT_COLUMNS) }

private fun averageAround(
    pixels: IntArray,
    x: Int,
    y: Int,
): Int {
    var red = 0
    var green = 0
    var blue = 0
    var count = 0
    for (row in (y - 1).coerceAtLeast(0)..(y + 1).coerceAtMost(AMBILIGHT_ROWS - 1)) {
        for (column in (x - 1).coerceAtLeast(0)..(x + 1).coerceAtMost(AMBILIGHT_COLUMNS - 1)) {
            val pixel = pixels[row * AMBILIGHT_COLUMNS + column]
            red += (pixel shr 16) and 0xFF
            green += (pixel shr 8) and 0xFF
            blue += pixel and 0xFF
            count++
        }
    }
    return (0xFF shl 24) or ((red / count) shl 16) or ((green / count) shl 8) or (blue / count)
}

private fun lerpArgb(
    from: Int,
    to: Int,
    fraction: Float,
): Int {
    val red = lerpChannel(from shr 16, to shr 16, fraction)
    val green = lerpChannel(from shr 8, to shr 8, fraction)
    val blue = lerpChannel(from, to, fraction)
    return (0xFF shl 24) or (red shl 16) or (green shl 8) or blue
}

private fun lerpChannel(
    from: Int,
    to: Int,
    fraction: Float,
): Int = ((from and 0xFF) + ((to and 0xFF) - (from and 0xFF)) * fraction).roundToInt()
