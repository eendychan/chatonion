package com.flxrs.dankchat.ui.main.dialog

import android.text.format.DateUtils
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.SkipPrevious
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.flxrs.dankchat.R
import com.flxrs.dankchat.data.UserName
import com.flxrs.dankchat.data.donations.DonationEvent
import com.flxrs.dankchat.preferences.donations.DonationProvider
import com.flxrs.dankchat.preferences.donations.DonationWidget
import org.koin.compose.viewmodel.koinViewModel

/**
 * Unified donations overlay: a single player fed by the merged donation history of all
 * widgets bound to the active channel. Auto-advances like a playlist; new donations are
 * appended live. Supports pause/resume and skipping.
 */
@Composable
fun DonationsDialog(
    widgets: List<DonationWidget>,
    activeChannel: UserName?,
    onDismiss: () -> Unit,
) {
    val visibleWidgets = visibleWidgetsFor(widgets, activeChannel)
    if (visibleWidgets.isEmpty()) {
        LaunchedEffect(Unit) { onDismiss() }
        return
    }

    val playbackViewModel: DonationsPlaybackViewModel = koinViewModel()
    val state by playbackViewModel.state.collectAsStateWithLifecycle()

    DisposableEffect(visibleWidgets) {
        playbackViewModel.start(visibleWidgets)
        onDispose { playbackViewModel.stop() }
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false, decorFitsSystemWindows = false),
    ) {
        Surface(
            shape = MaterialTheme.shapes.large,
            color = MaterialTheme.colorScheme.surfaceContainerHigh,
            modifier =
                Modifier
                    .fillMaxWidth(0.94f)
                    .fillMaxHeight(0.6f),
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .padding(start = 16.dp, end = 4.dp),
                ) {
                    Text(
                        text = stringResource(R.string.donations_dialog_title),
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.weight(1f),
                    )
                    IconButton(onClick = onDismiss) {
                        Icon(imageVector = Icons.Default.Close, contentDescription = stringResource(R.string.back))
                    }
                }

                Box(
                    contentAlignment = Alignment.Center,
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .weight(1f)
                            .padding(horizontal = 16.dp),
                ) {
                    val currentDonation = state.queue.getOrNull(state.currentIndex)
                    when {
                        state.isLoading -> CircularProgressIndicator()

                        currentDonation == null ->
                            Text(
                                text = stringResource(R.string.donations_empty),
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                textAlign = TextAlign.Center,
                            )

                        else -> DonationCard(donation = currentDonation)
                    }
                }

                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 8.dp),
                ) {
                    Text(
                        text = "${(state.currentIndex + 1).coerceAtMost(state.queue.size)} / ${state.queue.size}",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Box(modifier = Modifier.weight(1f))
                    IconButton(
                        onClick = playbackViewModel::previous,
                        enabled = state.currentIndex > 0,
                    ) {
                        Icon(
                            imageVector = Icons.Default.SkipPrevious,
                            contentDescription = stringResource(R.string.donations_previous),
                        )
                    }
                    IconButton(onClick = playbackViewModel::togglePlaying) {
                        Icon(
                            imageVector = if (state.isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                            contentDescription = stringResource(if (state.isPlaying) R.string.donations_pause else R.string.donations_resume),
                        )
                    }
                    IconButton(
                        onClick = playbackViewModel::skip,
                        enabled = state.currentIndex < state.queue.lastIndex,
                    ) {
                        Icon(
                            imageVector = Icons.Default.SkipNext,
                            contentDescription = stringResource(R.string.donations_skip),
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun DonationCard(donation: DonationEvent) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(6.dp),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Text(
            text = stringResource(providerLabelRes(donation.provider)),
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text(
            text = donation.amountText,
            style = MaterialTheme.typography.headlineMedium,
            color = MaterialTheme.colorScheme.primary,
            textAlign = TextAlign.Center,
        )
        Text(
            text = donation.username,
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurface,
            textAlign = TextAlign.Center,
        )
        if (donation.message.isNotBlank()) {
            Text(
                text = donation.message,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurface,
                textAlign = TextAlign.Center,
            )
        }
        if (donation.timestampEpochMs > 0L) {
            Text(
                text = DateUtils.getRelativeTimeSpanString(LocalContext.current, donation.timestampEpochMs).toString(),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

private fun providerLabelRes(provider: DonationProvider): Int = when (provider) {
    DonationProvider.DonationAlerts -> R.string.donation_provider_donationalerts
    DonationProvider.DonateX -> R.string.donation_provider_donatex
    DonationProvider.DonatePay -> R.string.donation_provider_donatepay
    DonationProvider.StreamElements -> R.string.donation_provider_streamelements
}

private fun visibleWidgetsFor(
    widgets: List<DonationWidget>,
    activeChannel: UserName?,
): List<DonationWidget> = widgets.filter { widget ->
    widget.isConfigured &&
        (widget.channel.isBlank() || widget.channel.equals(activeChannel?.value, ignoreCase = true))
}
