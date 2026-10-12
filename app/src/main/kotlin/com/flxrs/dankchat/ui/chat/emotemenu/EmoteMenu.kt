package com.flxrs.dankchat.ui.chat.emotemenu

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyGridScope
import androidx.compose.foundation.lazy.grid.LazyGridState
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Backspace
import androidx.compose.material.icons.filled.Celebration
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.StarBorder
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.PrimaryTabRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil3.compose.AsyncImage
import coil3.compose.rememberAsyncImagePainter
import com.flxrs.dankchat.R
import com.flxrs.dankchat.data.twitch.emote.EmoteEffect
import com.flxrs.dankchat.data.twitch.emote.GenericEmote
import com.flxrs.dankchat.preferences.components.DankBackground
import com.flxrs.dankchat.ui.chat.emote.EffectedEmoteImage
import com.flxrs.dankchat.ui.chat.emote.EmoteInfoViewModel
import com.flxrs.dankchat.ui.chat.emote.toEmoteSheetData
import com.flxrs.dankchat.ui.main.sheet.EmoteMenuViewModel
import kotlinx.coroutines.launch
import org.koin.compose.viewmodel.koinViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EmoteMenu(
    onEmoteClick: (String, String) -> Unit,
    onBackspace: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: EmoteMenuViewModel = koinViewModel(),
    emoteInfoViewModel: EmoteInfoViewModel = koinViewModel(),
) {
    val tabItems by viewModel.emoteTabItems.collectAsStateWithLifecycle()
    val selectedTabIndex by viewModel.selectedTabIndex.collectAsStateWithLifecycle()
    val scope = rememberCoroutineScope()
    val pagerState =
        rememberPagerState(
            initialPage = selectedTabIndex,
            pageCount = { tabItems.size },
        )

    LaunchedEffect(pagerState.currentPage) {
        viewModel.selectTab(pagerState.currentPage)
    }
    val subsGridState = rememberLazyGridState()
    val subsFirstHeader =
        tabItems
            .getOrNull(EmoteMenuTab.SUBS.ordinal)
            ?.items
            ?.firstOrNull()
            ?.let { (it as? EmoteItem.Header)?.title }

    LaunchedEffect(subsFirstHeader) {
        subsGridState.scrollToItem(0)
    }

    // Matches the chat's own background, not the input panel's - this is the actual surface the
    // whole menu renders on, so setting the color only on the wrapper around this (as before)
    // did nothing: this one sits on top and covers it entirely.
    Surface(
        modifier = modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background,
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            PrimaryTabRow(
                selectedTabIndex = pagerState.currentPage,
                containerColor = MaterialTheme.colorScheme.background,
            ) {
                tabItems.forEachIndexed { index, tabItem ->
                    val selected = pagerState.currentPage == index
                    Tab(
                        selected = selected,
                        onClick = { scope.launch { pagerState.animateScrollToPage(index) } },
                        icon = { EmoteMenuTabIcon(type = tabItem.type, selected = selected) },
                    )
                }
            }

            val navBarBottom = WindowInsets.navigationBars.getBottom(LocalDensity.current)
            val navBarBottomDp = with(LocalDensity.current) { navBarBottom.toDp() }

            Box(modifier = Modifier.fillMaxSize()) {
                val sampleEmoteUrl =
                    remember(tabItems) {
                        tabItems
                            .flatMap { it.items }
                            .filterIsInstance<EmoteItem.Emote>()
                            .firstOrNull { it.emote.code == EFFECT_PREVIEW_EMOTE_CODE }
                            ?.emote
                            ?.url
                            ?: EFFECT_PREVIEW_EMOTE_FALLBACK_URL
                    }

                HorizontalPager(
                    state = pagerState,
                    modifier = Modifier.fillMaxSize(),
                    beyondViewportPageCount = 1,
                ) { page ->
                    val tab = tabItems[page]
                    val onEmoteLongClick: (GenericEmote) -> Unit = { emote -> emoteInfoViewModel.show(listOf(emote.toEmoteSheetData())) }
                    when (tab.type) {
                        EmoteMenuTab.EFFECTS -> {
                            EffectsPage(
                                sampleEmoteUrl = sampleEmoteUrl,
                                navBarBottomDp = navBarBottomDp,
                                onEmoteClick = onEmoteClick,
                            )
                        }

                        EmoteMenuTab.GIF -> {
                            GifPage()
                        }

                        else -> {
                            EmoteGridPage(
                                tab = tab,
                                subsGridState = subsGridState,
                                navBarBottomDp = navBarBottomDp,
                                onEmoteClick = onEmoteClick,
                                onEmoteLongClick = onEmoteLongClick,
                            )
                        }
                    }
                }

                // Floating backspace button at bottom-end, matching keyboard position
                IconButton(
                    onClick = onBackspace,
                    colors =
                        IconButtonDefaults.iconButtonColors(
                            containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                        ),
                    modifier =
                        Modifier
                            .align(Alignment.BottomEnd)
                            .padding(end = 8.dp, bottom = 8.dp + navBarBottomDp)
                            .size(48.dp),
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.Backspace,
                        contentDescription = stringResource(R.string.backspace),
                        tint = MaterialTheme.colorScheme.onSurface,
                    )
                }
            }
        }
    }
}

