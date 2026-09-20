package com.flxrs.dankchat.data.api.ivr.dto

import androidx.annotation.Keep
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Keep
@Serializable
data class IvrSubageDto(
    @SerialName(value = "statusHidden") val statusHidden: Boolean = false,
    @SerialName(value = "cumulative") val cumulative: Cumulative? = null,
    @SerialName(value = "meta") val meta: Meta? = null,
) {
    @Keep
    @Serializable
    data class Cumulative(
        @SerialName(value = "months") val months: Int? = null,
    )

    @Keep
    @Serializable
    data class Meta(
        @SerialName(value = "tier") val tier: String? = null,
        @SerialName(value = "type") val type: String? = null,
    )
}
