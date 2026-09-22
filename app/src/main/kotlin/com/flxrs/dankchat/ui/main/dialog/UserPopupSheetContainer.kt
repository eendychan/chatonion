package com.flxrs.dankchat.ui.main.dialog

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
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

    if (cards.isEmpty()) {
        return
    }

    // All cards live in a single overlay, so pin toggles and z-order changes only
    // recompose the affected card instead of destroying and recreating a popup window
    val currentCards by rememberUpdatedState(cards)
    val hasTransientCard = cards.any { !it.isPinned }

    // Like a focusable popup, an unpinned card closes on outside tap and back press;
    // with only pinned cards the overlay stays touch-transparent, like a non-focusable popup
    if (hasTransientCard) {
        BackHandler {
            currentCards.filter { !it.isPinned }.forEach { userPopupViewModel.dismiss(it.id) }
        }
    }

    Box(
        modifier =
            Modifier
                .fillMaxSize()
                .then(
                    when {
                        hasTransientCard ->
                            Modifier.pointerInput(Unit) {
                                detectTapGestures(
                                    onTap = {
                                        currentCards.filter { card -> !card.isPinned }.forEach { card -> userPopupViewModel.dismiss(card.id) }
                                    },
                                )
                            }

                        else -> Modifier
                    },
                ),
    ) {
        // Later cards draw on top, so bumping the z-sequence brings a card to the front
        cards.sortedBy { it.zSequence }.forEach { card ->
            key(card.id) {
                Box(modifier = Modifier.align(Alignment.Center)) {
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
    }
}
