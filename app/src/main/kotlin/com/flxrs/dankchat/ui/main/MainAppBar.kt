package com.flxrs.dankchat.ui.main

import androidx.activity.compose.PredictiveBackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.SizeTransform
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Login
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Autorenew
import androidx.compose.material.icons.filled.Block
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.CloudUpload
import androidx.compose.material.icons.filled.EditNote
import androidx.compose.material.icons.filled.EmojiEmotions
import androidx.compose.material.icons.filled.Flag
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.OpenInBrowser
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.RemoveCircleOutline
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material.icons.outlined.Shield
import androidx.compose.material.icons.outlined.Videocam
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.VerticalDivider
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.boundsInWindow
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.flxrs.dankchat.R
import com.flxrs.dankchat.ui.theme.toolbarPillColor
import com.flxrs.dankchat.utils.compose.predictiveBackScale
import kotlinx.coroutines.CancellationException

@Immutable
sealed interface AppBarMenu {
    data object Main : AppBarMenu

    data object Upload : AppBarMenu

    data object Channel : AppBarMenu
}

internal const val RESTING_SCROLLBAR_ALPHA = 0.6f

internal class InlineMenuItemRegistry {
    var pressedKey by mutableStateOf<Any?>(null)
    private val items = mutableStateMapOf<Any, ItemEntry>()

    fun register(
        key: Any,
        bounds: Rect,
        onSelect: () -> Unit,
    ) {
        items[key] = ItemEntry(bounds, onSelect)
    }

    fun unregister(key: Any) {
        items.remove(key)
    }

    fun keyAt(window: Offset): Any? = items.entries.firstOrNull { it.value.bounds.contains(window) }?.key

    fun selectAt(window: Offset): Boolean {
        val entry = items.values.firstOrNull { it.bounds.contains(window) } ?: return false
        entry.onSelect()
        return true
    }

    private data class ItemEntry(
        val bounds: Rect,
        val onSelect: () -> Unit,
    )
}

internal val LocalInlineMenuItemRegistry = staticCompositionLocalOf<InlineMenuItemRegistry?> { null }

private val OVERFLOW_ITEM_SIZE = 44.dp
private val OVERFLOW_ITEM_ICON_SIZE = 20.dp

/**
 * Everything that used to live as separate icons/pills next to the channel tabs (add channel,
 * mentions, pinned/quick actions) plus the actions that used to sit below the message input
 * (search, last message, moderation, stream toggle) is collapsed into this single menu. It
 * expands left-to-right next to the trigger button and scrolls horizontally, mirroring the
 * channel tab strip instead of the old top-to-bottom dropdown.
 */
@Composable
fun InlineOverflowMenu(
    isLoggedIn: Boolean,
    isModerator: Boolean,
    hasActiveStream: Boolean,
    mentionCount: Int,
    onDismiss: () -> Unit,
    onAction: (ToolbarAction) -> Unit,
    initialMenu: AppBarMenu = AppBarMenu.Main,
    maxHeightDp: Dp = 0.dp,
) {
    var currentMenu by remember(initialMenu) { mutableStateOf(initialMenu) }
    var backProgress by remember { mutableFloatStateOf(0f) }

    PredictiveBackHandler { progress ->
        try {
            progress.collect { event ->
                backProgress = event.progress
            }
            when (currentMenu) {
                AppBarMenu.Main -> {
                    onDismiss()
                }

                else -> {
                    backProgress = 0f
                    currentMenu = AppBarMenu.Main
                }
            }
        } catch (_: CancellationException) {
            backProgress = 0f
        }
    }

    val scrollState = rememberScrollState()
    LaunchedEffect(currentMenu) { scrollState.scrollTo(0) }

    Surface(
        shape = MaterialTheme.shapes.large,
        color = MaterialTheme.colorScheme.toolbarPillColor,
        modifier = Modifier.predictiveBackScale(backProgress),
    ) {
        AnimatedContent(
            targetState = currentMenu,
            transitionSpec = {
                if (targetState != AppBarMenu.Main) {
                    (slideInHorizontally { it } + fadeIn()).togetherWith(slideOutHorizontally { -it } + fadeOut())
                } else {
                    (slideInHorizontally { -it } + fadeIn()).togetherWith(slideOutHorizontally { it } + fadeOut())
                }.using(SizeTransform(clip = false))
            },
            label = "InlineMenuTransition",
        ) { menu ->
            Row(
                modifier =
                    Modifier
                        .heightIn(max = maxHeightDp.takeIf { it > 0.dp } ?: OVERFLOW_ITEM_SIZE + 8.dp)
                        .horizontalScroll(scrollState)
                        .padding(horizontal = 4.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                when (menu) {
                    AppBarMenu.Main -> MainMenuContent(
                        isLoggedIn = isLoggedIn,
                        isModerator = isModerator,
                        hasActiveStream = hasActiveStream,
                        mentionCount = mentionCount,
                        onAction = onAction,
                        onDismiss = onDismiss,
                        onNavigateToUpload = { currentMenu = AppBarMenu.Upload },
                        onNavigateToChannel = { currentMenu = AppBarMenu.Channel },
                    )

                    AppBarMenu.Upload -> UploadMenuContent(
                        onAction = onAction,
                        onDismiss = onDismiss,
                        onBack = { currentMenu = AppBarMenu.Main },
                    )

                    AppBarMenu.Channel -> ChannelMenuContent(
                        isLoggedIn = isLoggedIn,
                        onAction = onAction,
                        onDismiss = onDismiss,
                        onBack = { currentMenu = AppBarMenu.Main },
                    )
                }
            }
        }
    }
}

/** Compact, icon-only, square menu entry (no label shown — meaning must be clear from the icon alone). */
@Composable
private fun InlineMenuIconItem(
    key: String,
    icon: ImageVector,
    contentDescription: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    tint: Color = MaterialTheme.colorScheme.onSurfaceVariant,
) {
    val registry = LocalInlineMenuItemRegistry.current
    val isPressed = registry?.pressedKey == key
    if (registry != null) {
        DisposableEffect(registry, key) {
            onDispose { registry.unregister(key) }
        }
    }
    val highlightColor = MaterialTheme.colorScheme.surfaceContainerHighest
    Row(
        modifier =
            modifier
                .size(OVERFLOW_ITEM_SIZE)
                .onGloballyPositioned { coords ->
                    registry?.register(key, coords.boundsInWindow(), onClick)
                }.background(if (isPressed) highlightColor else Color.Transparent, MaterialTheme.shapes.small)
                .clickable(onClick = onClick),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            imageVector = icon,
            contentDescription = contentDescription,
            tint = tint,
            modifier = Modifier.size(OVERFLOW_ITEM_ICON_SIZE),
        )
    }
}

