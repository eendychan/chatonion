package com.flxrs.dankchat.data.repo.cosmetics

import com.flxrs.dankchat.data.UserId
import com.flxrs.dankchat.data.UserName
import com.flxrs.dankchat.data.api.seventv.SevenTVApiClient
import com.flxrs.dankchat.data.api.seventv.SevenTVUserCosmetics
import com.flxrs.dankchat.data.twitch.badge.SevenTVBadgeCosmetic
import com.flxrs.dankchat.data.twitch.paint.SevenTVPaint
import com.flxrs.dankchat.di.DispatchersProvider
import com.flxrs.dankchat.preferences.tools.cache.EmoteCacheSettingsDataStore
import io.github.oshai.kotlinlogging.KotlinLogging
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull
import org.koin.core.annotation.Single
import java.util.concurrent.ConcurrentHashMap

private val logger = KotlinLogging.logger("SevenTVCosmeticsRepository")

/**
 * Stores 7TV name paints and badges (cosmetics) received via the 7TV EventAPI.
 *
 * Cosmetic definitions (paints/badges) are globally known by their id,
 * entitlements assign them to users. Paint assignments are keyed by the
 * lowercase Twitch user name, badge assignments by the Twitch user id,
 * matching Chatterino7's behavior.
 *
 * The EventAPI only pushes cosmetics of users with an active 7TV presence. To cover
 * everyone else, cosmetics are additionally fetched on demand via the GraphQL API
 * for each chatter that appears in chat ([requestUserCosmetics]).
 */
