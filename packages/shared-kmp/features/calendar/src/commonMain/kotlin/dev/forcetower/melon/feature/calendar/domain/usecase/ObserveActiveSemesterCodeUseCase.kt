package dev.forcetower.melon.feature.calendar.domain.usecase

import dev.forcetower.melon.core.database.dao.SemesterDao
import dev.forcetower.melon.core.database.entity.SemesterEntity
import dev.zacsweers.metro.Inject
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlin.time.Clock
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime

@Inject
class ObserveActiveSemesterCodeUseCase internal constructor(
    private val semesterDao: SemesterDao,
) {
    operator fun invoke(): Flow<String?> = semesterDao.observeAll().map { all ->
        val today = Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault()).date.toString()
        pickActiveSemester(all, today)?.code
    }

    // Same active-semester rule used in ObserveMeProfileUseCase / disciplines:
    // today inside [startDate, endDate]; otherwise fall back to the most
    // recently started semester. Duplicated by design — a 2-line helper isn't
    // worth coupling feature modules.
    private fun pickActiveSemester(all: List<SemesterEntity>, todayIso: String): SemesterEntity? {
        if (all.isEmpty()) return null
        return all.firstOrNull { it.startDate <= todayIso && todayIso <= it.endDate }
            ?: all.maxByOrNull { it.startDate }
    }
}
