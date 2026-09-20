package com.flxrs.dankchat.ui.main

import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Snackbar
import androidx.compose.material3.SnackbarData
import androidx.compose.material3.SnackbarDefaults
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarVisuals
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.rememberSwipeToDismissBoxState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer

private const val DRAG_DISMISS_THRESHOLD_FRACTION = 0.5f
private const val SWIPE_ALPHA_FADE = 0.4f

/** Snackbar visuals rendered with error (red) colors by [DismissibleSnackbar]. */
class ErrorSnackbarVisuals(
    override val message: String,
    override val actionLabel: String? = null,
    override val withDismissAction: Boolean = false,
    override val duration: SnackbarDuration = SnackbarDuration.Short,
) : SnackbarVisuals

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DismissibleSnackbar(data: SnackbarData) {
    val state = rememberSwipeToDismissBoxState(
        positionalThreshold = { total -> total * DRAG_DISMISS_THRESHOLD_FRACTION },
    )
    LaunchedEffect(state.currentValue) {
        if (state.currentValue == SwipeToDismissBoxValue.StartToEnd) {
            data.dismiss()
        }
    }
    val isError = data.visuals is ErrorSnackbarVisuals
    SwipeToDismissBox(
        state = state,
        backgroundContent = {},
        enableDismissFromStartToEnd = true,
        enableDismissFromEndToStart = false,
    ) {
        Snackbar(
            snackbarData = data,
            containerColor = if (isError) MaterialTheme.colorScheme.error else SnackbarDefaults.color,
            contentColor = if (isError) MaterialTheme.colorScheme.onError else SnackbarDefaults.contentColor,
            actionColor = if (isError) MaterialTheme.colorScheme.onError else SnackbarDefaults.actionColor,
            dismissActionContentColor = if (isError) MaterialTheme.colorScheme.onError else SnackbarDefaults.dismissActionContentColor,
            modifier = Modifier.graphicsLayer {
                val width = size.width
                if (width > 0f) {
                    val offset = runCatching { state.requireOffset() }.getOrDefault(0f)
                    val fraction = (offset / width).coerceIn(0f, 1f)
                    alpha = 1f - fraction * SWIPE_ALPHA_FADE
                }
            },
        )
    }
}
