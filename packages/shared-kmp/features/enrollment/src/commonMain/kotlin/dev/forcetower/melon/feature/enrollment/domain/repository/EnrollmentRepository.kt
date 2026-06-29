package dev.forcetower.melon.feature.enrollment.domain.repository

import dev.forcetower.melon.core.common.Outcome
import dev.forcetower.melon.feature.enrollment.domain.model.EnrollmentAvailability
import dev.forcetower.melon.feature.enrollment.domain.model.EnrollmentOffers
import dev.forcetower.melon.feature.enrollment.domain.model.EnrollmentSelection

// Enrollment is read live from SAGRES (via our API) and never cached — the
// offer/vacancy state changes by the second during a window.
internal interface EnrollmentRepository {
    suspend fun window(): Outcome<EnrollmentAvailability, EnrollmentError>

    suspend fun offers(): Outcome<EnrollmentOffers, EnrollmentError>

    suspend fun submit(selections: List<EnrollmentSelection>): Outcome<Unit, EnrollmentError>
}

sealed interface EnrollmentError {
    data object Unauthorized : EnrollmentError
    data object NoConnection : EnrollmentError
    data class Server(val message: String?) : EnrollmentError
    data object Unexpected : EnrollmentError
}