@Composable
private fun InlineSubMenuHeaderIcon(onBack: () -> Unit) {
    Row(
        modifier =
            Modifier
                .size(OVERFLOW_ITEM_SIZE)
                .clickable(onClick = onBack),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
            contentDescription = stringResource(R.string.back),
            tint = MaterialTheme.colorScheme.primary,
        )
    }
}

@Composable
private fun RowScope.MainMenuContent(
    isLoggedIn: Boolean,
    isModerator: Boolean,
    hasActiveStream: Boolean,
    mentionCount: Int,
    onAction: (ToolbarAction) -> Unit,
    onDismiss: () -> Unit,
    onNavigateToUpload: () -> Unit,
    onNavigateToChannel: () -> Unit,
) {
    InlineMenuIconItem(
        key = "settings",
        icon = Icons.Default.Settings,
        contentDescription = stringResource(R.string.settings),
        onClick = {
            onAction(ToolbarAction.OpenSettings)
            onDismiss()
        },
    )

    VerticalDivider(modifier = Modifier.padding(vertical = 8.dp))

    InlineMenuIconItem(
        key = "add_channel",
        icon = Icons.Default.Add,
        contentDescription = stringResource(R.string.add_channel),
        onClick = {
            onAction(ToolbarAction.AddChannel)
            onDismiss()
        },
    )
    if (isLoggedIn) {
        InlineMenuIconItem(
            key = "mentions",
            icon = if (mentionCount > 0) Icons.Default.Notifications else Icons.Outlined.Notifications,
            contentDescription = stringResource(R.string.mentions_title),
            tint = if (mentionCount > 0) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant,
            onClick = {
                onAction(ToolbarAction.OpenMentions)
                onDismiss()
            },
        )
    }

    VerticalDivider(modifier = Modifier.padding(vertical = 8.dp))

    InlineMenuIconItem(
        key = "search",
        icon = Icons.Default.Search,
        contentDescription = stringResource(R.string.input_action_search),
        onClick = {
            onAction(ToolbarAction.OpenSearch)
            onDismiss()
        },
    )
    InlineMenuIconItem(
        key = "last_message",
        icon = Icons.Default.History,
        contentDescription = stringResource(R.string.input_action_last_message),
        onClick = {
            onAction(ToolbarAction.LastMessage)
            onDismiss()
        },
    )
    if (isModerator) {
        InlineMenuIconItem(
            key = "mod_actions",
            icon = Icons.Outlined.Shield,
            contentDescription = stringResource(R.string.menu_mod_actions),
            onClick = {
                onAction(ToolbarAction.OpenModActions)
                onDismiss()
            },
        )
    }
    InlineMenuIconItem(
        key = "toggle_stream",
        icon = if (hasActiveStream) Icons.Filled.Videocam else Icons.Outlined.Videocam,
        contentDescription = stringResource(R.string.toggle_stream),
        tint = if (hasActiveStream) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
        onClick = {
            onAction(ToolbarAction.ToggleStream)
            onDismiss()
        },
    )

    VerticalDivider(modifier = Modifier.padding(vertical = 8.dp))

    if (!isLoggedIn) {
        InlineMenuIconItem(
            key = "login",
            icon = Icons.AutoMirrored.Filled.Login,
            contentDescription = stringResource(R.string.login),
            onClick = {
                onAction(ToolbarAction.Login)
                onDismiss()
            },
        )
    } else {
        InlineMenuIconItem(
            key = "relogin",
            icon = Icons.Default.Refresh,
            contentDescription = stringResource(R.string.relogin),
            onClick = {
                onAction(ToolbarAction.Relogin)
                onDismiss()
            },
        )
        InlineMenuIconItem(
            key = "logout",
            icon = Icons.AutoMirrored.Filled.Logout,
            contentDescription = stringResource(R.string.logout),
            onClick = {
                onAction(ToolbarAction.Logout)
                onDismiss()
            },
        )
    }

    VerticalDivider(modifier = Modifier.padding(vertical = 8.dp))

    InlineMenuIconItem(
        key = "manage_channels",
        icon = Icons.Default.EditNote,
        contentDescription = stringResource(R.string.manage_channels),
        onClick = {
            onAction(ToolbarAction.ManageChannels)
            onDismiss()
        },
    )
    InlineMenuIconItem(
        key = "remove_channel",
        icon = Icons.Default.RemoveCircleOutline,
        contentDescription = stringResource(R.string.remove_channel),
        onClick = {
            onAction(ToolbarAction.RemoveChannel)
            onDismiss()
        },
    )
    InlineMenuIconItem(
        key = "reload_emotes",
        icon = Icons.Default.EmojiEmotions,
        contentDescription = stringResource(R.string.reload_emotes),
        onClick = {
            onAction(ToolbarAction.ReloadEmotes)
            onDismiss()
        },
    )
    InlineMenuIconItem(
        key = "reconnect",
        icon = Icons.Default.Autorenew,
        contentDescription = stringResource(R.string.reconnect),
        onClick = {
            onAction(ToolbarAction.Reconnect)
            onDismiss()
        },
    )

    VerticalDivider(modifier = Modifier.padding(vertical = 8.dp))

    InlineMenuIconItem(
        key = "upload_media",
        icon = Icons.Default.CloudUpload,
        contentDescription = stringResource(R.string.upload_media),
        onClick = onNavigateToUpload,
    )
    InlineMenuIconItem(
        key = "channel_info",
        icon = Icons.Default.Info,
        contentDescription = stringResource(R.string.channel),
        onClick = onNavigateToChannel,
    )
}

