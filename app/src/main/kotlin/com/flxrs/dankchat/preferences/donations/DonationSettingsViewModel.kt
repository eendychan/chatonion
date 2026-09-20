package com.flxrs.dankchat.preferences.donations

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
data class DonationWidgetRow(
    val id: Long,
    val channel: String = "",
    val urlOrToken: String = "",
)

/**
 * Rows are kept in local state and persisted to the DataStore on every change.
 * The UI never reads back from the DataStore while editing, which keeps the text
 * fields stable (no asynchronous echo resetting the cursor position).
 */
@KoinViewModel
class DonationSettingsViewModel(
    private val donationSettingsDataStore: DonationSettingsDataStore,
) : ViewModel() {
    private val _rows = MutableStateFlow<List<DonationWidgetRow>>(emptyList())
    val rows: StateFlow<List<DonationWidgetRow>> = _rows.asStateFlow()

    private var nextRowId = 0L

    init {
        viewModelScope.launch {
            val storedRows =
                donationSettingsDataStore.settings
                    .first()
                    .widgets
                    .map { DonationWidgetRow(id = nextRowId++, channel = it.channel, urlOrToken = it.urlOrToken) }
            if (_rows.value.isEmpty()) {
                _rows.value = storedRows
            }
        }
    }

    fun addRow() = updateRows { it + DonationWidgetRow(id = nextRowId++) }

    fun removeRow(id: Long) = updateRows { rows -> rows.filterNot { it.id == id } }

    fun updateRow(row: DonationWidgetRow) = updateRows { rows -> rows.map { if (it.id == row.id) row else it } }

    private fun updateRows(transform: (List<DonationWidgetRow>) -> List<DonationWidgetRow>) {
        val updated = transform(_rows.value)
        _rows.value = updated
        viewModelScope.launch {
            donationSettingsDataStore.update { settings ->
                settings.copy(
                    widgets =
                        updated.map { row ->
                            DonationWidget(
                                urlOrToken = row.urlOrToken.trim(),
                                channel = row.channel.trim().removePrefix("@"),
                            )
                        },
                )
            }
        }
    }
}
