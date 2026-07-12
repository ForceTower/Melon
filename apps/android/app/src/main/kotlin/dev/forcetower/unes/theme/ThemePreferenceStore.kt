package dev.forcetower.unes.theme

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

// How the app resolves light/dark (dc `SettingsScreen` "Tema" segmented
// control). Device-local by design — unlike `user_settings` this never syncs:
// the same account on phone and tablet can disagree about darkness.
internal enum class ThemeMode(val storageKey: String) {
    Light("light"),
    System("system"),
    Dark("dark");

    companion object {
        fun fromStorageKey(value: String?): ThemeMode =
            entries.firstOrNull { it.storageKey == value } ?: System
    }
}

private val Context.themeDataStore by preferencesDataStore(name = "theme_preferences")

@Singleton
internal class ThemePreferenceStore @Inject constructor(
    @ApplicationContext context: Context,
) {
    private val dataStore = context.themeDataStore

    val mode: Flow<ThemeMode> = dataStore.data.map { preferences ->
        ThemeMode.fromStorageKey(preferences[themeModeKey])
    }

    suspend fun set(mode: ThemeMode) {
        dataStore.edit { preferences -> preferences[themeModeKey] = mode.storageKey }
    }

    private companion object {
        val themeModeKey = stringPreferencesKey("theme_mode")
    }
}
