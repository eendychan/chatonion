package com.flxrs.dankchat.preferences.tools.cache

import com.flxrs.dankchat.data.UserName
import kotlinx.serialization.Serializable

@Serializable
data class EmoteCacheSettings(
    val enabled: Boolean = false,
    val channels: List<String> = emptyList(),
    val sevenTvCosmeticsEnabled: Boolean = false,
) {
    fun isChannelCached(channel: UserName): Boolean = enabled && channels.any { it.equals(channel.value, ignoreCase = true) }
}
