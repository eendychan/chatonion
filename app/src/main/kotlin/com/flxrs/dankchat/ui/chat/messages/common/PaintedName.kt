package com.flxrs.dankchat.ui.chat.messages.common

import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import com.flxrs.dankchat.data.twitch.paint.SevenTVPaint

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
 * always span the exact name bounds. Image paints are masked out of a (possibly animated)
 * image like CSS background-clip: text, and every drop shadow of the paint is drawn as
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
 * Masks the name out of the paint's image (cover + center, like the 7TV web extension).
 * The image stays a composable, so animated webp paints keep animating; a plain colored
 * copy underneath keeps the name readable while the image is loading or failed to load.
 */
@Composable
private fun ImagePaintedName(
    name: PaintedNameUi,
    fontSize: Float,
    imageUrl: String,
) {
    val textStyle = TextStyle(fontSize = fontSize.sp, fontWeight = FontWeight.Bold)
    val textMeasurer = rememberTextMeasurer()
    val nameLayout = remember(name.text, fontSize) { textMeasurer.measure(AnnotatedString(name.text), style = textStyle) }
    val namePath = remember(nameLayout) { nameLayout.getPathForRange(0, name.text.length) }

    Box {
        // Keeps the name readable while the image is loading or failed to load
        BasicText(
            text = name.text,
            style = TextStyle(fontSize = fontSize.sp, fontWeight = FontWeight.Bold, color = name.fallbackColor),
            maxLines = 1,
            softWrap = false,
        )
        // Clipping to the glyph path works on the plain canvas, unlike blend modes that
        // depend on an offscreen compositing layer being honored by the device
        Box(
            modifier =
                Modifier.drawWithContent {
                    clipPath(namePath) {
                        this@drawWithContent.drawContent()
                    }
                },
        ) {
            AsyncImage(
                model = imageUrl,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.matchParentSize(),
            )
            // Sizes the box to the name bounds; the image is only visible inside the glyphs
            BasicText(
                text = name.text,
                style = TextStyle(fontSize = fontSize.sp, fontWeight = FontWeight.Bold, color = Color.Transparent),
                maxLines = 1,
                softWrap = false,
            )
        }
    }
}
