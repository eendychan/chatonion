package com.flxrs.dankchat.ui.chat.messages

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.text.appendInlineContent
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import com.flxrs.dankchat.R
import com.flxrs.dankchat.data.UserName
import com.flxrs.dankchat.ui.chat.ChatMessageUiState
import com.flxrs.dankchat.ui.chat.ChatMessageUiState.AutomodMessageUi.AutomodMessageStatus
import com.flxrs.dankchat.ui.chat.emote.emoteBaseHeight
import com.flxrs.dankchat.ui.chat.messages.common.BadgeInlineContent
import com.flxrs.dankchat.ui.chat.messages.common.EmoteDimensions
import com.flxrs.dankchat.ui.chat.messages.common.TextWithMeasuredInlineContent
import com.flxrs.dankchat.ui.chat.messages.common.appendInlineSpacer
import com.flxrs.dankchat.ui.chat.messages.common.rememberNormalizedColor
import kotlinx.collections.immutable.persistentMapOf
import kotlinx.collections.immutable.toImmutableMap

private val AutoModBlue = Color(0xFF448AFF)

private const val BAN_TAG = "BAN"

@Composable
fun AutomodMessageComposable(
    message: ChatMessageUiState.AutomodMessageUi,
    fontSize: Float,
    onAllow: (heldMessageId: String, channel: UserName) -> Unit,
    onDeny: (heldMessageId: String, channel: UserName) -> Unit,
    onMessageLongClick: () -> Unit,
    onBanUser: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val textColor = MaterialTheme.colorScheme.onSurface
    val timestampColor = MaterialTheme.colorScheme.onSurface
    val flaggedColor = MaterialTheme.colorScheme.error
    val denyColor = MaterialTheme.colorScheme.error
    val textSize = fontSize.sp
    val isPending = message.status == AutomodMessageStatus.Pending
    val isCompleted = !message.isUserSide && message.status != AutomodMessageStatus.Pending
    val backgroundColor = MaterialTheme.colorScheme.background
    val nameColor = rememberNormalizedColor(message.rawNameColor, backgroundColor)

    // Faded colors for completed (approved/denied/expired) header
    val headerTextColor = if (isCompleted) textColor.copy(alpha = 0.5f) else textColor

    // Resolve strings
    val heldLabel = stringResource(R.string.automod_message_held_label)
    val approvedText = stringResource(R.string.automod_status_approved)
    val deniedText = stringResource(R.string.automod_status_denied)
    val banUserText = stringResource(R.string.automod_ban_user)
    val expiredText = stringResource(R.string.automod_status_expired)
    val userHeldText = stringResource(R.string.automod_user_held)
    val userAcceptedText = stringResource(R.string.automod_user_accepted)
    val userDeniedText = stringResource(R.string.automod_user_denied)

    // Header line: [badge] "AutoMod: Message held:" (+ status word once resolved)
    val headerString =
        remember(
            message,
            headerTextColor,
            denyColor,
            heldLabel,
            approvedText,
            deniedText,
            expiredText,
            userHeldText,
            userAcceptedText,
            userDeniedText,
            banUserText,
        ) {
            buildAnnotatedString {
                // Badges
                message.badges.forEach { badge ->
                    appendInlineContent("BADGE_${badge.position}", "[badge]")
                    append(" ")
                }

                // "AutoMod: " in blue bold
                val autoModBlue = if (isCompleted) AutoModBlue.copy(alpha = 0.5f) else AutoModBlue
                withStyle(SpanStyle(color = autoModBlue, fontWeight = FontWeight.Bold)) {
                    append("AutoMod: ")
                }

                when {
                    // User-side: simple status messages
                    message.isUserSide -> {
                        when (message.status) {
                            AutomodMessageStatus.Pending -> withStyle(SpanStyle(color = headerTextColor)) { append(userHeldText) }
                            AutomodMessageStatus.Approved -> withStyle(SpanStyle(color = headerTextColor)) { append(userAcceptedText) }
                            AutomodMessageStatus.Denied -> withStyle(SpanStyle(color = headerTextColor)) { append(userDeniedText) }
                            AutomodMessageStatus.Expired -> withStyle(SpanStyle(color = textColor.copy(alpha = 0.5f))) { append(expiredText) }
                        }
                    }

                    // Mod-side: short fixed label - the flagged phrase is highlighted in the message itself,
                    // Allow/Deny are separate buttons below rather than inline text
                    else -> {
                        withStyle(SpanStyle(color = headerTextColor)) {
                            append(heldLabel)
                        }

                        when (message.status) {
                            AutomodMessageStatus.Approved -> {
                                append(" ")
                                withStyle(SpanStyle(color = MaterialTheme.colorScheme.primary.copy(alpha = 0.6f), fontWeight = FontWeight.Bold)) {
                                    append(approvedText)
                                }
                            }

                            AutomodMessageStatus.Denied -> {
                                append(" ")
                                withStyle(SpanStyle(color = denyColor.copy(alpha = 0.6f), fontWeight = FontWeight.Bold)) {
                                    append(deniedText)
                                }

                                pushStringAnnotation(tag = BAN_TAG, annotation = "ban")
                                withStyle(SpanStyle(color = denyColor, fontWeight = FontWeight.Bold)) {
                                    append("  $banUserText")
                                }
                                pop()
                            }

                            AutomodMessageStatus.Expired -> {
                                append(" ")
                                withStyle(SpanStyle(color = textColor.copy(alpha = 0.5f), fontWeight = FontWeight.Bold)) {
                                    append(expiredText)
                                }
                            }

                            AutomodMessageStatus.Pending -> Unit
                        }
                    }
                }
            }
        }

    // Body line: "timestamp {displayName}: {message}" - the flagged fragment(s), if any, are highlighted
    val bodyString =
        remember(message, textColor, nameColor, timestampColor, flaggedColor, textSize) {
            message.messageText?.let { text ->
                buildAnnotatedString {
                    // Timestamp for alignment
                    if (message.timestamp.isNotEmpty()) {
                        withStyle(
                            SpanStyle(
                                fontFamily = FontFamily.Monospace,
                                fontWeight = FontWeight.Bold,
                                fontSize = textSize * 0.8f,
                                color = timestampColor.copy(alpha = timestampColor.alpha * 0.6f),
                                letterSpacing = (-0.03).em,
                            ),
                        ) {
                            append(message.timestamp)
                        }
                        appendInlineSpacer(6.dp)
                    }

                    // Username in bold with user color
                    withStyle(SpanStyle(color = nameColor, fontWeight = FontWeight.Bold)) {
                        append("${message.userDisplayName}: ")
                    }

                    // Message text, with the flagged fragment(s) highlighted in red
                    var cursor = 0
                    val sortedRanges = message.flaggedRanges.sortedBy { it.first }
                    for (range in sortedRanges) {
                        val start = range.first.coerceIn(0, text.length)
                        val end = (range.last + 1).coerceIn(start, text.length)
                        if (start > cursor) {
                            withStyle(SpanStyle(color = textColor)) {
                                append(text.substring(cursor, start))
                            }
                        }
                        if (end > start) {
                            withStyle(SpanStyle(color = flaggedColor, fontWeight = FontWeight.Bold)) {
                                append(text.substring(start, end))
                            }
                        }
                        cursor = end
                    }
                    if (cursor < text.length) {
                        withStyle(SpanStyle(color = textColor)) {
                            append(text.substring(cursor))
                        }
                    }
                }
            }
        }

    // Badge inline content providers (same pattern as PrivMessage)
    val badgeSize = emoteBaseHeight(fontSize)
    val inlineContentProviders =
        remember(message.badges, fontSize) {
            buildMap<String, @Composable () -> Unit> {
                message.badges.forEach { badge ->
                    put("BADGE_${badge.position}") {
                        BadgeInlineContent(badge = badge, size = badgeSize)
                    }
                }
            }.toImmutableMap()
        }

    val density = LocalDensity.current
    val knownDimensions =
        remember(message.badges, fontSize) {
            buildMap {
                val badgeSizePx = with(density) { badgeSize.toPx().toInt() }
                message.badges.forEach { badge ->
                    put("BADGE_${badge.position}", EmoteDimensions("BADGE_${badge.position}", badgeSizePx, badgeSizePx))
                }
            }.toImmutableMap()
        }

    val resolvedAlpha =
        when {
            message.isUserSide -> 1f
            message.status == AutomodMessageStatus.Pending -> 1f
            else -> 0.5f
        }

    Column(
        modifier =
            modifier
                .fillMaxWidth()
                .wrapContentHeight()
                .padding(horizontal = 6.dp, vertical = 3.dp),
    ) {
        // Header line with badge inline content
        TextWithMeasuredInlineContent(
            text = headerString,
            inlineContentProviders = inlineContentProviders,
            style = TextStyle(fontSize = textSize),
            knownDimensions = knownDimensions,
            modifier = Modifier.fillMaxWidth(),
            onTextClick = { offset ->
                headerString
                    .getStringAnnotations(BAN_TAG, offset, offset)
                    .firstOrNull()
                    ?.let {
                        onBanUser()
                    }
            },
            onTextLongClick = { onMessageLongClick() },
        )

        // Allow/Deny as real, easy-to-tap buttons between the AutoMod line and the held message
        if (isPending && !message.isUserSide) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
            ) {
                OutlinedButton(
                    onClick = { onDeny(message.heldMessageId, message.channel) },
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = denyColor),
                ) {
                    Text(stringResource(R.string.automod_deny))
                }
                Button(onClick = { onAllow(message.heldMessageId, message.channel) }) {
                    Text(stringResource(R.string.automod_allow))
                }
            }
        }

        // Body line with held message text
        bodyString?.let {
            TextWithMeasuredInlineContent(
                text = it,
                inlineContentProviders = persistentMapOf(),
                style = TextStyle(fontSize = textSize),
                modifier = Modifier.fillMaxWidth().alpha(resolvedAlpha),
                onTextLongClick = { onMessageLongClick() },
            )
        }
    }
}
