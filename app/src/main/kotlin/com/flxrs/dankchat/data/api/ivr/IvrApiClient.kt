package com.flxrs.dankchat.data.api.ivr

import com.flxrs.dankchat.data.UserName
import com.flxrs.dankchat.data.api.ivr.dto.IvrSubageDto
import com.flxrs.dankchat.data.api.ivr.dto.IvrUserDto
import com.flxrs.dankchat.data.api.throwApiErrorOnFailure
import io.ktor.client.call.body
import kotlinx.serialization.json.Json
import org.koin.core.annotation.Single

@Single
class IvrApiClient(
    private val ivrApi: IvrApi,
    private val json: Json,
) {
    suspend fun getUser(login: UserName): Result<IvrUserDto?> = runCatching {
        ivrApi
            .getUser(login.value)
            .throwApiErrorOnFailure(json)
            .body<List<IvrUserDto>>()
            .firstOrNull()
    }

    suspend fun getSubAge(
        user: UserName,
        channel: UserName,
    ): Result<IvrSubageDto> = runCatching {
        ivrApi
            .getSubAge(user.value, channel.value)
            .throwApiErrorOnFailure(json)
            .body()
    }
}
