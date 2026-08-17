package dev.forcetower.melon.feature.courseprogress.domain.usecase

import dev.forcetower.melon.core.common.Outcome
import dev.forcetower.melon.feature.courseprogress.data.CourseProgressRepository
import dev.forcetower.melon.feature.courseprogress.domain.model.CourseProgressError
import dev.zacsweers.metro.Inject

// Drops the student's manual curriculum pick and hands the binding back to
// the server's resolution (`DELETE api/curriculum/version`). The rebuilt
// payload lands through `ObserveCourseProgressUseCase`.
@Inject
class ResetCurriculumVersionUseCase internal constructor(
    private val repository: CourseProgressRepository,
) {
    suspend operator fun invoke(): Outcome<Unit, CourseProgressError> = repository.resetVersion()
}
