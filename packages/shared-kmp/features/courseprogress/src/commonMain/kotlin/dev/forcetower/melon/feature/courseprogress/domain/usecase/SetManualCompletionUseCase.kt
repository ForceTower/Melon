package dev.forcetower.melon.feature.courseprogress.domain.usecase

import dev.forcetower.melon.core.common.Outcome
import dev.forcetower.melon.feature.courseprogress.data.CourseProgressRepository
import dev.forcetower.melon.feature.courseprogress.domain.model.CourseProgressError
import dev.zacsweers.metro.Inject

@Inject
class SetManualCompletionUseCase internal constructor(
    private val repository: CourseProgressRepository,
) {
    suspend operator fun invoke(code: String, completed: Boolean): Outcome<Unit, CourseProgressError> =
        repository.setManuallyCompleted(code, completed)
}
