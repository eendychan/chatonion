package com.flxrs.dankchat.ui.chat.messages.common

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import com.flxrs.dankchat.data.twitch.badge.Badge
import com.flxrs.dankchat.ui.chat.BadgeUi

private val FfzVipShape = RoundedCornerShape(percent = 15)
private val FfzBadgeShape = RoundedCornerShape(percent = 20)

@Composable
fun BadgeInlineContent(
    badge: BadgeUi,
    size: Dp,
    modifier: Modifier = Modifier,
) {
    when (badge.badge) {
        is Badge.SharedChatBadge -> {
            AsyncImage(
                model = badge.drawableResId ?: badge.url,
                contentDescription = badge.badge.type.name,
                modifier =
                    modifier
                        .size(size)
                        .clip(CircleShape),
            )
        }

        is Badge.FFZVipBadge -> {
            AsyncImage(
                model = badge.url,
                contentDescription = badge.badge.type.name,
                modifier =
                    modifier
                        .size(size)
                        .clip(FfzVipShape),
            )
        }

        is Badge.FFZBadge -> {
            // FFZ badges are designed for their own background color (supporter brown,
            // subwoofer blue, ...), given by the FFZ badge list
            val backgroundColor = badge.badge.backgroundColor
            AsyncImage(
                model = badge.url,
                contentDescription = badge.badge.type.name,
                modifier =
                    modifier
                        .size(size)
                        .clip(FfzBadgeShape)
                        .then(if (backgroundColor != null) Modifier.background(Color(backgroundColor)) else Modifier)
                        .padding(if (backgroundColor != null) 1.dp else 0.dp),
            )
        }

        else -> {
            AsyncImage(
                model = badge.drawableResId ?: badge.url,
                contentDescription = badge.badge.type.name,
                modifier = modifier.size(size),
            )
        }
    }
}
