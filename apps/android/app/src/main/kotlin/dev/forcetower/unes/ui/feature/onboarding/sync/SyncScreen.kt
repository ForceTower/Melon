package dev.forcetower.unes.ui.feature.onboarding.sync

import androidx.annotation.StringRes
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
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
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dev.forcetower.unes.R
import dev.forcetower.unes.designsystem.foundation.Mesh
import dev.forcetower.unes.designsystem.foundation.MeshVariant
import dev.forcetower.unes.designsystem.foundation.fadeUpOnAppear
import dev.forcetower.unes.designsystem.theme.MelonTheme
import dev.forcetower.unes.designsystem.theme.melon
import dev.forcetower.unes.mvi.collectAsEffect
import dev.forcetower.unes.ui.feature.onboarding.components.LivePulseDot
import kotlin.math.roundToInt

private data class SyncStepDisplay(val key: String, @StringRes val labelRes: Int)

// Display-only metadata — keys and order must match SyncViewModel.SYNC_STEPS
// so the active-row indicator lines up with the animation driver.
private val SYNC_STEPS = listOf(
    SyncStepDisplay("auth", R.string.onboarding_sync_step_auth),
    SyncStepDisplay("profile", R.string.onboarding_sync_step_profile),
    SyncStepDisplay("schedule", R.string.onboarding_sync_step_schedule),
    SyncStepDisplay("classes", R.string.onboarding_sync_step_classes),
    SyncStepDisplay("grades", R.string.onboarding_sync_step_grades),
    SyncStepDisplay("msgs", R.string.onboarding_sync_step_messages),
)

@Composable
fun SyncScreen(
    firstName: String,
    onDone: () -> Unit,
    onAuthFailed: () -> Unit,
    vm: SyncViewModel = hiltViewModel(),
) {
    val state by vm.state.collectAsStateWithLifecycle()

    vm.effects.collectAsEffect { effect ->
        when (effect) {
            SyncEffect.Done -> onDone()
            SyncEffect.AuthFailed -> onAuthFailed()
        }
    }

    SyncContent(firstName = firstName, state = state)
}

