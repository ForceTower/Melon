package dev.forcetower.unes.feature.onboarding

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
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
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
import dev.forcetower.unes.designsystem.foundation.Mesh
import dev.forcetower.unes.designsystem.foundation.MeshVariant
import dev.forcetower.unes.designsystem.theme.melon
import kotlinx.coroutines.delay
import kotlin.math.roundToInt

private val DarkBg = Color(0xFF1A0F28)
private val SurfaceLight = Color(0xFFFBF7F2)

private data class SyncStep(val key: String, val label: String)

private val SYNC_STEPS = listOf(
    SyncStep("auth", "Verificando matrícula"),
    SyncStep("profile", "Carregando seu perfil"),
    SyncStep("schedule", "Montando seu horário"),
    SyncStep("classes", "Conectando às suas turmas"),
    SyncStep("grades", "Baixando notas do semestre"),
    SyncStep("msgs", "Sincronizando recados"),
)

@Composable
fun SyncScreen(
    userId: String,
    onDone: () -> Unit,
) {
    var stepIdx by remember { mutableIntStateOf(0) }
    val done = remember { mutableStateListOf<String>() }

    LaunchedEffect(stepIdx) {
        if (stepIdx >= SYNC_STEPS.size) {
            delay(700)
            onDone()
            return@LaunchedEffect
        }
        val dwell = if (stepIdx == 0) 900L else (800L + (200..600).random())
        delay(dwell)
        done += SYNC_STEPS[stepIdx].key
        stepIdx += 1
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
                text = "◦ PREPARANDO SEU SEMESTRE",
                style = MaterialTheme.typography.labelSmall.copy(
                    fontSize = 11.sp,
                    letterSpacing = 1.8.sp,
                    fontFamily = FontFamily.Monospace,
                ),
                color = SurfaceLight.copy(alpha = 0.55f),
            )
            Spacer(Modifier.height(14.dp))
            Text(
                text = syncHeadline(userId, amber),
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
                            text = s.label,
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
                text = "Isso leva cerca de 12 segundos na sua conexão.",
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

private fun syncHeadline(userId: String, amber: Color): AnnotatedString =
    buildAnnotatedString {
        withStyle(SpanStyle(color = SurfaceLight)) { append("Quase lá,\n") }
        withStyle(SpanStyle(color = amber, fontStyle = FontStyle.Italic)) {
            append(userId.ifBlank { "estudante" })
            append(".")
        }
    }
