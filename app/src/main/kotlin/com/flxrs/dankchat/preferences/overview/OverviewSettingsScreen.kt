package com.flxrs.dankchat.preferences.overview

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.exclude
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ExitToApp
import androidx.compose.material.icons.filled.BatterySaver
import androidx.compose.material.icons.filled.Construction
import androidx.compose.material.icons.filled.DeveloperMode
import androidx.compose.material.icons.filled.FiberNew
import androidx.compose.material.icons.filled.Forum
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.Paid
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.ScaffoldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.withLink
import androidx.compose.ui.tooling.preview.PreviewDynamicColors
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.dp
import com.flxrs.dankchat.R
import com.flxrs.dankchat.preferences.components.NavigationBarSpacer
import com.flxrs.dankchat.preferences.components.PreferenceCategoryTitle
import com.flxrs.dankchat.preferences.components.PreferenceCategoryWithSummary
import com.flxrs.dankchat.preferences.components.PreferenceItem
import com.flxrs.dankchat.preferences.components.PreferenceSummary
import com.flxrs.dankchat.ui.theme.DankChatTheme
import com.flxrs.dankchat.utils.compose.buildClickableAnnotation
import com.flxrs.dankchat.utils.compose.buildLinkAnnotation

private const val CHATONION_TELEGRAM_URL = "https://t.me/echpzdzh"
private const val CHATONION_GITHUB_URL = "https://github.com/eendychan/chatonion"
private const val DANKCHAT_GITHUB_URL = "https://github.com/flex3r/DankChat"
private const val DANKCHAT_BADGE_URL = "https://streamelements.com/flex3rs/tip"
private const val TWITCH_TOS_URL = "https://legal.twitch.com/legal/terms-of-service"

sealed interface SettingsNavigation {
    data object Appearance : SettingsNavigation

    data object Notifications : SettingsNavigation

    data object Chat : SettingsNavigation

    data object Moderation : SettingsNavigation

    data object Donations : SettingsNavigation

    data object Streams : SettingsNavigation

    data object Battery : SettingsNavigation

    data object Tools : SettingsNavigation

    data object Developer : SettingsNavigation

    data object Changelog : SettingsNavigation

    data object About : SettingsNavigation
}

