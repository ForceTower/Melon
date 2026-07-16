package dev.forcetower.melon.feature.notifications.domain.usecase

import dev.forcetower.melon.core.common.Outcome
import dev.forcetower.melon.feature.notifications.data.dto.UnregisterNotificationTokenRequest
import dev.forcetower.melon.feature.notifications.domain.model.NotificationTokenError
import dev.forcetower.melon.feature.notifications.domain.repository.NotificationTokenRepository
import dev.zacsweers.metro.Inject

@Inject
class UnregisterNotificationTokenUseCase internal constructor(
    private val repository: NotificationTokenRepository,
) {
    suspend operator fun invoke(token: String): Outcome<Unit, NotificationTokenError> =
        repository.unregister(UnregisterNotificationTokenRequest(token = token))
}
