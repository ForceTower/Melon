package dev.forcetower.unes.enrollment.ui.onboarding

import androidx.compose.foundation.layout.Column
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import dev.forcetower.unes.enrollment.R
import dev.forcetower.unes.enrollment.ui.theme.EnrollmentTheme

@Composable
fun OnboardingScreen(modifier: Modifier = Modifier) {
    Column(modifier = modifier) {
        Text(stringResource(R.string.enrollment_onboarding_title))
        Text(stringResource(R.string.enrollment_onboarding_message))
        Text(stringResource(R.string.enrollment_onboarding_footnote))
    }
}

@Preview(showBackground = true)
@Composable
fun OnboardingScreenPreview() {
    EnrollmentTheme {
        OnboardingScreen()
    }
}