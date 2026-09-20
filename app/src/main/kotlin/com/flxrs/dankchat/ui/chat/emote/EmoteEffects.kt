package com.flxrs.dankchat.ui.chat.emote

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.ColorMatrix
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.flxrs.dankchat.data.twitch.emote.EmoteEffect
import kotlin.math.cos
import kotlin.math.sin

private const val WIDE_WIDTH_FACTOR = 2f
private const val SHAKE_DURATION_MS = 90
private const val PARTY_DURATION_MS = 1200
private const val CURSED_CONTRAST = 1.8f

// Grayscale (saturation 0) followed by a contrast boost, matching BTTV's "cursed" look
private val CURSED_COLOR_MATRIX =
    ColorMatrix(
        floatArrayOf(
            0.213f * CURSED_CONTRAST,
            0.715f * CURSED_CONTRAST,
            0.072f * CURSED_CONTRAST,
            0f,
            (1f - CURSED_CONTRAST) / 2f * 255f,
            0.213f * CURSED_CONTRAST,
            0.715f * CURSED_CONTRAST,
            0.072f * CURSED_CONTRAST,
            0f,
            (1f - CURSED_CONTRAST) / 2f * 255f,
            0.213f * CURSED_CONTRAST,
            0.715f * CURSED_CONTRAST,
            0.072f * CURSED_CONTRAST,
            0f,
            (1f - CURSED_CONTRAST) / 2f * 255f,
            0f,
            0f,
            0f,
            1f,
            0f,
        ),
    )

private fun hueRotationColorMatrix(degrees: Float): ColorMatrix {
    val radians = Math.toRadians(degrees.toDouble())
    val cos = cos(radians).toFloat()
    val sin = sin(radians).toFloat()

    // Luminance-preserving hue rotation (same weights as feColorMatrix hueRotate)
    val lumR = 0.213f
    val lumG = 0.715f
    val lumB = 0.072f

    return ColorMatrix(
        floatArrayOf(
            lumR + cos * (1f - lumR) + sin * -lumR,
            lumG + cos * -lumG + sin * -lumG,
            lumB + cos * -lumB + sin * (1f - lumB),
            0f,
            0f,
            lumR + cos * -lumR + sin * 0.143f,
            lumG + cos * (1f - lumG) + sin * 0.140f,
            lumB + cos * -lumB + sin * -0.283f,
            0f,
            0f,
            lumR + cos * -lumR + sin * -(1f - lumR),
            lumG + cos * -lumG + sin * lumG,
            lumB + cos * (1f - lumB) + sin * lumB,
            0f,
            0f,
            0f,
            0f,
            0f,
            1f,
            0f,
        ),
    )
}

/** Display size multipliers so effect-modified emotes reserve the correct inline slot. */
fun effectAdjustedSize(
    widthPx: Int,
    heightPx: Int,
    effects: Set<EmoteEffect>,
): Pair<Int, Int> {
    val isWide = EmoteEffect.Wide in effects
    val isRotated = EmoteEffect.RotateLeft in effects || EmoteEffect.RotateRight in effects

    val width = if (isWide) (widthPx * WIDE_WIDTH_FACTOR).toInt() else widthPx
    return when {
        isRotated -> heightPx to width
        else -> width to heightPx
    }
}

/**
 * Renders an emote drawable with BTTV/FFZ-style effects applied. Effects are applied via
 * graphicsLayer/colorFilter so animated drawables (GIF/WEBP) keep playing.
 */
@Composable
fun EffectedEmoteImage(
    painter: Painter,
    width: Dp,
    height: Dp,
    effects: Set<EmoteEffect>,
    animate: Boolean,
    alpha: Float,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    if (effects.isEmpty()) {
        Image(
            painter = painter,
            contentDescription = null,
            alpha = alpha,
            modifier =
                modifier
                    .size(width = width, height = height)
                    .clickable { onClick() },
        )
        return
    }

    val density = LocalDensity.current
    val isWide = EmoteEffect.Wide in effects
    val rotation =
        when {
            EmoteEffect.RotateLeft in effects -> -90f
            EmoteEffect.RotateRight in effects -> 90f
            else -> 0f
        }
    val isRotated = rotation != 0f

    val shakeProgress by
        rememberInfiniteTransition(label = "emoteShake")
            .animateFloat(
                initialValue = -1f,
                targetValue = 1f,
                animationSpec = infiniteRepeatable(tween(SHAKE_DURATION_MS, easing = LinearEasing), RepeatMode.Reverse),
                label = "shake",
            )
    val partyHue by
        rememberInfiniteTransition(label = "emoteParty")
            .animateFloat(
                initialValue = 0f,
                targetValue = 360f,
                animationSpec = infiniteRepeatable(tween(PARTY_DURATION_MS, easing = LinearEasing), RepeatMode.Restart),
                label = "party",
            )

    val shakeX = if (animate && EmoteEffect.Shake in effects) shakeProgress else 0f
    val hue = if (animate && EmoteEffect.Party in effects) partyHue else 0f
    val colorFilter =
        when {
            EmoteEffect.Party in effects -> ColorFilter.colorMatrix(hueRotationColorMatrix(hue))
            EmoteEffect.Cursed in effects -> ColorFilter.colorMatrix(CURSED_COLOR_MATRIX)
            else -> null
        }

    val boxWidth =
        when {
            isRotated -> height
            isWide -> width * WIDE_WIDTH_FACTOR
            else -> width
        }
    val boxHeight =
        when {
            isRotated -> width * if (isWide) WIDE_WIDTH_FACTOR else 1f
            else -> height
        }

    Box(
        contentAlignment = Alignment.Center,
        modifier =
            modifier
                .size(width = boxWidth, height = boxHeight)
                .clickable { onClick() },
    ) {
        Image(
            painter = painter,
            contentDescription = null,
            alpha = alpha,
            colorFilter = colorFilter,
            modifier =
                Modifier
                    .size(width = width, height = height)
                    .graphicsLayer {
                        val wideScale = if (isWide) WIDE_WIDTH_FACTOR else 1f
                        scaleX = wideScale * if (EmoteEffect.FlipHorizontal in effects) -1f else 1f
                        scaleY = if (EmoteEffect.FlipVertical in effects) -1f else 1f
                        rotationZ = rotation
                        if (shakeX != 0f) {
                            translationX = shakeX * with(density) { 2.dp.toPx() }
                        }
                    },
        )
    }
}
