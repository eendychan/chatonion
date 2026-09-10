package com.flxrs.dankchat.ui.main.input

import android.view.inputmethod.InputMethodManager
import androidx.activity.compose.PredictiveBackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.expandHorizontally
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkHorizontally
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.basicMarquee
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.text.input.TextFieldLineLimits
import androidx.compose.foundation.text.input.TextFieldState
import androidx.compose.foundation.text.input.clearText
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.AddComment
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Fullscreen
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.outlined.EmojiEmotions
import androidx.compose.material.icons.outlined.Keyboard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldColors
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.isShiftPressed
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.layout
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.min
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.flxrs.dankchat.R
import com.flxrs.dankchat.preferences.DankChatPreferenceStore
import com.flxrs.dankchat.preferences.appearance.InputAction
import com.flxrs.dankchat.ui.main.InputState
import com.flxrs.dankchat.ui.main.QuickActionsMenu
import com.flxrs.dankchat.utils.compose.predictiveBackScale
import com.flxrs.dankchat.utils.resolve
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.StateFlow

@Composable
fun ChatInputLayout(
    textFieldState: TextFieldState,
    uiState: ChatInputUiState,
    characterCounter: StateFlow<CharacterCounterState>,
    callbacks: ChatInputCallbacks,
    isSheetOpen: Boolean,
    isUploading: Boolean,
    isLoading: Boolean,
    isFullscreen: Boolean,
    isStreamActive: Boolean,
    isAudioOnly: Boolean,
    modifier: Modifier = Modifier,
    isTheaterMode: Boolean = false,
    debugMode: Boolean = false,
    overflowExpanded: Boolean = false,
    onOverflowExpandedChange: (Boolean) -> Unit = {},
    recentMessagesExpanded: Boolean = false,
    onRecentMessagesExpandedChange: (Boolean) -> Unit = {},
    tourState: TourOverlayState = TourOverlayState(),
    isRepeatedSendEnabled: Boolean = false,
    overflowMenuMaxHeightDp: Dp = Dp.Unspecified,
) {
    val inputState = uiState.inputState
    val enabled = uiState.enabled
    val canSend = uiState.canSend
    val isEmoteMenuOpen = uiState.isEmoteMenuOpen
    val helperText = if (isSheetOpen) HelperText() else uiState.helperText
    val overlay = uiState.overlay
    val showQuickActions = !isSheetOpen
    val onSend = {
        callbacks.onSend()
        onRecentMessagesExpandedChange(false)
    }
    val onLastMessageClick = callbacks.onLastMessageClick
    val onEmoteClick = callbacks.onEmoteClick
    val onOverlayDismiss = callbacks.onOverlayDismiss
    val onToggleFullscreen = callbacks.onToggleFullscreen
    val onToggleTheater = callbacks.onToggleTheater
    val onToggleInput = callbacks.onToggleInput
    val onToggleStream = callbacks.onToggleStream
    val onModActions = callbacks.onModActions
    val onSearchClick = callbacks.onSearchClick
    val onDebugInfoClick = callbacks.onDebugInfoClick
    val onNewWhisper = callbacks.onNewWhisper
    val onRepeatedSendChange = callbacks.onRepeatedSendChange

    val focusRequester = remember { FocusRequester() }
    val hint =
        when (inputState) {
            InputState.Default -> stringResource(R.string.hint_connected)
            InputState.Replying -> stringResource(R.string.hint_replying)
            InputState.Announcing -> stringResource(R.string.hint_announcing)
            InputState.Whispering -> stringResource(R.string.hint_whispering)
            InputState.NotLoggedIn -> stringResource(R.string.hint_not_logged_int)
            InputState.Disconnected -> stringResource(R.string.hint_disconnected)
        }

    val textFieldColors =
        TextFieldDefaults.colors(
            focusedContainerColor = Color.Transparent,
            unfocusedContainerColor = Color.Transparent,
            disabledContainerColor = Color.Transparent,
            focusedIndicatorColor = Color.Transparent,
            unfocusedIndicatorColor = Color.Transparent,
            disabledIndicatorColor = Color.Transparent,
            errorIndicatorColor = Color.Transparent,
        )
    val defaultColors = TextFieldDefaults.colors()
    val surfaceColor =
        if (enabled) {
            defaultColors.unfocusedContainerColor
        } else {
            defaultColors.disabledContainerColor
        }

    val view = LocalView.current
    val inputMethodManager = remember(view) { view.context.getSystemService(InputMethodManager::class.java) }
    val keyboardController = LocalSoftwareKeyboardController.current
    val quickActionsExpanded = overflowExpanded || tourState.forceOverflowOpen
    val topEndRadius by animateDpAsState(
        targetValue = if (quickActionsExpanded || recentMessagesExpanded) 0.dp else 24.dp,
        label = "topEndCornerRadius",
    )

    val inputContent: @Composable () -> Unit = {
        Surface(
            shape = RoundedCornerShape(topStart = 24.dp, topEnd = topEndRadius),
            color = surfaceColor,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Column(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .navigationBarsPadding(),
            ) {
                // Input mode overlay header
                AnimatedVisibility(
                    visible = overlay != InputOverlay.None,
                    enter = expandVertically() + fadeIn(),
                    exit = shrinkVertically() + fadeOut(),
                ) {
                    val headerText =
                        when (overlay) {
                            is InputOverlay.Reply -> stringResource(R.string.reply_header, overlay.name.value)
                            is InputOverlay.Whisper -> stringResource(R.string.whisper_header, overlay.target.value)
                            is InputOverlay.Announce -> stringResource(R.string.mod_actions_announce_header)
                            InputOverlay.None -> ""
                        }
                    val subtitleText = (overlay as? InputOverlay.Reply)?.message
                    InputOverlayHeader(
                        text = headerText,
                        subtitle = subtitleText,
                        onDismiss = onOverlayDismiss,
                    )
                }

                val density = LocalDensity.current
                var singleLineHeight by remember { mutableIntStateOf(0) }
                val textFieldEnabled = enabled && !tourState.isTourActive
                val onKeyboardSend: (() -> Unit) -> Unit = {
                    if (canSend) {
                        onSend()
                        inputMethodManager?.restartInput(view)
                    }
                }

                val sizeTrackingModifier =
                    Modifier
                        .defaultMinSize(minHeight = with(density) { singleLineHeight.toDp() })
                        .onSizeChanged { size ->
                            if (textFieldState.text.isEmpty()) {
                                singleLineHeight = maxOf(singleLineHeight, size.height)
                            }
                            callbacks.onInputMultilineChanged(singleLineHeight > 0 && size.height > singleLineHeight)
                        }

                val chatTextField: @Composable (Modifier, PaddingValues?) -> Unit = { textFieldModifier, contentPadding ->
                    ChatTextField(
                        textFieldState = textFieldState,
                        enabled = textFieldEnabled,
                        hint = hint,
                        characterCounter = characterCounter,
                        showClearInputButton = enabled && uiState.showClearInputButton,
                        focusRequester = focusRequester,
                        textFieldColors = textFieldColors,
                        onKeyboardAction = onKeyboardSend,
                        contentPadding = contentPadding,
                        modifier = textFieldModifier.then(sizeTrackingModifier),
                    )
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(start = 8.dp, end = 4.dp),
                ) {
                    chatTextField(Modifier.weight(1f), TextFieldDefaults.contentPaddingWithoutLabel(end = 8.dp))
                    if (onNewWhisper != null) {
                        IconButton(
                            onClick = onNewWhisper,
                        ) {
                            Icon(
                                imageVector = Icons.Default.AddComment,
                                contentDescription = stringResource(R.string.whisper_new),
                            )
                        }
                    }
                    EmoteKeyboardButton(
                        isEmoteMenuOpen = isEmoteMenuOpen,
                        enabled = textFieldEnabled,
                        focusRequester = focusRequester,
                        onEmoteClick = onEmoteClick,
                    )
                    if (showQuickActions) {
                        OverflowButton(
                            quickActionsExpanded = quickActionsExpanded,
                            tourState = tourState,
                            onOverflowExpandedChange = onOverflowExpandedChange,
                        )
                    }
                    if (uiState.showSendButton) {
                        SendButton(
                            enabled = canSend,
                            isRepeatedSendEnabled = isRepeatedSendEnabled,
                            onSend = {
                                onSend()
                                inputMethodManager?.restartInput(view)
                            },
                            onRepeatedSendChange = onRepeatedSendChange,
                            modifier = Modifier.size(44.dp),
                        )
                    }
                }

                HelperTextRow(helperText = helperText)

                // Progress indicator for uploads and data loading
                AnimatedVisibility(
                    visible = isUploading || isLoading,
                    enter = expandVertically() + fadeIn(),
                    exit = shrinkVertically() + fadeOut(),
                ) {
                    LinearProgressIndicator(
                        modifier =
                            Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 4.dp),
                    )
                }
            }
        }
    }

    BoxWithConstraints(modifier = modifier.fillMaxWidth()) {
        OptionalTourTooltip(
            tooltipState = tourState.swipeGestureTooltipState,
            text = stringResource(R.string.tour_swipe_gesture),
            onAdvance = tourState.onAdvance,
            onSkip = tourState.onSkip,
        ) {
            inputContent()
        }

        // Recent messages popup — overlays above input, end-aligned
        LaunchedEffect(uiState.recentMessages) {
            if (recentMessagesExpanded && uiState.recentMessages.isEmpty()) {
                onRecentMessagesExpandedChange(false)
            }
        }
        val recentMessagesMaxWidth = min(this.maxWidth / 2, 320.dp)
        AnimatedVisibility(
            visible = recentMessagesExpanded && uiState.recentMessages.isNotEmpty(),
            enter = expandVertically(expandFrom = Alignment.Bottom) + fadeIn(),
            exit = shrinkVertically(shrinkTowards = Alignment.Bottom) + fadeOut(),
            modifier =
                Modifier
                    .align(Alignment.TopEnd)
                    .layout { measurable, constraints ->
                        val placeable = measurable.measure(constraints)
                        layout(placeable.width, 0) {
                            placeable.placeRelative(0, -placeable.height)
                        }
                    },
        ) {
            var backProgress by remember { mutableFloatStateOf(0f) }
            PredictiveBackHandler { progress ->
                try {
                    progress.collect { event ->
                        backProgress = event.progress
                    }
                    onRecentMessagesExpandedChange(false)
                } catch (_: CancellationException) {
                    backProgress = 0f
                }
            }
            RecentMessagesPopup(
                messages = uiState.recentMessages,
                surfaceColor = surfaceColor,
                onMessageClick = { message ->
                    callbacks.onRecentMessageClick(message)
                    onRecentMessagesExpandedChange(false)
                },
                modifier =
                    Modifier
                        .predictiveBackScale(backProgress)
                        .widthIn(max = recentMessagesMaxWidth)
                        .heightIn(max = overflowMenuMaxHeightDp),
            )
        }

        // Overflow menu — overlays above input, end-aligned
        AnimatedVisibility(
            visible = quickActionsExpanded,
            enter = expandVertically(expandFrom = Alignment.Bottom) + fadeIn(),
            exit = shrinkVertically(shrinkTowards = Alignment.Bottom) + fadeOut(),
            modifier =
                Modifier
                    .align(Alignment.TopEnd)
                    .layout { measurable, constraints ->
                        val placeable = measurable.measure(constraints)
                        layout(placeable.width, 0) {
                            placeable.placeRelative(0, -placeable.height)
                        }
                    },
        ) {
            var backProgress by remember { mutableFloatStateOf(0f) }
            PredictiveBackHandler { progress ->
                try {
                    progress.collect { event ->
                        backProgress = event.progress
                    }
                    onOverflowExpandedChange(false)
                } catch (_: CancellationException) {
                    backProgress = 0f
                }
            }
            QuickActionsMenu(
                modifier = Modifier
                    .predictiveBackScale(backProgress)
                    .heightIn(max = overflowMenuMaxHeightDp),
                surfaceColor = surfaceColor,
                enabled = enabled,
                isStreamActive = isStreamActive,
                isAudioOnly = isAudioOnly,
                isFullscreen = isFullscreen,
                isTheaterMode = isTheaterMode,
                debugMode = debugMode,
                onActionClick = { action ->
                    when (action) {
                        InputAction.Search -> onSearchClick()
                        InputAction.LastMessage -> onLastMessageClick()
                        InputAction.Stream -> onToggleStream()
                        InputAction.ModActions -> onModActions()
                        InputAction.Fullscreen -> onToggleFullscreen()
                        InputAction.Theater -> onToggleTheater()
                        InputAction.HideInput -> onToggleInput()
                        InputAction.Debug -> onDebugInfoClick()
                    }
                    onOverflowExpandedChange(false)
                },
                onAudioOnly = {
                    callbacks.onAudioOnly()
                    onOverflowExpandedChange(false)
                },
                onUploadClick = {
                    callbacks.onChooseMedia()
                    onOverflowExpandedChange(false)
                },
            )
        }
    }
}

