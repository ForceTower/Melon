package dev.forcetower.melon.feature.enrollment.domain.usecase

import dev.forcetower.melon.core.common.Outcome
import dev.forcetower.melon.feature.enrollment.domain.model.EnrollmentOffers
import dev.forcetower.melon.feature.enrollment.domain.repository.EnrollmentError
import dev.forcetower.melon.feature.enrollment.domain.repository.EnrollmentRepository
import dev.zacsweers.metro.Inject

// The full disciplines tree for the current step.
@Inject
class GetEnrollmentOffersUseCase internal constructor(
    private val repository: EnrollmentRepository,
) {
    suspend operator fun invoke(): Outcome<EnrollmentOffers, EnrollmentError> = repository.offers()
}
