package dev.forcetower.unes.ui.feature.onboarding.sync

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
import androidx.compose.material.icons.filled.Check
import androidx.annotation.StringRes
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dev.forcetower.unes.R
import dev.forcetower.unes.designsystem.foundation.Mesh
import dev.forcetower.unes.designsystem.foundation.MeshVariant
import dev.forcetower.unes.designsystem.theme.melon
import dev.forcetower.unes.mvi.collectAsEffect
import kotlin.math.roundToInt

private val DarkBg = Color(0xFF1A0F28)
private val SurfaceLight = Color(0xFFFBF7F2)

private data class SyncStepDisplay(val key: String, @StringRes val labelRes: Int)

// Display-only metadata (label + key match what SyncViewModel runs). The
// orchestration durations live in SyncViewModel; this list just maps a key
// to its visible label.
private val SYNC_STEPS = listOf(
    SyncStepDisplay("auth", R.string.onboarding_sync_step_auth),
    SyncStepDisplay("profile", R.string.onboarding_sync_step_profile),
    SyncStepDisplay("classes", R.string.onboarding_sync_step_classes),
    SyncStepDisplay("schedule", R.string.onboarding_sync_step_schedule),
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
    val stepIdx = state.currentStepIdx
    val done = state.doneKeys

    vm.effects.collectAsEffect { effect ->
        when (effect) {
            SyncEffect.Done -> onDone()
            SyncEffect.AuthFailed -> onAuthFailed()
        }
    }

    val progress by animateFloatAsState(
        targetValue = (done.size.toFloat() / SYNC_STEPS.size).coerceAtMost(1f),
        animationSpec = tween(600, easing = LinearEasing),
        label = "progress",
    )

    val amber = MaterialTheme.melon.brand.amber

    Box(
        Modifier
            .fillMaxSize()
            .background(DarkBg),
    ) {
        Mesh(variant = MeshVariant.Warm, intensity = 0.9f, modifier = Modifier.fillMaxSize())

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(start = 28.dp, end = 28.dp, top = 120.dp, bottom = 60.dp),
        ) {
            Text(
                text = stringResource(R.string.onboarding_sync_eyebrow),
                style = MaterialTheme.typography.labelSmall.copy(
                    fontSize = 11.sp,
                    letterSpacing = 1.8.sp,
                    fontFamily = FontFamily.Monospace,
                ),
                color = SurfaceLight.copy(alpha = 0.55f),
            )
            Spacer(Modifier.height(14.dp))
            Text(
                text = syncHeadline(firstName, amber),
                style = MaterialTheme.typography.displayLarge.copy(
                    fontSize = 44.sp,
                    lineHeight = 44.sp,
                    letterSpacing = (-1.1).sp,
                    fontWeight = FontWeight.Normal,
                ),
            )

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .padding(top = 30.dp, bottom = 26.dp),
                contentAlignment = Alignment.Center,
            ) {
                ProgressOrb(
                    progress = progress,
                    doneCount = done.size,
                    totalCount = SYNC_STEPS.size,
                    amber = amber,
                )
            }

            // Step list
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(24.dp))
                    .background(Color.White.copy(alpha = 0.06f))
                    .border(1.dp, Color.White.copy(alpha = 0.08f), RoundedCornerShape(24.dp))
                    .padding(horizontal = 4.dp, vertical = 6.dp),
            ) {
                SYNC_STEPS.forEachIndexed { i, s ->
                    val isDone = done.contains(s.key)
                    val isActive = stepIdx == i && !isDone
                    val isPending = i > stepIdx
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 14.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        Box(
                            Modifier.size(20.dp),
                            contentAlignment = Alignment.Center,
                        ) {
                            when {
                                isDone -> StepCheck(amber = amber)
                                isActive -> StepSpinner(amber = amber)
                                else -> Box(
                                    Modifier
                                        .size(6.dp)
                                        .clip(CircleShape)
                                        .background(SurfaceLight.copy(alpha = 0.3f)),
                                )
                            }
                        }
                        Text(
                            text = stringResource(s.labelRes),
                            style = MaterialTheme.typography.bodyMedium.copy(
                                fontSize = 14.sp,
                                fontWeight = if (isActive) FontWeight.Medium else FontWeight.Normal,
                                letterSpacing = (-0.07).sp,
                                textDecoration = if (isDone) TextDecoration.LineThrough else TextDecoration.None,
                            ),
                            color = if (isDone) {
                                SurfaceLight.copy(alpha = 0.55f)
                            } else if (isPending) {
                                SurfaceLight.copy(alpha = 0.35f)
                            } else {
                                SurfaceLight
                            },
                        )
                    }
                }
            }

            Spacer(Modifier.height(20.dp))

            Text(
                text = stringResource(R.string.onboarding_sync_footer),
                style = MaterialTheme.typography.labelSmall.copy(
                    fontSize = 10.sp,
                    letterSpacing = 1.sp,
                    fontFamily = FontFamily.Monospace,
                ),
                color = SurfaceLight.copy(alpha = 0.4f),
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}