@Composable
private fun SendButton(
    enabled: Boolean,
    isRepeatedSendEnabled: Boolean,
    onSend: () -> Unit,
    onRepeatedSendChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
) {
    val contentColor =
        when {
            !enabled -> MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.38f)
            else -> MaterialTheme.colorScheme.primary
        }

    val gestureModifier =
        when {
            enabled && isRepeatedSendEnabled -> {
                Modifier.pointerInput(Unit) {
                    detectTapGestures(
                        onTap = { onSend() },
                        onLongPress = { onRepeatedSendChange(true) },
                        onPress = {
                            tryAwaitRelease()
                            onRepeatedSendChange(false)
                        },
                    )
                }
            }

            enabled -> {
                Modifier.clickable(
                    interactionSource = null,
                    indication = null,
                    onClick = onSend,
                )
            }

            else -> {
                Modifier
            }
        }

    Box(
        contentAlignment = Alignment.Center,
        modifier =
            modifier
                .then(gestureModifier)
                .padding(4.dp),
    ) {
        Icon(
            imageVector = Icons.AutoMirrored.Filled.Send,
            contentDescription = stringResource(R.string.send_hint),
            modifier = Modifier.size(28.dp),
            tint = contentColor,
        )
    }
}

@Composable
private fun InputOverlayHeader(
    text: String,
    onDismiss: () -> Unit,
    subtitle: String? = null,
) {
    Column {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
            modifier =
                Modifier
                    .fillMaxWidth()
                    .padding(start = 16.dp, end = 8.dp, top = 8.dp, bottom = 4.dp),
        ) {
            Text(
                text = text,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.Bold,
            )
            IconButton(
                onClick = onDismiss,
            ) {
                Icon(
                    imageVector = Icons.Default.Close,
                    contentDescription = stringResource(R.string.dialog_dismiss),
                    modifier = Modifier.size(16.dp),
                )
            }
        }
        if (!subtitle.isNullOrBlank()) {
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.padding(start = 16.dp, end = 8.dp, bottom = 4.dp),
            )
        }
        HorizontalDivider(
            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.2f),
            modifier = Modifier.padding(horizontal = 16.dp),
        )
    }
}

