package com.flxrs.dankchat.data.twitch.message

import androidx.compose.runtime.Immutable
import com.flxrs.dankchat.data.UserId
import com.flxrs.dankchat.data.UserName
import com.flxrs.dankchat.data.irc.IrcMessage
import com.flxrs.dankchat.utils.DateTimeUtils
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.toImmutableList

@Immutable
data class RoomState(
    val channel: UserName,
    val channelId: UserId,
    val tags: Map<RoomStateTag, Int> =
        mapOf(
            RoomStateTag.EMOTE to 0,
            RoomStateTag.SUBS to 0,
            RoomStateTag.SLOW to 0,
            RoomStateTag.R9K to 0,
            RoomStateTag.FOLLOW to -1,
        ),
) {
    val isEmoteMode get() = tags.getOrDefault(RoomStateTag.EMOTE, 0) > 0
    val isSubscriberMode get() = tags.getOrDefault(RoomStateTag.SUBS, 0) > 0
    val isSlowMode get() = tags.getOrDefault(RoomStateTag.SLOW, 0) > 0
    val isUniqueChatMode get() = tags.getOrDefault(RoomStateTag.R9K, 0) > 0
    val isFollowMode get() = tags.getOrDefault(RoomStateTag.FOLLOW, 0) >= 0

    val followerModeDuration get() = tags[RoomStateTag.FOLLOW]?.takeIf { it >= 0 }
    val slowModeWaitTime get() = tags[RoomStateTag.SLOW]?.takeIf { it > 0 }

    fun toDebugText(): String = tags
        .filter { (it.key == RoomStateTag.FOLLOW && it.value >= 0) || it.value > 0 }
        .map { (tag, value) ->
            when (tag) {
                RoomStateTag.FOLLOW -> {
                    when (value) {
                        0 -> "follow"
                        else -> "follow(${DateTimeUtils.formatSeconds(value * 60)})"
                    }
                }

                RoomStateTag.SLOW -> {
                    "slow(${DateTimeUtils.formatSeconds(value)})"
                }

                else -> {
                    tag.name.lowercase()
                }
            }
        }.joinToString()

    /**
     * Which modes are currently active, in a fixed display order. The compact helper text shows
     * these as small icons (see RoomStateIcon in ChatInputLayout) rather than full labels, so
     * only the tag identity is needed here - not the duration text (that's still shown in full,
     * with labels, in the moderation menu, which reads the tags/durations above directly).
     */
    fun toActiveTags(): ImmutableList<RoomStateTag> = tags
        .filter { (it.key == RoomStateTag.FOLLOW && it.value >= 0) || it.value > 0 }
        .keys
        .toImmutableList()

    fun copyFromIrcMessage(msg: IrcMessage): RoomState = copy(
        tags = tags.mapValues { (key, value) -> msg.getRoomStateTag(key, value) },
    )

    private fun IrcMessage.getRoomStateTag(
        tag: RoomStateTag,
        default: Int,
    ): Int = tags[tag.ircTag]?.toIntOrNull() ?: default
}
