package dev.forcetower.melon.feature.me.domain.usecase

import dev.forcetower.melon.core.database.dao.CredentialsDao
import dev.forcetower.melon.core.database.entity.CredentialsEntity
import dev.forcetower.melon.feature.me.domain.model.UserCredentials
import dev.zacsweers.metro.Inject
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

// Read-side surface for the upstream portal credentials. Backed directly by
// `CredentialsDao` so callers — Settings today, future silent-reauth flows
// tomorrow — don't have to depend on the Room entity. Emits `null` until the
// user logs in for the first time after install (or after destructive
// migration on the v4 → v5 bump).
@Inject
class ObserveCurrentCredentialsUseCase internal constructor(
    private val credentialsDao: CredentialsDao,
) {
    operator fun invoke(): Flow<UserCredentials?> =
        credentialsDao.observeCurrent().map { it?.toDomain() }
}

private fun CredentialsEntity.toDomain() =
    UserCredentials(username = username, password = password)