@Composable
private fun ProgressOrb(progress: Float, doneCount: Int, totalCount: Int, amber: Color) {
    val transition = rememberInfiniteTransition(label = "orb")
    val rotation by transition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(8000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart,
        ),
        label = "spin",
    )

    Box(
        modifier = Modifier.size(140.dp),
        contentAlignment = Alignment.Center,
    ) {
        // outer rotating dashed ring
        Canvas(
            modifier = Modifier
                .size(140.dp)
                .rotate(rotation),
        ) {
            val sw = 1.dp.toPx()
            drawCircle(
                color = SurfaceLight.copy(alpha = 0.1f),
                radius = (size.minDimension - sw) / 2f,
                style = Stroke(
                    width = sw,
                    pathEffect = PathEffect.dashPathEffect(floatArrayOf(2f * density, 4f * density)),
                ),
            )
        }
        // progress arc
        Canvas(modifier = Modifier.size(140.dp)) {
            val sw = 2.5.dp.toPx()
            drawArc(
                color = amber,
                startAngle = -90f,
                sweepAngle = 360f * progress,
                useCenter = false,
                topLeft = Offset(sw / 2f, sw / 2f),
                size = Size(size.width - sw, size.height - sw),
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
                        letterSpacing = (-1.3).sp,
                    ),
                    color = SurfaceLight,
                )
                Text(
                    text = "%",
                    style = MaterialTheme.typography.headlineSmall.copy(fontSize = 20.sp),
                    color = SurfaceLight.copy(alpha = 0.55f),
                    modifier = Modifier.padding(start = 2.dp, bottom = 8.dp),
                )
            }
            Text(
                text = "$doneCount/$totalCount",
                style = MaterialTheme.typography.labelSmall.copy(
                    fontSize = 9.sp,
                    letterSpacing = 1.6.sp,
                    fontFamily = FontFamily.Monospace,
                ),
                color = SurfaceLight.copy(alpha = 0.4f),
                modifier = Modifier.padding(top = 6.dp),
            )
        }
    }
}

@Composable
private fun StepCheck(amber: Color) {
    Box(
        Modifier
            .size(20.dp)
            .clip(CircleShape)
            .background(amber),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            imageVector = Icons.Filled.Check,
            contentDescription = null,
            tint = DarkBg,
            modifier = Modifier.size(12.dp),
        )
    }
}

@Composable
private fun StepSpinner(amber: Color) {
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
            .size(14.dp)
            .rotate(rotation),
    ) {
        val sw = 2.dp.toPx()
        // background ring
        drawCircle(
            color = SurfaceLight.copy(alpha = 0.3f),
            radius = (size.minDimension - sw) / 2f,
            style = Stroke(width = sw),
        )
        // arc on top
        drawArc(
            color = amber,
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
private fun syncHeadline(firstName: String, amber: Color): AnnotatedString {
    val top = stringResource(R.string.onboarding_sync_headline_top)
    val fallback = stringResource(R.string.onboarding_sync_default_user)
    return buildAnnotatedString {
        withStyle(SpanStyle(color = SurfaceLight)) { append("$top\n") }
        withStyle(SpanStyle(color = amber, fontStyle = FontStyle.Italic)) {
            append(firstName.ifBlank { fallback })
            append(".")
        }
    }
}
