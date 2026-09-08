package com.flxrs.dankchat.ui.main.channel

import androidx.compose.runtime.Immutable
import com.flxrs.dankchat.data.UserName
import com.flxrs.dankchat.data.state.ChannelLoadingState
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf

@Immutable
data class ChannelTabUiState(
    val tabs: ImmutableList<ChannelTabItem> = persistentListOf(),
    val selectedIndex: Int = 0,
    val loading: Boolean = true,
    val whisperMentionCount: Int = 0,
)

@Immutable
data class ChannelTabItem(
    val channel: UserName,
    val displayName: String,
    val avatarUrl: String? = null,
    val isSelected: Boolean,
    val hasUnread: Boolean,
    val mentionCount: Int,
    val loadingState: ChannelLoadingState,
)