// RECENT is a plain icon, matching the existing "message history" icon used elsewhere in the
// app. SUBS/CHANNEL/GLOBAL sit inside a small rounded-square "chip" that's outlined and empty
// when unselected, and fills in solid once selected - the star tab additionally swaps to
// Material's own filled star glyph for that same selected state, since one exists ready-made.
@Composable
private fun EmoteMenuTabIcon(
    type: EmoteMenuTab,
    selected: Boolean,
) {
    if (type == EmoteMenuTab.RECENT) {
        Icon(
            imageVector = Icons.Default.History,
            contentDescription = stringResource(R.string.emote_menu_tab_recent),
        )
        return
    }

    val containerColor = if (selected) MaterialTheme.colorScheme.primary else Color.Transparent
    val borderColor = if (selected) Color.Transparent else MaterialTheme.colorScheme.outline
    val contentColor = if (selected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant

    Box(
        modifier =
            Modifier
                .size(28.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(containerColor)
                .border(width = 1.5.dp, color = borderColor, shape = RoundedCornerShape(8.dp)),
        contentAlignment = Alignment.Center,
    ) {
        when (type) {
            EmoteMenuTab.SUBS -> {
                Icon(
                    imageVector = if (selected) Icons.Default.Star else Icons.Default.StarBorder,
                    contentDescription = stringResource(R.string.emote_menu_tab_subs),
                    tint = contentColor,
                    modifier = Modifier.size(18.dp),
                )
            }

            EmoteMenuTab.CHANNEL -> {
                Text(
                    text = "7TV",
                    color = contentColor,
                    fontSize = 9.sp,
                    fontWeight = FontWeight.Bold,
                )
            }

            EmoteMenuTab.GLOBAL -> {
                Icon(
                    painter = painterResource(R.drawable.ic_twitch),
                    contentDescription = stringResource(R.string.emote_menu_tab_global),
                    tint = contentColor,
                    modifier = Modifier.size(16.dp),
                )
            }

            EmoteMenuTab.EFFECTS -> {
                Icon(
                    imageVector = Icons.Default.Celebration,
                    contentDescription = stringResource(R.string.emote_menu_tab_effects),
                    tint = contentColor,
                    modifier = Modifier.size(18.dp),
                )
            }

            EmoteMenuTab.GIF -> {
                GifPaperIcon(color = contentColor)
            }

            EmoteMenuTab.RECENT -> Unit
        }
    }
}

// A small sheet of paper with one folded corner and "GIF" written on it.
@Composable
private fun GifPaperIcon(color: Color) {
    Box(
        modifier =
            Modifier
                .size(width = 17.dp, height = 19.dp)
                .drawBehind {
                    val fold = 5.dp.toPx()
                    val page =
                        Path().apply {
                            moveTo(0f, 0f)
                            lineTo(size.width - fold, 0f)
                            lineTo(size.width, fold)
                            lineTo(size.width, size.height)
                            lineTo(0f, size.height)
                            close()
                        }
                    drawPath(path = page, color = color, style = Stroke(width = 1.5.dp.toPx()))
                    val foldedCorner =
                        Path().apply {
                            moveTo(size.width - fold, 0f)
                            lineTo(size.width - fold, fold)
                            lineTo(size.width, fold)
                            close()
                        }
                    drawPath(path = foldedCorner, color = color)
                },
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = "GIF",
            color = color,
            fontSize = 6.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(top = 3.dp),
        )
    }
}

private val BTTV_EFFECTS = listOf("w!", "h!", "v!", "z!", "c!", "l!", "r!", "p!", "s!")
private val FFZ_EFFECTS = listOf("ffzW", "ffzX", "ffzY", "ffzCursed")

// DinoDance from Twitch's own emote CDN - animated and colorful, so every effect (hue rotation,
// cursed contrast, shake) is actually visible on it. Used when it isn't among the loaded emotes.
private const val EFFECT_PREVIEW_EMOTE_CODE = "DinoDance"
private const val EFFECT_PREVIEW_EMOTE_FALLBACK_URL = "https://static-cdn.jtvnw.net/emoticons/v2/115234/default/dark/2.0"

@Composable
private fun EffectsPage(
    sampleEmoteUrl: String,
    navBarBottomDp: Dp,
    onEmoteClick: (code: String, id: String) -> Unit,
) {
    LazyVerticalGrid(
        columns = GridCells.Adaptive(minSize = 80.dp),
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 8.dp, top = 8.dp, end = 8.dp, bottom = 56.dp + navBarBottomDp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        effectSection(title = "BTTV", codes = BTTV_EFFECTS, sampleEmoteUrl = sampleEmoteUrl, onEmoteClick = onEmoteClick)
        effectSection(title = "FFZ", codes = FFZ_EFFECTS, sampleEmoteUrl = sampleEmoteUrl, onEmoteClick = onEmoteClick)
    }
}

// Effects are modifiers typed in front of an emote (w! Kappa), so they have no image of their own:
// each tile previews the effect on a sample emote, and inserts its code when tapped.
private fun LazyGridScope.effectSection(
    title: String,
    codes: List<String>,
    sampleEmoteUrl: String,
    onEmoteClick: (code: String, id: String) -> Unit,
) {
    item(key = "effects-header-$title", span = { GridItemSpan(maxLineSpan) }) {
        Text(
            text = title,
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(start = 4.dp, top = 4.dp),
        )
    }
    items(count = codes.size, key = { "effect-${codes[it]}" }) { index ->
        val code = codes[index]
        val effects = remember(code) { EmoteEffect.fromPrefix(code)?.let { setOf(it) }.orEmpty() }
        Box(modifier = Modifier.fillMaxWidth().height(44.dp), contentAlignment = Alignment.Center) {
            EffectedEmoteImage(
                painter = rememberAsyncImagePainter(sampleEmoteUrl),
                width = 32.dp,
                height = 32.dp,
                effects = effects,
                animate = true,
                alpha = 1f,
                onClick = { onEmoteClick(code, "") },
            )
        }
    }
}

@Composable
private fun GifPage() {
    Box(modifier = Modifier.fillMaxSize().padding(24.dp), contentAlignment = Alignment.Center) {
        Text(
            text = stringResource(R.string.emote_menu_gif_tier_reminder),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
    }
}

@Composable
private fun EmoteGridPage(
    tab: EmoteMenuTabItem,
    subsGridState: LazyGridState,
    navBarBottomDp: Dp,
    onEmoteClick: (code: String, id: String) -> Unit,
    onEmoteLongClick: (GenericEmote) -> Unit,
) {
    val items = tab.items

    if (tab.type == EmoteMenuTab.RECENT && items.isEmpty()) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            DankBackground(visible = true)
            Text(
                text = stringResource(R.string.no_recent_emotes),
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 160.dp),
            )
        }
    } else {
        val gridState =
            when (tab.type) {
                EmoteMenuTab.SUBS -> subsGridState
                else -> rememberLazyGridState()
            }
        LazyVerticalGrid(
            columns = GridCells.Adaptive(minSize = 40.dp),
            state = gridState,
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(start = 8.dp, top = 8.dp, end = 8.dp, bottom = 56.dp + navBarBottomDp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            items(
                count = items.size,
                // Sections are unique by title and emote ids are deduplicated per section, so keys
                // stay stable across list shifts and items can be reused
                key = { index ->
                    when (val item = items[index]) {
                        is EmoteItem.Emote -> "emote-${item.emote.emoteType.title}-${item.emote.id}"
                        is EmoteItem.Header -> "header-${item.title}"
                    }
                },
                span = { index ->
                    when (items[index]) {
                        is EmoteItem.Header -> GridItemSpan(maxLineSpan)
                        is EmoteItem.Emote -> GridItemSpan(1)
                    }
                },
                contentType = { index ->
                    when (items[index]) {
                        is EmoteItem.Header -> "header"
                        is EmoteItem.Emote -> "emote"
                    }
                },
            ) { index ->
                val item = items[index]
                when (item) {
                    is EmoteItem.Header -> {
                        Text(
                            text = item.title,
                            style = MaterialTheme.typography.titleMedium,
                            modifier =
                                Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 8.dp),
                        )
                    }

                    is EmoteItem.Emote -> {
                        AsyncImage(
                            model = item.emote.url,
                            contentDescription = item.emote.code,
                            modifier =
                                Modifier
                                    .fillMaxWidth()
                                    .aspectRatio(1f)
                                    .pointerInput(item.emote) {
                                        detectTapGestures(
                                            onTap = { onEmoteClick(item.emote.code, item.emote.id) },
                                            onLongPress = { onEmoteLongClick(item.emote) },
                                        )
                                    },
                        )
                    }
                }
            }
        }
    }
}
