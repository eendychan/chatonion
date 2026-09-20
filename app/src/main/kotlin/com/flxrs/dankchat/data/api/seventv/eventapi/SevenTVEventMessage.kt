package com.flxrs.dankchat.data.api.seventv.eventapi

import com.flxrs.dankchat.data.DisplayName
import com.flxrs.dankchat.data.UserId
import com.flxrs.dankchat.data.UserName
import com.flxrs.dankchat.data.api.seventv.dto.SevenTVEmoteDto
import com.flxrs.dankchat.data.twitch.badge.SevenTVBadgeCosmetic
import com.flxrs.dankchat.data.twitch.paint.SevenTVPaint

sealed interface SevenTVEventMessage {
    data class EmoteSetUpdated(
        val emoteSetId: String,
        val actorName: DisplayName,
        val added: List<SevenTVEmoteDto>,
        val removed: List<RemovedEmote>,
        val updated: List<UpdatedEmote>,
    ) : SevenTVEventMessage {
        data class UpdatedEmote(
            val id: String,
            val name: String,
            val oldName: String,
        )

        data class RemovedEmote(
            val id: String,
            val name: String,
        )
    }

    data class UserUpdated(
        val actorName: DisplayName,
        val connectionIndex: Int,
        val emoteSetId: String,
        val oldEmoteSetId: String,
    ) : SevenTVEventMessage

    data class PaintCreated(
        val paint: SevenTVPaint,
    ) : SevenTVEventMessage

    data class BadgeCreated(
        val badge: SevenTVBadgeCosmetic,
    ) : SevenTVEventMessage

    data class EntitlementCreated(
        val entitlement: Entitlement,
    ) : SevenTVEventMessage

    data class EntitlementDeleted(
        val entitlement: Entitlement,
    ) : SevenTVEventMessage

    data class Entitlement(
        val kind: String,
        val refId: String,
        val twitchUserId: UserId?,
        val twitchUserName: UserName?,
    )
}
