package com.flxrs.dankchat.ui.main.dialog

import androidx.compose.runtime.Immutable
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.flxrs.dankchat.data.donations.DonationAggregator
import com.flxrs.dankchat.data.donations.DonationEvent
import com.flxrs.dankchat.preferences.donations.DonationWidget
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toImmutableList
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import org.koin.core.annotation.KoinViewModel

/**
 * Playback queue for the unified donations overlay: history of every configured widget
 * merged oldest-first, auto-advancing like a player. New donations are appended at the
 * end of the queue while the overlay polls the providers in the background.
 */
@KoinViewModel
class DonationsPlaybackViewModel(
    private val donationAggregator: DonationAggregator,
) : ViewModel() {
    @Immutable
    data class PlaybackState(
        val queue: ImmutableList<DonationEvent> = persistentListOf(),
        val currentIndex: Int = 0,
        val isPlaying: Boolean = true,
        val isLoading: Boolean = true,
    )

    private val _state = MutableStateFlow(PlaybackState())
    val state: StateFlow<PlaybackState> = _state.asStateFlow()

    private var widgets: List<DonationWidget> = emptyList()
    private var pollJob: Job? = null
    private var advanceJob: Job? = null

    fun start(widgets: List<DonationWidget>) {
        if (pollJob != null) return
        this.widgets = widgets
        _state.value = PlaybackState()
        pollJob =
            viewModelScope.launch {
                refresh()
                _state.update { it.copy(isLoading = false) }
                while (isActive) {
                    delay(POLL_INTERVAL_MS)
                    refresh()
                }
            }
        advanceJob =
            viewModelScope.launch {
                while (isActive) {
                    delay(DISPLAY_DURATION_MS)
                    advanceIfPlaying()
                }
            }
    }

    fun stop() {
        pollJob?.cancel()
        advanceJob?.cancel()
        pollJob = null
        advanceJob = null
    }

    fun skip() = _state.update { state ->
        if (state.queue.isEmpty()) {
            state
        } else {
            state.copy(currentIndex = (state.currentIndex + 1).coerceAtMost(state.queue.lastIndex))
        }
    }

    fun previous() = _state.update { state ->
        state.copy(currentIndex = (state.currentIndex - 1).coerceAtLeast(0))
    }

    fun togglePlaying() = _state.update { it.copy(isPlaying = !it.isPlaying) }

    private suspend fun refresh() {
        val fetched = donationAggregator.fetchRecent(widgets).toImmutableList()
        _state.update { state ->
            val currentId = state.queue.getOrNull(state.currentIndex)?.id
            val newIndex = currentId?.let { id -> fetched.indexOfFirst { it.id == id } }?.takeIf { it >= 0 } ?: state.currentIndex
            state.copy(queue = fetched, currentIndex = newIndex.coerceIn(0, (fetched.size - 1).coerceAtLeast(0)))
        }
    }

    private fun advanceIfPlaying() = _state.update { state ->
        if (state.isPlaying && state.currentIndex < state.queue.lastIndex) {
            state.copy(currentIndex = state.currentIndex + 1)
        } else {
            state
        }
    }

    private companion object {
        const val POLL_INTERVAL_MS = 20_000L
        const val DISPLAY_DURATION_MS = 6_000L
    }
}