@Composable
private fun RowScope.UploadMenuContent(
    onAction: (ToolbarAction) -> Unit,
    onDismiss: () -> Unit,
    onBack: () -> Unit,
) {
    InlineSubMenuHeaderIcon(onBack = onBack)
    VerticalDivider(modifier = Modifier.padding(vertical = 8.dp))
    InlineMenuIconItem(
        key = "take_picture",
        icon = Icons.Default.CameraAlt,
        contentDescription = stringResource(R.string.take_picture),
        onClick = {
            onAction(ToolbarAction.CaptureImage)
            onDismiss()
        },
    )
    InlineMenuIconItem(
        key = "record_video",
        icon = Icons.Default.Videocam,
        contentDescription = stringResource(R.string.record_video),
        onClick = {
            onAction(ToolbarAction.CaptureVideo)
            onDismiss()
        },
    )
    InlineMenuIconItem(
        key = "choose_media",
        icon = Icons.Default.Image,
        contentDescription = stringResource(R.string.choose_media),
        onClick = {
            onAction(ToolbarAction.ChooseMedia)
            onDismiss()
        },
    )
}

@Composable
private fun RowScope.ChannelMenuContent(
    isLoggedIn: Boolean,
    onAction: (ToolbarAction) -> Unit,
    onDismiss: () -> Unit,
    onBack: () -> Unit,
) {
    InlineSubMenuHeaderIcon(onBack = onBack)
    VerticalDivider(modifier = Modifier.padding(vertical = 8.dp))
    InlineMenuIconItem(
        key = "open_channel",
        icon = Icons.Default.OpenInBrowser,
        contentDescription = stringResource(R.string.open_channel),
        onClick = {
            onAction(ToolbarAction.OpenChannel)
            onDismiss()
        },
    )
    InlineMenuIconItem(
        key = "report_channel",
        icon = Icons.Default.Flag,
        contentDescription = stringResource(R.string.report_channel),
        onClick = {
            onAction(ToolbarAction.ReportChannel)
            onDismiss()
        },
    )
    if (isLoggedIn) {
        InlineMenuIconItem(
            key = "block_channel",
            icon = Icons.Default.Block,
            contentDescription = stringResource(R.string.block_channel),
            onClick = {
                onAction(ToolbarAction.BlockChannel)
                onDismiss()
            },
        )
    }
}
