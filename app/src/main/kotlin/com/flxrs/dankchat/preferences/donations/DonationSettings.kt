package com.flxrs.dankchat.preferences.donations

import kotlinx.serialization.Serializable

@Serializable
data class DonationSettings(
    val widgets: List<DonationWidget> = emptyList(),
) {
    val configuredWidgets: List<DonationWidget> get() = widgets.filter { it.isConfigured }
}

/**
 * A single donation widget row: an optional channel binding plus a widget link of any
 * donation aggregator (or a StreamElements JWT token). The provider is derived from the link.
 */
@Serializable
data class DonationWidget(
    val urlOrToken: String = "",
    val channel: String = "",
) {
    val isConfigured: Boolean get() = urlOrToken.isNotBlank()
    val provider: DonationProvider? get() = detectDonationProvider(urlOrToken)
}

enum class DonationProvider {
    DonationAlerts,
    DonateX,
    DonatePay,
    StreamElements,
}

/** Detects the donation service from a widget link (or a StreamElements JWT token). */
fun detectDonationProvider(input: String): DonationProvider? {
    val value = input.trim().lowercase()
    return when {
        value.isBlank() -> null
        "donationalerts.com" in value -> DonationProvider.DonationAlerts
        "donatex.gg" in value -> DonationProvider.DonateX
        "donatepay" in value -> DonationProvider.DonatePay
        isLikelyJwtToken(value) -> DonationProvider.StreamElements
        else -> null
    }
}

// StreamElements exposes no widget URL; its users paste a JWT token (three base64url parts separated by dots)
private fun isLikelyJwtToken(value: String): Boolean = !value.contains("://") && !value.contains(' ') && value.count { it == '.' } >= 2
