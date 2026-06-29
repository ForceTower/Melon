package dev.forcetower.melon.feature.enrollment.domain.usecase

import dev.forcetower.melon.core.common.Outcome
import dev.forcetower.melon.feature.enrollment.domain.model.EnrollmentAvailability
import dev.forcetower.melon.feature.enrollment.domain.repository.EnrollmentError
import dev.forcetower.melon.feature.enrollment.domain.repository.EnrollmentRepository
import dev.zacsweers.metro.Inject

// Cheap availability + window status: drives the hub gate and the entry screen.
@Inject
class GetEnrollmentWindowUseCase internal constructor(
    private val repository: EnrollmentRepository,
) {
    suspend operator fun invoke(): Outcome<EnrollmentAvailability, EnrollmentError> = repository.window()
}