@Composable
private fun ChatTextField(
    textFieldState: TextFieldState,
    enabled: Boolean,
    hint: String,
    characterCounter: StateFlow<CharacterCounterState>,
    showClearInputButton: Boolean,
    focusRequester: FocusRequester,
    textFieldColors: TextFieldColors,
    onKeyboardAction: (() -> Unit) -> Unit,
    modifier: Modifier = Modifier,
    contentPadding: PaddingValues? = null,
) {
    TextField(
        state = textFieldState,
        enabled = enabled,
        modifier =
            modifier
                .focusRequester(focusRequester)
                .onPreviewKeyEvent { event ->
                    val isEnter = event.key == Key.Enter || event.key == Key.NumPadEnter
                    when {
                        isEnter && !event.isShiftPressed -> {
                            if (event.type == KeyEventType.KeyUp) {
                                onKeyboardAction {}
                            }
                            true
                        }

                        else -> false
                    }
                },
        contentPadding = contentPadding ?: TextFieldDefaults.contentPaddingWithLabel(),
        label = { Text(hint) },
        suffix = {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier =
                    Modifier
                        .height(IntrinsicSize.Min)
                        .offset(y = (-8).dp),
            ) {
                // Collected here so per-keystroke counter updates only invalidate the suffix
                val counterState by characterCounter.collectAsStateWithLifecycle()
                when (val counter = counterState) {
                    is CharacterCounterState.Hidden -> Unit

                    is CharacterCounterState.Visible -> {
                        Text(
                            text = counter.text,
                            color =
                                when {
                                    counter.isOverLimit -> MaterialTheme.colorScheme.error
                                    else -> MaterialTheme.colorScheme.onSurfaceVariant
                                },
                            style = MaterialTheme.typography.labelSmall,
                        )
                    }
                }
                AnimatedVisibility(
                    visible = showClearInputButton && textFieldState.text.isNotEmpty(),
                    enter = fadeIn() + expandHorizontally(expandFrom = Alignment.Start),
                    exit = fadeOut() + shrinkHorizontally(shrinkTowards = Alignment.Start),
                ) {
                    Icon(
                        imageVector = Icons.Default.Clear,
                        contentDescription = stringResource(R.string.dialog_dismiss),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier =
                            Modifier
                                .padding(start = 4.dp)
                                .size(20.dp)
                                .clickable { textFieldState.clearText() },
                    )
                }
            }
        },
        colors = textFieldColors,
        shape = RoundedCornerShape(0.dp),
        lineLimits =
            TextFieldLineLimits.MultiLine(
                minHeightInLines = 1,
                maxHeightInLines = 5,
            ),
        keyboardOptions =
            KeyboardOptions(
                capitalization = KeyboardCapitalization.Sentences,
                imeAction = ImeAction.Send,
            ),
        onKeyboardAction = onKeyboardAction,
    )
}

