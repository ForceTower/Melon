package dev.forcetower.melon.feature.settings.domain.usecase

import dev.forcetower.melon.feature.settings.domain.model.UserSettings
import dev.forcetower.melon.feature.settings.domain.repository.SettingsRepository
import dev.zacsweers.metro.Inject
import kotlinx.coroutines.flow.Flow

@Inject
class ObserveSettingsUseCase internal constructor(
    private val repository: SettingsRepository,
) {
    operator fun invoke(): Flow<UserSettings?> = repository.observe()
}
