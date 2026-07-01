package dev.forcetower.melon.feature.overview.domain.usecase

import dev.forcetower.melon.core.common.ForegroundSignal
import dev.forcetower.melon.core.common.parseHhMm
import dev.forcetower.melon.core.common.tickerFlow
import dev.forcetower.melon.core.common.toUpstreamDay
import dev.forcetower.melon.core.database.dao.AcademicDao
import dev.forcetower.melon.core.database.dao.SemesterDao
import dev.forcetower.melon.core.database.query.SemesterAllocationRow
import dev.forcetower.melon.core.database.query.TodayLectureRow
import dev.forcetower.melon.feature.overview.domain.internal.pickActiveSemester
import dev.forcetower.melon.feature.overview.domain.model.OverviewClassState
import dev.forcetower.melon.feature.overview.domain.model.OverviewTodayItem
import dev.zacsweers.metro.Inject
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.datetime.LocalDateTime

// Today's classes in time order, each tagged DONE / NOW / NEXT / LATER. The
// "NOW" and "NEXT" tags are time-derived so the flow combines with the ticker
// — a class flips from NEXT to NOW when its window opens even if no DB write
// has happened.
@OptIn(ExperimentalCoroutinesApi::class)
@Inject
class ObserveTodayTimelineUseCase internal constructor(
    private val semesterDao: SemesterDao,
    private val academicDao: AcademicDao,
    private val foreground: ForegroundSignal,
) {
    operator fun invoke(): Flow<List<OverviewTodayItem>> {
        val keyFlow: Flow<Pair<String, String>?> = combine(
            semesterDao.observeAll(),
            tickerFlow(30_000, foreground.pulses),
        ) { semesters, now ->
            val dateIso = now.date.toString()
            pickActiveSemester(semesters, dateIso)?.let { it.id to dateIso }
        }.distinctUntilChanged()

        return keyFlow.flatMapLatest { key ->
            if (key == null) return@flatMapLatest flowOf(emptyList())
            combine(
                academicDao.observeSemesterAllocations(key.first),
                academicDao.observeTodayLecturesForSemester(key.first, key.second),
                tickerFlow(30_000, foreground.pulses),
            ) { allocations, lectures, now -> buildTodayTimeline(allocations, lectures, now) }
                .distinctUntilChanged()
        }
    }
}

internal fun buildTodayTimeline(
    allocations: List<SemesterAllocationRow>,
    lectures: List<TodayLectureRow>,
    now: LocalDateTime,
): List<OverviewTodayItem> {
    val todayDay = now.dayOfWeek.toUpstreamDay()
    val minutesOfDay = now.hour * 60 + now.minute

    val todayRows = allocations
        .filter { it.day == todayDay && parseHhMm(it.startTime) != null }
        .sortedBy { parseHhMm(it.startTime) ?: Int.MAX_VALUE }

    // The closest row whose start hasn't happened yet is the "next" one.
    val nextStartOrNull = todayRows
        .mapNotNull { parseHhMm(it.startTime) }
        .firstOrNull { it > minutesOfDay }

    return todayRows.map { row ->
        val start = parseHhMm(row.startTime) ?: 0
        val end = parseHhMm(row.endTime)
        val state = when {
            end != null && minutesOfDay in start until end -> OverviewClassState.NOW
            end != null && minutesOfDay >= end -> OverviewClassState.DONE
            end == null && minutesOfDay >= start -> OverviewClassState.DONE
            nextStartOrNull != null && start == nextStartOrNull -> OverviewClassState.NEXT
            else -> OverviewClassState.LATER
        }
        val topic = lectures.firstOrNull { it.classId == row.classId }?.subject
        OverviewTodayItem(
            classId = row.classId,
            code = row.disciplineCode,
            title = row.disciplineName,
            startTime = row.startTime.orEmpty(),
            endTime = row.endTime,
            roomLocation = row.spaceLocation,
            topic = topic,
            state = state,
        )
    }
}
