package com.flxrs.dankchat.ui.chat.messages.common

import android.graphics.drawable.Animatable
import android.graphics.drawable.Drawable
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.Paint
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.graphics.drawscope.CanvasDrawScope
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.sp
import coil3.SingletonImageLoader
import coil3.asDrawable
import coil3.compose.LocalPlatformContext
import coil3.request.ImageRequest
import coil3.request.SuccessResult
import com.flxrs.dankchat.data.twitch.paint.SevenTVPaint
import kotlinx.coroutines.awaitCancellation
import coil3.size.Size as CoilSize

const val PAINTED_NAME_INLINE_ID = "PAINTED_NAME"

/** A chat user name decorated with a 7TV name paint, rendered as inline content. */
data class PaintedNameUi(
    val text: String,
    val paint: SevenTVPaint,
    val fallbackColor: Color,
)

/** A painted mention of an active chatter inside a message, rendered as inline content. */
data class PaintedMentionUi(
    val inlineId: String,
    val name: PaintedNameUi,
)

/**
 * Renders a painted user name. Unlike a brush in a span style — where the shader is sized
 * to the whole message paragraph — here the name is its own inline element, so paints
 * always span the exact name bounds. Image paints are drawn through the glyph shapes of
 * the name like CSS background-clip: text, and every drop shadow of the paint is drawn as
 * its own underlay copy, mirroring the stacked filter: drop-shadow(...) of the web version.
 */
@Composable
fun PaintedNameText(
    name: PaintedNameUi,
    fontSize: Float,
    onClick: () -> Unit = {},
    onLongClick: () -> Unit = {},
) {
    val paint = name.paint
    val spanStyle = paint.toNameSpanStyle(fallbackColor = name.fallbackColor)
    val imageUrl = paint.imageUrl.takeIf { paint.function == SevenTVPaint.PaintFunction.ImageUrl }

    // Taps are handled here instead of through the text layout: an inline placeholder is a
    // single object-replacement character, so resolving the USER annotation by text offset
    // only works for taps landing exactly on its edges
    Box(
        modifier =
            Modifier.pointerInput(name.text, paint.id) {
                detectTapGestures(
                    onTap = { onClick() },
                    onLongPress = { onLongClick() },
                )
            },
    ) {
        paint.shadows.forEach { shadow ->
            BasicText(
                text = name.text,
                style =
                    TextStyle(
                        fontSize = fontSize.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(shadow.argb),
                        shadow =
                            Shadow(
                                color = Color(shadow.argb),
                                offset = Offset(shadow.xOffset, shadow.yOffset),
                                blurRadius = shadow.radius,
                            ),
                    ),
                maxLines = 1,
                softWrap = false,
            )
        }

        when {
            imageUrl != null -> ImagePaintedName(name = name, fontSize = fontSize, imageUrl = imageUrl)

            spanStyle.brush != null ->
                BasicText(
                    text = name.text,
                    style =
                        TextStyle(
                            fontSize = fontSize.sp,
                            fontWeight = FontWeight.Bold,
                            brush = spanStyle.brush,
                        ),
                    maxLines = 1,
                    softWrap = false,
                )

            else ->
                BasicText(
                    text = name.text,
                    style =
                        TextStyle(
                            fontSize = fontSize.sp,
                            fontWeight = FontWeight.Bold,
                            color = spanStyle.color,
                        ),
                    maxLines = 1,
                    softWrap = false,
                )
        }
    }
}

/**
 * Fills the glyph shapes of the name with the paint's image (cover + center, like the 7TV
 * web extension). The image is composited through a rasterized glyph mask with
 * [BlendMode.DstIn] inside a saved layer — clipping by TextLayoutResult.getPathForRange is
 * not an option, as that path is the selection rectangle of the range, not glyph outlines.
 * The image drawable is driven directly, so animated webp/gif paints keep animating;
 * a plain colored copy underneath keeps the name readable while the image loads.
 */
