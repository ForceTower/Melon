package dev.forcetower.unes.feature.onboarding

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.core.tween
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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.forcetower.unes.designsystem.components.MelonGlassButton
import dev.forcetower.unes.designsystem.components.MelonPrimaryButton
import dev.forcetower.unes.designsystem.foundation.Mesh
import dev.forcetower.unes.designsystem.foundation.MeshVariant
import dev.forcetower.unes.designsystem.theme.melon
import kotlinx.coroutines.delay

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
            FadeUp(delayMs = 100) {
                Text(
                    text = "◦ Bem-vinde ao UNES",
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontSize = 11.sp,
                        letterSpacing = 2.2.sp,
                        fontFamily = FontFamily.Monospace,
                    ),
                    color = SurfaceLight.copy(alpha = 0.6f),
                )
            }

            Column(
                modifier = Modifier
                    .padding(bottom = 20.dp),
                verticalArrangement = Arrangement.spacedBy(0.dp),
            ) {
                FadeUp(delayMs = 250) {
                    Text(
                        text = welcomeHeadline(MaterialTheme.melon.brand.amber, SurfaceLight),
                        style = MaterialTheme.typography.displayLarge.copy(
                            fontSize = 54.sp,
                            lineHeight = 53.sp,
                            letterSpacing = (-1.35).sp,
                            fontWeight = FontWeight.Normal,
                        ),
                    )
                }
                Spacer(Modifier.height(22.dp))
                FadeUp(delayMs = 450) {
                    Text(
                        text = "Horários, notas, recados da coordenação e turmas da UEFS — tudo conectado à sua matrícula.",
                        style = MaterialTheme.typography.bodyLarge.copy(
                            fontSize = 17.sp,
                            lineHeight = 25.sp,
                            letterSpacing = (-0.17).sp,
                        ),
                        color = SurfaceLight.copy(alpha = 0.72f),
                        modifier = Modifier.widthIn(max = 320.dp),
                    )
                }
            }

            FadeUp(delayMs = 650) {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    MelonPrimaryButton(
                        text = "Conhecer o app",
                        onClick = onNext,
                        background = SurfaceLight,
                        contentColor = androidx.compose.ui.graphics.Color(0xFF1A1420),
                    )
                    MelonGlassButton(
                        text = "Já tenho matrícula",
                        onClick = onLogin,
                    )
                }
            }
        }
    }
}

private fun welcomeHeadline(amber: androidx.compose.ui.graphics.Color, ink: androidx.compose.ui.graphics.Color): AnnotatedString =
    buildAnnotatedString {
        withStyle(SpanStyle(color = ink)) { append("Seu semestre,\n") }
        withStyle(
            SpanStyle(
                color = amber,
                fontStyle = FontStyle.Italic,
            ),
        ) { append("num só") }
        withStyle(SpanStyle(color = ink)) { append("\nlugar.") }
    }

@Composable
internal fun FadeUp(
    delayMs: Int,
    durationMs: Int = 600,
    content: @Composable () -> Unit,
) {
    var visible by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        delay(delayMs.toLong())
        visible = true
    }
    AnimatedVisibility(
        visible = visible,
        enter = fadeIn(animationSpec = tween(durationMs)) +
            slideInVertically(animationSpec = tween(durationMs), initialOffsetY = { it / 4 }),
    ) {
        content()
    }
}
