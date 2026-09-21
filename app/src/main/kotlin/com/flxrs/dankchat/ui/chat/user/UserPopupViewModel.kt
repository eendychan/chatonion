package com.flxrs.dankchat.ui.chat.user

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.flxrs.dankchat.data.UserId
import com.flxrs.dankchat.data.UserName
import com.flxrs.dankchat.data.api.ivr.IvrApiClient
import com.flxrs.dankchat.data.repo.IgnoresRepository
import com.flxrs.dankchat.data.repo.channel.ChannelRepository
import com.flxrs.dankchat.data.repo.chat.ChatRepository
import com.flxrs.dankchat.data.repo.chat.UserStateRepository
import com.flxrs.dankchat.data.repo.command.CommandRepository
import com.flxrs.dankchat.data.repo.command.CommandResult
import com.flxrs.dankchat.data.repo.data.DataRepository
import com.flxrs.dankchat.data.twitch.badge.Badge
import com.flxrs.dankchat.preferences.DankChatPreferenceStore
import com.flxrs.dankchat.utils.DateTimeUtils.asParsedZonedDateTime
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import org.koin.core.annotation.KoinViewModel

data class UserPopupUiState(
    val id: Long,
    val popupState: UserPopupState,
    val channel: UserName?,
    val badges: List<Badge>,
    val isOwnUser: Boolean,
    val canModerate: Boolean,
    val isPinned: Boolean = false,
    // Bumping the sequence re-creates the card's popup window, moving it above the others
    val zSequence: Long = 0L,
    val offsetX: Int = 0,
    val offsetY: Int = 0,
)

