package com.flxrs.dankchat.ui.chat.messages.common

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.LinearGradientShader
import androidx.compose.ui.graphics.RadialGradientShader
import androidx.compose.ui.graphics.Shader
import androidx.compose.ui.graphics.ShaderBrush
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.graphics.TileMode
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.font.FontWeight
import com.flxrs.dankchat.data.twitch.paint.SevenTVPaint
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.sin

/**
 * Converts a 7TV name paint to a [SpanStyle] for the chat user name,
 * see Chatterino7's SeventvPaints. Gradient angles follow CSS semantics
 * (0 degrees points up, 90 degrees points right).
 */
fun SevenTVPaint.toNameSpanStyle(fallbackColor: Color): SpanStyle {
    val paintShadow =
        strongestShadow?.let { shadow ->
            Shadow(
                color = Color(shadow.argb),
                offset = Offset(shadow.xOffset, shadow.yOffset),
                blurRadius = shadow.radius,
            )
        }

    val sortedStops = stops.sortedBy { it.at }
    return when {
        function != SevenTVPaint.PaintFunction.ImageUrl && sortedStops.size >= 2 -> {
            SpanStyle(
                fontWeight = FontWeight.Bold,
                brush = PaintShaderBrush(this, sortedStops),
                shadow = paintShadow,
            )
        }

        color != null -> {
            SpanStyle(
                fontWeight = FontWeight.Bold,
                color = Color(color),
                shadow = paintShadow,
            )
        }

        else -> {
            SpanStyle(
                fontWeight = FontWeight.Bold,
                color = fallbackColor,
                shadow = paintShadow,
            )
        }
    }
}

private class PaintShaderBrush(
    private val paint: SevenTVPaint,
    sortedStops: List<SevenTVPaint.Stop>,
) : ShaderBrush() {
    private val colors = sortedStops.map { Color(it.argb) }
    private val positions = sortedStops.map { it.at.coerceIn(0f, 1f) }

    override fun createShader(size: Size): Shader {
        val tileMode = if (paint.repeat) TileMode.Repeated else TileMode.Clamp
        if (paint.function == SevenTVPaint.PaintFunction.RadialGradient) {
            return RadialGradientShader(
                colors = colors,
                colorStops = positions,
                center = Offset(size.width / 2f, size.height / 2f),
                radius = max(size.width, size.height) / 2f,
                tileMode = tileMode,
            )
        }

        // CSS angle semantics: 0 degrees points up, 90 degrees points right
        val angleRad = Math.toRadians(paint.angleDegrees.toDouble())
        val directionX = sin(angleRad).toFloat()
        val directionY = -cos(angleRad).toFloat()
        val centerX = size.width / 2f
        val centerY = size.height / 2f

        // Span the gradient line across the projections of all rect corners on the direction vector
        val projections =
            CORNER_OFFSETS.map { (cornerX, cornerY) ->
                (cornerX * size.width - centerX) * directionX + (cornerY * size.height - centerY) * directionY
            }
        val minProjection = projections.min()
        val maxProjection = projections.max()

        return LinearGradientShader(
            colors = colors,
            colorStops = positions,
            from = Offset(centerX + directionX * minProjection, centerY + directionY * minProjection),
            to = Offset(centerX + directionX * maxProjection, centerY + directionY * maxProjection),
            tileMode = tileMode,
        )
    }

    private companion object {
        val CORNER_OFFSETS = listOf(0f to 0f, 1f to 0f, 0f to 1f, 1f to 1f)
    }
}
