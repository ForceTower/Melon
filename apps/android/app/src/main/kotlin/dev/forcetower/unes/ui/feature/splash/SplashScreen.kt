package dev.forcetower.unes.ui.feature.splash

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
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
import dev.forcetower.unes.mvi.collectAsEffect
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

// Minimum time the splash stays up before flowing into Welcome — only for
// users who still need onboarding. Mirrors iOS OnboardingFeature (2.6s).
private const val SplashHoldMillis = 2_600L

@Composable
fun SplashScreen(
    onGoHome: () -> Unit,
    onGoOnboarding: () -> Unit,
    vm: SplashViewModel = hiltViewModel(),
) {
    var holdDone by remember { mutableStateOf(false) }
    var pendingOnboarding by remember { mutableStateOf(false) }

    // Connected users jump straight to the shell the moment the session state
    // is known — the splash choreography never gates a returning user (same
    // behavior as iOS RootFeature.bootstrap). Only the onboarding path holds
    // for the intro animation.
    vm.effects.collectAsEffect { effect ->
        when (effect) {
            SplashEffect.GoHome -> onGoHome()
            SplashEffect.GoOnboarding -> if (holdDone) onGoOnboarding() else pendingOnboarding = true
        }
    }

    LaunchedEffect(Unit) {
        delay(SplashHoldMillis)
        holdDone = true
        if (pendingOnboarding) onGoOnboarding()
    }

    SplashContent()
}

@Composable
private fun SplashContent() {

    val night = MaterialTheme.melon.fixed.night
    val veil = MaterialTheme.melon.fixed.nightVeil
    val cream = MaterialTheme.melon.fixed.surfaceLight
    val onHero = MaterialTheme.melon.fixed.onHero
    val brand = MaterialTheme.melon.brand
    val accent = MaterialTheme.colorScheme.primary

    val iconScale = remember { Animatable(0.7f) }
    val iconAlpha = remember { Animatable(0f) }
    val dotScale = remember { Animatable(0f) }

    LaunchedEffect(Unit) {
        launch { iconAlpha.animateTo(1f, tween(800)) }
        launch {
            iconScale.animateTo(1f, tween(800, easing = CubicBezierEasing(0.2f, 0.9f, 0.3f, 1.3f)))
        }
        launch {
            delay(700)
            dotScale.animateTo(1f, tween(500, easing = CubicBezierEasing(0.2f, 0.9f, 0.3f, 1.6f)))
        }
    }

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
                        0f to veil.copy(alpha = 0.2f),
                        1f to veil.copy(alpha = 0.58f),
                    ),
                ),
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 28.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(22.dp, Alignment.CenterVertically),
        ) {
            // App-icon tile — brand gradient plate with the lowercase glyph.
            Box(
                modifier = Modifier
                    .scale(iconScale.value)
                    .alpha(iconAlpha.value)
                    .shadow(14.dp, RoundedCornerShape(22.dp))
                    .size(80.dp)
                    .clip(RoundedCornerShape(22.dp))
                    .background(
                        Brush.linearGradient(
                            0f to brand.amber,
                            0.52f to brand.coral,
                            1f to brand.magenta,
                        ),
                    ),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = stringResource(R.string.onboarding_splash_icon_letter),
                    style = MaterialTheme.typography.displayLarge.copy(
                        fontSize = 42.sp,
                        lineHeight = 42.sp,
                        letterSpacing = (-2.1).sp,
                        fontWeight = FontWeight.ExtraBold,
                    ),
                    color = onHero,
                )
            }

            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.fadeUpOnAppear(delayMs = 250, durationMs = 700, fromOffset = 16.dp),
            ) {
                Row(verticalAlignment = Alignment.Bottom) {
                    Text(
                        text = stringResource(R.string.onboarding_splash_wordmark),
                        style = MaterialTheme.typography.displayLarge.copy(
                            fontSize = 48.sp,
                            lineHeight = 48.sp,
                            letterSpacing = (-2.4).sp,
                            fontWeight = FontWeight.ExtraBold,
                        ),
                        color = cream,
                        modifier = Modifier.padding(end = 3.dp),
                    )
                    Box(
                        Modifier
                            .padding(bottom = 7.dp)
                            .size(9.dp)
                            .scale(dotScale.value)
                            .clip(CircleShape)
                            .background(accent),
                    )
                }
                Spacer(Modifier.height(14.dp))
                Text(
                    text = stringResource(R.string.onboarding_splash_caption),
                    style = MaterialTheme.typography.bodyMedium.copy(
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold,
                        letterSpacing = 0.26.sp,
                    ),
                    color = cream.copy(alpha = 0.6f),
                    textAlign = TextAlign.Center,
                )
            }
        }

        Column(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 54.dp)
                .fadeUpOnAppear(delayMs = 900, durationMs = 600),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            SplashSpinner()
            Spacer(Modifier.height(16.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    text = stringResource(R.string.onboarding_splash_credit_prefix),
                    style = MaterialTheme.typography.bodyMedium.copy(fontSize = 13.sp),
                    color = cream.copy(alpha = 0.5f),
                )
                Text(
                    text = stringResource(R.string.onboarding_splash_credit_university),
                    style = MaterialTheme.typography.bodyMedium.copy(
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                    ),
                    color = cream.copy(alpha = 0.82f),
                )
                Text(
                    text = stringResource(R.string.onboarding_splash_credit_location),
                    style = MaterialTheme.typography.bodyMedium.copy(fontSize = 13.sp),
                    color = cream.copy(alpha = 0.5f),
                )
            }
        }
    }
}

@Composable
private fun SplashSpinner() {
    val cream = MaterialTheme.melon.fixed.surfaceLight
    val transition = rememberInfiniteTransition(label = "splash-spinner")
    val rotation by transition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(800, easing = LinearEasing),
            repeatMode = RepeatMode.Restart,
        ),
        label = "spin",
    )
    Canvas(
        Modifier
            .size(30.dp)
            .rotate(rotation),
    ) {
        val sw = 3.dp.toPx()
        val inset = sw / 2f
        drawCircle(
            color = cream.copy(alpha = 0.2f),
            radius = (size.minDimension - sw) / 2f,
            style = Stroke(width = sw),
        )
        drawArc(
            color = cream.copy(alpha = 0.8f),
            startAngle = -90f,
            sweepAngle = 100f,
            useCenter = false,
            topLeft = androidx.compose.ui.geometry.Offset(inset, inset),
            size = androidx.compose.ui.geometry.Size(size.width - sw, size.height - sw),
            style = Stroke(width = sw, cap = StrokeCap.Round),
        )
    }
}

@Preview
@Composable
private fun SplashScreenPreview() {
    MelonTheme { SplashContent() }
}
