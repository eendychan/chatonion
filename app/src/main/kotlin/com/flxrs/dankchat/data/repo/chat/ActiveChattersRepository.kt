package com.flxrs.dankchat.data.repo.chat

import com.flxrs.dankchat.data.UserName
import com.flxrs.dankchat.data.twitch.paint.SevenTVPaint
import org.koin.core.annotation.Single
import java.util.concurrent.ConcurrentHashMap

/**
 * Tracks chatters seen in each channel during this session, together with the name
 * style (Twitch color / 7TV paint) observed on their messages. Used to highlight
 * mentions of chatters that are currently present in a channel.
 */
@Single
class ActiveChattersRepository {
    data class ChatterStyle(
        val color: Int?,
        val paint: SevenTVPaint?,
    )

    /** Per channel: lowercase user name -> last seen name style. */
    private val chattersByChannel = ConcurrentHashMap<UserName, ConcurrentHashMap<String, ChatterStyle>>()

    fun registerChatter(
        channel: UserName,
        userName: UserName,
        color: Int?,
        paint: SevenTVPaint?,
    ) {
        if (color == null && paint == null) {
            return
        }
        chattersByChannel
            .getOrPut(channel) { ConcurrentHashMap() }[userName.lowercase().value] = ChatterStyle(color = color, paint = paint)
    }

    fun getChatterStyle(
        channel: UserName,
        lowercaseName: String,
    ): ChatterStyle? = chattersByChannel[channel]?.get(lowercaseName)
}
