package dev.forcetower.unes.review

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringSetPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.first

private val Context.reviewDataStore by preferencesDataStore(name = "in_app_review")

@Singleton
internal class ReviewPreferenceStore @Inject constructor(
    @ApplicationContext context: Context,
) {
    private val dataStore = context.reviewDataStore

    suspend fun read(): ReviewState {
        val preferences = dataStore.data.first()
        return ReviewState(
            activeDays = preferences[activeDaysKey] ?: 0,
            lastActiveDay = preferences[lastActiveDayKey] ?: 0L,
            lastPromptedAtMs = preferences[lastPromptedAtKey] ?: 0L,
            lastTroubleAtMs = preferences[lastTroubleAtKey] ?: 0L,
        )
    }

    suspend fun noteActiveDay(epochDay: Long) {
        dataStore.edit { preferences ->
            if (preferences[lastActiveDayKey] == epochDay) return@edit
            preferences[lastActiveDayKey] = epochDay
            preferences[activeDaysKey] = (preferences[activeDaysKey] ?: 0) + 1
        }
    }

    suspend fun recordPrompt(nowMs: Long) {
        dataStore.edit { preferences -> preferences[lastPromptedAtKey] = nowMs }
    }

    suspend fun recordTrouble(nowMs: Long) {
        dataStore.edit { preferences -> preferences[lastTroubleAtKey] = nowMs }
    }

    // True only the first time this offer is seen as passed, so scrolling back
    // through a closed semester doesn't read as fresh news.
    suspend fun markCelebrated(offerId: String): Boolean {
        var fresh = false
        dataStore.edit { preferences ->
            val seen = preferences[celebratedOffersKey] ?: emptySet()
            fresh = offerId !in seen
            if (fresh) preferences[celebratedOffersKey] = seen + offerId
        }
        return fresh
    }

    private companion object {
        val activeDaysKey = intPreferencesKey("active_days")
        val lastActiveDayKey = longPreferencesKey("last_active_day")
        val lastPromptedAtKey = longPreferencesKey("last_prompted_at_ms")
        val lastTroubleAtKey = longPreferencesKey("last_trouble_at_ms")
        val celebratedOffersKey = stringSetPreferencesKey("celebrated_offers")
    }
}
