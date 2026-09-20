package com.flxrs.dankchat.data.repo.cosmetics

import com.flxrs.dankchat.data.UserId
import com.flxrs.dankchat.data.UserName
import com.flxrs.dankchat.data.twitch.badge.SevenTVBadgeCosmetic
import com.flxrs.dankchat.data.twitch.paint.SevenTVPaint
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import org.koin.core.annotation.Single
import java.util.concurrent.ConcurrentHashMap

/**
 * Stores 7TV name paints and badges (cosmetics) received via the 7TV EventAPI.
 *
 * Cosmetic definitions (paints/badges) are globally known by their id,
 * entitlements assign them to users. Paint assignments are keyed by the
 * lowercase Twitch user name, badge assignments by the Twitch user id,
 * matching Chatterino7's behavior.
 */
@Single
class SevenTVCosmeticsRepository {
    private val knownPaints = ConcurrentHashMap<String, SevenTVPaint>()
    private val knownBadges = ConcurrentHashMap<String, SevenTVBadgeCosmetic>()
    private val paintAssignments = ConcurrentHashMap<UserName, String>()
    private val badgeAssignments = ConcurrentHashMap<UserId, String>()

    private val _updates = MutableSharedFlow<Unit>(extraBufferCapacity = 1)

    /** Emits whenever paint or badge assignments change, so visible messages can be reparsed. */
    val updates: SharedFlow<Unit> = _updates.asSharedFlow()

    fun addPaint(paint: SevenTVPaint) {
        knownPaints[paint.id] = paint
    }

    fun addBadge(badge: SevenTVBadgeCosmetic) {
        knownBadges[badge.id] = badge
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
}
