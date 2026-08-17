package dev.forcetower.melon.feature.courseprogress.domain.usecase

import dev.forcetower.melon.core.common.Outcome
import dev.forcetower.melon.feature.courseprogress.data.CourseProgressRepository
import dev.forcetower.melon.feature.courseprogress.domain.model.CourseProgressError
import dev.zacsweers.metro.Inject

// Binds the student to one of the course's curriculum versions by hand
// (`PUT api/curriculum/version`). The rebuilt payload lands through
// `ObserveCourseProgressUseCase`; the outcome only says whether the switch
// took.
@Inject
class SelectCurriculumVersionUseCase internal constructor(
    private val repository: CourseProgressRepository,
) {
    suspend operator fun invoke(curriculumId: String): Outcome<Unit, CourseProgressError> =
        repository.selectVersion(curriculumId)
}
