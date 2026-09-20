package com.flxrs.dankchat.preferences.donations

import android.content.Context
import com.flxrs.dankchat.di.DispatchersProvider
import com.flxrs.dankchat.utils.datastore.createDataStore
import com.flxrs.dankchat.utils.datastore.safeData
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.runBlocking
import org.koin.core.annotation.Single

@Single
class DonationSettingsDataStore(
    context: Context,
    dispatchersProvider: DispatchersProvider,
) {
    private val dataStore =
        createDataStore(
            fileName = "donations",
            context = context,
            defaultValue = DonationSettings(),
            serializer = DonationSettings.serializer(),
            scope = CoroutineScope(dispatchersProvider.io + SupervisorJob()),
        )

    val settings = dataStore.safeData(DonationSettings())
    val currentSettings =
        settings.stateIn(
            scope = CoroutineScope(dispatchersProvider.io),
            started = SharingStarted.Eagerly,
            initialValue = runBlocking { settings.first() },
        )

    val configuredWidgets =
        settings
            .map { it.configuredWidgets }
            .distinctUntilChanged()

    val hasConfiguredWidgets =
        settings
            .map { it.configuredWidgets.isNotEmpty() }
            .distinctUntilChanged()

    fun current() = currentSettings.value

    suspend fun update(transform: suspend (DonationSettings) -> DonationSettings) {
        runCatching { dataStore.updateData(transform) }
    }
}
