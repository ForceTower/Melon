package dev.forcetower.unes.commute

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json

private val Context.commuteLiveDataStore by preferencesDataStore(name = "commute_live_update")

@Singleton
internal class CommuteLiveUpdateStore @Inject constructor(
    @ApplicationContext context: Context,
) {
    private val dataStore = context.commuteLiveDataStore
    private val json = Json { ignoreUnknownKeys = true }

    val record: Flow<CommuteLiveRecord?> = dataStore.data.map { preferences -> preferences[recordKey]?.let(::decode) }

    suspend fun current(): CommuteLiveRecord? = record.first()

    suspend fun save(record: CommuteLiveRecord) {
        dataStore.edit { preferences -> preferences[recordKey] = json.encodeToString(record) }
    }

    suspend fun clear() {
        dataStore.edit { preferences -> preferences.remove(recordKey) }
    }

    // A record an older build wrote in another shape is dropped, not crashed on.
    private fun decode(raw: String): CommuteLiveRecord? =
        try {
            json.decodeFromString<CommuteLiveRecord>(raw)
        } catch (_: SerializationException) {
            null
        } catch (_: IllegalArgumentException) {
            null
        }

    private companion object {
        val recordKey = stringPreferencesKey("record")
    }
}
