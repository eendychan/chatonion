package com.flxrs.dankchat.ui.chat.user

import androidx.compose.runtime.Immutable
import com.flxrs.dankchat.data.DisplayName
import com.flxrs.dankchat.data.UserId
import com.flxrs.dankchat.data.UserName

@Immutable
sealed interface UserPopupState {
    data class Loading(
        val userName: UserName,
        val displayName: DisplayName,
    ) : UserPopupState

    data class NotLoggedIn(
        val userName: UserName,
        val displayName: DisplayName,
    ) : UserPopupState

    data class Error(
        val throwable: Throwable? = null,
    ) : UserPopupState

    data class Success(
        val userId: UserId,
        val userName: UserName,
        val displayName: DisplayName,
        val created: String,
        val avatarUrl: String,
        val offlineImageUrl: String? = null,
        val showFollowingSince: Boolean = false,
        val followingSince: String? = null,
        val isBlocked: Boolean = false,
    ) : UserPopupState
}
