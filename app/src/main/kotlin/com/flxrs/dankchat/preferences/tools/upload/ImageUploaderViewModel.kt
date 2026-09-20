package com.flxrs.dankchat.preferences.tools.upload

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.flxrs.dankchat.preferences.tools.ImageUploaderConfig
import com.flxrs.dankchat.preferences.tools.ToolsSettingsDataStore
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.WhileSubscribed
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import org.koin.core.annotation.KoinViewModel
import kotlin.time.Duration.Companion.seconds

@KoinViewModel
class ImageUploaderViewModel(
    private val toolsSettingsDataStore: ToolsSettingsDataStore,
) : ViewModel() {
    val uploader =
        toolsSettingsDataStore.uploadConfig
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(5.seconds),
                initialValue = toolsSettingsDataStore.current().uploaderConfig,
            )

    val imagePreviewEnabled =
        toolsSettingsDataStore.settings
            .map { it.imagePreviewEnabled }
            .distinctUntilChanged()
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(5.seconds),
                initialValue = toolsSettingsDataStore.current().imagePreviewEnabled,
            )

    val imagePreviewStreamerMode =
        toolsSettingsDataStore.settings
            .map { it.imagePreviewStreamerMode }
            .distinctUntilChanged()
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(5.seconds),
                initialValue = toolsSettingsDataStore.current().imagePreviewStreamerMode,
            )

    fun setImagePreviewEnabled(enabled: Boolean) = viewModelScope.launch {
        toolsSettingsDataStore.update { it.copy(imagePreviewEnabled = enabled) }
    }

    fun setImagePreviewStreamerMode(enabled: Boolean) = viewModelScope.launch {
        toolsSettingsDataStore.update { it.copy(imagePreviewStreamerMode = enabled) }
    }

    fun save(uploader: ImageUploaderConfig) = viewModelScope.launch {
        val validated =
            uploader.copy(
                headers = uploader.headers?.takeIf { it.isNotBlank() },
                imageLinkPattern = uploader.imageLinkPattern?.takeIf { it.isNotBlank() },
                deletionLinkPattern = uploader.deletionLinkPattern?.takeIf { it.isNotBlank() },
            )
        toolsSettingsDataStore.update { it.copy(uploaderConfig = validated) }
    }

    fun reset() = viewModelScope.launch {
        toolsSettingsDataStore.update { it.copy(uploaderConfig = ImageUploaderConfig.DEFAULT) }
    }
}