@KoinViewModel
class UserPopupViewModel(
    private val channelRepository: ChannelRepository,
    private val dataRepository: DataRepository,
    private val ivrApiClient: IvrApiClient,
    private val ignoresRepository: IgnoresRepository,
    private val userStateRepository: UserStateRepository,
    private val preferenceStore: DankChatPreferenceStore,
    private val chatRepository: ChatRepository,
    private val commandRepository: CommandRepository,
) : ViewModel() {
    private val _states = MutableStateFlow<List<UserPopupUiState>>(emptyList())
    val states: StateFlow<List<UserPopupUiState>> = _states.asStateFlow()
    val isActive = _states.map { it.isNotEmpty() }

    private var nextCardId = 0L
    private var nextZSequence = 0L
    private val paramsByCardId = HashMap<Long, UserPopupStateParams>()
    private val loadJobs = HashMap<Long, Job>()

    /** Opens a card for the given user. Pinned cards stay open, only the transient (unpinned) card is replaced. */
    fun show(params: UserPopupStateParams) {
        _states.value.filter { !it.isPinned }.forEach { card -> removeCard(card.id) }
        val cardId = nextCardId++
        paramsByCardId[cardId] = params
        loadData(cardId, params)
    }

    fun dismiss(cardId: Long) = removeCard(cardId)

    fun togglePin(cardId: Long) = _states.update { cards ->
        cards.map { card -> if (card.id == cardId) card.copy(isPinned = !card.isPinned) else card }
    }

    /** Re-creates the card's window via a key change so it renders above all other cards. */
    fun bringToFront(cardId: Long) {
        val cards = _states.value
        val card = cards.find { it.id == cardId } ?: return
        // The card with the highest z-sequence is already on top, a re-creation would just flicker
        if (cards.all { it.id == cardId || it.zSequence < card.zSequence }) {
            return
        }
        _states.update { current ->
            current.map { if (it.id == cardId) it.copy(zSequence = nextZSequence++) else it }
        }
    }

    fun updateOffset(
        cardId: Long,
        offsetX: Int,
        offsetY: Int,
    ) = _states.update { cards ->
        cards.map { card -> if (card.id == cardId) card.copy(offsetX = offsetX, offsetY = offsetY) else card }
    }

    fun blockUser(cardId: Long) = updateStateWith(cardId) { targetUserId, targetUsername ->
        ignoresRepository.addUserBlock(targetUserId, targetUsername)
    }

    fun unblockUser(cardId: Long) = updateStateWith(cardId) { targetUserId, targetUsername ->
        ignoresRepository.removeUserBlock(targetUserId, targetUsername)
    }

    fun banUser(cardId: Long) = viewModelScope.launch {
        val name = cardSuccessState(cardId)?.userName ?: return@launch
        sendCommand(cardId, ".ban $name")
    }

    fun unbanUser(cardId: Long) = viewModelScope.launch {
        val name = cardSuccessState(cardId)?.userName ?: return@launch
        sendCommand(cardId, ".unban $name")
    }

    fun timeoutUser(
        cardId: Long,
        durationSeconds: Long,
    ) = viewModelScope.launch {
        val name = cardSuccessState(cardId)?.userName ?: return@launch
        sendCommand(cardId, ".timeout $name $durationSeconds")
    }

    private fun cardSuccessState(cardId: Long): UserPopupState.Success? = _states.value.firstOrNull { it.id == cardId }?.popupState as? UserPopupState.Success

    private fun removeCard(cardId: Long) {
        loadJobs.remove(cardId)?.cancel()
        paramsByCardId.remove(cardId)
        _states.update { cards -> cards.filterNot { it.id == cardId } }
    }

    private suspend fun sendCommand(
        cardId: Long,
        message: String,
    ) {
        val channel = paramsByCardId[cardId]?.channel ?: return
        val roomState = channelRepository.getRoomState(channel) ?: return
        val userState = userStateRepository.userState.value
        val result =
            runCatching {
                commandRepository.checkForCommands(message, channel, roomState, userState)
            }.getOrNull() ?: return

        when (result) {
            is CommandResult.IrcCommand -> chatRepository.sendMessage(message, forceIrc = true)
            is CommandResult.AcceptedTwitchCommand -> result.response?.let { chatRepository.makeAndPostCustomSystemMessage(it, channel) }
            else -> Unit
        }
    }

    private fun emitState(
        cardId: Long,
        params: UserPopupStateParams,
        popupState: UserPopupState,
    ) {
        _states.update { cards ->
            val existing = cards.find { it.id == cardId }
            when (existing) {
                null ->
                    cards + UserPopupUiState(
                        id = cardId,
                        popupState = popupState,
                        channel = params.channel,
                        badges = params.badges,
                        isOwnUser = params.targetUserId != null && preferenceStore.userIdString == params.targetUserId,
                        canModerate = params.channel != null && userStateRepository.isModeratorInChannel(params.channel),
                        zSequence = nextZSequence++,
                    )

                // Data refreshes keep the card's pin, position and z-order
                else -> cards.map { card -> if (card.id == cardId) card.copy(popupState = popupState) else card }
            }
        }
    }

    private inline fun updateStateWith(
        cardId: Long,
        crossinline block: suspend (targetUserId: UserId, targetUsername: UserName) -> Unit,
    ) {
        val params = paramsByCardId[cardId] ?: return
        viewModelScope.launch {
            if (!preferenceStore.isLoggedIn) {
                return@launch
            }

            val resolvedUserId = cardSuccessState(cardId)?.userId ?: params.targetUserId ?: return@launch
            val result = runCatching { block(resolvedUserId, params.targetUserName) }
            when {
                result.isFailure -> emitState(cardId, params, UserPopupState.Error(result.exceptionOrNull()))
                else -> loadData(cardId, params)
            }
        }
    }

    private fun loadData(
        cardId: Long,
        params: UserPopupStateParams,
    ) {
        loadJobs[cardId]?.cancel()
        val cachedUser = params.targetUserId?.let { channelRepository.getCachedUserDto(it) }
        val initialState = when (cachedUser) {
            null -> UserPopupState.Loading(params.targetUserName, params.targetDisplayName)

            else -> UserPopupState.Success(
                userId = cachedUser.id,
                userName = cachedUser.name,
                displayName = cachedUser.displayName,
                avatarUrl = cachedUser.avatarUrl,
                offlineImageUrl = cachedUser.offlineImageUrl.ifBlank { null },
                created = cachedUser.createdAt.asParsedZonedDateTime(),
            )
        }
        emitState(cardId, params, initialState)

        loadJobs[cardId] = viewModelScope.launch {
            val currentUserId = preferenceStore.userIdString
            if (!preferenceStore.isLoggedIn || currentUserId == null) {
                emitState(cardId, params, UserPopupState.NotLoggedIn(params.targetUserName, params.targetDisplayName))
                return@launch
            }

            val result =
                runCatching {
                    val user = when {
                        params.targetUserId != null -> channelRepository.getUserDto(params.targetUserId)
                        else -> channelRepository.getUserDtoByName(params.targetUserName)
                    }

                    val resolvedUserId = user?.id ?: params.targetUserId ?: UserId("")
                    val channelId = params.channel?.let { channelRepository.getChannel(it)?.id }
                    val isBlocked = ignoresRepository.isUserBlocked(resolvedUserId)
                    val canLoadFollows = channelId != resolvedUserId && (currentUserId == channelId || userStateRepository.isModeratorInChannel(params.channel))
                    val channelUserFollows = channelId?.takeIf { canLoadFollows }?.let { dataRepository.getChannelFollowers(channelId, resolvedUserId) }

                    user ?: return@runCatching UserPopupState.Error()

                    // Profile banner and follower count are not part of the Helix user response, ivr.fi provides both
                    val ivrUser = ivrApiClient.getUser(user.name).getOrNull()
                    // Subscription info is channel-specific and hidden when the user chose to hide it
                    val subage =
                        params.channel
                            ?.takeIf { it != user.name }
                            ?.let { ivrApiClient.getSubAge(user.name, it).getOrNull() }
                            ?.takeIf { !it.statusHidden }
                    val subscriptionTier = subage?.meta?.tier
                    val subscriptionMonths = subage?.cumulative?.months

                    UserPopupState.Success(
                        userId = user.id,
                        userName = user.name,
                        displayName = user.displayName,
                        avatarUrl = user.avatarUrl,
                        offlineImageUrl = user.offlineImageUrl.ifBlank { null },
                        bannerUrl = ivrUser?.banner?.takeIf { it.isNotBlank() },
                        followerCount = ivrUser?.followers,
                        subscriptionTier = subscriptionTier,
                        subscriptionMonths = subscriptionMonths,
                        created = user.createdAt.asParsedZonedDateTime(),
                        showFollowingSince = canLoadFollows,
                        followingSince = channelUserFollows
                            ?.data
                            ?.firstOrNull()
                            ?.followedAt
                            ?.asParsedZonedDateTime(),
                        isBlocked = isBlocked,
                    )
                }

            val popupState = result.getOrElse { UserPopupState.Error(it) }
            emitState(cardId, params, popupState)
        }
    }
}
