package dev.forcetower.unes.ui.feature.onboarding

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.forcetower.unes.R
import dev.forcetower.unes.designsystem.foundation.Mesh
import dev.forcetower.unes.designsystem.foundation.MeshVariant
import dev.forcetower.unes.designsystem.theme.melon
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

private val DarkBg = Color(0xFF1A0F28)
private val SurfaceLight = Color(0xFFFBF7F2)

@Composable
fun SplashScreen(onDone: () -> Unit) {
    val wordmarkOffsetY = remember { Animatable(12f) }
    val wordmarkAlpha = remember { Animatable(0f) }
    val dotScale = remember { Animatable(0f) }
    val captionAlpha = remember { Animatable(0f) }
    val creditAlpha = remember { Animatable(0f) }

    LaunchedEffect(Unit) {
        launch {
            wordmarkOffsetY.animateTo(0f, tween(1100, easing = CubicBezierEasing(0.2f, 0.8f, 0.2f, 1f)))
        }
        launch {
            wordmarkAlpha.animateTo(1f, tween(1100, easing = CubicBezierEasing(0.2f, 0.8f, 0.2f, 1f)))
        }
        launch {
            delay(800)
            dotScale.animateTo(1f, spring(dampingRatio = 0.55f, stiffness = Spring.StiffnessMedium))
        }
        launch {
            delay(600)
            captionAlpha.animateTo(1f, tween(800))
        }
        launch {
            delay(1200)
            creditAlpha.animateTo(1f, tween(600))
        }
        delay(2600)
        onDone()
    }

    Box(
        Modifier
            .fillMaxSize()
            .background(DarkBg),
    ) {
        Mesh(variant = MeshVariant.Warm, modifier = Modifier.fillMaxSize())

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 28.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            Row(verticalAlignment = Alignment.Bottom) {
                Text(
                    text = stringResource(R.string.onboarding_splash_wordmark),
                    style = MaterialTheme.typography.displayLarge.copy(
                        fontSize = 88.sp,
                        lineHeight = 88.sp,
                        letterSpacing = (-3.5).sp,
                        fontWeight = FontWeight.Normal,
                    ),
                    color = SurfaceLight,
                    modifier = Modifier
                        .offset(y = wordmarkOffsetY.value.dp)
                        .alpha(wordmarkAlpha.value)
                        .padding(end = 2.dp),
                )
                // Dot sits roughly at the wordmark baseline — Bottom alignment
                // pins it to the descender line, then a small bottom padding
                // lifts it back up to where the baseline lives.
                Box(
                    Modifier
                        .padding(bottom = 12.dp)
                        .size(10.dp)
                        .scale(dotScale.value)
                        .clip(CircleShape)
                        .background(MaterialTheme.melon.brand.amber),
                )
            }

            Spacer(Modifier.height(18.dp))

            Text(
                text = stringResource(R.string.onboarding_splash_caption),
                style = MaterialTheme.typography.labelSmall.copy(
                    fontSize = 11.sp,
                    letterSpacing = 2.sp,
                    fontFamily = FontFamily.Monospace,
                ),
                color = SurfaceLight.copy(alpha = 0.55f),
                textAlign = TextAlign.Center,
                modifier = Modifier.alpha(captionAlpha.value),
            )
        }

        Row(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 50.dp)
                .alpha(creditAlpha.value),
            horizontalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Text(
                text = stringResource(R.string.onboarding_splash_credit_prefix),
                style = MaterialTheme.typography.bodyMedium.copy(fontSize = 13.sp),
                color = SurfaceLight.copy(alpha = 0.45f),
            )
            Text(
                text = stringResource(R.string.onboarding_splash_credit_university),
                style = MaterialTheme.typography.bodyMedium.copy(
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Medium,
                ),
                color = SurfaceLight.copy(alpha = 0.8f),
            )
            Text(
                text = stringResource(R.string.onboarding_splash_credit_location),
                style = MaterialTheme.typography.bodyMedium.copy(fontSize = 13.sp),
                color = SurfaceLight.copy(alpha = 0.45f),
            )
        }
    }
}
