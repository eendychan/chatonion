package com.flxrs.dankchat.ui.main

import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BugReport
import androidx.compose.material.icons.filled.CloudUpload
import androidx.compose.material.icons.filled.Fullscreen
import androidx.compose.material.icons.filled.FullscreenExit
import androidx.compose.material.icons.filled.Headphones
import androidx.compose.material.icons.filled.Paid
import androidx.compose.material.icons.filled.Theaters
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material.icons.outlined.Videocam
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.flxrs.dankchat.R
import com.flxrs.dankchat.preferences.appearance.InputAction

/**
 * These actions always live behind the overflow trigger next to the input field. Shown as a
 * horizontally scrollable row of icon+label chips - a continuation of the input field itself
 * (no surface/background of its own; the caller places this inside the input panel), the same
 * way the top bar's own overflow row works, just with labels next to the icons instead of
 * icon-only.
 */
@Composable
fun QuickActionsMenu(
    enabled: Boolean,
    isStreamActive: Boolean,
    isAudioOnly: Boolean,
    isFullscreen: Boolean,
    isTheaterMode: Boolean,
    debugMode: Boolean,
    onActionClick: (InputAction) -> Unit,
    onAudioOnly: () -> Unit,
    onUploadClick: () -> Unit,
    modifier: Modifier = Modifier,
    showDonations: Boolean = false,
    onDonationsClick: () -> Unit = {},
) {
    val scrollState = rememberScrollState()

    Row(
        horizontalArrangement = Arrangement.spacedBy(2.dp),
        modifier =
            modifier
                .horizontalScroll(scrollState)
                .padding(horizontal = 12.dp, vertical = 8.dp),
    ) {
        // Used to live in the top toolbar's overflow menu - now first here per the fixed order.
        QuickActionItem(
            text = stringResource(R.string.upload_media),
            icon = Icons.Default.CloudUpload,
            enabled = enabled,
            onClick = onUploadClick,
        )

        if (showDonations) {
            QuickActionItem(
                text = stringResource(R.string.donations_menu_item),
                icon = Icons.Default.Paid,
                enabled = enabled,
                onClick = onDonationsClick,
            )
        }

        // Theater mode is already fullscreen, so toggling chat fullscreen makes no sense there -
        // still shown per the fixed order, just disabled.
        QuickActionItem(
            text = stringResource(if (isFullscreen) R.string.menu_exit_fullscreen else R.string.menu_fullscreen),
            icon = if (isFullscreen) Icons.Default.FullscreenExit else Icons.Default.Fullscreen,
            enabled = enabled && !isTheaterMode,
            onClick = { onActionClick(InputAction.Fullscreen) },
        )

        // Always visible per the fixed order - disabled rather than hidden when there's no
        // stream to switch modes for.
        QuickActionItem(
            text = stringResource(if (isTheaterMode) R.string.menu_exit_theater_mode else R.string.menu_theater_mode),
            icon = Icons.Default.Theaters,
            enabled = enabled && isStreamActive,
            onClick = { onActionClick(InputAction.Theater) },
        )

        QuickActionItem(
            text = stringResource(R.string.menu_hide_input),
            icon = Icons.Default.VisibilityOff,
            enabled = enabled,
            onClick = { onActionClick(InputAction.HideInput) },
        )

        QuickActionItem(
            text = stringResource(if (isAudioOnly) R.string.menu_exit_audio_only else R.string.menu_audio_only),
            icon = if (isAudioOnly) Icons.Outlined.Videocam else Icons.Default.Headphones,
            enabled = enabled && isStreamActive,
            onClick = onAudioOnly,
        )

        if (debugMode) {
            QuickActionItem(
                text = stringResource(R.string.input_action_debug),
                icon = Icons.Default.BugReport,
                enabled = enabled,
                onClick = { onActionClick(InputAction.Debug) },
            )
        }
    }
}

@Composable
private fun QuickActionItem(
    text: String,
    icon: ImageVector,
    enabled: Boolean,
    onClick: () -> Unit,
) {
    val contentColor =
        when {
            enabled -> MaterialTheme.colorScheme.onSurface
            else -> MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.38f)
        }

    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        modifier =
            Modifier
                .clip(RoundedCornerShape(12.dp))
                .clickable(enabled = enabled, onClick = onClick)
                .padding(horizontal = 10.dp, vertical = 8.dp),
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = contentColor,
            modifier = Modifier.size(20.dp),
        )
        Text(
            text = text,
            color = contentColor,
            style = MaterialTheme.typography.labelMedium,
            maxLines = 1,
            softWrap = false,
        )
    }
}
