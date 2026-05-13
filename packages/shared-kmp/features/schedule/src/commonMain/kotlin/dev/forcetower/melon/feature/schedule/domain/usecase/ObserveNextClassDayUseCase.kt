package dev.forcetower.melon.feature.schedule.domain.usecase

import dev.forcetower.melon.core.common.parseHhMm
import dev.forcetower.melon.core.common.toUpstreamDay
import dev.forcetower.melon.core.database.dao.AcademicDao
import dev.forcetower.melon.core.database.dao.SemesterDao
import dev.forcetower.melon.core.database.entity.SemesterEntity
import dev.forcetower.melon.core.database.query.SemesterAllocationRow
import dev.forcetower.melon.feature.schedule.domain.internal.pickActiveSemester
import dev.forcetower.melon.feature.schedule.domain.internal.ticker
import dev.forcetower.melon.feature.schedule.domain.model.NextClassDay
import dev.forcetower.melon.feature.schedule.domain.model.ScheduleClass
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

// First future day with at least one scheduled class in the active semester.
// Unlike `ObserveScheduleWeekUseCase`, this looks past the current week so
// callers (the iOS widget snapshot publisher) can answer "what's next?" on
// a Friday with no weekend classes — it'll keep scanning into next week.
//
// The scan walks day-by-day from tomorrow up to the semester end (capped at
// `MAX_LOOKAHEAD_DAYS`). For each calendar day it matches against the
// upstream day-of-week stored on each allocation, returning the first
// populated day. Pure once you have the allocations + semester window, so
// the heavy work happens inside `pickNextClassDay` — flow boilerplate just
// keeps subscriptions alive across the ticker.
@OptIn(ExperimentalCoroutinesApi::class)
@Inject
class ObserveNextClassDayUseCase internal constructor(
    private val semesterDao: SemesterDao,
    private val academicDao: AcademicDao,
) {
    operator fun invoke(): Flow<NextClassDay?> {
        val keyFlow: Flow<Key?> = combine(
            semesterDao.observeAll(),
            ticker(),
        ) { semesters, now ->
            val today = now.date
            val semester = pickActiveSemester(semesters, today.toString())
            semester?.let { Key(it, today) }
        }.distinctUntilChanged()

        return keyFlow.flatMapLatest { key ->
            if (key == null) return@flatMapLatest flowOf(null)
            academicDao.observeSemesterAllocations(key.semester.id).map { allocations ->
                pickNextClassDay(allocations, key.today, key.semester)
            }
        }.distinctUntilChanged()
    }

    private data class Key(val semester: SemesterEntity, val today: LocalDate)
}

private const val MAX_LOOKAHEAD_DAYS = 30

internal fun pickNextClassDay(
    allocations: List<SemesterAllocationRow>,
    today: LocalDate,
    semester: SemesterEntity,
): NextClassDay? {
    if (allocations.isEmpty()) return null
    val semesterEnd = runCatching { LocalDate.parse(semester.endDate) }.getOrNull()
    for (offset in 1..MAX_LOOKAHEAD_DAYS) {
        val date = today.plus(DatePeriod(days = offset))
        if (semesterEnd != null && date > semesterEnd) return null
        val upstreamDay = date.dayOfWeek.toUpstreamDay()
        val rows = allocations
            .asSequence()
            .filter { it.day == upstreamDay && parseHhMm(it.startTime) != null }
            .sortedBy { parseHhMm(it.startTime) ?: Int.MAX_VALUE }
            .map { it.toScheduleClass() }
            .toList()
        if (rows.isEmpty()) continue
        return NextClassDay(
            dateIso = date.toString(),
            daysAway = offset,
            first = rows.first(),
            classes = rows,
        )
    }
    return null
}

private fun SemesterAllocationRow.toScheduleClass(): ScheduleClass = ScheduleClass(
    allocationId = allocationId,
    classId = classId,
    offerId = offerId,
    code = disciplineCode,
    title = disciplineName,
    startTime = startTime.orEmpty(),
    endTime = endTime,
    teacherName = teacherName,
    modulo = spaceModulo,
    room = spaceLocation,
    campus = spaceCampus,
    topic = null,
)
