package com.flxrs.dankchat.ui.main

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BugReport
import androidx.compose.material.icons.filled.Fullscreen
import androidx.compose.material.icons.filled.FullscreenExit
import androidx.compose.material.icons.filled.Headphones
import androidx.compose.material.icons.filled.Theaters
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material.icons.outlined.Videocam
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.flxrs.dankchat.R
import com.flxrs.dankchat.preferences.appearance.InputAction

/**
 * The old configurable/overflow-able action row is gone - these five actions always live behind
 * the overflow trigger next to the input field, shown vertically with labels.
 */
@Composable
fun QuickActionsMenu(
    surfaceColor: Color,
    enabled: Boolean,
    isStreamActive: Boolean,
    isAudioOnly: Boolean,
    isFullscreen: Boolean,
    isTheaterMode: Boolean,
    debugMode: Boolean,
    onActionClick: (InputAction) -> Unit,
    onAudioOnly: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val scrollState = rememberScrollState()

    Surface(
        shape = RoundedCornerShape(topStart = 12.dp, topEnd = 12.dp),
        color = surfaceColor,
        modifier = modifier,
    ) {
        Column(
            modifier = Modifier
                .width(IntrinsicSize.Max)
                .verticalScroll(scrollState),
        ) {
            // Theater mode is already fullscreen, so toggling chat fullscreen makes no sense there
            if (!isTheaterMode) {
                DropdownMenuItem(
                    text = { Text(stringResource(if (isFullscreen) R.string.menu_exit_fullscreen else R.string.menu_fullscreen)) },
                    onClick = { onActionClick(InputAction.Fullscreen) },
                    enabled = enabled,
                    leadingIcon = {
                        Icon(
                            imageVector = if (isFullscreen) Icons.Default.FullscreenExit else Icons.Default.Fullscreen,
                            contentDescription = null,
                        )
                    },
                )
            }

            if (isStreamActive) {
                DropdownMenuItem(
                    text = { Text(stringResource(if (isTheaterMode) R.string.menu_exit_theater_mode else R.string.menu_theater_mode)) },
                    onClick = { onActionClick(InputAction.Theater) },
                    enabled = enabled,
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Default.Theaters,
                            contentDescription = null,
                        )
                    },
                )
            }

            DropdownMenuItem(
                text = { Text(stringResource(R.string.menu_hide_input)) },
                onClick = { onActionClick(InputAction.HideInput) },
                enabled = enabled,
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Default.VisibilityOff,
                        contentDescription = null,
                    )
                },
            )

            if (isStreamActive) {
                DropdownMenuItem(
                    text = { Text(stringResource(if (isAudioOnly) R.string.menu_exit_audio_only else R.string.menu_audio_only)) },
                    onClick = onAudioOnly,
                    enabled = enabled,
                    leadingIcon = {
                        Icon(
                            imageVector = if (isAudioOnly) Icons.Outlined.Videocam else Icons.Default.Headphones,
                            contentDescription = null,
                        )
                    },
                )
            }

            if (debugMode) {
                DropdownMenuItem(
                    text = { Text(stringResource(R.string.input_action_debug)) },
                    onClick = { onActionClick(InputAction.Debug) },
                    enabled = enabled,
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Default.BugReport,
                            contentDescription = null,
                        )
                    },
                )
            }
        }
    }
}
