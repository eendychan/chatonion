package com.flxrs.dankchat.preferences.donations

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import org.koin.core.annotation.KoinViewModel

@KoinViewModel
class DonationSettingsViewModel(
    private val donationSettingsDataStore: DonationSettingsDataStore,
) : ViewModel() {
    // Exactly one editable slot per provider, restored from storage when present
    val widgetSlots: StateFlow<List<DonationWidget>> =
        donationSettingsDataStore.settings
            .map { settings ->
                DonationProvider.entries.map { provider ->
                    settings.widgets.firstOrNull { it.provider == provider } ?: DonationWidget(provider = provider)
                }
            }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    fun updateWidget(widget: DonationWidget) {
        viewModelScope.launch {
            donationSettingsDataStore.update { settings ->
                val updated = settings.widgets.filterNot { it.provider == widget.provider } + widget
                settings.copy(widgets = updated)
            }
        }
    }

    companion object {
        fun urlHintFor(provider: DonationProvider): String = when (provider) {
            DonationProvider.DonationAlerts -> "https://www.donationalerts.com/widget/alerts?…&token=…"
            DonationProvider.DonateX -> "https://donatex.gg/widget/…?token=…"
            DonationProvider.DonatePay -> "https://widget.donatepay.ru/widgets/page/…?token=…"
            DonationProvider.StreamElements -> "JWT"
        }
    }
}
