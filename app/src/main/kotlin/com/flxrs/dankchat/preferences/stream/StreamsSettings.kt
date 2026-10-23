package com.flxrs.dankchat.preferences.stream

import kotlinx.serialization.Serializable

@Serializable
data class StreamsSettings(
    val fetchStreams: Boolean = true,
    val showStreamInfo: Boolean = true,
    val showStreamCategory: Boolean = true,
    val showLiveMessages: Boolean = true,
    val showStreamExtensions: Boolean = false,
    val preventStreamReloads: Boolean = true,
    val enablePiP: Boolean = false,
    val ambilightEnabled: Boolean = true,
    // percent, 0-100
    val ambilightOpacity: Int = 40,
    val splitFraction: Float = 0.6f,
)
