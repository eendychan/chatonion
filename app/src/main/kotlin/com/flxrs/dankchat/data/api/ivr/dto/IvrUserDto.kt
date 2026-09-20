package com.flxrs.dankchat.data.api.ivr.dto

import androidx.annotation.Keep
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Keep
@Serializable
data class IvrUserDto(
    @SerialName(value = "id") val id: String? = null,
    @SerialName(value = "login") val login: String? = null,
    @SerialName(value = "displayName") val displayName: String? = null,
    @SerialName(value = "banner") val banner: String? = null,
    @SerialName(value = "followers") val followers: Int? = null,
)
