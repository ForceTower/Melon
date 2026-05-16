package dev.forcetower.melon.feature.overview.domain.usecase

import dev.forcetower.melon.core.database.dao.AcademicDao
import dev.forcetower.melon.core.database.dao.SemesterDao
import dev.forcetower.melon.feature.overview.domain.internal.pickActiveSemester
import dev.forcetower.melon.feature.overview.domain.internal.ticker
import dev.forcetower.melon.feature.overview.domain.model.OverviewNextTestTile
import dev.zacsweers.metro.Inject
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.datetime.LocalDate

// Heuristic: the closest un-graded future StudentGrade row. Emits null when
// none exists — the UI's existing empty state handles that. CLAUDE.md
// follow-up: swap for authoritative evaluation dates once upstream surfaces
// them.
@OptIn(ExperimentalCoroutinesApi::class)
@Inject
class ObserveNextTestTileUseCase internal constructor(
    private val semesterDao: SemesterDao,
    private val academicDao: AcademicDao,
) {
    operator fun invoke(): Flow<OverviewNextTestTile?> {
        val keyFlow: Flow<Pair<String, String>?> = combine(
            semesterDao.observeAll(),
            ticker(),
        ) { semesters, now ->
            val dateIso = now.date.toString()
            pickActiveSemester(semesters, dateIso)?.let { it.id to dateIso }
        }.distinctUntilChanged()

        return keyFlow.flatMapLatest { key ->
            if (key == null) return@flatMapLatest flowOf(null)
            combine(
                academicDao.observeClosestUpcomingEvaluation(key.first, key.second),
                ticker(),
            ) { row, now ->
                row?.let {
                    val evalDate = runCatching { LocalDate.parse(it.date) }.getOrNull()
                        ?: return@combine null
                    val today = now.date
                    val days = evalDate.toEpochDays() - today.toEpochDays()
                    OverviewNextTestTile(
                        label = it.evaluationName?.takeIf(String::isNotBlank) ?: "Avaliação",
                        disciplineName = it.disciplineName,
                        date = it.date,
                        daysUntil = days.coerceAtLeast(0).toInt(),
                    )
                }
            }.distinctUntilChanged()
        }
    }
}
