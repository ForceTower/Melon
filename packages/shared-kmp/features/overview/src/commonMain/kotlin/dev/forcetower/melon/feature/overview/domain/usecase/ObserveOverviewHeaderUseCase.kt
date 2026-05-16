package dev.forcetower.melon.feature.overview.domain.usecase

import dev.forcetower.melon.core.database.dao.UserDao
import dev.forcetower.melon.feature.overview.domain.model.OverviewHeader
import dev.zacsweers.metro.Inject
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

@Inject
class ObserveOverviewHeaderUseCase internal constructor(
    private val userDao: UserDao,
) {
    operator fun invoke(): Flow<OverviewHeader?> =
        userDao.observeCurrent().map { user ->
            user?.let { OverviewHeader(userName = it.name, avatarUrl = it.imageUrl) }
        }
}
