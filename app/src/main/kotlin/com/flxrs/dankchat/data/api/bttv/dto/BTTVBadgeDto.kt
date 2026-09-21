package com.flxrs.dankchat.data.api.bttv.dto

import androidx.annotation.Keep
import com.flxrs.dankchat.data.UserId
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Keep
@Serializable
data class BTTVBadgeDto(
    @SerialName(value = "providerId") val providerId: UserId,
    @SerialName(value = "badge") val badge: BTTVBadgeInfoDto,
)

@Keep
@Serializable
data class BTTVBadgeInfoDto(
    @SerialName(value = "description") val description: String,
    @SerialName(value = "svg") val svg: String,
)
