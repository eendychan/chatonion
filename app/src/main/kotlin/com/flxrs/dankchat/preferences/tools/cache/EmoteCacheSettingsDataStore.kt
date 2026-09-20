package com.flxrs.dankchat.preferences.tools.cache

import android.content.Context
import com.flxrs.dankchat.di.DispatchersProvider
import com.flxrs.dankchat.utils.datastore.createDataStore
import com.flxrs.dankchat.utils.datastore.safeData
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.runBlocking
import org.koin.core.annotation.Single

@Single
class EmoteCacheSettingsDataStore(
    context: Context,
    dispatchersProvider: DispatchersProvider,
) {
    private val dataStore =
        createDataStore(
            // must not collide with the "emote_cache" directory used by EmoteDiskCache
            fileName = "emote_cache_settings",
            context = context,
            defaultValue = EmoteCacheSettings(),
            serializer = EmoteCacheSettings.serializer(),
            scope = CoroutineScope(dispatchersProvider.io + SupervisorJob()),
        )

    val settings = dataStore.safeData(EmoteCacheSettings())
    val currentSettings =
        settings.stateIn(
            scope = CoroutineScope(dispatchersProvider.io),
            started = SharingStarted.Eagerly,
            initialValue = runBlocking { settings.first() },
        )

    fun current() = currentSettings.value

    suspend fun update(transform: suspend (EmoteCacheSettings) -> EmoteCacheSettings) {
        runCatching { dataStore.updateData(transform) }
    }
}