@Composable
fun OverviewSettingsScreen(
    isLoggedIn: Boolean,
    hasChangelog: Boolean,
    onBack: () -> Unit,
    onLogout: () -> Unit,
    onNavigate: (SettingsNavigation) -> Unit,
    onNavigateToSearch: () -> Unit,
) {
    val scrollBehavior = TopAppBarDefaults.pinnedScrollBehavior()
    Scaffold(
        contentWindowInsets = ScaffoldDefaults.contentWindowInsets.exclude(WindowInsets.navigationBars),
        modifier =
            Modifier
                .nestedScroll(scrollBehavior.nestedScrollConnection)
                .imePadding(),
        topBar = {
            TopAppBar(
                scrollBehavior = scrollBehavior,
                title = { Text(stringResource(R.string.settings)) },
                navigationIcon = {
                    IconButton(
                        onClick = onBack,
                        content = { Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back") },
                    )
                },
                actions = {
                    IconButton(onClick = onNavigateToSearch) {
                        Icon(Icons.Default.Search, contentDescription = stringResource(R.string.settings_search_hint))
                    }
                },
            )
        },
    ) { padding ->
        Column(
            modifier =
                Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .padding(top = 16.dp, start = 16.dp, end = 16.dp)
                    .verticalScroll(rememberScrollState()),
        ) {
            PreferenceItem(
                title = stringResource(R.string.preference_appearance_header),
                icon = Icons.Default.Palette,
                onClick = { onNavigate(SettingsNavigation.Appearance) },
            )
            PreferenceItem(
                title = stringResource(R.string.preference_highlights_ignores_header),
                icon = Icons.Default.NotificationsActive,
                onClick = { onNavigate(SettingsNavigation.Notifications) },
            )
            PreferenceItem(stringResource(R.string.preference_chat_header), Icons.Default.Forum, onClick = {
                onNavigate(SettingsNavigation.Chat)
            })
            PreferenceItem(stringResource(R.string.moderation_settings_title), Icons.Default.Shield, onClick = {
                onNavigate(SettingsNavigation.Moderation)
            })
            PreferenceItem(stringResource(R.string.donation_settings_title), Icons.Default.Paid, onClick = {
                onNavigate(SettingsNavigation.Donations)
            })
            PreferenceItem(stringResource(R.string.preference_streams_header), Icons.Default.PlayArrow, onClick = {
                onNavigate(SettingsNavigation.Streams)
            })
            PreferenceItem(stringResource(R.string.preference_battery_header), Icons.Default.BatterySaver, onClick = {
                onNavigate(SettingsNavigation.Battery)
            })
            PreferenceItem(stringResource(R.string.preference_tools_header), Icons.Default.Construction, onClick = {
                onNavigate(SettingsNavigation.Tools)
            })
            PreferenceItem(stringResource(R.string.preference_developer_header), Icons.Default.DeveloperMode, onClick = {
                onNavigate(SettingsNavigation.Developer)
            })

            AnimatedVisibility(hasChangelog) {
                PreferenceItem(stringResource(R.string.preference_whats_new_header), Icons.Default.FiberNew, onClick = {
                    onNavigate(SettingsNavigation.Changelog)
                })
            }

            PreferenceItem(stringResource(R.string.logout), Icons.AutoMirrored.Default.ExitToApp, isEnabled = isLoggedIn, onClick = onLogout)
            SecretDankerModeTrigger {
                PreferenceCategoryWithSummary(
                    title = {
                        PreferenceCategoryTitle(
                            text = stringResource(R.string.preference_about_header),
                            modifier = Modifier.dankClickable(),
                        )
                    },
                ) {
                    val uriHandler = LocalUriHandler.current
                    Column(
                        modifier = Modifier.padding(top = 16.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp),
                    ) {
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            PreferenceSummary(stringResource(R.string.preference_about_chatonion_summary))
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                AssistChip(
                                    onClick = { uriHandler.openUri(CHATONION_TELEGRAM_URL) },
                                    label = { Text(stringResource(R.string.preference_about_telegram_button)) },
                                )
                                AssistChip(
                                    onClick = { uriHandler.openUri(CHATONION_GITHUB_URL) },
                                    label = { Text(stringResource(R.string.preference_about_github_button)) },
                                )
                            }
                        }
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            PreferenceSummary(stringResource(R.string.preference_about_summary))
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                AssistChip(
                                    onClick = { uriHandler.openUri(DANKCHAT_GITHUB_URL) },
                                    label = { Text(stringResource(R.string.preference_about_github_button)) },
                                )
                                AssistChip(
                                    onClick = { uriHandler.openUri(DANKCHAT_BADGE_URL) },
                                    label = { Text(stringResource(R.string.preference_about_dankchat_badge_button)) },
                                )
                            }
                        }
                        val tosText = stringResource(R.string.preference_about_tos)
                        val licenseText = stringResource(R.string.open_source_licenses)
                        val links =
                            buildAnnotatedString {
                                withLink(link = buildLinkAnnotation(TWITCH_TOS_URL)) {
                                    append(tosText)
                                }
                                appendLine()
                                withLink(link = buildClickableAnnotation(text = licenseText, onClick = { onNavigate(SettingsNavigation.About) })) {
                                    append(licenseText)
                                }
                            }
                        PreferenceSummary(links)
                    }
                }
            }
            NavigationBarSpacer()
        }
    }
}

@Suppress("UnusedPrivateFunction")
@Composable
@PreviewDynamicColors
@PreviewLightDark
private fun OverviewSettingsPreview() {
    DankChatTheme {
        OverviewSettingsScreen(
            isLoggedIn = false,
            hasChangelog = true,
            onBack = { },
            onLogout = { },
            onNavigate = { },
            onNavigateToSearch = { },
        )
    }
}
