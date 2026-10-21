package dev.forcetower.unes.ui.feature.commute

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.commuteDataStore by preferencesDataStore(name = "commute_preferences")

// The Configurações switch that keeps Meu trajeto off Hoje for students who
// don't take the bus (iOS `CommuteHeroFeature.onHomeKey`). Device-local by
// design, like `ThemePreferenceStore`: the saved plan itself lives on the
// server, this only decides whether Hoje shows it.
@Singleton
internal class CommutePreferenceStore @Inject constructor(
    @ApplicationContext context: Context,
) {
    private val dataStore = context.commuteDataStore

    val onHome: Flow<Boolean> = dataStore.data.map { preferences ->
        preferences[onHomeKey] ?: true
    }

    suspend fun setOnHome(enabled: Boolean) {
        dataStore.edit { preferences -> preferences[onHomeKey] = enabled }
    }

    private companion object {
        val onHomeKey = booleanPreferencesKey("commute_on_home")
    }
}
