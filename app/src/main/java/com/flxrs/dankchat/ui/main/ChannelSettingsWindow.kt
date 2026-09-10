package com.flxrs.dankchat.ui.main

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Launch
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Block
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DragHandle
import androidx.compose.material.icons.filled.Flag
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Popup
import androidx.compose.ui.window.PopupProperties
import coil3.compose.AsyncImage
import com.flxrs.dankchat.R
import com.flxrs.dankchat.data.UserName
import com.flxrs.dankchat.preferences.model.ChannelWithRename
import sh.calvin.reorderable.ReorderableItem
import sh.calvin.reorderable.rememberReorderableLazyListState
import kotlin.math.roundToInt

private val WINDOW_WIDTH = 340.dp
private val WINDOW_MAX_HEIGHT = 420.dp

/**
 * The channel-settings surface used to be a bottom sheet ("manage channels"). It's now a small
 * floating, draggable window - drag the header to move it, tap the X to close. It's clamped so
 * its center can never leave the screen, meaning at least half of it always stays visible.
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun ChannelSettingsWindow(
    channels: List<ChannelWithRename>,
    avatarUrls: Map<UserName, String?>,
    activeChannel: UserName?,
    onApplyChanges: (List<ChannelWithRename>) -> Unit,
    onSwitchToChannel: (UserName) -> Unit,
    onOpenChannelInBrowser: (UserName) -> Unit,
    onReportChannel: (UserName) -> Unit,
    onBlockChannel: (UserName) -> Unit,
    onAddChannel: () -> Unit,
    onDismiss: () -> Unit,
) {
    var channelToDelete by remember { mutableStateOf<UserName?>(null) }

    val localChannels = remember { mutableStateListOf<ChannelWithRename>() }
    LaunchedEffect(channels) {
        if (localChannels.isEmpty() && channels.isNotEmpty()) {
            localChannels.addAll(channels)
        }
    }

    val lazyListState = rememberLazyListState()
    val reorderableState =
        rememberReorderableLazyListState(lazyListState) { from, to ->
            if (from.index in localChannels.indices && to.index in localChannels.indices) {
                localChannels.apply {
                    add(to.index, removeAt(from.index))
                }
            }
        }

    // Jump straight to whichever channel the person is already on - convenient when there are a
    // lot of channels and they just want to act on the current one.
    LaunchedEffect(activeChannel, localChannels.size) {
        val index = localChannels.indexOfFirst { it.channel == activeChannel }
        if (index > 0) {
            lazyListState.scrollToItem(index)
        }
    }

    val density = LocalDensity.current
    val configuration = LocalConfiguration.current
    val screenWidthPx = with(density) { configuration.screenWidthDp.dp.toPx() }
    val screenHeightPx = with(density) { configuration.screenHeightDp.dp.toPx() }
    var dragOffset by remember { mutableStateOf(IntOffset.Zero) }

    Popup(
        alignment = Alignment.Center,
        offset = dragOffset,
        properties = PopupProperties(focusable = true, usePlatformDefaultWidth = false),
        onDismissRequest = {
            onApplyChanges(localChannels.toList())
            onDismiss()
        },
    ) {
        Surface(
            shape = RoundedCornerShape(16.dp),
            color = MaterialTheme.colorScheme.surfaceContainerHigh,
            shadowElevation = 8.dp,
            modifier = Modifier.width(WINDOW_WIDTH),
        ) {
            Column {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .pointerInputDragHeader(
                                onDrag = { delta ->
                                    val maxX = (screenWidthPx / 2).roundToInt()
                                    val maxY = (screenHeightPx / 2).roundToInt()
                                    val newX = (dragOffset.x + delta.x.roundToInt()).coerceIn(-maxX, maxX)
                                    val newY = (dragOffset.y + delta.y.roundToInt()).coerceIn(-maxY, maxY)
                                    dragOffset = IntOffset(newX, newY)
                                },
                            ).padding(start = 16.dp, end = 4.dp, top = 4.dp, bottom = 4.dp),
                ) {
                    Text(
                        text = stringResource(R.string.manage_channels),
                        style = MaterialTheme.typography.titleMedium,
                        modifier = Modifier.weight(1f),
                    )
                    IconButton(onClick = onAddChannel) {
                        Icon(imageVector = Icons.Default.Add, contentDescription = stringResource(R.string.add_channel))
                    }
                    IconButton(onClick = {
                        onApplyChanges(localChannels.toList())
                        onDismiss()
                    }) {
                        Icon(imageVector = Icons.Default.Close, contentDescription = stringResource(R.string.dialog_cancel))
                    }
                }
                HorizontalDivider()

                AnimatedContent(targetState = channelToDelete, label = "ChannelSettingsContent") { deleteTarget ->
                    when (deleteTarget) {
                        null -> {
                            LazyColumn(
                                state = lazyListState,
                                modifier = Modifier.heightIn(max = WINDOW_MAX_HEIGHT),
                            ) {
                                itemsIndexed(localChannels, key = { _, item -> item.channel.value }) { index, channelWithRename ->
                                    ReorderableItem(reorderableState, key = channelWithRename.channel.value) { isDragging ->
                                        val elevation by animateDpAsState(if (isDragging) 8.dp else 0.dp, label = "rowElevation")
                                        Surface(
                                            shadowElevation = elevation,
                                            color = if (isDragging) MaterialTheme.colorScheme.surfaceContainerHighest else Color.Transparent,
                                        ) {
                                            Column {
                                                ChannelSettingsRow(
                                                    channelWithRename = channelWithRename,
                                                    avatarUrl = avatarUrls[channelWithRename.channel],
                                                    dragHandleModifier =
                                                        Modifier.longPressDraggableHandle(
                                                            onDragStarted = {},
                                                            onDragStopped = {},
                                                        ),
                                                    onSwitchTo = {
                                                        onApplyChanges(localChannels.toList())
                                                        onSwitchToChannel(channelWithRename.channel)
                                                        onDismiss()
                                                    },
                                                    onOpenInBrowser = { onOpenChannelInBrowser(channelWithRename.channel) },
                                                    onReport = { onReportChannel(channelWithRename.channel) },
                                                    onBlock = { onBlockChannel(channelWithRename.channel) },
                                                    onRename = { newName ->
                                                        val rename = newName?.ifBlank { null }?.let { UserName(it) }
                                                        localChannels[index] = localChannels[index].copy(rename = rename)
                                                    },
                                                    onDelete = { channelToDelete = channelWithRename.channel },
                                                )
                                                if (index < localChannels.lastIndex) {
                                                    HorizontalDivider(
                                                        modifier = Modifier.padding(horizontal = 16.dp),
                                                        color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f),
                                                    )
                                                }
                                            }
                                        }
                                    }
                                }

                                if (localChannels.isEmpty()) {
                                    item {
                                        Text(
                                            text = stringResource(R.string.no_channels_added),
                                            style = MaterialTheme.typography.bodyMedium,
                                            modifier = Modifier.padding(16.dp),
                                        )
                                    }
                                }
                            }
                        }

                        else -> {
                            ChannelSettingsDeleteConfirmation(
                                channelName = deleteTarget,
                                onConfirm = {
                                    localChannels.removeAll { it.channel == deleteTarget }
                                    onApplyChanges(localChannels.toList())
                                    channelToDelete = null
                                },
                                onBack = { channelToDelete = null },
                            )
                        }
                    }
                }
            }
        }
    }
}

private fun Modifier.pointerInputDragHeader(onDrag: (Offset) -> Unit): Modifier = this.pointerInput(Unit) {
    detectDragGestures { change, dragAmount ->
        change.consume()
        onDrag(dragAmount)
    }
}

@Composable
private fun ChannelSettingsRow(
    channelWithRename: ChannelWithRename,
    avatarUrl: String?,
    dragHandleModifier: Modifier,
    onSwitchTo: () -> Unit,
    onOpenInBrowser: () -> Unit,
    onReport: () -> Unit,
    onBlock: () -> Unit,
    onRename: (String?) -> Unit,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var isEditing by remember(channelWithRename.channel) { mutableStateOf(false) }

    Column(modifier = modifier) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth().padding(start = 4.dp, end = 4.dp, top = 6.dp, bottom = 6.dp),
        ) {
            Icon(
                imageVector = Icons.Default.DragHandle,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = dragHandleModifier.padding(6.dp).size(20.dp),
            )

            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier.size(32.dp).clip(CircleShape).padding(start = 4.dp),
            ) {
                if (avatarUrl != null) {
                    AsyncImage(
                        model = avatarUrl,
                        contentDescription = null,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.size(32.dp).clip(CircleShape),
                    )
                } else {
                    Surface(
                        shape = CircleShape,
                        color = MaterialTheme.colorScheme.surfaceContainerHighest,
                        modifier = Modifier.size(32.dp),
                    ) {}
                }
            }

            Text(
                text = channelWithRename.rename?.value ?: channelWithRename.channel.value,
                style = MaterialTheme.typography.bodyMedium,
                maxLines = 1,
                modifier = Modifier.weight(1f, fill = false).padding(start = 8.dp, end = 4.dp),
            )

            // Kept at the default (accessible) IconButton touch target size - the row scrolls
            // horizontally instead of shrinking the touch targets to fit.
            Row(modifier = Modifier.horizontalScroll(rememberScrollState())) {
                IconButton(onClick = { isEditing = !isEditing }) {
                    Icon(
                        painter = painterResource(R.drawable.ic_edit),
                        contentDescription = stringResource(R.string.edit_dialog_title),
                        modifier = Modifier.size(20.dp),
                    )
                }
                IconButton(onClick = onSwitchTo) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.OpenInNew,
                        contentDescription = stringResource(R.string.switch_to_channel),
                        modifier = Modifier.size(20.dp),
                    )
                }
                IconButton(onClick = onOpenInBrowser) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.Launch,
                        contentDescription = stringResource(R.string.open_channel),
                        modifier = Modifier.size(20.dp),
                    )
                }
                IconButton(onClick = onDelete) {
                    Icon(
                        painter = painterResource(R.drawable.ic_delete_outline),
                        contentDescription = stringResource(R.string.remove_channel),
                        modifier = Modifier.size(20.dp),
                    )
                }
                IconButton(onClick = onReport) {
                    Icon(
                        imageVector = Icons.Default.Flag,
                        contentDescription = stringResource(R.string.report_channel),
                        modifier = Modifier.size(20.dp),
                    )
                }
                IconButton(onClick = onBlock) {
                    Icon(
                        imageVector = Icons.Default.Block,
                        contentDescription = stringResource(R.string.block_channel),
                        modifier = Modifier.size(20.dp),
                    )
                }
            }
        }

        if (isEditing) {
            ChannelSettingsRenameField(
                channelWithRename = channelWithRename,
                onRename = {
                    onRename(it)
                    isEditing = false
                },
            )
        }
    }
}

@Composable
private fun ChannelSettingsRenameField(
    channelWithRename: ChannelWithRename,
    onRename: (String?) -> Unit,
) {
    var renameText by remember(channelWithRename.channel) {
        mutableStateOf(channelWithRename.rename?.value.orEmpty())
    }
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.fillMaxWidth().padding(start = 56.dp, end = 8.dp, bottom = 8.dp),
    ) {
        OutlinedTextField(
            value = renameText,
            onValueChange = { renameText = it },
            placeholder = { Text(channelWithRename.channel.value) },
            singleLine = true,
            modifier = Modifier.weight(1f),
        )
        TextButton(onClick = { onRename(renameText) }) {
            Text(stringResource(R.string.save))
        }
    }
}

@Composable
private fun ChannelSettingsDeleteConfirmation(
    channelName: UserName,
    onConfirm: () -> Unit,
    onBack: () -> Unit,
) {
    Column(modifier = Modifier.fillMaxWidth().navigationBarsPadding().padding(16.dp)) {
        Text(
            text = stringResource(R.string.confirm_channel_removal_message_named, channelName.value),
            style = MaterialTheme.typography.bodyMedium,
        )
        Row(modifier = Modifier.fillMaxWidth().padding(top = 16.dp)) {
            OutlinedButton(onClick = onBack, modifier = Modifier.weight(1f)) {
                Text(stringResource(R.string.dialog_cancel))
            }
            Spacer(modifier = Modifier.width(12.dp))
            Button(
                onClick = onConfirm,
                modifier = Modifier.weight(1f),
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
            ) {
                Text(stringResource(R.string.confirm_channel_removal_positive_button))
            }
        }
    }
}
