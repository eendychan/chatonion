package com.flxrs.dankchat.data.api.homies

import io.ktor.client.HttpClient
import io.ktor.client.request.get

class HomiesApi(
    private val ktorClient: HttpClient,
) {
    suspend fun getHomiesBadges() = ktorClient.get(HOMIES_BADGES_URL)

    suspend fun getItzAlexBadges() = ktorClient.get(ITZALEX_BADGES_URL)

    suspend fun getItzAlexBadges2() = ktorClient.get(ITZALEX_BADGES_2_URL)

    private companion object {
        const val HOMIES_BADGES_URL = "https://chatterinohomies.com/api/badges/list"
        const val ITZALEX_BADGES_URL = "https://itzalex.github.io/badges"
        const val ITZALEX_BADGES_2_URL = "https://itzalex.github.io/badges2"
    }
}
