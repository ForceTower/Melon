package dev.forcetower.unes.firebase

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.stringSetPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.first

private val Context.fcmDataStore by preferencesDataStore(name = "fcm_preferences")

// Caches the FCM registration token (onNewToken / getToken) plus the
// identifiers whose backend DELETE hasn't succeeded yet. `installation_id` is
// the Firebase Installation ID cache left behind by the retired FID-targeting
// builds (same DataStore file) — it's only read so the FID row a previous app
// version registered can be deleted, then cleared for good.
@Singleton
internal class PushIdentifierStore @Inject constructor(
    @ApplicationContext context: Context,
) {
    private val dataStore = context.fcmDataStore

    suspend fun token(): String? = dataStore.data.first()[tokenKey]

    suspend fun setToken(token: String) {
        dataStore.edit { preferences -> preferences[tokenKey] = token }
    }

    suspend fun fid(): String? = dataStore.data.first()[fidKey]

    suspend fun clearFid() {
        dataStore.edit { preferences -> preferences.remove(fidKey) }
    }

    suspend fun pendingDeletes(): Set<String> = dataStore.data.first()[pendingDeletesKey].orEmpty()

    suspend fun addPendingDelete(identifier: String) {
        dataStore.edit { preferences ->
            preferences[pendingDeletesKey] = preferences[pendingDeletesKey].orEmpty() + identifier
        }
    }

    suspend fun removePendingDelete(identifier: String) {
        dataStore.edit { preferences ->
            preferences[pendingDeletesKey] = preferences[pendingDeletesKey].orEmpty() - identifier
        }
    }

    suspend fun clear() {
        dataStore.edit { preferences -> preferences.clear() }
    }

    private companion object {
        val tokenKey = stringPreferencesKey("registration_token")
        val fidKey = stringPreferencesKey("installation_id")
        val pendingDeletesKey = stringSetPreferencesKey("pending_deletes")
    }
}
