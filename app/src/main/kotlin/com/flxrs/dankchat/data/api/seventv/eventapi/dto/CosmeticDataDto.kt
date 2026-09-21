package com.flxrs.dankchat.data.api.seventv.eventapi.dto

import androidx.annotation.Keep
import com.flxrs.dankchat.data.twitch.badge.SevenTVBadgeCosmetic
import com.flxrs.dankchat.data.twitch.paint.SevenTVPaint
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Payload of a "PAINT" cosmetic, see the data field of a cosmetic.create dispatch.
 * Colors are packed as RGBA uint32 (R in the highest byte, A in the lowest) and are
 * serialized by 7TV as unsigned numbers, so they must be parsed as [Long] — values
 * with the R or A byte >= 0x80 overflow a signed Int.
 */
@Keep
@Serializable
data class SevenTVPaintDataDto(
    val id: String,
    val name: String,
    val color: Long? = null,
    val function: String = FUNCTION_LINEAR_GRADIENT,
    val repeat: Boolean = false,
    val angle: Float = 0f,
    val stops: List<PaintStopDto> = emptyList(),
    val shadows: List<PaintShadowDto> = emptyList(),
    @SerialName("image_url") val imageUrl: String? = null,
) {
    fun toDomain(): SevenTVPaint {
        val paintFunction =
            when (function) {
                FUNCTION_RADIAL_GRADIENT -> SevenTVPaint.PaintFunction.RadialGradient
                FUNCTION_IMAGE_URL -> SevenTVPaint.PaintFunction.ImageUrl
                else -> SevenTVPaint.PaintFunction.LinearGradient
            }

        return SevenTVPaint(
            id = id,
            name = name,
            color = color?.let { SevenTVPaint.rgbaToArgb(it.toInt()) },
            function = paintFunction,
            repeat = repeat,
            angleDegrees = angle,
            stops =
                stops.map { stop ->
                    SevenTVPaint.Stop(at = stop.at, argb = SevenTVPaint.rgbaToArgb(stop.color.toInt()))
                },
            shadows =
                shadows.map { shadow ->
                    SevenTVPaint.Shadow(
                        xOffset = shadow.xOffset,
                        yOffset = shadow.yOffset,
                        radius = shadow.radius,
                        argb = SevenTVPaint.rgbaToArgb(shadow.color.toInt()),
                    )
                },
            imageUrl = imageUrl?.takeIf { it.isNotBlank() },
        )
    }

    companion object {
        const val FUNCTION_LINEAR_GRADIENT = "LINEAR_GRADIENT"
        const val FUNCTION_RADIAL_GRADIENT = "RADIAL_GRADIENT"
        const val FUNCTION_IMAGE_URL = "URL"
    }
}

@Keep
@Serializable
data class PaintStopDto(
    val at: Float,
    val color: Long,
)

@Keep
@Serializable
data class PaintShadowDto(
    @SerialName("x_offset") val xOffset: Float = 0f,
    @SerialName("y_offset") val yOffset: Float = 0f,
    val radius: Float = 0f,
    val color: Long,
)

/**
 * Payload of a "BADGE" cosmetic, see the data field of a cosmetic.create dispatch.
 */
@Keep
@Serializable
data class SevenTVBadgeDataDto(
    val id: String,
    val name: String,
    val tooltip: String? = null,
    val host: BadgeHostDto,
) {
    fun toDomain(): SevenTVBadgeCosmetic? {
        val file = host.files.maxByOrNull { it.width } ?: return null
        return SevenTVBadgeCosmetic(
            id = id,
            name = name,
            tooltip = tooltip,
            imageUrl = "https:${host.url}/${file.name}",
        )
    }
}

@Keep
@Serializable
data class BadgeHostDto(
    val url: String,
    val files: List<BadgeFileDto> = emptyList(),
)

@Keep
@Serializable
data class BadgeFileDto(
    val name: String,
    val width: Int = 0,
    val height: Int = 0,
)
