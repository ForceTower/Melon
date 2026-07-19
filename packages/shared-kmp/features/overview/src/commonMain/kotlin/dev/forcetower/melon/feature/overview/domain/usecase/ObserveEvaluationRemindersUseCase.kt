package dev.forcetower.melon.feature.overview.domain.usecase

import dev.forcetower.melon.core.common.ForegroundSignal
import dev.forcetower.melon.core.common.tickerFlow
import dev.forcetower.melon.core.database.dao.AcademicDao
import dev.forcetower.melon.core.database.dao.SemesterDao
import dev.forcetower.melon.feature.overview.domain.internal.pickActiveSemester
import dev.forcetower.melon.feature.overview.domain.model.OverviewEvaluationReminder
import dev.zacsweers.metro.Inject
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map

// Same heuristic as ObserveNextTestTileUseCase, but the full upcoming set:
// the reminder scheduler needs every pending evaluation, not just the
// closest. Deduplicated by (discipline, platform grade id) because the
// backend replicates the discipline-level grade set onto every group row.
@OptIn(ExperimentalCoroutinesApi::class)
@Inject
class ObserveEvaluationRemindersUseCase internal constructor(
    private val semesterDao: SemesterDao,
    private val academicDao: AcademicDao,
    private val foreground: ForegroundSignal,
) {
    operator fun invoke(): Flow<List<OverviewEvaluationReminder>> {
        val keyFlow: Flow<Pair<String, String>?> = combine(
            semesterDao.observeAll(),
            tickerFlow(30_000, foreground.pulses),
        ) { semesters, now ->
            val dateIso = now.date.toString()
            pickActiveSemester(semesters, dateIso)?.let { it.id to dateIso }
        }.distinctUntilChanged()

        return keyFlow.flatMapLatest { key ->
            if (key == null) return@flatMapLatest flowOf(emptyList())
            academicDao.observeUpcomingEvaluations(key.first, key.second)
                .map { rows ->
                    val seen = mutableSetOf<String>()
                    rows.mapNotNull { row ->
                        val reminderKey = "${row.disciplineId}/${row.gradePlatformId}"
                        if (!seen.add(reminderKey)) return@mapNotNull null
                        OverviewEvaluationReminder(
                            key = reminderKey,
                            label = row.gradeName?.takeIf(String::isNotBlank)
                                ?: row.evaluationName?.takeIf(String::isNotBlank)
                                ?: "Avaliação",
                            disciplineName = row.disciplineName,
                            date = row.date,
                        )
                    }
                }
                .distinctUntilChanged()
        }
    }
}
