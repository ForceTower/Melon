package dev.forcetower.unes.ui.feature.onboarding.welcome

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.forcetower.unes.R
import dev.forcetower.unes.designsystem.components.MelonGlassButton
import dev.forcetower.unes.designsystem.components.MelonPrimaryButton
import dev.forcetower.unes.designsystem.foundation.Mesh
import dev.forcetower.unes.designsystem.foundation.MeshVariant
import dev.forcetower.unes.designsystem.foundation.fadeUpOnAppear
import dev.forcetower.unes.designsystem.theme.MelonTheme
import dev.forcetower.unes.designsystem.theme.melon

private val DarkBg = androidx.compose.ui.graphics.Color(0xFF1A0F28)
private val SurfaceLight = androidx.compose.ui.graphics.Color(0xFFFBF7F2)

@Composable
fun WelcomeScreen(
    onNext: () -> Unit,
    onLogin: () -> Unit,
) {
    Box(
        Modifier
            .fillMaxSize()
            .background(DarkBg),
    ) {
        Mesh(variant = MeshVariant.Warm, modifier = Modifier.fillMaxSize())

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(start = 28.dp, end = 28.dp, top = 110.dp, bottom = 50.dp),
            verticalArrangement = Arrangement.SpaceBetween,
        ) {
            Text(
                text = stringResource(R.string.onboarding_welcome_eyebrow),
                style = MaterialTheme.typography.labelSmall.copy(
                    fontSize = 11.sp,
                    letterSpacing = 2.2.sp,
                    fontFamily = FontFamily.Monospace,
                ),
                color = SurfaceLight.copy(alpha = 0.6f),
                modifier = Modifier.fadeUpOnAppear(delayMs = 100),
            )

            Column(
                modifier = Modifier.padding(bottom = 20.dp),
                verticalArrangement = Arrangement.spacedBy(0.dp),
            ) {
                Text(
                    text = welcomeHeadline(MaterialTheme.melon.brand.amber, SurfaceLight),
                    style = MaterialTheme.typography.displayLarge.copy(
                        fontSize = 54.sp,
                        lineHeight = 53.sp,
                        letterSpacing = (-1.35).sp,
                        fontWeight = FontWeight.Normal,
                    ),
                    modifier = Modifier.fadeUpOnAppear(delayMs = 250),
                )
                Spacer(Modifier.height(22.dp))
                Text(
                    text = stringResource(R.string.onboarding_welcome_body),
                    style = MaterialTheme.typography.bodyLarge.copy(
                        fontSize = 17.sp,
                        lineHeight = 25.sp,
                        letterSpacing = (-0.17).sp,
                    ),
                    color = SurfaceLight.copy(alpha = 0.72f),
                    modifier = Modifier
                        .widthIn(max = 320.dp)
                        .fadeUpOnAppear(delayMs = 450),
                )
            }

            Column(
                verticalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.fadeUpOnAppear(delayMs = 650),
            ) {
                MelonPrimaryButton(
                    text = stringResource(R.string.onboarding_welcome_primary_cta),
                    onClick = onNext,
                    background = SurfaceLight,
                    contentColor = androidx.compose.ui.graphics.Color(0xFF1A1420),
                )
                MelonGlassButton(
                    text = stringResource(R.string.onboarding_welcome_secondary_cta),
                    onClick = onLogin,
                )
            }
        }
    }
}

@Preview
@Composable
private fun WelcomeScreenPreview() {
    MelonTheme { WelcomeScreen(onNext = {}, onLogin = {}) }
}

@Composable
private fun welcomeHeadline(amber: androidx.compose.ui.graphics.Color, ink: androidx.compose.ui.graphics.Color): AnnotatedString {
    val top = stringResource(R.string.onboarding_welcome_headline_top)
    val accent = stringResource(R.string.onboarding_welcome_headline_accent)
    val bottom = stringResource(R.string.onboarding_welcome_headline_bottom)
    return buildAnnotatedString {
        withStyle(SpanStyle(color = ink)) { append("$top\n") }
        withStyle(
            SpanStyle(
                color = amber,
                fontStyle = FontStyle.Italic,
            ),
        ) { append(accent) }
        withStyle(SpanStyle(color = ink)) { append("\n$bottom") }
    }
}
