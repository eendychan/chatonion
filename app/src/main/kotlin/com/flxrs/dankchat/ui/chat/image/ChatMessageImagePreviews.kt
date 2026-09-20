package com.flxrs.dankchat.ui.chat.image

import android.os.Build
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil3.compose.AsyncImage
import com.flxrs.dankchat.R
import com.flxrs.dankchat.preferences.tools.ToolsSettingsDataStore
import kotlinx.collections.immutable.ImmutableList
import org.koin.compose.koinInject
import org.koin.compose.viewmodel.koinViewModel

private val PREVIEW_MAX_WIDTH = 280.dp
private val PREVIEW_MAX_HEIGHT = 200.dp

/**
 * Renders clickable image previews under a chat message.
 * Previews keep the image's aspect ratio, so the whole image is visible without cropping.
 * In streamer mode images are blurred: the first tap removes the blur,
 * the second tap opens the movable viewer window.
 */
@Composable
fun ChatMessageImagePreviews(
    imageLinks: ImmutableList<ImageLinkUi>,
    modifier: Modifier = Modifier,
) {
    if (imageLinks.isEmpty()) return

    val toolsSettingsDataStore: ToolsSettingsDataStore = koinInject()
    val settings by toolsSettingsDataStore.settings.collectAsStateWithLifecycle(initialValue = toolsSettingsDataStore.current())
    if (!settings.imagePreviewEnabled) return

    val streamerMode = settings.imagePreviewStreamerMode
    val previewViewModel: ChatImagePreviewViewModel = koinViewModel()

    Row(
        modifier =
            modifier
                .padding(top = 4.dp)
                .horizontalScroll(rememberScrollState()),
    ) {
        imageLinks.forEach { link ->
            var unblurred by remember(link.url) { mutableStateOf(false) }
            var imageRatio by remember(link.imageUrl) { mutableStateOf<Float?>(null) }
            val blurred = streamerMode && !unblurred

            Box(
                modifier =
                    Modifier
                        .padding(end = 6.dp)
                        .then(
                            when (val ratio = imageRatio) {
                                // placeholder box until the image dimensions are known
                                null -> Modifier.size(width = PREVIEW_MAX_WIDTH, height = PREVIEW_MAX_HEIGHT)

                                else ->
                                    Modifier
                                        .widthIn(max = PREVIEW_MAX_WIDTH)
                                        .heightIn(max = PREVIEW_MAX_HEIGHT)
                                        .aspectRatio(ratio)
                            },
                        ).clip(RoundedCornerShape(8.dp))
                        .background(MaterialTheme.colorScheme.surfaceContainerHighest)
                        .clickable {
                            if (blurred) {
                                unblurred = true
                            } else {
                                previewViewModel.show(ChatImagePreview(imageUrl = link.imageUrl, sourceUrl = link.url))
                            }
                        },
            ) {
                AsyncImage(
                    model = link.imageUrl,
                    contentDescription = link.url,
                    contentScale = ContentScale.Fit,
                    onSuccess = { state ->
                        val size = state.painter.intrinsicSize
                        if (size.width > 0f && size.height > 0f) {
                            imageRatio = size.width / size.height
                        }
                    },
                    modifier =
                        Modifier
                            .fillMaxSize()
                            .then(
                                if (blurred && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                                    Modifier.blur(24.dp)
                                } else {
                                    Modifier
                                },
                            ),
                )
                if (blurred) {
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier =
                            Modifier
                                .fillMaxSize()
                                .background(Color.Black.copy(alpha = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) 0.25f else 0.85f)),
                    ) {
                        Icon(
                            imageVector = Icons.Default.VisibilityOff,
                            contentDescription = stringResource(R.string.image_preview_streamer_mode_title),
                            tint = Color.White,
                            modifier = Modifier.size(28.dp),
                        )
                    }
                }
            }
        }
    }
}