@Composable
private fun SyncContent(
    firstName: String,
    state: SyncUiState,
) {

    val night = MaterialTheme.melon.fixed.night
    val veil = MaterialTheme.melon.fixed.nightVeil
    val cream = MaterialTheme.melon.fixed.surfaceLight
    val onHero = MaterialTheme.melon.fixed.onHero
    val live = MaterialTheme.melon.fixed.live
    val accent = MaterialTheme.colorScheme.primary

    val stepIdx = state.currentStepIdx
    val done = state.doneKeys
    val progress by animateFloatAsState(
        targetValue = (done.size.toFloat() / SYNC_STEPS.size).coerceAtMost(1f),
        animationSpec = tween(600, easing = LinearEasing),
        label = "progress",
    )

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
                        0f to veil.copy(alpha = 0.28f),
                        1f to veil.copy(alpha = 0.68f),
                    ),
                ),
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(start = 24.dp, end = 24.dp, top = 108.dp, bottom = 46.dp),
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fadeUpOnAppear(delayMs = 0, durationMs = 500),
            ) {
                LivePulseDot(color = live)
                Text(
                    text = stringResource(R.string.onboarding_sync_eyebrow).uppercase(),
                    style = MaterialTheme.typography.labelMedium.copy(
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 0.78.sp,
                    ),
                    color = onHero.copy(alpha = 0.92f),
                )
            }
            Spacer(Modifier.height(12.dp))
            Text(
                text = syncHeadline(firstName = firstName, cream = cream, accent = accent),
                style = MaterialTheme.typography.displayLarge.copy(
                    fontSize = 40.sp,
                    lineHeight = 41.sp,
                    letterSpacing = (-1.6).sp,
                    fontWeight = FontWeight.ExtraBold,
                ),
                modifier = Modifier.fadeUpOnAppear(delayMs = 100),
            )

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                contentAlignment = Alignment.Center,
            ) {
                SyncProgressRing(
                    progress = progress,
                    doneCount = done.size,
                    totalCount = SYNC_STEPS.size,
                )
            }

            // Step list — frosted glass card.
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(24.dp))
                    .background(onHero.copy(alpha = 0.08f))
                    .border(1.dp, onHero.copy(alpha = 0.10f), RoundedCornerShape(24.dp))
                    .padding(horizontal = 6.dp, vertical = 8.dp),
            ) {
                SYNC_STEPS.forEachIndexed { i, step ->
                    val isDone = done.contains(step.key)
                    val isActive = stepIdx == i && !isDone
                    val isPending = !isDone && !isActive
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .alpha(if (isPending) 0.4f else 1f)
                            .padding(horizontal = 14.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        Box(Modifier.size(20.dp), contentAlignment = Alignment.Center) {
                            when {
                                isDone -> Icon(
                                    imageVector = Icons.Filled.CheckCircle,
                                    contentDescription = null,
                                    tint = accent,
                                    modifier = Modifier.size(20.dp),
                                )
                                isActive -> StepSpinner(accent = accent, track = onHero.copy(alpha = 0.3f))
                                else -> Box(
                                    Modifier
                                        .size(6.dp)
                                        .clip(CircleShape)
                                        .background(onHero.copy(alpha = 0.35f)),
                                )
                            }
                        }
                        Text(
                            text = stringResource(step.labelRes),
                            style = MaterialTheme.typography.bodyLarge.copy(
                                fontSize = 15.sp,
                                fontWeight = if (isActive) FontWeight.Bold else FontWeight.Medium,
                                letterSpacing = (-0.15).sp,
                            ),
                            color = if (isDone) onHero.copy(alpha = 0.5f) else onHero,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun SyncProgressRing(progress: Float, doneCount: Int, totalCount: Int) {
    val onHero = MaterialTheme.melon.fixed.onHero
    val accent = MaterialTheme.colorScheme.primary

    val transition = rememberInfiniteTransition(label = "ring")
    val rotation by transition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(9000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart,
        ),
        label = "spin",
    )

    Box(modifier = Modifier.size(132.dp), contentAlignment = Alignment.Center) {
        // Outer rotating dashed halo.
        Canvas(
            modifier = Modifier
                .size(132.dp)
                .rotate(rotation),
        ) {
            val sw = 1.dp.toPx()
            drawCircle(
                color = onHero.copy(alpha = 0.1f),
                radius = (size.minDimension - sw) / 2f,
                style = Stroke(
                    width = sw,
                    pathEffect = PathEffect.dashPathEffect(floatArrayOf(2f * density, 5f * density)),
                ),
            )
        }
        // Track + progress arc.
        Canvas(modifier = Modifier.size(123.dp)) {
            val sw = 9.dp.toPx()
            val inset = sw / 2f
            val arcSize = Size(size.width - sw, size.height - sw)
            drawCircle(
                color = onHero.copy(alpha = 0.14f),
                radius = (size.minDimension - sw) / 2f,
                style = Stroke(width = sw),
            )
            drawArc(
                color = accent,
                startAngle = -90f,
                sweepAngle = 360f * progress,
                useCenter = false,
                topLeft = Offset(inset, inset),
                size = arcSize,
                style = Stroke(width = sw, cap = StrokeCap.Round),
            )
        }
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Row(verticalAlignment = Alignment.Bottom) {
                Text(
                    text = (progress * 100).roundToInt().toString(),
                    style = MaterialTheme.typography.displayLarge.copy(
                        fontSize = 44.sp,
                        lineHeight = 44.sp,
                        letterSpacing = (-1.76).sp,
                        fontWeight = FontWeight.ExtraBold,
                    ),
                    color = onHero,
                )
                Text(
                    text = stringResource(R.string.onboarding_sync_percent_sign),
                    style = MaterialTheme.typography.headlineSmall.copy(
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                    ),
                    color = onHero.copy(alpha = 0.6f),
                    modifier = Modifier.padding(start = 1.dp, bottom = 6.dp),
                )
            }
            Text(
                text = stringResource(R.string.onboarding_sync_step_count, doneCount, totalCount),
                style = MaterialTheme.typography.labelSmall.copy(
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 0.8.sp,
                ),
                color = onHero.copy(alpha = 0.5f),
                modifier = Modifier.padding(top = 4.dp),
            )
        }
    }
}

@Composable
private fun StepSpinner(accent: Color, track: Color) {
    val transition = rememberInfiniteTransition(label = "step-spin")
    val rotation by transition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(700, easing = LinearEasing),
            repeatMode = RepeatMode.Restart,
        ),
        label = "spin",
    )
    Canvas(
        modifier = Modifier
            .size(15.dp)
            .rotate(rotation),
    ) {
        val sw = 2.dp.toPx()
        drawCircle(
            color = track,
            radius = (size.minDimension - sw) / 2f,
            style = Stroke(width = sw),
        )
        drawArc(
            color = accent,
            startAngle = -90f,
            sweepAngle = 90f,
            useCenter = false,
            topLeft = Offset(sw / 2f, sw / 2f),
            size = Size(size.width - sw, size.height - sw),
            style = Stroke(width = sw, cap = StrokeCap.Round),
        )
    }
}

@Composable
private fun syncHeadline(firstName: String, cream: Color, accent: Color): AnnotatedString {
    val top = stringResource(R.string.onboarding_sync_headline_top)
    val fallback = stringResource(R.string.onboarding_sync_default_user)
    return buildAnnotatedString {
        withStyle(SpanStyle(color = cream)) { append("$top\n") }
        withStyle(SpanStyle(color = accent)) {
            append(firstName.ifBlank { fallback })
            append(".")
        }
    }
}

@Preview
@Composable
private fun SyncScreenPreview() {
    MelonTheme {
        SyncContent(
            firstName = "Joana",
            state = SyncUiState(
                currentStepIdx = 3,
                doneKeys = setOf("auth", "profile", "schedule"),
            ),
        )
    }
}
