package com.flxrs.dankchat.data.api.seventv.dto

import androidx.annotation.Keep
import com.flxrs.dankchat.data.twitch.badge.SevenTVBadgeCosmetic
import com.flxrs.dankchat.data.twitch.paint.SevenTVPaint
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlin.math.roundToInt

/**
 * DTOs for the 7TV v4 GraphQL API (https://api.7tv.app/v4/gql), used to fetch a user's
 * active name paint and badge on demand. The EventAPI only pushes cosmetics of users
 * with an active 7TV presence, so chat cosmetics of everyone else are resolved through
 * a batched `userByConnection` query.
 */
@Keep
@Serializable
data class SevenTVGraphQLRequest(
    val query: String,
)

@Keep
@Serializable
data class SevenTVUserCosmeticsResponse(
    val data: UsersData? = null,
) {
    @Keep
    @Serializable
    data class UsersData(
        val users: Map<String, UserCosmeticsDto?> = emptyMap(),
    )
}

@Keep
@Serializable
data class UserCosmeticsDto(
    val style: UserStyleDto? = null,
)

@Keep
@Serializable
data class UserStyleDto(
    val activePaint: ActivePaintDto? = null,
    val activeBadge: ActiveBadgeDto? = null,
)

@Keep
@Serializable
data class ActivePaintDto(
    val id: String,
    val name: String,
    val data: PaintDataDto? = null,
) {
    fun toDomain(): SevenTVPaint? {
        val layer = data?.layers?.firstOrNull()
        val shadows =
            data?.shadows.orEmpty().mapNotNull { shadow ->
                shadow.color.hex.toArgbOrNull()?.let { argb ->
                    SevenTVPaint.Shadow(
                        xOffset = shadow.offsetX,
                        yOffset = shadow.offsetY,
                        radius = shadow.blur,
                        argb = argb,
                    )
                }
            }

        return when (layer?.ty?.typename) {
            LAYER_SINGLE_COLOR ->
                SevenTVPaint(
                    id = id,
                    name = name,
                    color = layer.ty.color
                        ?.hex
                        ?.toArgbOrNull()
                        ?.withOpacity(layer.opacity),
                    function = SevenTVPaint.PaintFunction.LinearGradient,
                    repeat = false,
                    angleDegrees = 0f,
                    stops = emptyList(),
                    shadows = shadows,
                )

            LAYER_LINEAR_GRADIENT ->
                SevenTVPaint(
                    id = id,
                    name = name,
                    color = null,
                    function = SevenTVPaint.PaintFunction.LinearGradient,
                    repeat = layer.ty.repeating,
                    angleDegrees = layer.ty.angle,
                    stops = layer.ty.toStops(layer.opacity),
                    shadows = shadows,
                )

            LAYER_RADIAL_GRADIENT ->
                SevenTVPaint(
                    id = id,
                    name = name,
                    color = null,
                    function = SevenTVPaint.PaintFunction.RadialGradient,
                    repeat = layer.ty.repeating,
                    angleDegrees = 0f,
                    stops = layer.ty.toStops(layer.opacity),
                    shadows = shadows,
                )

            LAYER_IMAGE ->
                SevenTVPaint(
                    id = id,
                    name = name,
                    color = null,
                    function = SevenTVPaint.PaintFunction.ImageUrl,
                    repeat = layer.ty.repeating,
                    angleDegrees = 0f,
                    stops = emptyList(),
                    shadows = shadows,
                    imageUrl = layer.ty.images
                        .firstOrNull()
                        ?.url,
                )

            else -> null
        }
    }

    private fun PaintLayerTypeDto.toStops(opacity: Float): List<SevenTVPaint.Stop> = stops.mapNotNull { stop ->
        stop.color.hex.toArgbOrNull()?.let { argb ->
            SevenTVPaint.Stop(at = stop.at, argb = argb.withOpacity(opacity))
        }
    }

    companion object {
        const val LAYER_SINGLE_COLOR = "PaintLayerTypeSingleColor"
        const val LAYER_LINEAR_GRADIENT = "PaintLayerTypeLinearGradient"
        const val LAYER_RADIAL_GRADIENT = "PaintLayerTypeRadialGradient"
        const val LAYER_IMAGE = "PaintLayerTypeImage"
    }
}

@Keep
@Serializable
data class PaintDataDto(
    val layers: List<PaintLayerDto> = emptyList(),
    val shadows: List<GqlPaintShadowDto> = emptyList(),
)

@Keep
@Serializable
data class PaintLayerDto(
    val opacity: Float = 1f,
    val ty: PaintLayerTypeDto,
)

/**
 * Union of all paint layer types, discriminated by [typename]. Only the fields of the
 * matching type are populated.
 */
@Keep
@Serializable
data class PaintLayerTypeDto(
    @SerialName("__typename") val typename: String,
    val color: HexColorDto? = null,
    val angle: Float = 0f,
    val repeating: Boolean = false,
    val stops: List<GqlPaintStopDto> = emptyList(),
    val images: List<PaintImageDto> = emptyList(),
)

@Keep
@Serializable
data class GqlPaintStopDto(
    val at: Float,
    val color: HexColorDto,
)

@Keep
@Serializable
data class PaintImageDto(
    val url: String,
    val width: Int = 0,
    val height: Int = 0,
)

@Keep
@Serializable
data class GqlPaintShadowDto(
    val offsetX: Float = 0f,
    val offsetY: Float = 0f,
    val blur: Float = 0f,
    val color: HexColorDto,
)

/** v4 colors are serialized as CSS-style hex strings in RGBA order (#RRGGBBAA). */
@Keep
@Serializable
data class HexColorDto(
    val hex: String,
)

@Keep
@Serializable
data class ActiveBadgeDto(
    val id: String,
    val name: String,
    val description: String? = null,
    val images: List<BadgeImageDto> = emptyList(),
) {
    fun toDomain(): SevenTVBadgeCosmetic? {
        // Match the EventAPI badge path: animated WEBP in the highest available scale
        val image =
            images
                .filter { it.mime == MIME_WEBP }
                .maxWithOrNull(compareBy({ it.scale }, { it.frameCount }))
                ?: images.maxByOrNull { it.width }
                ?: return null

        return SevenTVBadgeCosmetic(
            id = id,
            name = name,
            tooltip = description,
            imageUrl = image.url,
        )
    }

    private companion object {
        const val MIME_WEBP = "image/webp"
    }
}

@Keep
@Serializable
data class BadgeImageDto(
    val url: String,
    val width: Int = 0,
    val height: Int = 0,
    val scale: Int = 1,
    val frameCount: Int = 1,
    val mime: String = "",
)

private fun String.toArgbOrNull(): Int? {
    val hex = removePrefix("#")
    val value = hex.toLongOrNull(16) ?: return null
    return when (hex.length) {
        // #RRGGBBAA
        8 -> {
            val r = (value shr 24) and 0xFF
            val g = (value shr 16) and 0xFF
            val b = (value shr 8) and 0xFF
            val a = value and 0xFF
            ((a shl 24) or (r shl 16) or (g shl 8) or b).toInt()
        }

        // #RRGGBB
        6 -> (0xFF000000 or value).toInt()

        else -> null
    }
}

private fun Int.withOpacity(opacity: Float): Int {
    if (opacity >= 1f) {
        return this
    }

    val alpha = ((this ushr 24) * opacity).roundToInt().coerceIn(0, 255)
    return (this and 0x00FFFFFF) or (alpha shl 24)
}
