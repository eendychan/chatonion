package com.flxrs.dankchat.preferences.tools.cache

import androidx.compose.runtime.Immutable
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import org.koin.core.annotation.KoinViewModel

@Immutable
data class EmoteCacheChannelRow(
    val id: Long,
    val channel: String = "",
)

/**
 * Rows are kept in local state and persisted to the DataStore on every change.
 * The UI never reads back from the DataStore while editing, which keeps the text
 * fields stable (no asynchronous echo resetting the cursor position).
 */
@KoinViewModel
class EmoteCacheSettingsViewModel(
    private val emoteCacheSettingsDataStore: EmoteCacheSettingsDataStore,
) : ViewModel() {
    private val _enabled = MutableStateFlow(emoteCacheSettingsDataStore.current().enabled)
    val enabled: StateFlow<Boolean> = _enabled.asStateFlow()

    private val _cosmeticsEnabled = MutableStateFlow(emoteCacheSettingsDataStore.current().sevenTvCosmeticsEnabled)
    val cosmeticsEnabled: StateFlow<Boolean> = _cosmeticsEnabled.asStateFlow()

    private val _rows = MutableStateFlow<List<EmoteCacheChannelRow>>(emptyList())
    val rows: StateFlow<List<EmoteCacheChannelRow>> = _rows.asStateFlow()

    private var nextRowId = 0L

    init {
        viewModelScope.launch {
            val storedChannels = emoteCacheSettingsDataStore.settings.first().channels
            if (_rows.value.isEmpty()) {
                _rows.value = storedChannels.map { EmoteCacheChannelRow(id = nextRowId++, channel = it) }
            }
        }
    }

    fun setEnabled(enabled: Boolean) {
        _enabled.value = enabled
        viewModelScope.launch {
            emoteCacheSettingsDataStore.update { it.copy(enabled = enabled) }
        }
    }

    fun setCosmeticsEnabled(enabled: Boolean) {
        _cosmeticsEnabled.value = enabled
        viewModelScope.launch {
            emoteCacheSettingsDataStore.update { it.copy(sevenTvCosmeticsEnabled = enabled) }
        }
    }

    fun addRow() = updateRows { it + EmoteCacheChannelRow(id = nextRowId++) }

    fun removeRow(id: Long) = updateRows { rows -> rows.filterNot { it.id == id } }

    fun updateRow(row: EmoteCacheChannelRow) = updateRows { rows -> rows.map { if (it.id == row.id) row else it } }

    private fun updateRows(transform: (List<EmoteCacheChannelRow>) -> List<EmoteCacheChannelRow>) {
        val updated = transform(_rows.value)
        _rows.value = updated
        viewModelScope.launch {
            emoteCacheSettingsDataStore.update { settings ->
                settings.copy(
                    channels =
                        updated
                            .map { it.channel.trim().removePrefix("@") }
                            .filter { it.isNotBlank() }
                            .distinct(),
                )
            }
        }
    }
}
