package dev.forcetower.melon.feature.overview.domain.usecase

import dev.forcetower.melon.core.database.dao.AcademicDao
import dev.forcetower.melon.core.database.dao.SemesterDao
import dev.forcetower.melon.core.database.query.AttendanceSummaryRow
import dev.forcetower.melon.core.database.query.RecentLectureRow
import dev.forcetower.melon.feature.overview.domain.internal.pickActiveSemester
import dev.forcetower.melon.feature.overview.domain.model.OverviewAttendanceTile
import dev.zacsweers.metro.Inject
import kotlin.math.floor
import kotlin.math.roundToInt
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.datetime.Clock
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime

// Upstream `situation` code convention: 0 == present, everything else is some
// flavor of absence / justified / pending. The strip shows "present vs not".
private const val PRESENT_SITUATION = 0

// 75% attendance rule — SAGRES allows at most 25% misses.
private const val ALLOWED_MISS_FRACTION = 0.25

// Last N lecture rows used to build the strip. Matches the fixture's 14 cells.
private const val STRIP_SIZE = 14

@OptIn(ExperimentalCoroutinesApi::class)
@Inject
class ObserveAttendanceTileUseCase internal constructor(
    private val semesterDao: SemesterDao,
    private val academicDao: AcademicDao,
) {
    private val timeZone = TimeZone.currentSystemDefault()

    operator fun invoke(): Flow<OverviewAttendanceTile> =
        semesterDao.observeAll().flatMapLatest { semesters ->
            val today = Clock.System.now().toLocalDateTime(timeZone).date.toString()
            val semester = pickActiveSemester(semesters, today)
                ?: return@flatMapLatest flowOf(emptyAttendanceTile())

            combine(
                academicDao.observeAttendanceSummary(semester.id),
                academicDao.observeRecentLectures(semester.id, STRIP_SIZE),
            ) { summary, recent -> buildAttendanceTile(summary, recent) }
        }
}

internal fun buildAttendanceTile(
    summary: AttendanceSummaryRow,
    recent: List<RecentLectureRow>,
): OverviewAttendanceTile {
    val percentage = if (summary.totalHours > 0) {
        val missedRatio = summary.totalMissed.toDouble() / summary.totalHours.toDouble()
        ((1.0 - missedRatio) * 100).roundToInt().coerceIn(0, 100)
    } else {
        null
    }
    val allowed = floor(summary.totalHours * ALLOWED_MISS_FRACTION).toInt()
    // Most-recent-first in the query; reverse so the strip reads oldest → newest,
    // which matches the ascending visual intensity in the fixture.
    val days = recent.map { it.situation == PRESENT_SITUATION }.reversed()
    return OverviewAttendanceTile(
        percentage = percentage,
        lastDays = days,
        allowedAbsences = allowed,
        periodDays = STRIP_SIZE,
    )
}

private fun emptyAttendanceTile() = OverviewAttendanceTile(
    percentage = null,
    lastDays = emptyList(),
    allowedAbsences = 0,
    periodDays = STRIP_SIZE,
)
