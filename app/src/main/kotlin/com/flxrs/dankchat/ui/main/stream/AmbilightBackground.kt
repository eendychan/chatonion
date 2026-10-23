package com.flxrs.dankchat.ui.main.stream

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
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

/**
 * A soft glow in the chat background that follows the colors along the edge of the stream next to
 * the chat - the bottom edge when the chat sits under the video, the right edge when it sits beside
 * it - fading out with distance from the video.
 */
@Composable
fun AmbilightBackground(
    chatBesideStream: Boolean,
    modifier: Modifier = Modifier,
) {
    val streamViewModel: StreamViewModel = koinViewModel()
    val enabled by streamViewModel.ambilightEnabled.collectAsStateWithLifecycle()
    val opacity by streamViewModel.ambilightOpacity.collectAsStateWithLifecycle()
    val frame by streamViewModel.ambilightColors.collectAsStateWithLifecycle()

    val pixels = frame
    if (!enabled || pixels == null || pixels.size != AMBILIGHT_COLUMNS * AMBILIGHT_ROWS) return

    val bitmap =
        remember(pixels) {
            ImageBitmap(AMBILIGHT_COLUMNS, AMBILIGHT_ROWS).also {
                it.asAndroidBitmap().setPixels(pixels, 0, AMBILIGHT_COLUMNS, 0, 0, AMBILIGHT_COLUMNS, AMBILIGHT_ROWS)
            }
        }

    // Only the strip of the frame that borders the chat is used, stretched across the whole chat
    // area - bilinear scaling of such a tiny image is what gives the glow its blur
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
        drawImage(
            image = bitmap,
            srcOffset = srcOffset,
            srcSize = srcSize,
            dstSize = IntSize(size.width.roundToInt(), size.height.roundToInt()),
            filterQuality = FilterQuality.Low,
        )
        // Strongest right next to the video, fading out over the chat
        val fade =
            if (chatBesideStream) {
                Brush.horizontalGradient(listOf(Color.Black, Color.Transparent), startX = 0f, endX = size.width)
            } else {
                Brush.verticalGradient(listOf(Color.Black, Color.Transparent), startY = 0f, endY = size.height)
            }
        drawRect(brush = fade, topLeft = Offset.Zero, size = size, blendMode = BlendMode.DstIn)
    }
}
