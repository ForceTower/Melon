package dev.forcetower.melon.feature.dashboard.domain.usecase

import dev.forcetower.melon.core.common.parseHhMm
import dev.forcetower.melon.core.common.toUpstreamDay
import dev.forcetower.melon.core.common.weekSlot
import dev.forcetower.melon.core.common.weekSlotDelta
import dev.forcetower.melon.core.database.dao.AcademicDao
import dev.forcetower.melon.core.database.dao.SemesterDao
import dev.forcetower.melon.core.database.entity.SemesterEntity
import dev.forcetower.melon.core.database.query.SemesterAllocationRow
import dev.forcetower.melon.feature.dashboard.domain.model.NextClassInfo
import dev.forcetower.melon.feature.dashboard.domain.model.ReadyOverview
import dev.zacsweers.metro.Inject
import kotlinx.datetime.Clock
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
        val nowSlot = weekSlot(now.dayOfWeek.toUpstreamDay(), now.hour * 60 + now.minute)

        val winner = rows
            .mapNotNull { row ->
                val day = row.day ?: return@mapNotNull null
                val startMinutes = parseHhMm(row.startTime) ?: return@mapNotNull null
                row to weekSlotDelta(nowSlot, weekSlot(day, startMinutes))
            }
            .minByOrNull { it.second }
            ?: return null

        return NextClassInfo(
            disciplineName = winner.first.disciplineName,
            startTime = winner.first.startTime.orEmpty(),
            endTime = winner.first.endTime,
            spaceLocation = winner.first.spaceLocation,
            teacherName = winner.first.teacherName,
            startsInMinutes = winner.second,
        )
    }
}
