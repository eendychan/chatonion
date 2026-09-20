package com.flxrs.dankchat.data.repo.command

import com.flxrs.dankchat.data.UserId
import com.flxrs.dankchat.data.UserName
import com.flxrs.dankchat.data.twitch.command.TwitchCommand
import com.flxrs.dankchat.utils.TextResource

sealed interface CommandResult {
    data object Accepted : CommandResult

    data class AcceptedTwitchCommand(
        val command: TwitchCommand,
        val response: TextResource? = null,
    ) : CommandResult

    data class AcceptedWithResponse(
        val response: TextResource,
    ) : CommandResult

    data class Message(
        val message: String,
    ) : CommandResult

    data class OpenUserPopup(
        val targetUserId: UserId,
        val targetUserName: UserName,
        val channel: UserName,
    ) : CommandResult

    data object UserNotFound : CommandResult

    data object NotFound : CommandResult

    data class UnknownCommand(
        val trigger: String,
    ) : CommandResult

    data object IrcCommand : CommandResult

    data object Blocked : CommandResult
}
