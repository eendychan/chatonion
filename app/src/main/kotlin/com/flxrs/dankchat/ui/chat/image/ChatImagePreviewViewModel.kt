package com.flxrs.dankchat.ui.chat.image

import androidx.compose.runtime.Immutable
import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.koin.core.annotation.KoinViewModel

/**
 * [imageUrl] is the direct image to show first, [sourceUrl] is the original chat link —
 * for eblo.id posts it is used to discover additional album images.
 */
@Immutable
data class ChatImagePreview(
    val imageUrl: String,
    val sourceUrl: String,
)

@KoinViewModel
class ChatImagePreviewViewModel : ViewModel() {
    private val _preview = MutableStateFlow<ChatImagePreview?>(null)
    val preview: StateFlow<ChatImagePreview?> = _preview.asStateFlow()

    fun show(preview: ChatImagePreview) {
        _preview.value = preview
    }

    fun dismiss() {
        _preview.value = null
    }
}
