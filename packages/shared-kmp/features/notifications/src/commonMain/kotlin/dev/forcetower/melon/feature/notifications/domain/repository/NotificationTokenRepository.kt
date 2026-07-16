package dev.forcetower.melon.feature.notifications.domain.repository

import dev.forcetower.melon.core.common.Outcome
import dev.forcetower.melon.feature.notifications.data.dto.RegisterNotificationTokenRequest
import dev.forcetower.melon.feature.notifications.data.dto.UnregisterNotificationTokenRequest
import dev.forcetower.melon.feature.notifications.domain.model.NotificationTokenError

internal interface NotificationTokenRepository {
    suspend fun register(request: RegisterNotificationTokenRequest): Outcome<Unit, NotificationTokenError>

    suspend fun unregister(request: UnregisterNotificationTokenRequest): Outcome<Unit, NotificationTokenError>
}
