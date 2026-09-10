package com.flxrs.dankchat.ui.main

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.expandHorizontally
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkHorizontally
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.isImeVisible
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.pager.PagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.PushPin
import androidx.compose.material.icons.outlined.PushPin
import androidx.compose.material3.Badge
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RichTooltip
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TooltipAnchorPosition
import androidx.compose.material3.TooltipBox
import androidx.compose.material3.TooltipDefaults
import androidx.compose.material3.TooltipState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import com.flxrs.dankchat.R
import com.flxrs.dankchat.data.UserName
import com.flxrs.dankchat.ui.main.channel.ChannelTabUiState
import com.flxrs.dankchat.ui.main.stream.AudioOnlyBar
import com.flxrs.dankchat.ui.theme.toolbarPillColor
import com.flxrs.dankchat.utils.compose.rememberPagerTabIndicatorState
import com.flxrs.dankchat.utils.compose.reportPosition
import kotlinx.coroutines.flow.dropWhile
import kotlinx.coroutines.flow.first

@Suppress("MultipleEmitters")
@OptIn(ExperimentalFoundationApi::class, ExperimentalMaterial3Api::class)
@Composable
fun FloatingToolbar(
    tabState: ChannelTabUiState,
    composePagerState: PagerState,
    showAppBar: Boolean,
    isFullscreen: Boolean,
    isLoggedIn: Boolean,
    currentStream: UserName?,
    isAudioOnly: Boolean,
    streamHeightDp: Dp,
    totalMentionCount: Int,
    hasActivePinnedMessage: Boolean,
    isPinnedMessageShown: Boolean,
    onAction: (ToolbarAction) -> Unit,
    onAudioOnly: () -> Unit,
    onStreamClose: () -> Unit,
    modifier: Modifier = Modifier,
    isModerator: Boolean = false,
    endAligned: Boolean = false,
    showTabs: Boolean = true,
    addChannelTooltipState: TooltipState? = null,
    onAddChannelTooltipDismiss: () -> Unit = {},
    onSkipTour: () -> Unit = {},
    onToolbarBottomChange: (Int) -> Unit = {},
    isEmoteMenuOpen: Boolean = false,
    onCloseEmoteMenu: () -> Unit = {},
    onMenuVisibleChange: (Boolean) -> Unit = {},
    streamToolbarAlpha: () -> Float = { 1f },
) {
    val density = LocalDensity.current
    var showOverflowMenu by remember { mutableStateOf(false) }
    var toolbarRowHeight by remember { mutableFloatStateOf(0f) }

    val statusBarTopPx = WindowInsets.statusBars.getTop(density)
    val toolbarBottomPx = with(density) {
        when {
            isFullscreen -> 0
            !showAppBar -> statusBarTopPx
            else -> statusBarTopPx + (8.dp.toPx() + 16.dp.toPx()).toInt() + toolbarRowHeight.toInt()
        }
    }
    LaunchedEffect(toolbarBottomPx) { onToolbarBottomChange(toolbarBottomPx) }

    val totalTabs = tabState.tabs.size
    val selectedIndex = composePagerState.currentPage
    val tabScrollState = rememberScrollState()
    rememberCoroutineScope()

    val tabLayoutState = rememberPagerTabIndicatorState(totalTabs)
    var tabViewportWidth by remember { mutableIntStateOf(0) }

    val keyboardController = LocalSoftwareKeyboardController.current

    // Reset menu when toolbar hides or keyboard opens
    LaunchedEffect(showAppBar) {
        if (!showAppBar) {
            showOverflowMenu = false
        }
    }
    val isKeyboardOpen = WindowInsets.isImeVisible
    LaunchedEffect(isKeyboardOpen) {
        if (isKeyboardOpen) {
            showOverflowMenu = false
        }
    }
    LaunchedEffect(isEmoteMenuOpen) {
        if (isEmoteMenuOpen) {
            showOverflowMenu = false
        }
    }
    LaunchedEffect(showOverflowMenu) {
        onMenuVisibleChange(showOverflowMenu)
    }

    // Dismiss scrim for the menu
    if (showOverflowMenu) {
        Box(
            modifier =
                Modifier
                    .fillMaxSize()
                    .clickable(
                        indication = null,
                        interactionSource = remember { MutableInteractionSource() },
                    ) {
                        showOverflowMenu = false
                    },
        )
    }

    val hasStream = currentStream != null && streamHeightDp > 0.dp

    AnimatedVisibility(
        visible = showAppBar && !isFullscreen,
        enter = slideInVertically(initialOffsetY = { -it }) + fadeIn(),
        exit = slideOutVertically(targetOffsetY = { -it }) + fadeOut(),
        modifier =
            modifier
                .fillMaxWidth()
                .padding(top = if (hasStream) streamHeightDp + 8.dp else 0.dp)
                .graphicsLayer { alpha = streamToolbarAlpha() },
    ) {
        val scrimColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.7f)
        val statusBarPx = statusBarTopPx.toFloat()
        val scrimModifier =
            if (hasStream) {
                Modifier.fillMaxWidth()
            } else {
                Modifier
                    .fillMaxWidth()
                    .drawBehind {
                        if (toolbarRowHeight > 0f) {
                            val gradientHeight = statusBarPx + 8.dp.toPx() + toolbarRowHeight + 16.dp.toPx()
                            drawRect(
                                brush =
                                    Brush.verticalGradient(
                                        0f to scrimColor,
                                        0.75f to scrimColor,
                                        1f to scrimColor.copy(alpha = 0f),
                                        endY = gradientHeight,
                                    ),
                                size = Size(size.width, gradientHeight),
                            )
                        }
                    }.padding(top = with(density) { WindowInsets.statusBars.getTop(density).toDp() } + 6.dp)
            }

        Column(modifier = scrimModifier) {
            if (currentStream != null && isAudioOnly) {
                AudioOnlyBar(
                    channel = currentStream,
                    onExpandVideo = onAudioOnly,
                    onClose = onStreamClose,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 6.dp, end = 6.dp, bottom = 6.dp),
                )
            }
            Box {
                // Center selected tab when selection changes
                LaunchedEffect(selectedIndex, tabLayoutState.ready, tabViewportWidth) {
                    if (!tabLayoutState.ready || selectedIndex !in tabLayoutState.offsets.indices || tabViewportWidth <= 0) {
                        return@LaunchedEffect
                    }
                    val tabOffset = tabLayoutState.offsets[selectedIndex]
                    val tabWidth = tabLayoutState.widths[selectedIndex]
                    val centeredOffset = tabOffset - (tabViewportWidth / 2 - tabWidth / 2)
                    val clampedOffset = centeredOffset.coerceIn(0, tabScrollState.maxValue)
                    if (tabScrollState.value != clampedOffset) {
                        tabScrollState.animateScrollTo(clampedOffset)
                    }
                }

                // Mention indicators based on scroll position and tab positions
                val hasLeftMention by remember(tabState.tabs) {
                    derivedStateOf {
                        val scrollPos = tabScrollState.value
                        tabState.tabs.indices.any { i ->
                            i < tabLayoutState.offsets.size &&
                                tabLayoutState.offsets[i] + tabLayoutState.widths[i] < scrollPos &&
                                tabState.tabs[i].mentionCount > 0
                        }
                    }
                }
                val hasRightMention by remember(tabState.tabs) {
                    derivedStateOf {
                        val scrollPos = tabScrollState.value
                        tabState.tabs.indices.any { i ->
                            i < tabLayoutState.offsets.size &&
                                tabLayoutState.offsets[i] > scrollPos + tabViewportWidth &&
                                tabState.tabs[i].mentionCount > 0
                        }
                    }
                }

                Row(
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 6.dp)
                            .onSizeChanged {
                                val h = it.height.toFloat()
                                if (toolbarRowHeight == 0f || h < toolbarRowHeight) toolbarRowHeight = h
                            },
                    verticalAlignment = Alignment.Top,
                ) {
                    // Push the pill to the end when this toolbar variant isn't showing tabs at all.
                    // Once the pill is shown it always fills the row via weight(1f), so an empty tab
                    // list on its own is no longer a reason to push - it still needs to host the
                    // pin/overflow buttons (and, from inside the overflow menu, "add channel").
                    if (endAligned && !showTabs) {
                        Spacer(modifier = Modifier.weight(1f))
                    }

                    // One continuous pill: channel avatars (or, once expanded, the overflow menu's
                    // icons) on the left, with the pin toggle and the overflow trigger fixed at the
                    // end so they never disappear while the menu is open.
                    AnimatedVisibility(
                        visible = showTabs,
                        modifier = Modifier.weight(1f, fill = endAligned),
                        enter = expandHorizontally(expandFrom = Alignment.Start) + fadeIn(),
                        exit = shrinkHorizontally(shrinkTowards = Alignment.Start) + fadeOut(),
                    ) {
                        val overflowMenuRegistry = remember { InlineMenuItemRegistry() }
                        val mentionGradientColor = MaterialTheme.colorScheme.error
                        Surface(
                            shape = MaterialTheme.shapes.extraLarge,
                            color = MaterialTheme.colorScheme.toolbarPillColor,
                            modifier =
                                Modifier
                                    .then(if (endAligned) Modifier.fillMaxWidth() else Modifier)
                                    .clip(MaterialTheme.shapes.extraLarge)
                                    .drawWithContent {
                                        drawContent()
                                        if (showOverflowMenu) return@drawWithContent
                                        val gradientWidth = 24.dp.toPx()
                                        if (hasLeftMention) {
                                            drawRect(
                                                brush =
                                                    Brush.horizontalGradient(
                                                        colors =
                                                            listOf(
                                                                mentionGradientColor.copy(alpha = 0.5f),
                                                                mentionGradientColor.copy(alpha = 0f),
                                                            ),
                                                        endX = gradientWidth,
                                                    ),
                                                size = Size(gradientWidth, size.height),
                                            )
                                        }
                                        if (hasRightMention) {
                                            drawRect(
                                                brush =
                                                    Brush.horizontalGradient(
                                                        colors =
                                                            listOf(
                                                                mentionGradientColor.copy(alpha = 0f),
                                                                mentionGradientColor.copy(alpha = 0.5f),
                                                            ),
                                                        startX = size.width - gradientWidth,
                                                        endX = size.width,
                                                    ),
                                                topLeft = Offset(size.width - gradientWidth, 0f),
                                                size = Size(gradientWidth, size.height),
                                            )
                                        }
                                    },
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(modifier = Modifier.weight(1f)) {
                                    Crossfade(targetState = showOverflowMenu, label = "ToolbarPillContent") { isOverflow ->
                                        if (isOverflow) {
                                            CompositionLocalProvider(LocalInlineMenuItemRegistry provides overflowMenuRegistry) {
                                                InlineOverflowMenu(
                                                    isLoggedIn = isLoggedIn,
                                                    isModerator = isModerator,
                                                    hasActiveStream = currentStream != null,
                                                    mentionCount = totalMentionCount,
                                                    onDismiss = {
                                                        showOverflowMenu = false
                                                    },
                                                    onAction = onAction,
                                                )
                                            }
                                        } else {
                                            Box(
                                                modifier =
                                                    Modifier
                                                        .padding(horizontal = 8.dp)
                                                        .onSizeChanged { tabViewportWidth = it.width }
                                                        .clipToBounds()
                                                        .horizontalScroll(tabScrollState),
                                            ) {
                                                Row(verticalAlignment = Alignment.CenterVertically) {
                                                    tabState.tabs.forEachIndexed { index, tab ->
                                                        val isSelected = index == selectedIndex
                                                        val hasActivity = tab.mentionCount > 0 || tab.hasUnread
                                                        val ringColor = when {
                                                            isSelected -> MaterialTheme.colorScheme.primary
                                                            hasActivity -> MaterialTheme.colorScheme.onSurface
                                                            else -> Color.Transparent
                                                        }
                                                        Box(
                                                            contentAlignment = Alignment.Center,
                                                            modifier =
                                                                Modifier
                                                                    .combinedClickable(
                                                                        // Let taps fall through to the dismiss scrim while a menu is open
                                                                        enabled = !showOverflowMenu,
                                                                        onClick = { onAction(ToolbarAction.SelectTab(index)) },
                                                                        onLongClick = { onAction(ToolbarAction.LongClickTab) },
                                                                    ).defaultMinSize(minHeight = 48.dp)
                                                                    .padding(horizontal = 5.dp, vertical = 6.dp)
                                                                    .reportPosition(tabLayoutState, index),
                                                        ) {
                                                            Box(
                                                                contentAlignment = Alignment.Center,
                                                                modifier =
                                                                    Modifier
                                                                        .size(34.dp)
                                                                        .clip(CircleShape)
                                                                        .background(MaterialTheme.colorScheme.surfaceContainerHighest)
                                                                        .border(
                                                                            width = if (isSelected) 2.dp else 1.dp,
                                                                            color = ringColor,
                                                                            shape = CircleShape,
                                                                        ),
                                                            ) {
                                                                if (tab.avatarUrl != null) {
                                                                    AsyncImage(
                                                                        model = tab.avatarUrl,
                                                                        contentDescription = tab.displayName,
                                                                        contentScale = ContentScale.Crop,
                                                                        modifier = Modifier
                                                                            .fillMaxSize()
                                                                            .clip(CircleShape),
                                                                    )
                                                                } else {
                                                                    Text(
                                                                        text = tab.displayName.take(1).uppercase(),
                                                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                                                        style = MaterialTheme.typography.labelLarge,
                                                                        fontWeight = FontWeight.Bold,
                                                                    )
                                                                }
                                                            }
                                                            if (tab.mentionCount > 0) {
                                                                Badge(modifier = Modifier.align(Alignment.TopEnd))
                                                            }
                                                        }
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }

                                // Pin toggle - fixed at the end, always visible regardless of whether
                                // the overflow menu is open.
                                val pinButtonWidth by animateDpAsState(
                                    targetValue = if (hasActivePinnedMessage) 48.dp else 0.dp,
                                    label = "pinButtonWidth",
                                )
                                if (pinButtonWidth > 0.dp) {
                                    Box(
                                        contentAlignment = Alignment.Center,
                                        modifier =
                                            Modifier
                                                .width(pinButtonWidth)
                                                .clipToBounds()
                                                .graphicsLayer { alpha = pinButtonWidth.value / 48f },
                                    ) {
                                        IconButton(onClick = { onAction(ToolbarAction.TogglePinnedMessage) }) {
                                            when {
                                                isPinnedMessageShown -> Icon(
                                                    imageVector = Icons.Default.PushPin,
                                                    contentDescription = stringResource(R.string.pinned_message_collapse),
                                                    tint = MaterialTheme.colorScheme.primary,
                                                )

                                                else -> Icon(
                                                    imageVector = Icons.Outlined.PushPin,
                                                    contentDescription = stringResource(R.string.pinned_message_show),
                                                )
                                            }
                                        }
                                    }
                                }

                                // Overflow trigger - fixed at the end, always visible.
                                var triggerLayoutCoords by remember { mutableStateOf<LayoutCoordinates?>(null) }
                                val overflowTrigger: @Composable () -> Unit = {
                                    Box(
                                        modifier =
                                            Modifier
                                                .size(44.dp)
                                                .onGloballyPositioned { triggerLayoutCoords = it }
                                                .pointerInput(overflowMenuRegistry) {
                                                    awaitEachGesture {
                                                        awaitFirstDown(requireUnconsumed = false)
                                                        if (showOverflowMenu) {
                                                            showOverflowMenu = false
                                                            return@awaitEachGesture
                                                        }
                                                        showOverflowMenu = true
                                                        keyboardController?.hide()
                                                        onCloseEmoteMenu()
                                                        while (true) {
                                                            val event = awaitPointerEvent()
                                                            val change = event.changes.first()
                                                            val coords = triggerLayoutCoords
                                                            val windowPos =
                                                                coords?.localToWindow(change.position) ?: change.position
                                                            overflowMenuRegistry.pressedKey = overflowMenuRegistry.keyAt(windowPos)
                                                            if (!change.pressed) {
                                                                overflowMenuRegistry.selectAt(windowPos)
                                                                overflowMenuRegistry.pressedKey = null
                                                                break
                                                            }
                                                        }
                                                    }
                                                },
                                        contentAlignment = Alignment.Center,
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.MoreVert,
                                            contentDescription = stringResource(R.string.more),
                                            tint =
                                                if (totalMentionCount > 0) {
                                                    MaterialTheme.colorScheme.error
                                                } else {
                                                    LocalContentColor.current
                                                },
                                        )
                                    }
                                }
                                if (addChannelTooltipState != null) {
                                    LaunchedEffect(Unit) {
                                        addChannelTooltipState.show()
                                    }
                                    LaunchedEffect(Unit) {
                                        snapshotFlow { addChannelTooltipState.isVisible }
                                            .dropWhile { !it } // skip initial false
                                            .first { !it } // wait for dismiss (any cause)
                                        onAddChannelTooltipDismiss()
                                    }
                                    TooltipBox(
                                        positionProvider =
                                            TooltipDefaults.rememberTooltipPositionProvider(
                                                TooltipAnchorPosition.Above,
                                                spacingBetweenTooltipAndAnchor = 8.dp,
                                            ),
                                        tooltip = {
                                            val tourColors =
                                                TooltipDefaults.richTooltipColors(
                                                    containerColor = MaterialTheme.colorScheme.secondaryContainer,
                                                    contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
                                                    titleContentColor = MaterialTheme.colorScheme.onSecondaryContainer,
                                                    actionContentColor = MaterialTheme.colorScheme.secondary,
                                                )
                                            RichTooltip(
                                                colors = tourColors,
                                                caretShape = TooltipDefaults.caretShape(caretSize = DpSize(24.dp, 12.dp)),
                                                action = {
                                                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                                        TextButton(onClick = {
                                                            addChannelTooltipState.dismiss()
                                                            onAddChannelTooltipDismiss()
                                                            onSkipTour()
                                                        }) {
                                                            Text(stringResource(R.string.tour_skip))
                                                        }
                                                        TextButton(onClick = {
                                                            addChannelTooltipState.dismiss()
                                                            onAddChannelTooltipDismiss()
                                                        }) {
                                                            Text(stringResource(R.string.tour_next))
                                                        }
                                                    }
                                                },
                                            ) {
                                                Text(stringResource(R.string.tour_add_more_channels_hint))
                                            }
                                        },
                                        state = addChannelTooltipState,
                                        hasAction = true,
                                    ) {
                                        overflowTrigger()
                                    }
                                } else {
                                    overflowTrigger()
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
