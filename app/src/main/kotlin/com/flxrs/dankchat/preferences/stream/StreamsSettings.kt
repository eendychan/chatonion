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
    val splitFraction: Float = 0.6f,
)
