package com.flxrs.dankchat.preferences.search

import androidx.annotation.StringRes
import com.flxrs.dankchat.R
import com.flxrs.dankchat.ui.main.AboutSettings
import com.flxrs.dankchat.ui.main.AppearanceSettings
import com.flxrs.dankchat.ui.main.BatterySettings
import com.flxrs.dankchat.ui.main.ChatSettings
import com.flxrs.dankchat.ui.main.CustomCommandsSettings
import com.flxrs.dankchat.ui.main.DeveloperSettings
import com.flxrs.dankchat.ui.main.DonationsSettings
import com.flxrs.dankchat.ui.main.ModerationSettings
import com.flxrs.dankchat.ui.main.NotificationsSettings
import com.flxrs.dankchat.ui.main.StreamsSettings
import com.flxrs.dankchat.ui.main.ToolsSettings
import com.flxrs.dankchat.ui.main.UserDisplaySettings

/**
 * A hand-picked index of the settings screens that are reachable through their own dedicated nav
 * route - lets [SettingsSearchScreen] jump straight to the right screen instead of the person
 * hunting through the settings tree by hand.
 */
data class SettingsSearchEntry(
    @StringRes val titleRes: Int,
    val route: Any,
)

val settingsSearchIndex =
    listOf(
        SettingsSearchEntry(R.string.preference_appearance_header, AppearanceSettings),
        SettingsSearchEntry(R.string.preference_highlights_ignores_header, NotificationsSettings),
        SettingsSearchEntry(R.string.preference_chat_header, ChatSettings),
        SettingsSearchEntry(R.string.commands_title, CustomCommandsSettings),
        SettingsSearchEntry(R.string.custom_user_display_title, UserDisplaySettings),
        SettingsSearchEntry(R.string.moderation_settings_title, ModerationSettings),
        SettingsSearchEntry(R.string.donation_settings_title, DonationsSettings),
        SettingsSearchEntry(R.string.preference_streams_header, StreamsSettings),
        SettingsSearchEntry(R.string.preference_battery_header, BatterySettings),
        SettingsSearchEntry(R.string.preference_tools_header, ToolsSettings),
        SettingsSearchEntry(R.string.preference_developer_header, DeveloperSettings),
        SettingsSearchEntry(R.string.preference_about_header, AboutSettings),
    )
