package com.flxrs.dankchat.ui.chat.messages.common

import android.graphics.Bitmap
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.ImageShader
import androidx.compose.ui.graphics.Shader
import androidx.compose.ui.graphics.ShaderBrush
import androidx.compose.ui.graphics.TileMode
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import coil3.asDrawable
import coil3.compose.LocalPlatformContext
import coil3.imageLoader
import coil3.request.ImageRequest
import coil3.size.Size
import com.flxrs.dankchat.data.twitch.paint.SevenTVPaint
import kotlin.math.roundToInt
import androidx.compose.ui.geometry.Size as GeometrySize

const val PAINTED_NAME_INLINE_ID = "PAINTED_NAME"

/** A chat user name decorated with a 7TV name paint, rendered as inline content. */
data class PaintedNameUi(
    val text: String,
    val paint: SevenTVPaint,
    val fallbackColor: Color,
)

/**
 * Renders a painted user name. Unlike a brush in a span style — where the shader is sized
 * to the whole message paragraph — here the brush shader receives the exact bounds of the
 * name, so gradients always span the full nickname instead of showing only a slice of it.
 */
@Composable
fun PaintedNameText(
    name: PaintedNameUi,
    fontSize: Float,
    heightPx: Int,
) {
    val paint = name.paint
    val imageUrl = paint.imageUrl.takeIf { paint.function == SevenTVPaint.PaintFunction.ImageUrl }
    val imageBrush = imageUrl?.let { rememberPaintImageBrush(url = it, heightPx = heightPx) }

    val spanStyle = paint.toNameSpanStyle(fallbackColor = name.fallbackColor)
    val brush = imageBrush ?: spanStyle.brush
    val style =
        TextStyle(
            fontSize = fontSize.sp,
            fontWeight = FontWeight.Bold,
            brush = brush,
            color = if (brush != null) Color.Unspecified else spanStyle.color,
            shadow = spanStyle.shadow,
        )

    BasicText(
        text = name.text,
        style = style,
        maxLines = 1,
        softWrap = false,
    )
}

/**
 * Loads the image of an image-based paint and turns it into a horizontally repeating brush,
 * scaled to the line height like in Chatterino7 and the browser extension.
 */
@Composable
private fun rememberPaintImageBrush(
    url: String,
    heightPx: Int,
): ShaderBrush? {
    val context = LocalPlatformContext.current
    val bitmap by produceState<ImageBitmap?>(initialValue = null, url, heightPx) {
        value =
            runCatching {
                val request =
                    ImageRequest
                        .Builder(context)
                        .data(url)
                        .size(Size.ORIGINAL)
                        .build()
                val drawable = context.imageLoader
                    .execute(request)
                    .image
                    ?.asDrawable(context.resources) ?: return@runCatching null
                val srcWidth = drawable.intrinsicWidth
                val srcHeight = drawable.intrinsicHeight
                if (srcWidth <= 0 || srcHeight <= 0 || heightPx <= 0) {
                    return@runCatching null
                }

                val scaledWidth = (srcWidth * (heightPx.toFloat() / srcHeight)).roundToInt().coerceAtLeast(1)
                val bitmap = Bitmap.createBitmap(scaledWidth, heightPx, Bitmap.Config.ARGB_8888)
                drawable.setBounds(0, 0, scaledWidth, heightPx)
                drawable.draw(android.graphics.Canvas(bitmap))
                bitmap.asImageBitmap()
            }.getOrNull()
    }

    return bitmap?.let { remember(it) { ImagePaintBrush(it) } }
}

private class ImagePaintBrush(
    image: ImageBitmap,
) : ShaderBrush() {
    private val shader = ImageShader(image, TileMode.Repeated, TileMode.Clamp)

    override fun createShader(size: GeometrySize): Shader = shader
}
