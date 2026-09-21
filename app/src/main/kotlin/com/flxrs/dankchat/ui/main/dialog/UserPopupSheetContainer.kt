package com.flxrs.dankchat.ui.main.dialog

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.ui.unit.IntOffset
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.flxrs.dankchat.data.DisplayName
import com.flxrs.dankchat.data.UserName
import com.flxrs.dankchat.preferences.chat.moderation.ModerationSettingsViewModel
import com.flxrs.dankchat.ui.chat.history.HistoryChannel
import com.flxrs.dankchat.ui.chat.user.UserPopupDialog
import com.flxrs.dankchat.ui.chat.user.UserPopupViewModel
import com.flxrs.dankchat.ui.main.input.ChatInputViewModel
import com.flxrs.dankchat.ui.main.sheet.FullScreenSheetState
import com.flxrs.dankchat.ui.main.sheet.SheetNavigationViewModel
import org.koin.compose.viewmodel.koinViewModel

@Composable
fun UserPopupSheetContainer(onOpenUrl: (String) -> Unit) {
    val userPopupViewModel: UserPopupViewModel = koinViewModel()
    val chatInputViewModel: ChatInputViewModel = koinViewModel()
    val sheetNavigationViewModel: SheetNavigationViewModel = koinViewModel()
    val moderationSettingsViewModel: ModerationSettingsViewModel = koinViewModel()

    val cards by userPopupViewModel.states.collectAsStateWithLifecycle()
    val timeoutDurationsSeconds by moderationSettingsViewModel.timeoutDurationsSeconds.collectAsStateWithLifecycle()

    val currentSheetState by sheetNavigationViewModel.fullScreenSheetState.collectAsStateWithLifecycle()
    val isHistoryOpen = currentSheetState is FullScreenSheetState.History

    cards.forEach { card ->
        // Pin toggles and z-order changes re-create the popup window: a pinned card becomes
        // pass-through for touches, a re-created window renders above the others
        key(card.id, card.isPinned, card.zSequence) {
            UserPopupDialog(
                state = card.popupState,
                isPinned = card.isPinned,
                offset = IntOffset(card.offsetX, card.offsetY),
                onDrag = { newOffset -> userPopupViewModel.updateOffset(card.id, newOffset.x, newOffset.y) },
                onTogglePin = { userPopupViewModel.togglePin(card.id) },
                onInteraction = { userPopupViewModel.bringToFront(card.id) },
                isOwnUser = card.isOwnUser,
                canModerate = card.canModerate,
                timeoutDurationsSeconds = timeoutDurationsSeconds,
                onBlockUser = { userPopupViewModel.blockUser(card.id) },
                onUnblockUser = { userPopupViewModel.unblockUser(card.id) },
                onBanUser = { userPopupViewModel.banUser(card.id) },
                onUnbanUser = { userPopupViewModel.unbanUser(card.id) },
                onTimeoutUser = { duration -> userPopupViewModel.timeoutUser(card.id, duration) },
                onDismiss = { userPopupViewModel.dismiss(card.id) },
                onMention = when {
                    isHistoryOpen -> null

                    else -> { name: String, displayName: String ->
                        chatInputViewModel.mentionUser(UserName(name), DisplayName(displayName))
                    }
                },
                onWhisper = when {
                    isHistoryOpen -> null

                    else -> { name: String ->
                        sheetNavigationViewModel.openWhispers()
                        chatInputViewModel.setWhisperTarget(UserName(name))
                    }
                },
                onOpenChannel = { userName -> onOpenUrl("https://twitch.tv/$userName") },
                onReport = { userName -> onOpenUrl("https://twitch.tv/$userName/report") },
                onMessageHistory = when {
                    isHistoryOpen -> null

                    else -> { userName: String ->
                        card.channel?.let { channel ->
                            sheetNavigationViewModel.openHistory(HistoryChannel.Channel(channel), "from:$userName")
                            userPopupViewModel.dismiss(card.id)
                        }
                    }
                },
                onViewHistory = when {
                    isHistoryOpen -> { userName: String ->
                        val historyState = currentSheetState as FullScreenSheetState.History
                        sheetNavigationViewModel.openHistory(historyState.channel, "from:$userName")
                        userPopupViewModel.dismiss(card.id)
                    }

                    else -> null
                },
            )
        }
    }
}
