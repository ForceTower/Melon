package dev.forcetower.unes.ui.feature.enrollment

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import dev.forcetower.melon.feature.enrollment.domain.repository.EnrollmentError
import dev.forcetower.unes.R

// User-facing copy for enrollment failures (mirrors iOS
// `EnrollmentFormat.message`). Server messages pass through verbatim when
// the backend sent one.
@Composable
internal fun enrollmentErrorMessage(error: EnrollmentError?): String = when (error) {
    EnrollmentError.Unauthorized -> stringResource(R.string.enrollment_error_session)
    EnrollmentError.NoConnection -> stringResource(R.string.enrollment_error_network)
    is EnrollmentError.Server ->
        error.message?.takeIf { it.isNotBlank() } ?: stringResource(R.string.enrollment_error_generic)
    else -> stringResource(R.string.enrollment_error_generic)
}
