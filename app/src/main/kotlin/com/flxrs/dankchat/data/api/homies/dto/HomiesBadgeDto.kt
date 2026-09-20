package com.flxrs.dankchat.data.api.homies.dto

import androidx.annotation.Keep
import com.flxrs.dankchat.data.UserId
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Keep
@Serializable
data class HomiesBadgeListDto(
    @SerialName(value = "badges") val badges: List<HomiesBadgeDto> = emptyList(),
)

@Keep
@Serializable
data class HomiesBadgeDto(
    // chatterinohomies.com assigns one badge per userId, the itzalex lists have a list of users per badge
    @SerialName(value = "userId") val userId: UserId? = null,
    @SerialName(value = "users") val users: List<UserId> = emptyList(),
    @SerialName(value = "image1") val image1: String? = null,
    @SerialName(value = "image2") val image2: String? = null,
    @SerialName(value = "image3") val image3: String? = null,
    @SerialName(value = "tooltip") val tooltip: String? = null,
)