@Composable
private fun ImagePaintedName(
    name: PaintedNameUi,
    fontSize: Float,
    imageUrl: String,
) {
    val textStyle = TextStyle(fontSize = fontSize.sp, fontWeight = FontWeight.Bold)
    val textMeasurer = rememberTextMeasurer()
    val density = LocalDensity.current
    val nameLayout = remember(name.text, fontSize) { textMeasurer.measure(AnnotatedString(name.text), style = textStyle) }

    // Opaque name glyphs on a transparent bitmap, rasterized once per name text
    val glyphMask = remember(nameLayout) {
        val maskWidth = nameLayout.size.width.coerceAtLeast(1)
        val maskHeight = nameLayout.size.height.coerceAtLeast(1)
        ImageBitmap(maskWidth, maskHeight).also { bitmap ->
            val maskCanvas = androidx.compose.ui.graphics
                .Canvas(bitmap)
            CanvasDrawScope().draw(
                density = density,
                layoutDirection = LayoutDirection.Ltr,
                canvas = maskCanvas,
                size = Size(maskWidth.toFloat(), maskHeight.toFloat()),
            ) {
                drawText(textLayoutResult = nameLayout)
            }
        }
    }

    val context = LocalPlatformContext.current
    var frameTick by remember { mutableIntStateOf(0) }
    val paintDrawable by produceState<Drawable?>(initialValue = null, imageUrl) {
        val imageLoader = SingletonImageLoader.get(context)
        val result = imageLoader.execute(
            ImageRequest
                .Builder(context)
                .data(imageUrl)
                .size(CoilSize.ORIGINAL)
                .build(),
        )
        val drawable = (result as? SuccessResult)?.image?.asDrawable(context.resources)
        if (drawable != null) {
            drawable.callback =
                object : Drawable.Callback {
                    override fun invalidateDrawable(who: Drawable) {
                        frameTick++
                    }

                    override fun scheduleDrawable(
                        who: Drawable,
                        what: Runnable,
                        `when`: Long,
                    ) = Unit

                    override fun unscheduleDrawable(
                        who: Drawable,
                        what: Runnable,
                    ) = Unit
                }
            (drawable as? Animatable)?.start()
            value = drawable
        }
        try {
            awaitCancellation()
        } finally {
            (drawable as? Animatable)?.stop()
            drawable?.callback = null
        }
    }

    val layerPaint = remember { Paint() }
    val maskPaint = remember { Paint().apply { blendMode = BlendMode.DstIn } }

    Box {
        // Keeps the name readable while the image is loading or failed to load
        BasicText(
            text = name.text,
            style = TextStyle(fontSize = fontSize.sp, fontWeight = FontWeight.Bold, color = name.fallbackColor),
            maxLines = 1,
            softWrap = false,
        )

        val drawable = paintDrawable
        if (drawable != null && drawable.intrinsicWidth > 0 && drawable.intrinsicHeight > 0) {
            Canvas(
                modifier =
                    with(density) {
                        Modifier.size(nameLayout.size.width.toDp(), nameLayout.size.height.toDp())
                    },
            ) {
                frameTick // reading the frame counter re-runs this draw for every animation frame
                drawIntoCanvas { canvas ->
                    canvas.saveLayer(Rect(Offset.Zero, size), layerPaint)
                    val scale = maxOf(size.width / drawable.intrinsicWidth, size.height / drawable.intrinsicHeight)
                    canvas.save()
                    canvas.translate(
                        dx = (size.width - drawable.intrinsicWidth * scale) / 2f,
                        dy = (size.height - drawable.intrinsicHeight * scale) / 2f,
                    )
                    canvas.scale(scale, scale)
                    drawable.setBounds(0, 0, drawable.intrinsicWidth, drawable.intrinsicHeight)
                    drawable.draw(canvas.nativeCanvas)
                    canvas.restore()
                    canvas.drawImageRect(
                        image = glyphMask,
                        srcOffset = IntOffset.Zero,
                        srcSize = IntSize(glyphMask.width, glyphMask.height),
                        dstOffset = IntOffset.Zero,
                        dstSize = IntSize(size.width.toInt(), size.height.toInt()),
                        paint = maskPaint,
                    )
                    canvas.restore()
                }
            }
        }
    }
}
