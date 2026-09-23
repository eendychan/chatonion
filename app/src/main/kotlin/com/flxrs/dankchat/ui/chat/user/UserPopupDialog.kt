package com.flxrs.dankchat.ui.chat.user

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.Crossfade
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Chat
import androidx.compose.material.icons.filled.AlternateEmail
import androidx.compose.material.icons.filled.Block
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PushPin
import androidx.compose.material.icons.filled.Report
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.layout
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import com.flxrs.dankchat.R
import com.flxrs.dankchat.data.DisplayName
import com.flxrs.dankchat.data.UserName
import com.flxrs.dankchat.utils.DateTimeUtils
import com.flxrs.dankchat.utils.compose.SheetErrorContent
import java.text.NumberFormat
import kotlin.math.roundToInt

private val CARD_WIDTH = 320.dp
private val BANNER_HEIGHT = 64.dp
private val BANNER_FADE_HEIGHT = 52.dp

// How far the card can be dragged from its initial centered position, in any direction.
// The window reserves exactly this much space around the card (see the Box wrapper in
// UserPopupDialog), so the card can never be dragged far enough to hit its own window edge.
private val DRAG_MARGIN = 160.dp

// Where the card-color fade starts on the banner (top part stays fully visible)
private const val BANNER_FADE_START_FRACTION = 0.35f

// How far the identity row (avatar + name) reaches into the banner area,
// so it sits right on the banner-to-card gradient
private val IDENTITY_OVERLAP = 28.dp

