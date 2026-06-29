package dev.forcetower.melon.feature.enrollment.domain.usecase

import dev.forcetower.melon.core.common.Outcome
import dev.forcetower.melon.feature.enrollment.domain.model.EnrollmentSelection
import dev.forcetower.melon.feature.enrollment.domain.repository.EnrollmentError
import dev.forcetower.melon.feature.enrollment.domain.repository.EnrollmentRepository
import dev.zacsweers.metro.Inject

// Submits the complete desired set (open → publish → close happens server-side).
@Inject
class SubmitEnrollmentUseCase internal constructor(
    private val repository: EnrollmentRepository,
) {
    suspend operator fun invoke(selections: List<EnrollmentSelection>): Outcome<Unit, EnrollmentError> =
        repository.submit(selections)
}
