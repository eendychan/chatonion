package com.flxrs.dankchat.data.repo.cosmetics

import android.content.Context
import com.flxrs.dankchat.data.twitch.badge.SevenTVBadgeCosmetic
import com.flxrs.dankchat.data.twitch.paint.SevenTVPaint
import com.flxrs.dankchat.di.DispatchersProvider
import com.flxrs.dankchat.utils.datastore.createDataStore
import com.flxrs.dankchat.utils.datastore.safeData
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.first
import kotlinx.serialization.Serializable
import org.koin.core.annotation.Single

@Serializable
data class CachedSevenTVCosmetics(
    val entries: List<Entry> = emptyList(),
) {
    @Serializable
    data class Entry(
        val userId: String,
        val userName: String,
        val fetchedAt: Long,
        val paint: CachedPaint? = null,
        val badge: CachedBadge? = null,
    )

    @Serializable
    data class CachedPaint(
        val id: String,
        val name: String,
        val color: Int?,
        val function: String,
        val repeat: Boolean,
        val angleDegrees: Float,
        val stops: List<CachedStop>,
        val shadows: List<CachedShadow>,
        val imageUrl: String? = null,
    )

    @Serializable
    data class CachedStop(
        val at: Float,
        val argb: Int,
    )

    @Serializable
    data class CachedShadow(
        val xOffset: Float,
        val yOffset: Float,
        val radius: Float,
        val argb: Int,
    )

    @Serializable
    data class CachedBadge(
        val id: String,
        val name: String,
        val tooltip: String?,
        val imageUrl: String,
    )
}

fun SevenTVPaint.toCached(): CachedSevenTVCosmetics.CachedPaint = CachedSevenTVCosmetics.CachedPaint(
    id = id,
    name = name,
    color = color,
    function = function.name,
    repeat = repeat,
    angleDegrees = angleDegrees,
    stops = stops.map { CachedSevenTVCosmetics.CachedStop(at = it.at, argb = it.argb) },
    shadows = shadows.map { CachedSevenTVCosmetics.CachedShadow(xOffset = it.xOffset, yOffset = it.yOffset, radius = it.radius, argb = it.argb) },
    imageUrl = imageUrl,
)

fun CachedSevenTVCosmetics.CachedPaint.toDomain(): SevenTVPaint? {
    val paintFunction =
        runCatching { SevenTVPaint.PaintFunction.valueOf(function) }.getOrNull() ?: return null
    return SevenTVPaint(
        id = id,
        name = name,
        color = color,
        function = paintFunction,
        repeat = repeat,
        angleDegrees = angleDegrees,
        stops = stops.map { SevenTVPaint.Stop(at = it.at, argb = it.argb) },
        shadows = shadows.map { SevenTVPaint.Shadow(xOffset = it.xOffset, yOffset = it.yOffset, radius = it.radius, argb = it.argb) },
        imageUrl = imageUrl,
    )
}

fun SevenTVBadgeCosmetic.toCached(): CachedSevenTVCosmetics.CachedBadge = CachedSevenTVCosmetics.CachedBadge(id = id, name = name, tooltip = tooltip, imageUrl = imageUrl)

fun CachedSevenTVCosmetics.CachedBadge.toDomain(): SevenTVBadgeCosmetic = SevenTVBadgeCosmetic(id = id, name = name, tooltip = tooltip, imageUrl = imageUrl)

/**
 * Persistent cache of 7TV user cosmetics (paints/badges), so they survive app restarts
 * and don't have to be refetched for every chatter on every launch. Written only while
 * the corresponding setting is enabled.
 */
@Single
class SevenTVCosmeticsCacheDataStore(
    context: Context,
    dispatchersProvider: DispatchersProvider,
) {
    private val dataStore =
        createDataStore(
            fileName = "seventv_cosmetics_cache",
            context = context,
            defaultValue = CachedSevenTVCosmetics(),
            serializer = CachedSevenTVCosmetics.serializer(),
            scope = CoroutineScope(dispatchersProvider.io + SupervisorJob()),
        )

    suspend fun load(): CachedSevenTVCosmetics = dataStore.safeData(CachedSevenTVCosmetics()).first()

    /** Merges freshly fetched entries by user id, keeping the newest [MAX_ENTRIES]. */
    suspend fun merge(newEntries: List<CachedSevenTVCosmetics.Entry>) {
        if (newEntries.isEmpty()) {
            return
        }
        runCatching {
            dataStore.updateData { current ->
                val merged =
                    (current.entries.filter { old -> newEntries.none { it.userId == old.userId } } + newEntries)
                        .sortedByDescending { it.fetchedAt }
                        .take(MAX_ENTRIES)
                current.copy(entries = merged)
            }
        }
    }

    private companion object {
        const val MAX_ENTRIES = 2_000
    }
}
