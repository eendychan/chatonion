package com.flxrs.dankchat.data.donations

import androidx.compose.runtime.Immutable
import com.flxrs.dankchat.preferences.donations.DonationProvider

/**
 * A single donation from any supported aggregator, normalized for the unified
 * donations overlay. [id] is unique across providers ("provider:remoteId").
 */
@Immutable
data class DonationEvent(
    val id: String,
    val provider: DonationProvider,
    val username: String,
    val amountText: String,
    val message: String,
    val timestampEpochMs: Long,
)
