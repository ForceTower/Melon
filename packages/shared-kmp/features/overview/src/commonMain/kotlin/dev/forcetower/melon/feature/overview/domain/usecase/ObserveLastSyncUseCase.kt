package dev.forcetower.melon.feature.overview.domain.usecase

import dev.forcetower.melon.core.database.dao.StudentDao
import dev.zacsweers.metro.Inject
import kotlinx.coroutines.flow.Flow

// Raw ISO-8601 string of the most recent upstream fetch the server completed
// for this student. ViewModel formats "há X min". Null until the first sync.
@Inject
class ObserveLastSyncUseCase internal constructor(
    private val studentDao: StudentDao,
) {
    operator fun invoke(): Flow<String?> = studentDao.observeLastSyncCompletedAt()
}
