package com.flxrs.dankchat.ui.chat

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.flxrs.dankchat.R
import com.flxrs.dankchat.ui.chat.messages.PrivMessageComposable

/**
 * The pin/unpin visibility toggle lives on the toolbar next to the channel tabs — this banner
 * only shows the message itself (without the sender name/timestamp, which don't matter for a
 * pinned message) plus a small close button to unpin it entirely.
 */
@Composable
fun PinnedMessageBanner(
    state: PinnedMessageUiState.Expanded,
    fontSize: Float,
    animateGifs: Boolean,
    callbacks: ChatScreenCallbacks,
    onUnpin: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.medium,
        color = MaterialTheme.colorScheme.surfaceContainerHigh,
        shadowElevation = 4.dp,
    ) {
        Box {
            Column(
                modifier =
                    Modifier
                        .align(Alignment.CenterStart)
                        .padding(top = 6.dp, bottom = 6.dp, start = 4.dp, end = 36.dp),
            ) {
                PrivMessageComposable(
                    message = state.message,
                    fontSize = fontSize,
                    showHeader = false,
                    onUserClick = callbacks.onUserClick,
                    onMessageLongClick = callbacks.onMessageLongClick,
                    onEmoteClick = callbacks.onEmoteClick,
                    onReplyClick = callbacks.onReplyClick,
                    animateGifs = animateGifs,
                )
            }
            if (state.canModerate) {
                IconButton(
                    onClick = onUnpin,
                    modifier = Modifier
                        .align(Alignment.CenterEnd)
                        .padding(end = 2.dp)
                        .size(28.dp),
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = stringResource(R.string.pinned_message_unpin),
                        modifier = Modifier.size(16.dp),
                    )
                }
            }
        }
    }
}
