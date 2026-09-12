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
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.ScaffoldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.flxrs.dankchat.R
import com.flxrs.dankchat.preferences.components.NavigationBarSpacer
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
                OutlinedTextField(
                    value = seconds.toString(),
                    onValueChange = { newValue ->
                        val parsed = newValue.filter { it.isDigit() }.toLongOrNull()
                        if (parsed != null) {
                            viewModel.updateTimeoutDuration(index, parsed)
                        }
                    },
                    label = { Text(stringResource(R.string.moderation_timeout_label, index + 1)) },
                    suffix = { Text(stringResource(R.string.moderation_timeout_seconds_hint)) },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp),
                )
            }
            NavigationBarSpacer()
        }
    }
}
