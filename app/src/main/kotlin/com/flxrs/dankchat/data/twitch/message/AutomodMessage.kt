package com.flxrs.dankchat.data.twitch.message

import com.flxrs.dankchat.data.DisplayName
import com.flxrs.dankchat.data.UserName
import com.flxrs.dankchat.data.twitch.badge.Badge
import com.flxrs.dankchat.utils.TextResource

data class AutomodMessage(
    override val timestamp: Long,
    override val id: String,
    override val highlights: Set<Highlight> = emptySet(),
    val channel: UserName,
    val heldMessageId: String,
    val userName: UserName,
    val userDisplayName: DisplayName,
    val messageText: String?,
    val reason: TextResource,
    val badges: List<Badge> = emptyList(),
    val color: Int? = null,
    val status: Status = Status.Pending,
    val isUserSide: Boolean = false,
    val flaggedRanges: List<IntRange> = emptyList(),
) : Message {
    enum class Status { Pending, Approved, Denied, Expired }
}
