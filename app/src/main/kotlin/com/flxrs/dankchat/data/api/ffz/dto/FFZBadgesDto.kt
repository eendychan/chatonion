package com.flxrs.dankchat.data.api.ffz.dto

import androidx.annotation.Keep
import com.flxrs.dankchat.data.UserId
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Keep
@Serializable
data class FFZBadgesDto(
    @SerialName(value = "badges") val badges: List<FFZBadgeDto> = emptyList(),
    // badge id -> list of Twitch user ids the badge is assigned to
    @SerialName(value = "users") val users: Map<String, List<UserId>> = emptyMap(),
)

@Keep
@Serializable
data class FFZBadgeDto(
    @SerialName(value = "id") val id: Int,
    @SerialName(value = "name") val name: String,
    @SerialName(value = "title") val title: String,
    @SerialName(value = "image") val image: String? = null,
    @SerialName(value = "urls") val urls: Map<String, String> = emptyMap(),
    // Background color the badge is meant to be rendered on, e.g. "#755000"
    @SerialName(value = "color") val color: String? = null,
) {
    val bestUrl: String? get() = urls["2"] ?: urls["1"] ?: image
}
