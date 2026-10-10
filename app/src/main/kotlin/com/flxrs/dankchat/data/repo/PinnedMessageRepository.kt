package com.flxrs.dankchat.data.repo

import com.flxrs.dankchat.data.DisplayName
import com.flxrs.dankchat.data.UserId
import com.flxrs.dankchat.data.UserName
import com.flxrs.dankchat.data.api.helix.HelixApiClient
import com.flxrs.dankchat.data.api.helix.dto.PinnedChatMessageDto
import com.flxrs.dankchat.data.api.shared.dto.toEmotesWithPositions
import com.flxrs.dankchat.data.auth.AuthDataStore
import com.flxrs.dankchat.data.repo.channel.ChannelRepository
import com.flxrs.dankchat.data.twitch.message.EmoteWithPositions
import com.flxrs.dankchat.di.DispatchersProvider
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import org.koin.core.annotation.Single
import java.util.concurrent.ConcurrentHashMap
import kotlin.concurrent.atomics.AtomicLong
import kotlin.concurrent.atomics.incrementAndFetch
import kotlin.time.Clock
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds
import kotlin.time.Instant

sealed interface PinnedMessageState {
    data object None : PinnedMessageState

    data class Pinned(
        val message: PinnedMessage,
    ) : PinnedMessageState
}

data class PinnedMessage(
    val messageId: String,
    val channel: UserName,
    val text: String,
    val emotesWithPositions: List<EmoteWithPositions>,
    val senderId: UserId,
    val senderLogin: UserName,
    val senderName: DisplayName,
    val pinnedByName: DisplayName,
    val startsAt: Instant,
    val endsAt: Instant?,
)

@Single
class PinnedMessageRepository(
    private val helixApiClient: HelixApiClient,
    private val channelRepository: ChannelRepository,
    private val authDataStore: AuthDataStore,
    dispatchersProvider: DispatchersProvider,
) {
    private val scope = CoroutineScope(SupervisorJob() + dispatchersProvider.default)
    private val states = ConcurrentHashMap<UserName, MutableStateFlow<PinnedMessageState>>()

    // Incremented before each fetch and on clear, so stale responses can't override newer state
    private val requestIds = ConcurrentHashMap<UserName, AtomicLong>()
    private val appliedIds = ConcurrentHashMap<UserName, AtomicLong>()
    private val expiryJobs = ConcurrentHashMap<UserName, Job>()
    private val retryJobs = ConcurrentHashMap<UserName, Job>()

    fun getState(channel: UserName): StateFlow<PinnedMessageState> = stateFlowOf(channel)

    suspend fun fetch(channel: UserName) {
        if (tryFetch(channel)) {
            return
        }

        // At app start the channel id, the login or the token aren't always ready yet, or the
        // first request fails - without a retry the pin then stayed invisible until something
        // else (a new pin, a pubsub update) triggered another fetch.
        retryJobs.remove(channel)?.cancel()
        retryJobs[channel] =
            scope.launch {
                for (retryDelay in RETRY_DELAYS) {
                    delay(retryDelay)
                    if (tryFetch(channel)) {
                        return@launch
                    }
                }
            }
    }

    /** Returns true when there's nothing left to retry: applied, or superseded by a newer request/clear. */
    private suspend fun tryFetch(channel: UserName): Boolean {
        val channelId = channelRepository.getChannel(channel)?.id ?: return false
        val moderatorId = authDataStore.userIdString ?: return false
        val requestId = requestIdOf(channel).incrementAndFetch()
        return helixApiClient.getPinnedChatMessage(channelId, moderatorId).fold(
            onSuccess = { dto ->
                // A response only has to be newer than what was last applied - not be the very
                // latest request issued. Otherwise a valid response got thrown away whenever a
                // newer request (e.g. from the reconnect) had been started and then failed.
                val applied = appliedIdOf(channel)
                if (requestId > applied.load()) {
                    applied.store(requestId)
                    applyState(channel, dto)
                }
                true
            },
            onFailure = { requestIdOf(channel).load() != requestId },
        )
    }

    fun clear(channel: UserName) {
        // Anything requested before this must not be applied anymore
        appliedIdOf(channel).store(requestIdOf(channel).incrementAndFetch())
        retryJobs.remove(channel)?.cancel()
        expiryJobs.remove(channel)?.cancel()
        stateFlowOf(channel).value = PinnedMessageState.None
    }

    suspend fun pin(
        channel: UserName,
        messageId: String,
        duration: Duration?,
    ): Result<Unit> {
        val channelId = channelRepository.getChannel(channel)?.id ?: return Result.failure(IllegalStateException("Unknown channel $channel"))
        val moderatorId = authDataStore.userIdString ?: return Result.failure(IllegalStateException("Not logged in"))
        return helixApiClient
            .pinChatMessage(channelId, moderatorId, messageId, durationSeconds = duration?.inWholeSeconds)
            .onSuccess { scope.launch { fetch(channel) } }
    }

    suspend fun unpin(channel: UserName): Result<Unit> {
        val pinned = stateFlowOf(channel).value as? PinnedMessageState.Pinned ?: return Result.success(Unit)
        val channelId = channelRepository.getChannel(channel)?.id ?: return Result.failure(IllegalStateException("Unknown channel $channel"))
        val moderatorId = authDataStore.userIdString ?: return Result.failure(IllegalStateException("Not logged in"))
        return helixApiClient
            .unpinChatMessage(channelId, moderatorId, pinned.message.messageId)
            .onSuccess { clear(channel) }
    }

    fun removeChannel(channel: UserName) {
        expiryJobs.remove(channel)?.cancel()
        retryJobs.remove(channel)?.cancel()
        requestIds.remove(channel)
        appliedIds.remove(channel)
        states.remove(channel)
    }

    private fun applyState(
        channel: UserName,
        dto: PinnedChatMessageDto?,
    ) {
        expiryJobs.remove(channel)?.cancel()
        val message = dto?.toPinnedMessage(channel)
        val endsAt = message?.endsAt
        val state =
            when {
                message == null -> PinnedMessageState.None
                endsAt != null && endsAt <= Clock.System.now() -> PinnedMessageState.None
                else -> PinnedMessageState.Pinned(message)
            }
        stateFlowOf(channel).value = state
        if (state is PinnedMessageState.Pinned && endsAt != null) {
            expiryJobs[channel] = scope.launch {
                delay(endsAt - Clock.System.now())
                clear(channel)
            }
        }
    }

    private fun stateFlowOf(channel: UserName): MutableStateFlow<PinnedMessageState> = states.getOrPut(channel) { MutableStateFlow(PinnedMessageState.None) }

    private fun requestIdOf(channel: UserName): AtomicLong = requestIds.getOrPut(channel) { AtomicLong(0L) }

    private fun appliedIdOf(channel: UserName): AtomicLong = appliedIds.getOrPut(channel) { AtomicLong(0L) }

    private fun PinnedChatMessageDto.toPinnedMessage(channel: UserName): PinnedMessage = PinnedMessage(
        messageId = messageId,
        channel = channel,
        text = message.text,
        emotesWithPositions = message.fragments.toEmotesWithPositions(),
        senderId = senderUserId,
        senderLogin = senderUserLogin,
        senderName = senderUserName,
        pinnedByName = pinnedByUserName,
        startsAt = startsAt,
        endsAt = endsAt?.takeIf { it.isNotBlank() }?.let { runCatching { Instant.parse(it) }.getOrNull() },
    )
}

private val RETRY_DELAYS = listOf(2.seconds, 5.seconds, 10.seconds)
