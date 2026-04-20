package dev.forcetower.melon.feature.dashboard.domain.usecase

import dev.forcetower.melon.core.database.dao.AcademicDao
import dev.forcetower.melon.core.database.dao.SemesterDao
import dev.forcetower.melon.core.database.entity.SemesterEntity
import dev.forcetower.melon.core.database.query.SemesterAllocationRow
import dev.forcetower.melon.feature.dashboard.domain.model.NextClassInfo
import dev.forcetower.melon.feature.dashboard.domain.model.ReadyOverview
import dev.zacsweers.metro.Inject
import kotlinx.datetime.Clock
import kotlinx.datetime.DayOfWeek
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime

@Inject
class GetReadyOverviewUseCase internal constructor(
    private val semesterDao: SemesterDao,
    private val academicDao: AcademicDao,
) {
    suspend operator fun invoke(): ReadyOverview {
        val timeZone = TimeZone.currentSystemDefault()
        val semester = pickActiveSemester(semesterDao.listAll(), timeZone)
            ?: return ReadyOverview(semesterCode = null, classCount = 0, totalCredits = 0, nextClass = null)

        val aggregate = academicDao.getSemesterAggregate(semester.id)
        val allocations = academicDao.listSemesterAllocations(semester.id)
        val next = pickNextClass(allocations, timeZone)

        return ReadyOverview(
            semesterCode = semester.code,
            classCount = aggregate.classCount,
            totalCredits = aggregate.totalHours,
            nextClass = next,
        )
    }

    // Same rule used by iOS SyncViewModel: first semester whose [startDate,
    // endDate] contains today; otherwise the most recent one. Lex compare of
    // yyyy-MM-dd matches calendar order.
    private fun pickActiveSemester(all: List<SemesterEntity>, timeZone: TimeZone): SemesterEntity? {
        if (all.isEmpty()) return null
        val today = Clock.System.now().toLocalDateTime(timeZone).date.toString()
        val active = all.firstOrNull { it.startDate <= today && today <= it.endDate }
        if (active != null) return active
        return all.maxByOrNull { it.startDate }
    }

    private fun pickNextClass(rows: List<SemesterAllocationRow>, timeZone: TimeZone): NextClassInfo? {
        if (rows.isEmpty()) return null
        val now = Clock.System.now().toLocalDateTime(timeZone)
        val nowSlot = now.dayOfWeek.upstreamDay() * SLOTS_IN_DAY + now.hour * 60 + now.minute
        val WEEK = 7 * SLOTS_IN_DAY

        data class Scored(val row: SemesterAllocationRow, val deltaMinutes: Int)

        val scored = rows.mapNotNull { row ->
            val day = row.day ?: return@mapNotNull null
            val startMinutes = parseHhMm(row.startTime) ?: return@mapNotNull null
            val rowSlot = day * SLOTS_IN_DAY + startMinutes
            val delta = ((rowSlot - nowSlot) % WEEK + WEEK) % WEEK
            Scored(row, delta)
        }

        val winner = scored.minByOrNull { it.deltaMinutes } ?: return null
        return NextClassInfo(
            disciplineName = winner.row.disciplineName,
            startTime = winner.row.startTime.orEmpty(),
            endTime = winner.row.endTime,
            spaceLocation = winner.row.spaceLocation,
            teacherName = winner.row.teacherName,
            startsInMinutes = winner.deltaMinutes,
        )
    }

    // Upstream `dia` encoding is 1=Sunday..7=Saturday (PT-BR convention).
    // Translate ISO DayOfWeek to match so comparisons stay in one space.
    private fun DayOfWeek.upstreamDay(): Int = when (this) {
        DayOfWeek.SUNDAY -> 1
        DayOfWeek.MONDAY -> 2
        DayOfWeek.TUESDAY -> 3
        DayOfWeek.WEDNESDAY -> 4
        DayOfWeek.THURSDAY -> 5
        DayOfWeek.FRIDAY -> 6
        DayOfWeek.SATURDAY -> 7
    }

    private fun parseHhMm(value: String?): Int? {
        if (value.isNullOrBlank()) return null
        val parts = value.split(":")
        if (parts.size < 2) return null
        val h = parts[0].toIntOrNull() ?: return null
        val m = parts[1].take(2).toIntOrNull() ?: return null
        return h * 60 + m
    }

    private companion object {
        const val SLOTS_IN_DAY = 24 * 60
    }
}
