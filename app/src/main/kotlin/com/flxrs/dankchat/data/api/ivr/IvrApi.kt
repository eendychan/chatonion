package com.flxrs.dankchat.data.api.ivr

import io.ktor.client.HttpClient
import io.ktor.client.request.get
import io.ktor.client.request.parameter

class IvrApi(
    private val ktorClient: HttpClient,
) {
    suspend fun getUser(login: String) = ktorClient.get("twitch/user") {
        parameter("login", login)
    }

    suspend fun getSubAge(
        user: String,
        channel: String,
    ) = ktorClient.get("twitch/subage/$user/$channel")
}