@Composable
fun UserPopupDialog(
    state: UserPopupState,
    onBlockUser: () -> Unit,
    onUnblockUser: () -> Unit,
    onDismiss: () -> Unit,
    onTogglePin: () -> Unit,
    onInteraction: () -> Unit,
    onDrag: (IntOffset) -> Unit,
    offset: IntOffset,
    isPinned: Boolean,
    onOpenChannel: (String) -> Unit,
    onReport: (String) -> Unit,
    onMention: ((String, String) -> Unit)? = null,
    onWhisper: ((String) -> Unit)? = null,
    isOwnUser: Boolean = false,
    onMessageHistory: ((String) -> Unit)? = null,
    onViewHistory: ((String) -> Unit)? = null,
    canModerate: Boolean = false,
    timeoutDurationsSeconds: List<Long> = emptyList(),
    onBanUser: () -> Unit = {},
    onUnbanUser: () -> Unit = {},
    onTimeoutUser: (Long) -> Unit = {},
) {
    var showBlockConfirmation by remember { mutableStateOf(false) }

    val density = LocalDensity.current

    // Drag position is tracked locally for smooth, 1:1 60fps updates, and mirrored to the
    // view model via onDrag so it survives configuration changes.
    var dragOffset by remember { mutableStateOf(offset) }

    // This card is its own top-level window (see UserPopupSheetContainer). The wrapper below
    // reserves a fixed margin around the card so the window is always big enough to contain the
    // full drag range - the card can never be offset far enough to exceed its own window's
    // bounds, so it can never get clipped, no matter how far (within DRAG_MARGIN) it's dragged.
    Box(
        modifier =
            Modifier.layout { measurable, constraints ->
                val marginPx = DRAG_MARGIN.roundToPx()
                val placeable = measurable.measure(constraints.copy(minWidth = 0, minHeight = 0))
                val width = placeable.width + marginPx * 2
                val height = placeable.height + marginPx * 2
                layout(width, height) {
                    placeable.placeRelative(marginPx + dragOffset.x, marginPx + dragOffset.y)
                }
            },
    ) {
        Surface(
            shape = RoundedCornerShape(16.dp),
            color = MaterialTheme.colorScheme.surfaceContainerHigh,
            shadowElevation = 8.dp,
            modifier =
                Modifier
                    .width(CARD_WIDTH)
                    .pointerInput(Unit) {
                        // Tapping a card moves it above the other open cards
                        detectTapGestures(onTap = { onInteraction() })
                    },
        ) {
            AnimatedContent(
                targetState = showBlockConfirmation,
                label = "UserPopupContent",
            ) { isBlockConfirmation ->
                when {
                    isBlockConfirmation -> {
                        Column(modifier = Modifier.fillMaxWidth().padding(16.dp)) {
                            Text(
                                text = stringResource(R.string.confirm_user_block_message),
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.onSurface,
                                textAlign = TextAlign.Center,
                                modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
                            )

                            Row(modifier = Modifier.fillMaxWidth().padding(top = 16.dp)) {
                                OutlinedButton(onClick = { showBlockConfirmation = false }, modifier = Modifier.weight(1f)) {
                                    Text(stringResource(R.string.dialog_cancel))
                                }
                                Spacer(modifier = Modifier.width(12.dp))
                                Button(
                                    onClick = {
                                        onBlockUser()
                                        showBlockConfirmation = false
                                    },
                                    modifier = Modifier.weight(1f),
                                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                                ) {
                                    Text(stringResource(R.string.confirm_user_block_positive_button))
                                }
                            }
                        }
                    }

                    else -> {
                        Column {
                            Box(modifier = Modifier.fillMaxWidth()) {
                                Column(modifier = Modifier.fillMaxWidth()) {
                                    UserBannerHeader(
                                        state = state,
                                        isPinned = isPinned,
                                        onTogglePin = onTogglePin,
                                        onClose = onDismiss,
                                        onDrag = { delta ->
                                            // Fixed margin clamp (see the Box wrapper above) - the card
                                            // can move up to DRAG_MARGIN from center in each direction,
                                            // which is exactly the space reserved for it, so it can
                                            // never be dragged far enough to hit its own window's edge.
                                            val marginPx = with(density) { DRAG_MARGIN.roundToPx() }
                                            val newX = (dragOffset.x + delta.x.roundToInt()).coerceIn(-marginPx, marginPx)
                                            val newY = (dragOffset.y + delta.y.roundToInt()).coerceIn(-marginPx, marginPx)
                                            dragOffset = IntOffset(newX, newY)
                                            onDrag(dragOffset)
                                        },
                                    )
                                    // Space where the banner fade blends into the solid card color
                                    Spacer(modifier = Modifier.height(BANNER_FADE_HEIGHT))
                                }

                                // Avatar + name sit right on the banner-to-card gradient
                                if (state !is UserPopupState.Error) {
                                    Box(
                                        modifier =
                                            Modifier
                                                .align(Alignment.TopCenter)
                                                .padding(top = BANNER_HEIGHT - IDENTITY_OVERLAP),
                                    ) {
                                        UserIdentitySection(state = state, onOpenChannel = onOpenChannel)
                                    }
                                }
                            }

                            when (state) {
                                is UserPopupState.Error -> {
                                    SheetErrorContent()
                                }

                                else -> {
                                    val userName = state.userName
                                    val displayName = state.displayName
                                    val isSuccess = state is UserPopupState.Success
                                    val isLoggedIn = state !is UserPopupState.NotLoggedIn
                                    val isBlocked = (state as? UserPopupState.Success)?.isBlocked == true

                                    UserActionsRow(
                                        isLoggedIn = isLoggedIn,
                                        isOwnUser = isOwnUser,
                                        isSuccess = isSuccess,
                                        isBlocked = isBlocked,
                                        onMention =
                                            onMention?.let { callback ->
                                                {
                                                    callback(userName.value, displayName.value)
                                                    onDismiss()
                                                }
                                            },
                                        onWhisper =
                                            onWhisper?.let { callback ->
                                                {
                                                    callback(userName.value)
                                                    onDismiss()
                                                }
                                            },
                                        onHistory =
                                            (onViewHistory ?: onMessageHistory)?.let { callback ->
                                                {
                                                    callback(userName.value)
                                                    onDismiss()
                                                }
                                            },
                                        onBlockToggle = {
                                            when {
                                                isBlocked -> onUnblockUser()
                                                else -> showBlockConfirmation = true
                                            }
                                        },
                                        onReport = {
                                            onReport(userName.value)
                                            onDismiss()
                                        },
                                    )

                                    if (canModerate && isSuccess && !isOwnUser) {
                                        ModeratorActionsRow(
                                            timeoutDurationsSeconds = timeoutDurationsSeconds,
                                            onBanUser = onBanUser,
                                            onUnbanUser = onUnbanUser,
                                            onTimeoutUser = onTimeoutUser,
                                        )
                                    }

                                    Spacer(modifier = Modifier.height(12.dp))
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun UserBannerHeader(
    state: UserPopupState,
    isPinned: Boolean,
    onTogglePin: () -> Unit,
    onClose: () -> Unit,
    onDrag: (Offset) -> Unit,
) {
    // The Twitch profile banner comes from ivr.fi, the Helix offline image is the fallback
    val bannerUrl = (state as? UserPopupState.Success)?.let { it.bannerUrl ?: it.offlineImageUrl }

    Box(
        modifier =
            Modifier
                .fillMaxWidth()
                .height(BANNER_HEIGHT)
                .clip(RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp))
                .background(MaterialTheme.colorScheme.surfaceContainerHighest)
                .pointerInput(Unit) {
                    detectDragGestures { change, dragAmount ->
                        change.consume()
                        onDrag(dragAmount)
                    }
                },
    ) {
        if (bannerUrl != null) {
            AsyncImage(
                model = bannerUrl,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxWidth().height(64.dp),
            )
            // Darken the banner so the avatar/name stay readable on top of it
            Box(modifier = Modifier.fillMaxWidth().height(64.dp).background(Color.Black.copy(alpha = 0.45f)))
            // The card's solid color fades in over the lower part of the banner, so the
            // banner blends smoothly into the card without being covered entirely
            Box(
                modifier =
                    Modifier
                        .fillMaxSize()
                        .background(
                            Brush.verticalGradient(
                                0f to Color.Transparent,
                                BANNER_FADE_START_FRACTION to Color.Transparent,
                                1f to MaterialTheme.colorScheme.surfaceContainerHigh,
                            ),
                        ),
            )
        }

        val buttonTint =
            when {
                bannerUrl != null -> Color.White
                else -> MaterialTheme.colorScheme.onSurfaceVariant
            }
        IconButton(onClick = onClose, modifier = Modifier.align(Alignment.TopStart).padding(2.dp)) {
            Icon(
                imageVector = Icons.Default.Close,
                contentDescription = stringResource(R.string.user_popup_close),
                tint = buttonTint,
            )
        }
        IconButton(onClick = onTogglePin, modifier = Modifier.align(Alignment.TopEnd).padding(2.dp)) {
            Icon(
                imageVector = Icons.Default.PushPin,
                contentDescription = stringResource(if (isPinned) R.string.user_popup_unpin else R.string.user_popup_pin),
                tint = if (isPinned) MaterialTheme.colorScheme.primary else buttonTint,
            )
        }
    }
}

@Composable
private fun UserIdentitySection(
    state: UserPopupState,
    onOpenChannel: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val userName = state.userName
    val displayName = state.displayName
    Row(
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier.fillMaxWidth().padding(16.dp),
    ) {
        Crossfade(targetState = state, label = "Avatar") { targetState ->
            when (targetState) {
                is UserPopupState.Success -> {
                    AsyncImage(
                        model = targetState.avatarUrl,
                        contentDescription = null,
                        contentScale = ContentScale.Crop,
                        modifier =
                            Modifier
                                .size(64.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.surfaceContainerHigh)
                                .clickable { onOpenChannel(targetState.userName.value) },
                    )
                }

                is UserPopupState.Loading -> {
                    Box(
                        modifier = Modifier.size(64.dp).clip(CircleShape).background(MaterialTheme.colorScheme.surfaceContainerHigh),
                        contentAlignment = Alignment.Center,
                    ) {
                        CircularProgressIndicator(modifier = Modifier.size(28.dp))
                    }
                }

                is UserPopupState.NotLoggedIn -> {
                    Box(
                        modifier =
                            Modifier
                                .size(64.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.surfaceContainerHighest)
                                .clickable { onOpenChannel(targetState.userName.value) },
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(imageVector = Icons.Default.Person, contentDescription = null, modifier = Modifier.size(32.dp))
                    }
                }

                is UserPopupState.Error -> {
                    Spacer(modifier = Modifier.size(64.dp))
                }
            }
        }

        Spacer(modifier = Modifier.width(12.dp))

        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = userName.formatWithDisplayName(displayName),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center,
            )

            val successState = state as? UserPopupState.Success
            if (successState != null) {
                val followerCount = successState.followerCount
                if (followerCount != null) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Favorite,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(12.dp),
                        )
                        Spacer(modifier = Modifier.width(3.dp))
                        Text(
                            text = remember(followerCount) { NumberFormat.getIntegerInstance().format(followerCount) },
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center,
                        )
                    }
                }
                Text(
                    text = stringResource(R.string.user_popup_created, successState.created),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                )
                if (successState.showFollowingSince) {
                    Text(
                        text =
                            successState.followingSince?.let {
                                stringResource(R.string.user_popup_following_since, it)
                            } ?: stringResource(R.string.user_popup_not_following),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center,
                    )
                }
                val subscriptionTier = successState.subscriptionTier
                val subscriptionMonths = successState.subscriptionMonths
                if (subscriptionTier != null && subscriptionMonths != null) {
                    Text(
                        text = stringResource(R.string.user_popup_subscription, subscriptionTier, subscriptionMonths),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center,
                    )
                }
            }
        }
    }
}

/** Icon-only, horizontal (no labels) - was a vertical labeled list before. */
@Composable
private fun UserActionsRow(
    isLoggedIn: Boolean,
    isOwnUser: Boolean,
    isSuccess: Boolean,
    isBlocked: Boolean,
    onMention: (() -> Unit)?,
    onWhisper: (() -> Unit)?,
    onHistory: (() -> Unit)?,
    onBlockToggle: () -> Unit,
    onReport: () -> Unit,
) {
    Row(
        horizontalArrangement = Arrangement.Center,
        modifier = Modifier.fillMaxWidth().padding(top = 4.dp, bottom = 4.dp),
    ) {
        if (onMention != null && isLoggedIn) {
            IconButton(onClick = onMention) {
                Icon(imageVector = Icons.Default.AlternateEmail, contentDescription = stringResource(R.string.user_popup_mention))
            }
        }
        if (onWhisper != null && isLoggedIn && !isOwnUser) {
            IconButton(onClick = onWhisper) {
                Icon(imageVector = Icons.AutoMirrored.Filled.Chat, contentDescription = stringResource(R.string.user_popup_whisper))
            }
        }
        if (onHistory != null) {
            IconButton(onClick = onHistory) {
                Icon(imageVector = Icons.Default.History, contentDescription = stringResource(R.string.message_history))
            }
        }
        if (isSuccess && !isOwnUser) {
            IconButton(onClick = onBlockToggle) {
                Icon(
                    imageVector = Icons.Default.Block,
                    contentDescription = if (isBlocked) stringResource(R.string.user_popup_unblock) else stringResource(R.string.user_popup_block),
                    tint = if (isBlocked) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        if (!isOwnUser) {
            IconButton(onClick = onReport) {
                Icon(imageVector = Icons.Default.Report, contentDescription = stringResource(R.string.user_popup_report))
            }
        }
    }
}

/** Ban, then the configured timeout durations, then Unban - scrolls horizontally if it doesn't fit. */
@Composable
private fun ModeratorActionsRow(
    timeoutDurationsSeconds: List<Long>,
    onBanUser: () -> Unit,
    onUnbanUser: () -> Unit,
    onTimeoutUser: (Long) -> Unit,
) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        modifier =
            Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 4.dp),
    ) {
        OutlinedButton(onClick = onBanUser, colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error)) {
            Text(stringResource(R.string.user_popup_ban))
        }
        timeoutDurationsSeconds.forEach { seconds ->
            OutlinedButton(onClick = { onTimeoutUser(seconds) }) {
                Text(DateTimeUtils.formatSeconds(seconds.toInt()))
            }
        }
        OutlinedButton(onClick = onUnbanUser) {
            Text(stringResource(R.string.unban_user))
        }
    }
}

private val UserPopupState.userName: UserName
    get() =
        when (this) {
            is UserPopupState.Loading -> userName
            is UserPopupState.NotLoggedIn -> userName
            is UserPopupState.Success -> userName
            is UserPopupState.Error -> UserName("")
        }

private val UserPopupState.displayName: DisplayName
    get() =
        when (this) {
            is UserPopupState.Loading -> displayName
            is UserPopupState.NotLoggedIn -> displayName
            is UserPopupState.Success -> displayName
            is UserPopupState.Error -> DisplayName("")
        }
