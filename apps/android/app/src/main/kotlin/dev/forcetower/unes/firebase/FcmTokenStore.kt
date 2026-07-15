package dev.forcetower.unes.firebase

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.first

private val Context.fcmDataStore by preferencesDataStore(name = "fcm_preferences")

// The FCM registration token now only arrives via onNewToken (getToken() was
// deprecated in firebase-messaging 25.1.0). We persist it here on receipt so
// deferred consumers (e.g. post-login sync) can read the last known token
// without a live SDK fetch.
@Singleton
internal class FcmTokenStore @Inject constructor(
    @ApplicationContext context: Context,
) {
    private val dataStore = context.fcmDataStore

    suspend fun token(): String? = dataStore.data.first()[tokenKey]

    suspend fun setToken(token: String) {
        dataStore.edit { preferences -> preferences[tokenKey] = token }
    }

    private companion object {
        val tokenKey = stringPreferencesKey("registration_token")
    }
}
