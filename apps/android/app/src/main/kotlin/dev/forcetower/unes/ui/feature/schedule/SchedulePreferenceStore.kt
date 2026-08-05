package dev.forcetower.unes.ui.feature.schedule

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.scheduleDataStore by preferencesDataStore(name = "schedule_preferences")

// The Configurações switch that swaps the Horário tab between the day
// timeline (`ScheduleScreen`) and the week grid (`ScheduleGridScreen`).
// Device-local by design, like `ThemePreferenceStore` — a pure presentation
// choice that never rides `user_settings`.
@Singleton
internal class SchedulePreferenceStore @Inject constructor(
    @ApplicationContext context: Context,
) {
    private val dataStore = context.scheduleDataStore

    val gridEnabled: Flow<Boolean> = dataStore.data.map { preferences ->
        preferences[gridKey] ?: false
    }

    suspend fun setGridEnabled(enabled: Boolean) {
        dataStore.edit { preferences -> preferences[gridKey] = enabled }
    }

    private companion object {
        val gridKey = booleanPreferencesKey("schedule_grid_enabled")
    }
}
