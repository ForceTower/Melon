package dev.forcetower.melon.feature.overview.domain.usecase

import dev.forcetower.melon.core.common.ForegroundSignal
import dev.forcetower.melon.core.common.parseHhMm
import dev.forcetower.melon.core.common.tickerFlow
import dev.forcetower.melon.core.common.toUpstreamDay
import dev.forcetower.melon.core.common.weekSlot
import dev.forcetower.melon.core.common.weekSlotDelta
import dev.forcetower.melon.core.database.dao.AcademicDao
import dev.forcetower.melon.core.database.dao.SemesterDao
import dev.forcetower.melon.core.database.query.SemesterAllocationRow
import dev.forcetower.melon.core.database.query.TodayLectureRow
import dev.forcetower.melon.feature.overview.domain.internal.pickActiveSemester
import dev.forcetower.melon.feature.overview.domain.model.OverviewNowClass
import dev.zacsweers.metro.Inject
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.datetime.LocalDateTime

// Surfaces either the class running right now, or the closest upcoming one if
// nothing is in session. Three flow sources combine: the semester list (to
// pick "current"), the allocation set for that semester, today's lectures
// (for the topic), and a 30s ticker so derived state flips on minute bounds.
@OptIn(ExperimentalCoroutinesApi::class)
@Inject
class ObserveNowClassUseCase internal constructor(
    private val semesterDao: SemesterDao,
    private val academicDao: AcademicDao,
    private val foreground: ForegroundSignal,
) {
    operator fun invoke(): Flow<OverviewNowClass?> {
        // (semesterId, dateIso) pair: resubs happen only when the semester
        // changes or the local day rolls over, not on every ticker beat.
        val keyFlow: Flow<Pair<String, String>?> = combine(
            semesterDao.observeAll(),
            tickerFlow(30_000, foreground.pulses),
        ) { semesters, now ->
            val dateIso = now.date.toString()
            pickActiveSemester(semesters, dateIso)?.let { it.id to dateIso }
        }.distinctUntilChanged()

        return keyFlow.flatMapLatest { key ->
            if (key == null) return@flatMapLatest flowOf(null)
            combine(
                academicDao.observeSemesterAllocations(key.first),
                academicDao.observeTodayLecturesForSemester(key.first, key.second),
                tickerFlow(30_000, foreground.pulses),
            ) { allocations, lectures, now -> pickNowClass(allocations, lectures, now) }
                .distinctUntilChanged()
        }
    }
}

// Hoisted out for unit testing. Not part of the class so tests exercise it
// without mocking DAOs.
internal fun pickNowClass(
    allocations: List<SemesterAllocationRow>,
    lectures: List<TodayLectureRow>,
    now: LocalDateTime,
): OverviewNowClass? {
    if (allocations.isEmpty()) return null
    val todayDay = now.dayOfWeek.toUpstreamDay()
    val minutesOfDay = now.hour * 60 + now.minute
    val nowSlot = weekSlot(todayDay, minutesOfDay)

    val running = allocations.firstOrNull { row ->
        val day = row.day ?: return@firstOrNull false
        if (day != todayDay) return@firstOrNull false
        val start = parseHhMm(row.startTime) ?: return@firstOrNull false
        val end = parseHhMm(row.endTime) ?: return@firstOrNull false
        minutesOfDay in start until end
    }

    val chosen = running ?: allocations
        .mapNotNull { row ->
            val day = row.day ?: return@mapNotNull null
            val start = parseHhMm(row.startTime) ?: return@mapNotNull null
            row to weekSlotDelta(nowSlot, weekSlot(day, start))
        }
        .minByOrNull { it.second }
        ?.first
        ?: return null

    val chosenStart = parseHhMm(chosen.startTime) ?: return null
    val chosenDay = chosen.day ?: return null
    val startsInMinutes = if (running != null) {
        -(minutesOfDay - chosenStart)
    } else {
        weekSlotDelta(nowSlot, weekSlot(chosenDay, chosenStart))
    }
    val topic = lectures.firstOrNull { it.classId == chosen.classId }?.subject

    return OverviewNowClass(
        code = chosen.disciplineCode,
        title = chosen.disciplineName,
        teacherName = chosen.teacherName,
        roomLocation = chosen.spaceLocation,
        startTime = chosen.startTime.orEmpty(),
        endTime = chosen.endTime,
        startsInMinutes = startsInMinutes,
        topic = topic,
        isHappeningNow = running != null,
    )
}
