package com.flxrs.dankchat.data.twitch.emote

/**
 * BTTV/FFZ-style emote modifiers, applied via prefixes like `w! EMOTE`.
 * Also supported for 7TV and Twitch emotes.
 */
enum class EmoteEffect {
    Wide, // w! - stretched horizontally
    Cursed, // c! - grayscale + high contrast
    RotateLeft, // l! - rotated 90° counter-clockwise
    RotateRight, // r! - rotated 90° clockwise
    FlipHorizontal, // h! - mirrored horizontally
    FlipVertical, // v! - mirrored vertically
    Party, // p! - animated hue rotation
    Shake, // s! - animated horizontal shake
    ZeroWidth, // z! - overlays the previous emote
    ;

    companion object {
        private val BY_PREFIX = mapOf(
            "w!" to Wide,
            "c!" to Cursed,
            "l!" to RotateLeft,
            "r!" to RotateRight,
            "h!" to FlipHorizontal,
            "v!" to FlipVertical,
            "p!" to Party,
            "s!" to Shake,
            "z!" to ZeroWidth,
        )

        fun fromPrefix(word: String): EmoteEffect? = BY_PREFIX[word]
    }
}
