package dev.forcetower.melon.feature.me.domain.usecase

import dev.forcetower.melon.core.database.dao.CredentialsDao
import dev.forcetower.melon.core.database.entity.CredentialsEntity
import dev.forcetower.melon.core.session.domain.SessionStore
import dev.forcetower.melon.feature.me.domain.model.UserCredentials
import dev.zacsweers.metro.Inject
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine

// Read-side surface for the upstream portal credentials. Backed directly by
// `CredentialsDao` so callers — Settings today, future silent-reauth flows
// tomorrow — don't have to depend on the Room entity. The row only exists on
// a device that signed in with the portal login; otherwise the username polled
// from `api/me/status` stands in, and `null` means neither is known yet.
@Inject
class ObserveCurrentCredentialsUseCase internal constructor(
    private val credentialsDao: CredentialsDao,
    private val sessionStore: SessionStore,
) {
    operator fun invoke(): Flow<UserCredentials?> =
        combine(credentialsDao.observeCurrent(), sessionStore.upstreamUsername) { row, username ->
            row?.toDomain() ?: username?.let { UserCredentials(username = it, password = null) }
        }
}

private fun CredentialsEntity.toDomain() = UserCredentials(username = username, password = password)
