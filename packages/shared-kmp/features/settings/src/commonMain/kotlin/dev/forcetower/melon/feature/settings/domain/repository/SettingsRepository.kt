package dev.forcetower.melon.feature.settings.domain.repository

import dev.forcetower.melon.core.common.Outcome
import dev.forcetower.melon.feature.settings.domain.model.UserSettings
import dev.forcetower.melon.feature.settings.domain.model.UserSettingsPatch
import kotlinx.coroutines.flow.Flow

internal interface SettingsRepository {
    fun observe(): Flow<UserSettings?>

    // Optimistic write: applies the patch to the local DAO immediately,
    // then forwards it to the API. On any failure the patched fields are
    // rolled back, so observers re-emit the previous values, and the error
    // is returned for callers to surface.
    suspend fun update(patch: UserSettingsPatch): Outcome<UserSettings, SettingsError>
}

sealed interface SettingsError {
    data object NoLocalUser : SettingsError
    data object Unauthorized : SettingsError
    data object NoConnection : SettingsError
    data class Server(
        val message: String?,
    ) : SettingsError
    data object Unexpected : SettingsError
}
