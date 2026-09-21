package com.flxrs.dankchat.data.api.seventv

import com.flxrs.dankchat.data.UserId
import com.flxrs.dankchat.data.api.recoverNotFoundWith
import com.flxrs.dankchat.data.api.seventv.dto.SevenTVEmoteDto
import com.flxrs.dankchat.data.api.seventv.dto.SevenTVEmoteSetDto
import com.flxrs.dankchat.data.api.seventv.dto.SevenTVUserCosmeticsResponse
import com.flxrs.dankchat.data.api.seventv.dto.SevenTVUserDto
import com.flxrs.dankchat.data.api.throwApiErrorOnFailure
import com.flxrs.dankchat.data.twitch.badge.SevenTVBadgeCosmetic
import com.flxrs.dankchat.data.twitch.paint.SevenTVPaint
import io.ktor.client.call.body
import kotlinx.serialization.json.Json
import org.koin.core.annotation.Single

@Single
class SevenTVApiClient(
    private val sevenTVApi: SevenTVApi,
    private val json: Json,
) {
    suspend fun getSevenTVChannelEmotes(channelId: UserId): Result<SevenTVUserDto?> = runCatching {
        sevenTVApi
            .getChannelEmotes(channelId)
            .throwApiErrorOnFailure(json)
            .body<SevenTVUserDto>()
    }.recoverNotFoundWith(default = null)

    suspend fun getSevenTVEmoteSet(emoteSetId: String): Result<SevenTVEmoteSetDto> = runCatching {
        sevenTVApi
            .getEmoteSet(emoteSetId)
            .throwApiErrorOnFailure(json)
            .body()
    }

    suspend fun getSevenTVGlobalEmotes(): Result<List<SevenTVEmoteDto>> = runCatching {
        sevenTVApi
            .getGlobalEmotes()
            .throwApiErrorOnFailure(json)
            .body<SevenTVEmoteSetDto>()
            .emotes
            .orEmpty()
    }

    /**
     * Fetches the active name paint and badge for the given Twitch users via the v4 GraphQL API.
     * All users are queried in a single aliased request, users without a 7TV account come back
     * as null entries. Cosmetics of users that have neither paint nor badge are null.
     */
    suspend fun getUserCosmetics(userIds: List<UserId>): Result<Map<UserId, SevenTVUserCosmetics>> = runCatching {
        if (userIds.isEmpty()) {
            return@runCatching emptyMap()
        }

        val query = buildUserCosmeticsQuery(userIds)
        val response =
            sevenTVApi
                .postUserCosmetics(query)
                .throwApiErrorOnFailure(json)
                .body<SevenTVUserCosmeticsResponse>()
        val users = response.data?.users ?: error("7TV GraphQL response contains no data")

        userIds
            .mapIndexedNotNull { index, userId ->
                val style = users[alias(index)]?.style ?: return@mapIndexedNotNull null
                val paint = style.activePaint?.toDomain()
                val badge = style.activeBadge?.toDomain()
                if (paint == null && badge == null) {
                    return@mapIndexedNotNull null
                }

                userId to SevenTVUserCosmetics(paint = paint, badge = badge)
            }.toMap()
    }

    private fun buildUserCosmeticsQuery(userIds: List<UserId>): String = buildString {
        append("query { users { ")
        userIds.forEachIndexed { index, userId ->
            // Twitch user ids are numeric, so plain interpolation is injection-safe
            append(alias(index))
                .append(": userByConnection(platform: TWITCH, platformId: \"")
                .append(userId.value)
                .append("\") { ")
                .append(USER_COSMETICS_FIELDS)
                .append(" } ")
        }
        append("} }")
    }

    private fun alias(index: Int) = "u$index"

    companion object {
        private const val USER_COSMETICS_FIELDS =
            "style { " +
                "activePaint { id name data { " +
                "layers { opacity ty { __typename " +
                "... on PaintLayerTypeSingleColor { color { hex } } " +
                "... on PaintLayerTypeLinearGradient { angle repeating stops { at color { hex } } } " +
                "... on PaintLayerTypeRadialGradient { repeating stops { at color { hex } } } " +
                "... on PaintLayerTypeImage { images { url width height } } } } " +
                "shadows { offsetX offsetY blur color { hex } } } } " +
                "activeBadge { id name description images { url width height scale frameCount mime } } " +
                "}"
    }
}

/** The active 7TV cosmetics of a single user, resolved via the GraphQL API. */
data class SevenTVUserCosmetics(
    val paint: SevenTVPaint?,
    val badge: SevenTVBadgeCosmetic?,
)