@Composable
private fun EmoteKeyboardButton(
    isEmoteMenuOpen: Boolean,
    enabled: Boolean,
    focusRequester: FocusRequester,
    onEmoteClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    IconButton(
        onClick = {
            if (isEmoteMenuOpen) {
                focusRequester.requestFocus()
            }
            onEmoteClick()
        },
        enabled = enabled,
        modifier = modifier,
    ) {
        Icon(
            imageVector = if (isEmoteMenuOpen) Icons.Outlined.Keyboard else Icons.Outlined.EmojiEmotions,
            contentDescription =
                stringResource(
                    if (isEmoteMenuOpen) R.string.dialog_dismiss else R.string.emote_menu_hint,
                ),
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun OverflowButton(
    quickActionsExpanded: Boolean,
    tourState: TourOverlayState,
    onOverflowExpandedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
) {
    OptionalTourTooltip(
        tooltipState = tourState.overflowMenuTooltipState,
        text = stringResource(R.string.tour_overflow_menu),
        onAdvance = tourState.onAdvance,
        onSkip = tourState.onSkip,
    ) {
        IconButton(
            onClick = {
                if (tourState.overflowMenuTooltipState != null) {
                    tourState.onAdvance?.invoke()
                } else {
                    onOverflowExpandedChange(!quickActionsExpanded)
                }
            },
            modifier = modifier,
        ) {
            Icon(
                imageVector = Icons.Default.MoreVert,
                contentDescription = stringResource(R.string.more),
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun HelperTextRow(helperText: HelperText) {
    AnimatedVisibility(
        visible = !helperText.isEmpty,
        enter = expandVertically() + fadeIn(),
        exit = shrinkVertically() + fadeOut(),
    ) {
        ExpandableHelperText(
            helperText = helperText,
            modifier =
                Modifier
                    .padding(horizontal = 16.dp)
                    .padding(bottom = 4.dp),
        )
    }
}

@Composable
internal fun ExpandableHelperText(
    helperText: HelperText,
    modifier: Modifier = Modifier,
) {
    val resolvedRoomState = helperText.roomStateParts.map { it.resolve() }
    val partSeparator = if (helperText.isCompact) " · " else ", "
    val sectionSeparator = if (helperText.isCompact) " · " else " - "
    val roomStateText = resolvedRoomState.joinToString(separator = partSeparator)
    val streamInfoText = helperText.streamInfo
    val combinedText = listOfNotNull(roomStateText.ifEmpty { null }, streamInfoText).joinToString(separator = sectionSeparator)
    val style = MaterialTheme.typography.labelSmall

    when {
        helperText.isCompact -> {
            Text(
                text = coloredHelperText(combinedText, MaterialTheme.colorScheme.error),
                style = style,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = modifier.fillMaxWidth(),
            )
        }

        else -> {
            ExpandableMarqueeHelperText(
                roomStateText = roomStateText,
                streamInfoText = streamInfoText,
                combinedText = combinedText,
                style = style,
                modifier = modifier,
            )
        }
    }
}

@Composable
private fun ExpandableMarqueeHelperText(
    roomStateText: String,
    streamInfoText: String?,
    combinedText: String,
    style: TextStyle,
    modifier: Modifier = Modifier,
) {
    val textMeasurer = rememberTextMeasurer()
    val density = LocalDensity.current
    var expanded by remember { mutableStateOf(false) }

    BoxWithConstraints(
        modifier =
            modifier
                .fillMaxWidth(),
    ) {
        val maxWidthPx = with(density) { maxWidth.roundToPx() }
        val fitsOnOneLine =
            remember(combinedText, style, maxWidthPx) {
                textMeasurer.measure(combinedText, style).size.width <= maxWidthPx
            }
        val canExpand = !fitsOnOneLine && streamInfoText != null && roomStateText.isNotEmpty()
        val showTwoLines = expanded && canExpand
        val contentModifier =
            when {
                canExpand -> Modifier.clickable { expanded = !expanded }
                else -> Modifier
            }
        Box(modifier = contentModifier.fillMaxWidth().animateContentSize()) {
            when {
                showTwoLines -> {
                    Column {
                        Text(
                            text = roomStateText,
                            style = style,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            modifier = Modifier.fillMaxWidth().basicMarquee(),
                        )
                        Text(
                            text = streamInfoText.orEmpty().let { coloredHelperText(it, MaterialTheme.colorScheme.error) },
                            style = style,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            modifier = Modifier.fillMaxWidth().basicMarquee(),
                        )
                    }
                }

                else -> {
                    Text(
                        text = coloredHelperText(combinedText, MaterialTheme.colorScheme.error),
                        style = style,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        modifier = Modifier.fillMaxWidth().basicMarquee(),
                    )
                }
            }
        }
    }
}

/** Colors the "●" live indicator (see [DankChatPreferenceStore.LIVE_DOT]) instead of showing a "Live"/"is live" label. */
private fun coloredHelperText(
    text: String,
    dotColor: Color,
): AnnotatedString {
    val dotIndex = text.indexOf(DankChatPreferenceStore.LIVE_DOT)
    return buildAnnotatedString {
        append(text)
        if (dotIndex >= 0) {
            addStyle(SpanStyle(color = dotColor), dotIndex, dotIndex + DankChatPreferenceStore.LIVE_DOT.length)
        }
    }
}
