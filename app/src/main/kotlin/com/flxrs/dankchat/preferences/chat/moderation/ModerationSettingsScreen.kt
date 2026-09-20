package com.flxrs.dankchat.preferences.chat.moderation

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.exclude
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.ScaffoldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.flxrs.dankchat.R
import com.flxrs.dankchat.preferences.components.NavigationBarSpacer
import com.flxrs.dankchat.utils.DateTimeUtils
import org.koin.compose.viewmodel.koinViewModel

@Composable
fun ModerationSettingsScreen(onNavBack: () -> Unit) {
    val viewModel = koinViewModel<ModerationSettingsViewModel>()
    val durations by viewModel.timeoutDurationsSeconds.collectAsStateWithLifecycle()

    Scaffold(
        contentWindowInsets = ScaffoldDefaults.contentWindowInsets.exclude(WindowInsets.navigationBars),
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.moderation_settings_title)) },
                navigationIcon = {
                    IconButton(onClick = onNavBack) {
                        Icon(imageVector = Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.back))
                    }
                },
                actions = {
                    TextButton(onClick = viewModel::resetToDefaults) {
                        Text(stringResource(R.string.reset))
                    }
                },
            )
        },
    ) { padding ->
        Column(
            modifier =
                Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 16.dp, vertical = 8.dp),
        ) {
            durations.forEachIndexed { index, seconds ->
                TimeoutDurationField(
                    index = index,
                    seconds = seconds,
                    onDurationChange = { viewModel.updateTimeoutDuration(index, it) },
                )
            }
            NavigationBarSpacer()
        }
    }
}

@Composable
private fun TimeoutDurationField(
    index: Int,
    seconds: Long,
    onDurationChange: (Long) -> Unit,
) {
    var text by remember { mutableStateOf(seconds.toString()) }

    // Sync external changes (e.g. reset to defaults) without clobbering text like "10m"
    // that already parses to the stored value.
    LaunchedEffect(seconds) {
        val current = DateTimeUtils.durationToSeconds(text)?.toLong()
        if (current == null || current != seconds) {
            text = seconds.toString()
        }
    }

    val parsed = DateTimeUtils.durationToSeconds(text)?.toLong()
    val isInvalid = text.isNotBlank() && parsed == null

    OutlinedTextField(
        value = text,
        onValueChange = { newValue ->
            text = newValue
            DateTimeUtils
                .durationToSeconds(newValue)
                ?.toLong()
                ?.coerceIn(ModerationSettingsViewModel.MIN_TIMEOUT_SECONDS, ModerationSettingsViewModel.MAX_TIMEOUT_SECONDS)
                ?.let(onDurationChange)
        },
        label = { Text(stringResource(R.string.moderation_timeout_label, index + 1)) },
        supportingText = {
            when {
                isInvalid -> {
                    Text(
                        text = stringResource(R.string.moderation_timeout_invalid),
                        color = MaterialTheme.colorScheme.error,
                    )
                }

                parsed != null -> {
                    Text(stringResource(R.string.moderation_timeout_format_hint, DateTimeUtils.formatSeconds(parsed.toInt())))
                }
            }
        },
        isError = isInvalid,
        singleLine = true,
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Text),
        modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp),
    )
}
