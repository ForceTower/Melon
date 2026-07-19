package dev.forcetower.unes.reminders

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.reminderDataStore by preferencesDataStore(name = "evaluation_reminder_preferences")

// The Configurações switch for evening-before evaluation reminders. Device-
// local by design, like `ThemePreferenceStore` — the alarm schedules on this
// device, so the choice never rides `user_settings`.
@Singleton
internal class EvaluationReminderPreferenceStore @Inject constructor(
    @ApplicationContext context: Context,
) {
    private val dataStore = context.reminderDataStore

    val enabled: Flow<Boolean> = dataStore.data.map { preferences ->
        preferences[enabledKey] ?: true
    }

    suspend fun set(enabled: Boolean) {
        dataStore.edit { preferences -> preferences[enabledKey] = enabled }
    }

    private companion object {
        val enabledKey = booleanPreferencesKey("evaluation_reminders_enabled")
    }
}