@Single
class SevenTVCosmeticsRepository(
    private val sevenTVApiClient: SevenTVApiClient,
    private val emoteCacheSettingsDataStore: EmoteCacheSettingsDataStore,
    private val cacheDataStore: SevenTVCosmeticsCacheDataStore,
    dispatchersProvider: DispatchersProvider,
) {
    private val scope = CoroutineScope(SupervisorJob() + dispatchersProvider.io)
    private val knownPaints = ConcurrentHashMap<String, SevenTVPaint>()
    private val knownBadges = ConcurrentHashMap<String, SevenTVBadgeCosmetic>()
    private val paintAssignments = ConcurrentHashMap<UserName, String>()
    private val badgeAssignments = ConcurrentHashMap<UserId, String>()

    private val pendingCosmeticRequests = Channel<Pair<UserId, UserName>>(capacity = Channel.UNLIMITED)

    /** Users whose cosmetics were already fetched this session (also negative results). */
    private val fetchedUsers = ConcurrentHashMap.newKeySet<UserId>()

    /** Last failed fetch attempt per user, retried after [RETRY_COOLDOWN_MS]. */
    private val failedLookups = ConcurrentHashMap<UserId, Long>()

    private val _updates = MutableSharedFlow<Unit>(extraBufferCapacity = 1)

    /** Emits whenever paint or badge assignments change, so visible messages can be reparsed. */
    val updates: SharedFlow<Unit> = _updates.asSharedFlow()

    init {
        scope.launch {
            restoreFromCache()
        }
        scope.launch {
            while (isActive) {
                val batch = mutableListOf(pendingCosmeticRequests.receive())
                // Collect more requests for a short window so busy chats are batched into one query
                withTimeoutOrNull(BATCH_WINDOW_MS) {
                    while (batch.size < BATCH_SIZE) {
                        batch += pendingCosmeticRequests.receive()
                    }
                }
                fetchCosmeticsBatch(batch.distinctBy { (userId, _) -> userId })
            }
        }
    }

    /**
     * Requests the active cosmetics of a chatter, applied to their messages once loaded.
     * Deduplicated per user and session, failures are retried with a cooldown.
     */
    fun requestUserCosmetics(
        userId: UserId,
        userName: UserName,
    ) {
        if (userId.value.isBlank() || userName.value.isBlank() || userId in fetchedUsers) {
            return
        }

        val failedAt = failedLookups[userId]
        if (failedAt != null && System.currentTimeMillis() - failedAt < RETRY_COOLDOWN_MS) {
            return
        }

        if (fetchedUsers.add(userId)) {
            pendingCosmeticRequests.trySend(userId to userName)
        }
    }

    private suspend fun fetchCosmeticsBatch(users: List<Pair<UserId, UserName>>) {
        if (users.isEmpty()) {
            return
        }

        sevenTVApiClient
            .getUserCosmetics(users.map { (userId, _) -> userId })
            .onSuccess { cosmeticsByUser ->
                users.forEach { (userId, userName) ->
                    val cosmetics = cosmeticsByUser[userId] ?: return@forEach
                    cosmetics.paint?.let { paint ->
                        addPaint(paint)
                        assignPaint(userName, paint.id)
                    }
                    cosmetics.badge?.let { badge ->
                        addBadge(badge)
                        assignBadge(userId, badge.id)
                    }
                }
                persistFetched(users, cosmeticsByUser)
            }.onFailure { throwable ->
                logger.debug(throwable) { "Failed to fetch 7TV user cosmetics" }
                users.forEach { (userId, _) ->
                    fetchedUsers.remove(userId)
                    failedLookups[userId] = System.currentTimeMillis()
                }
            }
    }

    fun addPaint(paint: SevenTVPaint) {
        val isNew = knownPaints.put(paint.id, paint) == null
        // Entitlement replays can arrive before the cosmetic itself, so a late paint
        // definition must also refresh the visible messages that reference it
        if (isNew) {
            _updates.tryEmit(Unit)
        }
    }

    fun addBadge(badge: SevenTVBadgeCosmetic) {
        val isNew = knownBadges.put(badge.id, badge) == null
        if (isNew) {
            _updates.tryEmit(Unit)
        }
    }

    fun assignPaint(
        userName: UserName,
        paintId: String,
    ) {
        if (paintAssignments.put(userName.lowercase(), paintId) != paintId) {
            _updates.tryEmit(Unit)
        }
    }

    fun unassignPaint(
        userName: UserName,
        paintId: String,
    ) {
        if (paintAssignments.remove(userName.lowercase(), paintId)) {
            _updates.tryEmit(Unit)
        }
    }

    fun assignBadge(
        userId: UserId,
        badgeId: String,
    ) {
        if (badgeAssignments.put(userId, badgeId) != badgeId) {
            _updates.tryEmit(Unit)
        }
    }

    fun unassignBadge(
        userId: UserId,
        badgeId: String,
    ) {
        if (badgeAssignments.remove(userId, badgeId)) {
            _updates.tryEmit(Unit)
        }
    }

    fun getPaintForUser(userName: UserName): SevenTVPaint? = paintAssignments[userName.lowercase()]?.let(knownPaints::get)

    fun getBadgeForUser(userId: UserId): SevenTVBadgeCosmetic? = badgeAssignments[userId]?.let(knownBadges::get)

    /**
     * Applies cached cosmetics instantly after a restart: cached users count as already
     * fetched, so no GraphQL requests are made for them again this session.
     */
    private suspend fun restoreFromCache() {
        if (!emoteCacheSettingsDataStore.current().sevenTvCosmeticsEnabled) {
            return
        }
        val restored = runCatching { cacheDataStore.load() }.getOrNull() ?: return
        restored.entries.forEach { entry ->
            val paint = entry.paint?.toDomain()
            val badge = entry.badge?.toDomain()
            if (paint == null && badge == null) {
                return@forEach
            }
            paint?.let { knownPaints.putIfAbsent(it.id, it) }
            badge?.let { knownBadges.putIfAbsent(it.id, it) }
            if (paint != null) {
                paintAssignments.putIfAbsent(UserName(entry.userName).lowercase(), paint.id)
            }
            if (badge != null) {
                badgeAssignments.putIfAbsent(UserId(entry.userId), badge.id)
            }
            fetchedUsers += UserId(entry.userId)
        }
        if (restored.entries.isNotEmpty()) {
            _updates.tryEmit(Unit)
        }
    }

    private fun persistFetched(
        users: List<Pair<UserId, UserName>>,
        cosmeticsByUser: Map<UserId, SevenTVUserCosmetics>,
    ) {
        if (!emoteCacheSettingsDataStore.current().sevenTvCosmeticsEnabled) {
            return
        }
        val fetchedAt = System.currentTimeMillis()
        val entries =
            users.mapNotNull { (userId, userName) ->
                val cosmetics = cosmeticsByUser[userId] ?: return@mapNotNull null
                if (cosmetics.paint == null && cosmetics.badge == null) {
                    return@mapNotNull null
                }
                CachedSevenTVCosmetics.Entry(
                    userId = userId.value,
                    userName = userName.lowercase().value,
                    fetchedAt = fetchedAt,
                    paint = cosmetics.paint?.toCached(),
                    badge = cosmetics.badge?.toCached(),
                )
            }
        scope.launch { cacheDataStore.merge(entries) }
    }

    companion object {
        private const val BATCH_SIZE = 25
        private const val BATCH_WINDOW_MS = 400L
        private const val RETRY_COOLDOWN_MS = 60_000L
    }
}
