package com.flxrs.dankchat.data.twitch.badge

enum class BadgeType {
    Authority,
    Predictions,
    Channel,
    Subscriber,
    Vanity,
    DankChat,
    SharedChat,
    SevenTV,
    Homies,
    FrankerFaceZ,
    BetterTTV,
    ;

    companion object {
        fun parseFromBadgeId(id: String): BadgeType = when (id) {
            "staff", "admin", "global_admin" -> Authority
            "predictions" -> Predictions
            "lead_moderator", "moderator", "vip", "broadcaster" -> Channel
            "subscriber", "founder" -> Subscriber
            else -> Vanity
        }
    }
}
