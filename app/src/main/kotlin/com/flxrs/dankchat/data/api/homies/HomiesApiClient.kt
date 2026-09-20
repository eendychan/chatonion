package com.flxrs.dankchat.data.api.homies

import com.flxrs.dankchat.data.api.homies.dto.HomiesBadgeDto
import com.flxrs.dankchat.data.api.homies.dto.HomiesBadgeListDto
import com.flxrs.dankchat.data.api.throwApiErrorOnFailure
import io.ktor.client.call.body
import kotlinx.serialization.json.Json
import org.koin.core.annotation.Single

@Single
class HomiesApiClient(
    private val homiesApi: HomiesApi,
    private val json: Json,
) {
    suspend fun getHomiesBadges(): Result<List<HomiesBadgeDto>> = runCatching {
        homiesApi
            .getHomiesBadges()
            .throwApiErrorOnFailure(json)
            .body<HomiesBadgeListDto>()
            .badges
    }

    suspend fun getItzAlexBadges(): Result<List<HomiesBadgeDto>> = runCatching {
        homiesApi
            .getItzAlexBadges()
            .throwApiErrorOnFailure(json)
            .body<HomiesBadgeListDto>()
            .badges
    }

    suspend fun getItzAlexBadges2(): Result<List<HomiesBadgeDto>> = runCatching {
        homiesApi
            .getItzAlexBadges2()
            .throwApiErrorOnFailure(json)
            .body<HomiesBadgeListDto>()
            .badges
    }
}
