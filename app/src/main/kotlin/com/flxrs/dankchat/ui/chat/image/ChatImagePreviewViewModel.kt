package com.flxrs.dankchat.ui.chat.image

import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.koin.core.annotation.KoinViewModel

@KoinViewModel
class ChatImagePreviewViewModel : ViewModel() {
    private val _previewImageUrl = MutableStateFlow<String?>(null)
    val previewImageUrl: StateFlow<String?> = _previewImageUrl.asStateFlow()

    fun show(imageUrl: String) {
        _previewImageUrl.value = imageUrl
    }

    fun dismiss() {
        _previewImageUrl.value = null
    }
}
