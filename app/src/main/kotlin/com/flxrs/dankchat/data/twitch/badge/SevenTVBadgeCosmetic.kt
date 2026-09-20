package com.flxrs.dankchat.data.twitch.badge

/**
 * A 7TV badge cosmetic, see Chatterino7's SeventvBadges.
 * Assigned to users via EventAPI entitlements.
 */
data class SevenTVBadgeCosmetic(
    val id: String,
    val name: String,
    val tooltip: String?,
    val imageUrl: String,
)
