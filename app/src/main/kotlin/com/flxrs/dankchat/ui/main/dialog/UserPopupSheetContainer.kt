package com.flxrs.dankchat.ui.main.dialog

import androidx.activity.compose.BackHandler
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntRect
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.window.Popup
import androidx.compose.ui.window.PopupPositionProvider
import androidx.compose.ui.window.PopupProperties
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

// Centers the popup on screen, offset by however far the card has been dragged. The offset is
// read from a State (kept fresh via rememberUpdatedState at the call site) rather than captured
// as a plain value, so the SAME provider instance can be reused across drag updates instead of
// a new one being allocated - and, in turn, the window - on every single frame of a drag.
private class DraggableCenteredPopupPositionProvider(
    private val dragOffset: () -> IntOffset,
) : PopupPositionProvider {
    override fun calculatePosition(
        anchorBounds: IntRect,
        windowSize: IntSize,
        layoutDirection: LayoutDirection,
        popupContentSize: IntSize,
    ): IntOffset {
        val offset = dragOffset()
        return IntOffset(
            x = (windowSize.width - popupContentSize.width) / 2 + offset.x,
            y = (windowSize.height - popupContentSize.height) / 2 + offset.y,
        )
    }
}

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

    val currentCards by rememberUpdatedState(cards)
    val hasTransientCard = cards.any { !it.isPinned }

    // Back press dismisses every transient (unpinned) card at once, same as before.
    if (hasTransientCard) {
        BackHandler {
            currentCards.filter { !it.isPinned }.forEach { userPopupViewModel.dismiss(it.id) }
        }
    }

    // Each card gets its OWN window, sized to just that card - not one window covering the whole
    // screen. That keeps every pixel outside the card(s) free of any popup window, so taps there
    // reach the chat/stream underneath normally, letting you interact with anything outside the
    // card - or open/pin any number of additional cards - even while one is pinned. Windows still
    // draw above the WebView stream player regardless, since each is its own Android window on
    // top of the host window. At most one transient card exists at a time (show() clears prior
    // unpinned ones), so per-card "dismiss on outside tap" can never cross-dismiss a second one.
    cards.sortedBy { it.zSequence }.forEachIndexed { slotIndex, card ->
        // The window is keyed by its STACK POSITION, not by which card currently occupies it.
        // Android stacks a newly-added window above whatever already exists, and "bring to
        // front" always means "show this card in the topmost slot" - so reassigning which card's
        // data a slot displays is all that's needed; no window is ever destroyed and recreated
        // just to reorder it, which is what caused the popups to visibly flicker before.
        key(slotIndex) {
            // Everything below is additionally keyed by card.id, so it correctly resets whenever
            // this slot starts showing a different card after a reorder - even though the window
            // itself (this key(slotIndex) block, and the Popup inside it) stays exactly as is.
            var dragOffset by remember(card.id) { mutableStateOf(IntOffset(card.offsetX, card.offsetY)) }
            val latestDragOffset by rememberUpdatedState(dragOffset)
            val positionProvider = remember(card.id) { DraggableCenteredPopupPositionProvider { latestDragOffset } }

            val dismissOnOutsideTap = !card.isPinned
            val popupProperties = remember(dismissOnOutsideTap) {
                PopupProperties(
                    focusable = false,
                    dismissOnBackPress = false,
                    dismissOnClickOutside = dismissOnOutsideTap,
                    clippingEnabled = false,
                    usePlatformDefaultWidth = false,
                )
            }
            Popup(
                popupPositionProvider = positionProvider,
                onDismissRequest = { userPopupViewModel.dismiss(card.id) },
                properties = popupProperties,
            ) {
                key(card.id) {
                    UserPopupDialog(
                        state = card.popupState,
                        isPinned = card.isPinned,
                        offset = dragOffset,
                        onDrag = { newOffset ->
                            dragOffset = newOffset
                            userPopupViewModel.updateOffset(card.id, newOffset.x, newOffset.y)
                        },
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
