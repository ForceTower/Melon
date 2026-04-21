package dev.forcetower.melon.feature.schedule.domain.usecase

import dev.forcetower.melon.core.common.parseHhMm
import dev.forcetower.melon.core.database.dao.AcademicDao
import dev.forcetower.melon.core.database.dao.SemesterDao
import dev.forcetower.melon.core.database.entity.SemesterEntity
import dev.forcetower.melon.core.database.query.SemesterAllocationRow
import dev.forcetower.melon.core.database.query.WeekLectureRow
import dev.forcetower.melon.feature.schedule.domain.internal.ticker
import dev.forcetower.melon.feature.schedule.domain.model.ScheduleClass
import dev.forcetower.melon.feature.schedule.domain.model.ScheduleDay
import dev.forcetower.melon.feature.schedule.domain.model.ScheduleWeek
import dev.zacsweers.metro.Inject
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.datetime.DatePeriod
import kotlinx.datetime.LocalDate
import kotlinx.datetime.minus
import kotlinx.datetime.plus

// Drives the week-focused Schedule view. Pipeline:
//   (semester list, ticker) → pick semester active for today (falling back
//   to the most recent past when nothing is currently running), anchor on
//   today's Monday — or on the fallback semester's final week — →
//   resubscribe (flatMapLatest) to the semester's allocations + the week's
//   lectures → bucket into 7 days.
// Ticker fires every 60s; the real work only resubscribes on day-roll or
// semester-change thanks to distinctUntilChanged on the key.
@OptIn(ExperimentalCoroutinesApi::class)
@Inject
class ObserveScheduleWeekUseCase internal constructor(
    private val semesterDao: SemesterDao,
    private val academicDao: AcademicDao,
) {
    operator fun invoke(): Flow<ScheduleWeek> {
        val keyFlow: Flow<WeekKey> = combine(
            semesterDao.observeAll(),
            ticker(),
        ) { semesters, now ->
            val today = now.date
            val todayIso = today.toString()
            val running = semesters.firstOrNull { todayIso in it.startDate..it.endDate }
            // Between semesters the view still shows the last one the student
            // attended, anchored on that semester's final week so the column
            // reflects real allocations instead of a phantom today.
            val semester = running ?: semesters
                .filter { runCatching { LocalDate.parse(it.endDate) }.isSuccess }
                .maxByOrNull { it.endDate }
            val anchorDay = when {
                running != null -> today
                semester != null -> LocalDate.parse(semester.endDate)
                else -> today
            }
            val monday = anchorDay.minus(DatePeriod(days = anchorDay.dayOfWeek.ordinal))
            WeekKey(semester, today, monday)
        }.distinctUntilChanged()

        return keyFlow.flatMapLatest { key ->
            val semester = key.semester
                ?: return@flatMapLatest flowOf(emptyWeek(key.today, key.monday))
            val sunday = key.monday.plus(DatePeriod(days = 6))
            combine(
                academicDao.observeSemesterAllocations(semester.id),
                academicDao.observeLecturesInRange(
                    semesterId = semester.id,
                    start = key.monday.toString(),
                    end = sunday.toString(),
                ),
            ) { allocations, lectures ->
                buildScheduleWeek(semester, allocations, lectures, key.today, key.monday)
            }
        }.distinctUntilChanged()
    }

    private data class WeekKey(
        val semester: SemesterEntity?,
        val today: LocalDate,
        val monday: LocalDate,
    )
}

// Hoisted out for unit testing. The function is pure: given allocations,
// lectures, and the calendar frame, it deterministically produces the 7-day
// bucketed view.
internal fun buildScheduleWeek(
    semester: SemesterEntity,
    allocations: List<SemesterAllocationRow>,
    lectures: List<WeekLectureRow>,
    today: LocalDate,
    monday: LocalDate,
): ScheduleWeek {
    val days = (0..6).map { idx ->
        val date = monday.plus(DatePeriod(days = idx))
        val dateIso = date.toString()
        val rows = allocations
            .asSequence()
            .mapNotNull { row ->
                val day = row.day ?: return@mapNotNull null
                // ClassAllocation.day is 0=Sunday..6=Saturday (see
                // ClassAllocationEntity.day — upstream Snowpiercer
                // `time.day` is persisted untransformed by apps/api).
                // Schedule's UI indexes Monday..Sunday, so shift by 6:
                //   1 (Mon) → 0, 2 (Tue) → 1, ..., 6 (Sat) → 5, 0 (Sun) → 6.
                val rowIdx = (day + 6) % 7
                if (rowIdx != idx) return@mapNotNull null
                if (parseHhMm(row.startTime) == null) return@mapNotNull null
                row
            }
            .sortedBy { parseHhMm(it.startTime) ?: Int.MAX_VALUE }
            .map { row -> row.toScheduleClass(topicFor(row.classId, dateIso, lectures)) }
            .toList()
        ScheduleDay(dayIndex = idx, dateIso = dateIso, classes = rows)
    }

    val sunday = monday.plus(DatePeriod(days = 6))
    val todayInWeek = today in monday..sunday
    return ScheduleWeek(
        semesterId = semester.id,
        semesterCode = semester.code,
        weekNumber = weekOfSemester(semester, if (todayInWeek) today else monday),
        weekStartIso = monday.toString(),
        todayDayIndex = today.dayOfWeek.ordinal.takeIf { todayInWeek },
        days = days,
    )
}

// Populated week with 7 empty days anchored on today's Mon-Sun. Used when no
// semester is active so the view still renders a valid frame.
internal fun emptyWeek(today: LocalDate, monday: LocalDate): ScheduleWeek {
    val days = (0..6).map { idx ->
        val date = monday.plus(DatePeriod(days = idx))
        ScheduleDay(dayIndex = idx, dateIso = date.toString(), classes = emptyList())
    }
    return ScheduleWeek(
        semesterId = null,
        semesterCode = null,
        weekNumber = 0,
        weekStartIso = monday.toString(),
        todayDayIndex = today.dayOfWeek.ordinal,
        days = days,
    )
}

// Week count since semester.startDate, 1-based. The reference date is
// clamped into the semester window so the fallback between-semesters path
// (where the anchor sits at the semester's final week) still reports a
// meaningful week number instead of 0.
internal fun weekOfSemester(semester: SemesterEntity, reference: LocalDate): Int {
    val start = runCatching { LocalDate.parse(semester.startDate) }.getOrNull() ?: return 0
    val end = runCatching { LocalDate.parse(semester.endDate) }.getOrNull() ?: return 0
    val clamped = when {
        reference < start -> start
        reference > end -> end
        else -> reference
    }
    val days = clamped.toEpochDays() - start.toEpochDays()
    return (days / 7) + 1
}

private fun topicFor(classId: String, dateIso: String, lectures: List<WeekLectureRow>): String? =
    lectures.firstOrNull { it.classId == classId && it.date == dateIso }?.subject

private fun SemesterAllocationRow.toScheduleClass(topic: String?): ScheduleClass = ScheduleClass(
    allocationId = allocationId,
    classId = classId,
    code = disciplineCode,
    title = disciplineName,
    startTime = startTime.orEmpty(),
    endTime = endTime,
    teacherName = teacherName,
    modulo = spaceModulo,
    room = spaceLocation,
    campus = spaceCampus,
    topic = topic,
)
