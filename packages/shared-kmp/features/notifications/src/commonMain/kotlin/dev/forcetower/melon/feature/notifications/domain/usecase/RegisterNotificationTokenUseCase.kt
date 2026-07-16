package dev.forcetower.melon.feature.notifications.domain.usecase

import dev.forcetower.melon.core.common.Outcome
import dev.forcetower.melon.feature.notifications.data.dto.RegisterNotificationTokenRequest
import dev.forcetower.melon.feature.notifications.domain.model.NotificationTokenError
import dev.forcetower.melon.feature.notifications.domain.repository.NotificationTokenRepository
import dev.zacsweers.metro.Inject

@Inject
class RegisterNotificationTokenUseCase internal constructor(
    private val repository: NotificationTokenRepository,
) {
    suspend operator fun invoke(
        token: String,
        identifierType: String,
        platform: String,
        deviceName: String? = null,
        appVersion: String? = null,
        locale: String? = null,
    ): Outcome<Unit, NotificationTokenError> =
        repository.register(
            RegisterNotificationTokenRequest(
                token = token,
                identifierType = identifierType,
                platform = platform,
                deviceName = deviceName,
                appVersion = appVersion,
                locale = locale,
            ),
        )
}
