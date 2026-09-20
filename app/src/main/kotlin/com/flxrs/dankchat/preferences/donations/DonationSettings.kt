package com.flxrs.dankchat.preferences.donations

import kotlinx.serialization.Serializable

@Serializable
data class DonationSettings(
    val widgets: List<DonationWidget> = emptyList(),
) {
    val configuredWidgets: List<DonationWidget> get() = widgets.filter { it.isConfigured }
}

@Serializable
data class DonationWidget(
    val provider: DonationProvider,
    val urlOrToken: String = "",
    val channel: String = "",
) {
    val isConfigured: Boolean get() = urlOrToken.isNotBlank()
}

@Serializable
enum class DonationProvider {
    DonationAlerts,
    DonateX,
    DonatePay,
    StreamElements,
}
