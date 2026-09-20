package com.flxrs.dankchat.data.twitch.paint

/**
 * A 7TV name paint (cosmetic), see Chatterino7's SeventvPaints.
 * Colors from the API are packed as RGBA int32 and converted to ARGB here.
 */
data class SevenTVPaint(
    val id: String,
    val name: String,
    val color: Int?,
    val function: PaintFunction,
    val repeat: Boolean,
    val angleDegrees: Float,
    val stops: List<Stop>,
    val shadows: List<Shadow>,
) {
    enum class PaintFunction {
        LinearGradient,
        RadialGradient,
        ImageUrl,
    }

    data class Stop(
        val at: Float,
        val argb: Int,
    )

    data class Shadow(
        val xOffset: Float,
        val yOffset: Float,
        val radius: Float,
        val argb: Int,
    )

    val strongestShadow: Shadow? get() = shadows.maxByOrNull { it.radius }

    companion object {
        /** 7TV packs colors as RGBA int32 (R in the highest byte, A in the lowest). */
        fun rgbaToArgb(color: Int): Int {
            val r = color ushr 24 and 0xFF
            val g = color ushr 16 and 0xFF
            val b = color ushr 8 and 0xFF
            val a = color and 0xFF
            return (a shl 24) or (r shl 16) or (g shl 8) or b
        }
    }
}
