package dev.forcetower.unes.ui.feature.onboarding.welcome

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import dev.forcetower.unes.R
import dev.forcetower.unes.designsystem.foundation.Mesh
import dev.forcetower.unes.designsystem.foundation.MeshVariant
import dev.forcetower.unes.designsystem.foundation.fadeUpOnAppear
import dev.forcetower.unes.designsystem.theme.MelonTheme
import dev.forcetower.unes.designsystem.theme.melon
import dev.forcetower.unes.ui.feature.onboarding.components.OnboardingPillButton

@Composable
internal fun WelcomeScreen(
    onNext: () -> Unit,
    onLogin: () -> Unit,
    vm: WelcomeViewModel = hiltViewModel(),
) {
    WelcomeContent(
        onNext = {
            vm.trackStart()
            onNext()
        },
        onLogin = {
            vm.trackLogin()
            onLogin()
        },
    )
}

@Composable
private fun WelcomeContent(
    onNext: () -> Unit,
    onLogin: () -> Unit,
) {

    val night = MaterialTheme.melon.fixed.night
    val veil = MaterialTheme.melon.fixed.nightVeil
    val cream = MaterialTheme.melon.fixed.surfaceLight
    val onHero = MaterialTheme.melon.fixed.onHero
    val inkOnCream = MaterialTheme.melon.fixed.onSurfaceLight
    val accent = MaterialTheme.colorScheme.primary

    Box(
        Modifier
            .fillMaxSize()
            .background(night),
    ) {
        Mesh(variant = MeshVariant.Warm, modifier = Modifier.fillMaxSize())
        Box(
            Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        0f to veil.copy(alpha = 0.4f),
                        0.42f to veil.copy(alpha = 0.12f),
                        1f to veil.copy(alpha = 0.74f),
                    ),
                ),
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(start = 30.dp, end = 30.dp, top = 108.dp, bottom = 46.dp),
        ) {
            Text(
                text = stringResource(R.string.onboarding_welcome_eyebrow).uppercase(),
                style = MaterialTheme.typography.labelMedium.copy(
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 0.78.sp,
                ),
                color = onHero.copy(alpha = 0.92f),
                modifier = Modifier.fadeUpOnAppear(delayMs = 100),
            )

            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(bottom = 24.dp),
                verticalArrangement = Arrangement.Bottom,
            ) {
                Text(
                    text = welcomeHeadline(accent = accent, cream = cream),
                    style = MaterialTheme.typography.displayLarge.copy(
                        fontSize = 54.sp,
                        lineHeight = 54.sp,
                        letterSpacing = (-2.4).sp,
                        fontWeight = FontWeight.ExtraBold,
                    ),
                    modifier = Modifier.fadeUpOnAppear(delayMs = 220, durationMs = 750),
                )
                Spacer(Modifier.height(20.dp))
                Text(
                    text = stringResource(R.string.onboarding_welcome_body),
                    style = MaterialTheme.typography.bodyLarge.copy(
                        fontSize = 17.sp,
                        lineHeight = 25.5.sp,
                        fontWeight = FontWeight.Medium,
                        letterSpacing = (-0.17).sp,
                    ),
                    color = cream.copy(alpha = 0.78f),
                    modifier = Modifier
                        .widthIn(max = 320.dp)
                        .fadeUpOnAppear(delayMs = 360, durationMs = 750),
                )
            }

            Column(
                verticalArrangement = Arrangement.spacedBy(11.dp),
                modifier = Modifier.fadeUpOnAppear(delayMs = 500, durationMs = 750),
            ) {
                OnboardingPillButton(
                    text = stringResource(R.string.onboarding_welcome_primary_cta),
                    onClick = onNext,
                    containerColor = cream,
                    contentColor = inkOnCream,
                    showArrow = true,
                    arrowIcon = Icons.AutoMirrored.Filled.ArrowForward,
                )
                OnboardingPillButton(
                    text = stringResource(R.string.onboarding_welcome_secondary_cta),
                    onClick = onLogin,
                    containerColor = onHero.copy(alpha = 0.10f),
                    contentColor = cream,
                    border = BorderStroke(1.dp, onHero.copy(alpha = 0.18f)),
                )
            }
        }
    }
}

@Composable
private fun welcomeHeadline(accent: Color, cream: Color): AnnotatedString {
    val top = stringResource(R.string.onboarding_welcome_headline_top)
    val middle = stringResource(R.string.onboarding_welcome_headline_accent)
    val bottom = stringResource(R.string.onboarding_welcome_headline_bottom)
    return buildAnnotatedString {
        withStyle(SpanStyle(color = cream)) { append("$top\n") }
        withStyle(SpanStyle(color = accent)) { append(middle) }
        withStyle(SpanStyle(color = cream)) { append("\n$bottom") }
    }
}

@Preview
@Composable
private fun WelcomeScreenPreview() {
    MelonTheme { WelcomeContent(onNext = {}, onLogin = {}) }
}
