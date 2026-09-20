package com.flxrs.dankchat.ui.chat.image

import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.OpenInBrowser
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Popup
import androidx.compose.ui.window.PopupProperties
import coil3.compose.AsyncImage
import coil3.compose.LocalPlatformContext
import com.flxrs.dankchat.R
import com.flxrs.dankchat.ui.chat.messages.common.launchCustomTab
import kotlin.math.roundToInt

private const val MIN_SCALE = 1f
private const val MAX_SCALE = 6f

/**
 * Movable, zoomable image viewer window — behaves like the user popup card:
 * drag the header to move, pinch/double-tap the image to zoom.
 */
@Composable
fun ChatImagePreviewPopup(
    imageUrl: String,
    onDismiss: () -> Unit,
) {
    val density = LocalDensity.current
    val configuration = LocalConfiguration.current
    val context = LocalPlatformContext.current
    val screenWidthPx = with(density) { configuration.screenWidthDp.dp.toPx() }
    val screenHeightPx = with(density) { configuration.screenHeightDp.dp.toPx() }
    var dragOffset by remember { mutableStateOf(IntOffset.Zero) }
    var scale by remember { mutableFloatStateOf(1f) }
    var translation by remember { mutableStateOf(Offset.Zero) }

    Popup(
        alignment = Alignment.Center,
        offset = dragOffset,
        properties = PopupProperties(focusable = true, usePlatformDefaultWidth = false, clippingEnabled = false),
        onDismissRequest = onDismiss,
    ) {
        Surface(
            shape = RoundedCornerShape(16.dp),
            color = MaterialTheme.colorScheme.surfaceContainerHigh,
            shadowElevation = 8.dp,
            modifier =
                Modifier
                    .fillMaxWidth(0.85f)
                    .height((configuration.screenHeightDp * 0.6f).dp),
        ) {
            Box(modifier = Modifier.fillMaxSize()) {
                // Zoomable image
                AsyncImage(
                    model = imageUrl,
                    contentDescription = stringResource(R.string.image_preview_enabled_title),
                    contentScale = ContentScale.Fit,
                    modifier =
                        Modifier
                            .fillMaxSize()
                            .padding(top = 48.dp)
                            .clip(RoundedCornerShape(bottomStart = 16.dp, bottomEnd = 16.dp))
                            .pointerInput(Unit) {
                                detectTransformGestures { _, pan, zoom, _ ->
                                    val newScale = (scale * zoom).coerceIn(MIN_SCALE, MAX_SCALE)
                                    scale = newScale
                                    translation =
                                        if (newScale > MIN_SCALE) {
                                            translation + pan
                                        } else {
                                            Offset.Zero
                                        }
                                }
                            }.pointerInput(Unit) {
                                detectTapGestures(
                                    onDoubleTap = {
                                        if (scale > MIN_SCALE) {
                                            scale = MIN_SCALE
                                            translation = Offset.Zero
                                        } else {
                                            scale = 2.5f
                                        }
                                    },
                                )
                            }.graphicsLayer(
                                scaleX = scale,
                                scaleY = scale,
                                translationX = translation.x,
                                translationY = translation.y,
                            ),
                )

                // Drag handle header
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                            .background(Color.Black.copy(alpha = 0.35f))
                            .pointerInput(Unit) {
                                detectDragGestures { change, dragAmount ->
                                    change.consume()
                                    val maxX = (screenWidthPx / 2).roundToInt()
                                    val maxY = (screenHeightPx / 2).roundToInt()
                                    val newX = (dragOffset.x + dragAmount.x.roundToInt()).coerceIn(-maxX, maxX)
                                    val newY = (dragOffset.y + dragAmount.y.roundToInt()).coerceIn(-maxY, maxY)
                                    dragOffset = IntOffset(newX, newY)
                                }
                            }.padding(horizontal = 4.dp),
                ) {
                    IconButton(onClick = { launchCustomTab(context, imageUrl) }) {
                        Icon(
                            imageVector = Icons.Default.OpenInBrowser,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(20.dp),
                        )
                    }
                    Box(modifier = Modifier.weight(1f))
                    IconButton(onClick = onDismiss) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = stringResource(R.string.back),
                            tint = Color.White,
                            modifier = Modifier.size(20.dp),
                        )
                    }
                }
            }
        }
    }
}
