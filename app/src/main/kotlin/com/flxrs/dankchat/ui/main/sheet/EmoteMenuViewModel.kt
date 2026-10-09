package com.flxrs.dankchat.ui.main.sheet

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.flxrs.dankchat.data.repo.chat.ChatChannelProvider
import com.flxrs.dankchat.data.repo.data.DataRepository
import com.flxrs.dankchat.data.repo.emote.EmoteUsageRepository
import com.flxrs.dankchat.data.repo.emote.Emotes
import com.flxrs.dankchat.data.twitch.emote.EmoteType
import com.flxrs.dankchat.di.DispatchersProvider
import com.flxrs.dankchat.ui.chat.emotemenu.EmoteMenuTab
import com.flxrs.dankchat.ui.chat.emotemenu.EmoteMenuTabItem
import com.flxrs.dankchat.utils.extensions.flatMapLatestOrDefault
import com.flxrs.dankchat.utils.extensions.toEmoteItems
import com.flxrs.dankchat.utils.extensions.toEmoteItemsWithFront
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.toImmutableList
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.withContext
import org.koin.core.annotation.KoinViewModel

@KoinViewModel
class EmoteMenuViewModel(
    dataRepository: DataRepository,
    chatChannelProvider: ChatChannelProvider,
    emoteUsageRepository: EmoteUsageRepository,
    dispatchersProvider: DispatchersProvider,
) : ViewModel() {
    private val _selectedTabIndex = MutableStateFlow(0)
    val selectedTabIndex: StateFlow<Int> = _selectedTabIndex.asStateFlow()

    fun selectTab(index: Int) {
        _selectedTabIndex.value = index
    }

    private val activeChannel = chatChannelProvider.activeChannel

    private val emotes =
        activeChannel
            .flatMapLatestOrDefault(Emotes()) { dataRepository.getEmotes(it) }

    private val recentEmotes =
        emoteUsageRepository.getRecentUsages().distinctUntilChanged { old, new ->
            new.all { newEmote -> old.any { it.emoteId == newEmote.emoteId } }
        }

    val emoteTabItems: StateFlow<ImmutableList<EmoteMenuTabItem>> =
        combine(emotes, recentEmotes, activeChannel) { emotes, recentEmotes, channel ->
            withContext(dispatchersProvider.default) {
                val sortedEmotes = emotes.sorted
                val availableRecents =
                    recentEmotes.mapNotNull { usage ->
                        sortedEmotes
                            .firstOrNull { it.id == usage.emoteId }
                            ?.copy(emoteType = EmoteType.RecentUsageEmote)
                    }

                val groupedByType =
                    sortedEmotes.groupBy {
                        when {
                            it.code in FFZ_EFFECT_CODES -> EmoteMenuTab.EFFECTS
                            else -> it.emoteType.toMenuTab()
                        }
                    }
                listOf(
                    async { EmoteMenuTabItem(EmoteMenuTab.RECENT, availableRecents.toEmoteItems()) },
                    async { EmoteMenuTabItem(EmoteMenuTab.SUBS, groupedByType[EmoteMenuTab.SUBS].toEmoteItemsWithFront(channel)) },
                    async { EmoteMenuTabItem(EmoteMenuTab.CHANNEL, groupedByType[EmoteMenuTab.CHANNEL].orEmpty().toEmoteItems()) },
                    async { EmoteMenuTabItem(EmoteMenuTab.GLOBAL, groupedByType[EmoteMenuTab.GLOBAL].orEmpty().toEmoteItems()) },
                    async { EmoteMenuTabItem(EmoteMenuTab.EFFECTS, groupedByType[EmoteMenuTab.EFFECTS].orEmpty().toEmoteItems()) },
                    async { EmoteMenuTabItem(EmoteMenuTab.GIF, emptyList()) },
                ).awaitAll().toImmutableList()
            }
        }.stateIn(
            viewModelScope,
            SharingStarted.WhileSubscribed(stopTimeoutMillis = 5000),
            EmoteMenuTab.entries.map { EmoteMenuTabItem(it, emptyList()) }.toImmutableList(),
        )
}

// FFZ's own "effect" emotes live in the effects tab rather than among the regular global ones.
private val FFZ_EFFECT_CODES = setOf("ffzW", "ffzX", "ffzY", "ffzCursed")

private fun EmoteType.toMenuTab(): EmoteMenuTab = when (this) {
    is EmoteType.ChannelTwitchEmote,
    is EmoteType.ChannelTwitchBitEmote,
    is EmoteType.ChannelTwitchFollowerEmote,
    -> EmoteMenuTab.SUBS

    is EmoteType.ChannelFFZEmote,
    is EmoteType.ChannelBTTVEmote,
    is EmoteType.ChannelSevenTVEmote,
    -> EmoteMenuTab.CHANNEL

    else -> EmoteMenuTab.GLOBAL
}
