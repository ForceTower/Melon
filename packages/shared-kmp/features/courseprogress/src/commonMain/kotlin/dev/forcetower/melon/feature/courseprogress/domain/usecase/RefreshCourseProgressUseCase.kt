package dev.forcetower.melon.feature.courseprogress.domain.usecase

import dev.forcetower.melon.core.common.Outcome
import dev.forcetower.melon.feature.courseprogress.data.CourseProgressRepository
import dev.forcetower.melon.feature.courseprogress.domain.model.CourseProgressError
import dev.zacsweers.metro.Inject

// Re-pulls `api/curriculum` and replaces the mirror. Fired on every entry into
// the feature; the result lands through `ObserveCourseProgressUseCase`, so the
// outcome here only decides whether a failure gets narrated (it does, but only
// when there's nothing mirrored to fall back on).
@Inject
class RefreshCourseProgressUseCase internal constructor(
    private val repository: CourseProgressRepository,
) {
    suspend operator fun invoke(): Outcome<Unit, CourseProgressError> = repository.refresh()
}
