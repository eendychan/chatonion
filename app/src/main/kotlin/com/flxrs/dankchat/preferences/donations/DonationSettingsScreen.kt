package com.flxrs.dankchat.preferences.donations

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
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.ScaffoldDefaults
import androidx.compose.material3.Text
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
fun DonationSettingsScreen(onNavBack: () -> Unit) {
    val viewModel = koinViewModel<DonationSettingsViewModel>()
    val widgets by viewModel.widgetSlots.collectAsStateWithLifecycle()

    Scaffold(
        contentWindowInsets = ScaffoldDefaults.contentWindowInsets.exclude(WindowInsets.navigationBars),
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.donation_settings_title)) },
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
                text = stringResource(R.string.donation_settings_summary),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(bottom = 8.dp),
            )
            widgets.forEachIndexed { index, widget ->
                DonationWidgetSection(
                    widget = widget,
                    onWidgetChange = viewModel::updateWidget,
                )
                if (index < widgets.lastIndex) {
                    HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp))
                }
            }
            NavigationBarSpacer()
        }
    }
}

@Composable
private fun DonationWidgetSection(
    widget: DonationWidget,
    onWidgetChange: (DonationWidget) -> Unit,
) {
    val providerName = stringResource(providerNameRes(widget.provider))
    val isTokenProvider = widget.provider == DonationProvider.StreamElements

    Text(
        text = providerName,
        style = MaterialTheme.typography.titleMedium,
        color = MaterialTheme.colorScheme.onSurface,
        modifier = Modifier.padding(vertical = 4.dp),
    )

    OutlinedTextField(
        value = widget.urlOrToken,
        onValueChange = { onWidgetChange(widget.copy(urlOrToken = it.trim())) },
        label = {
            Text(
                stringResource(
                    if (isTokenProvider) R.string.donation_widget_token_label else R.string.donation_widget_url_label,
                ),
            )
        },
        placeholder = { Text(DonationSettingsViewModel.urlHintFor(widget.provider)) },
        supportingText = {
            Text(
                stringResource(
                    if (isTokenProvider) R.string.donation_widget_token_hint else R.string.donation_widget_url_hint,
                ),
            )
        },
        singleLine = true,
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Uri),
        modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp),
    )

    OutlinedTextField(
        value = widget.channel,
        onValueChange = { onWidgetChange(widget.copy(channel = it.trim().removePrefix("@"))) },
        label = { Text(stringResource(R.string.donation_widget_channel_label, providerName)) },
        singleLine = true,
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Ascii),
        modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp),
    )
}

private fun providerNameRes(provider: DonationProvider): Int =
    when (provider) {
        DonationProvider.DonationAlerts -> R.string.donation_provider_donationalerts
        DonationProvider.DonateX -> R.string.donation_provider_donatex
        DonationProvider.DonatePay -> R.string.donation_provider_donatepay
        DonationProvider.StreamElements -> R.string.donation_provider_streamelements
    }
