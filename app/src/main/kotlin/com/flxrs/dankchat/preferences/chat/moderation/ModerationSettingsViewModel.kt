package com.flxrs.dankchat.preferences.chat.moderation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.flxrs.dankchat.preferences.chat.ChatSettings
import com.flxrs.dankchat.preferences.chat.ChatSettingsDataStore
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import org.koin.core.annotation.KoinViewModel

@KoinViewModel
class ModerationSettingsViewModel(
    private val chatSettingsDataStore: ChatSettingsDataStore,
) : ViewModel() {
    val timeoutDurationsSeconds: StateFlow<List<Long>> =
        chatSettingsDataStore.moderationTimeoutDurationsSeconds
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), ChatSettings.DEFAULT_MODERATION_TIMEOUTS)

    fun updateTimeoutDuration(
        index: Int,
        durationSeconds: Long,
    ) {
        viewModelScope.launch {
            chatSettingsDataStore.update { settings ->
                val updated = settings.moderationTimeoutDurationsSeconds.toMutableList()
                if (index in updated.indices) {
                    updated[index] = durationSeconds.coerceAtLeast(1L)
                }
                settings.copy(moderationTimeoutDurationsSeconds = updated)
            }
        }
    }

    fun resetToDefaults() {
        viewModelScope.launch {
            chatSettingsDataStore.update { settings ->
                settings.copy(moderationTimeoutDurationsSeconds = ChatSettings.DEFAULT_MODERATION_TIMEOUTS)
            }
        }
    }
}
