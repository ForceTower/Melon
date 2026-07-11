package dev.forcetower.melon.feature.overview.domain.usecase

import dev.forcetower.melon.core.common.ForegroundSignal
import dev.forcetower.melon.core.common.parseHhMm
import dev.forcetower.melon.core.common.tickerFlow
import dev.forcetower.melon.core.common.toUpstreamDay
import dev.forcetower.melon.core.database.dao.AcademicDao
import dev.forcetower.melon.core.database.dao.SemesterDao
import dev.forcetower.melon.core.database.query.SemesterAllocationRow
import dev.forcetower.melon.feature.overview.domain.internal.pickActiveSemester
import dev.forcetower.melon.feature.overview.domain.model.OverviewTomorrowPreview
import dev.zacsweers.metro.Inject
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.datetime.DatePeriod
import kotlinx.datetime.LocalDate
import kotlinx.datetime.plus

// Tomorrow's first class + how many more follow it. Feeds the hero's
// "Dia concluído" state once every class today has wrapped.
@OptIn(ExperimentalCoroutinesApi::class)
@Inject
class ObserveTomorrowPreviewUseCase internal constructor(
    private val semesterDao: SemesterDao,
    private val academicDao: AcademicDao,
    private val foreground: ForegroundSignal,
) {
    operator fun invoke(): Flow<OverviewTomorrowPreview?> {
        val keyFlow: Flow<Pair<String, String>?> = combine(
            semesterDao.observeAll(),
            tickerFlow(30_000, foreground.pulses),
        ) { semesters, now ->
            val dateIso = now.date.toString()
            pickActiveSemester(semesters, dateIso)?.let { it.id to dateIso }
        }.distinctUntilChanged()

        return keyFlow.flatMapLatest { key ->
            if (key == null) return@flatMapLatest flowOf(null)
            academicDao.observeSemesterAllocations(key.first)
                .map { allocations -> buildTomorrowPreview(allocations, LocalDate.parse(key.second)) }
        }.distinctUntilChanged()
    }
}

internal fun buildTomorrowPreview(
    allocations: List<SemesterAllocationRow>,
    today: LocalDate,
): OverviewTomorrowPreview? {
    val tomorrowDay = today.plus(DatePeriod(days = 1)).dayOfWeek.toUpstreamDay()
    val rows = allocations
        .filter { it.day == tomorrowDay && parseHhMm(it.startTime) != null }
        .sortedBy { parseHhMm(it.startTime) ?: Int.MAX_VALUE }
    val first = rows.firstOrNull() ?: return null
    return OverviewTomorrowPreview(
        code = first.disciplineCode,
        title = first.disciplineName,
        startTime = first.startTime.orEmpty(),
        roomLocation = first.spaceLocation,
        extraCount = rows.size - 1,
    )
}
