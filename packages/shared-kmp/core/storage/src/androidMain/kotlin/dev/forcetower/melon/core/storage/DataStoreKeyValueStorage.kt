package dev.forcetower.melon.core.storage

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import dev.forcetower.melon.core.common.ApplicationContext
import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.ContributesTo
import dev.zacsweers.metro.Provides
import dev.zacsweers.metro.SingleIn
import kotlinx.coroutines.flow.first

private const val DATASTORE_NAME = "melon.kv"

private val Context.melonDataStore: DataStore<Preferences> by preferencesDataStore(name = DATASTORE_NAME)

internal class DataStoreKeyValueStorage(
    private val dataStore: DataStore<Preferences>,
) : KeyValueStorage {

    override suspend fun get(key: String): String? =
        dataStore.data.first()[stringPreferencesKey(key)]

    override suspend fun put(key: String, value: String) {
        dataStore.edit { it[stringPreferencesKey(key)] = value }
    }

    override suspend fun remove(key: String) {
        dataStore.edit { it.remove(stringPreferencesKey(key)) }
    }
}

@ContributesTo(AppScope::class)
interface AndroidStorageGraph {
    companion object {
        @Provides
        @SingleIn(AppScope::class)
        fun keyValueStorage(appContext: ApplicationContext): KeyValueStorage =
            DataStoreKeyValueStorage(appContext.context.melonDataStore)
    }
}
