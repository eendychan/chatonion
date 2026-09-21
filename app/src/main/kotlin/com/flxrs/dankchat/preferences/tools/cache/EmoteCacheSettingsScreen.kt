package com.flxrs.dankchat.preferences.tools.cache

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
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
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.ScaffoldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.flxrs.dankchat.R
import com.flxrs.dankchat.preferences.components.NavigationBarSpacer
import com.flxrs.dankchat.preferences.components.SwitchPreferenceItem
import org.koin.compose.viewmodel.koinViewModel

@Composable
fun EmoteCacheSettingsScreen(onNavBack: () -> Unit) {
    val viewModel = koinViewModel<EmoteCacheSettingsViewModel>()
    val enabled by viewModel.enabled.collectAsStateWithLifecycle()
    val cosmeticsEnabled by viewModel.cosmeticsEnabled.collectAsStateWithLifecycle()
    val rows by viewModel.rows.collectAsStateWithLifecycle()

    Scaffold(
        contentWindowInsets = ScaffoldDefaults.contentWindowInsets.exclude(WindowInsets.navigationBars),
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.emote_cache_settings_title)) },
                navigationIcon = {
                    IconButton(onClick = onNavBack) {
                        Icon(imageVector = Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.back))
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
            Text(
                text = stringResource(R.string.emote_cache_settings_summary),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(bottom = 8.dp),
            )
            SwitchPreferenceItem(
                title = stringResource(R.string.emote_cache_enabled_title),
                summary = stringResource(R.string.emote_cache_enabled_summary),
                isChecked = enabled,
                onClick = viewModel::setEnabled,
            )
            SwitchPreferenceItem(
                title = stringResource(R.string.emote_cache_cosmetics_enabled_title),
                summary = stringResource(R.string.emote_cache_cosmetics_enabled_summary),
                isChecked = cosmeticsEnabled,
                onClick = viewModel::setCosmeticsEnabled,
            )
            rows.forEachIndexed { index, row ->
                EmoteCacheChannelRowItem(
                    row = row,
                    onRowChange = viewModel::updateRow,
                    onRowRemove = { viewModel.removeRow(row.id) },
                )
                if (index < rows.lastIndex) {
                    HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))
                }
            }
            OutlinedButton(
                onClick = viewModel::addRow,
                modifier = Modifier.padding(top = 12.dp),
            ) {
                Icon(imageVector = Icons.Default.Add, contentDescription = null)
                Text(stringResource(R.string.emote_cache_channel_add))
            }
            NavigationBarSpacer()
        }
    }
}

@Composable
private fun EmoteCacheChannelRowItem(
    row: EmoteCacheChannelRow,
    onRowChange: (EmoteCacheChannelRow) -> Unit,
    onRowRemove: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier.fillMaxWidth(),
    ) {
        OutlinedTextField(
            value = row.channel,
            onValueChange = { onRowChange(row.copy(channel = it)) },
            label = { Text(stringResource(R.string.emote_cache_channel_label)) },
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Ascii),
            modifier = Modifier.weight(1f).padding(vertical = 6.dp),
        )
        IconButton(onClick = onRowRemove) {
            Icon(imageVector = Icons.Default.Delete, contentDescription = stringResource(R.string.emote_cache_channel_remove))
        }
    }
}
