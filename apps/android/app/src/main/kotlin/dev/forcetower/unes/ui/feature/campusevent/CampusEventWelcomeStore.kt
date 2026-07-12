package dev.forcetower.unes.ui.feature.campusevent

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.campusEventDataStore by preferencesDataStore(name = "campus_event_preferences")

// Which event edition already played its fullscreen welcome reveal —
// device-local, like iOS's `@Shared(.appStorage) welcomeSeenEventId`. A new
// edition (new event id) plays the reveal again.
@Singleton
internal class CampusEventWelcomeStore @Inject constructor(
    @ApplicationContext context: Context,
) {
    private val dataStore = context.campusEventDataStore

    val seenEventId: Flow<String?> = dataStore.data.map { preferences ->
        preferences[seenEventIdKey]
    }

    suspend fun markSeen(eventId: String) {
        dataStore.edit { preferences -> preferences[seenEventIdKey] = eventId }
    }

    private companion object {
        val seenEventIdKey = stringPreferencesKey("welcome_seen_event_id")
    }
}
