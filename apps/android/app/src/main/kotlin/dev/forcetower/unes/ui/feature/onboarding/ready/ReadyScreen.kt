package dev.forcetower.unes.ui.feature.onboarding.ready

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.CubicBezierEasing
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
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.ui.platform.LocalContext
import dev.forcetower.unes.R
import dev.forcetower.unes.designsystem.components.MelonPrimaryButton
import dev.forcetower.unes.designsystem.foundation.Mesh
import dev.forcetower.unes.designsystem.foundation.MeshVariant
import dev.forcetower.unes.designsystem.foundation.fadeUpOnAppear
import dev.forcetower.unes.designsystem.theme.MelonTheme
import dev.forcetower.unes.designsystem.theme.melon
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

private val SurfaceLight = Color(0xFFFBF7F2)

@Composable
fun ReadyScreen(
    firstName: String,
    onEnter: () -> Unit,
    vm: ReadyViewModel = hiltViewModel(),
) {
    val state by vm.state.collectAsStateWithLifecycle()
    ReadyContent(firstName = firstName, state = state, onEnter = onEnter)
}

@Composable
private fun ReadyContent(
    firstName: String,
    state: ReadyUiState,
    onEnter: () -> Unit,
) {
    val context = LocalContext.current

    val ink = MaterialTheme.colorScheme.onBackground
    val ink3 = MaterialTheme.colorScheme.onSurfaceVariant
    val accent = MaterialTheme.colorScheme.primary
    val surface = MaterialTheme.colorScheme.surface
    val line = MaterialTheme.melon.surface.line

    val circleScale = remember { Animatable(0.92f) }
    val circleAlpha = remember { Animatable(0f) }
    val ringProgress = remember { Animatable(0f) }
    val checkProgress = remember { Animatable(0f) }
    val eyebrowAlpha = remember { Animatable(0f) }

    LaunchedEffect(Unit) {
        delay(100)
        coroutineScope {
            launch { circleScale.animateTo(1f, tween(600, easing = CubicBezierEasing(0.2f, 0.9f, 0.3f, 1.5f))) }
            launch { circleAlpha.animateTo(1f, tween(500)) }
        }
        ringProgress.animateTo(1f, tween(700, easing = CubicBezierEasing(0.4f, 0f, 0.2f, 1f)))
        checkProgress.animateTo(1f, tween(400, easing = CubicBezierEasing(0.4f, 0f, 0.2f, 1f)))
        eyebrowAlpha.animateTo(1f, tween(500))
    }

    Box(
        Modifier
            .fillMaxSize()
            .background(surface),
    ) {
        // Mesh hero faded into surface bottom-half.
        Box(
            Modifier
                .fillMaxWidth()
                .height(420.dp),
        ) {
            Mesh(variant = MeshVariant.Fresh, intensity = 0.9f, modifier = Modifier.fillMaxSize())
            Box(
                Modifier
                    .fillMaxSize()
                    .background(
                        androidx.compose.ui.graphics.Brush.verticalGradient(
                            0f to Color.Transparent,
                            0.5f to Color.Transparent,
                            1f to surface,
                        ),
                    ),
            )
        }

        // Check medallion
        Column(
            modifier = Modifier
                .padding(top = 120.dp)
                .fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Box(
                modifier = Modifier
                    .size(96.dp)
                    .scale(circleScale.value)
                    .clip(CircleShape)
                    .background(SurfaceLight.copy(alpha = circleAlpha.value * 0.15f))
                    .border(1.dp, SurfaceLight.copy(alpha = circleAlpha.value * 0.25f), CircleShape),
                contentAlignment = Alignment.Center,
            ) {
                CheckMedallion(
                    ringProgress = ringProgress.value,
                    checkProgress = checkProgress.value,
                )
            }
            Spacer(Modifier.height(20.dp))
            Text(
                text = stringResource(R.string.onboarding_ready_eyebrow),
                style = MaterialTheme.typography.labelSmall.copy(
                    fontSize = 12.sp,
                    letterSpacing = 1.4.sp,
                    fontWeight = FontWeight.Medium,
                ),
                color = SurfaceLight.copy(alpha = eyebrowAlpha.value * 0.7f),
            )
        }

        // Body
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(top = 320.dp, start = 28.dp, end = 28.dp, bottom = 40.dp),
        ) {
            Text(
                text = readyHeadline(firstName, ink, accent),
                style = MaterialTheme.typography.displayLarge.copy(
                    fontSize = 44.sp,
                    lineHeight = 44.sp,
                    letterSpacing = (-1.1).sp,
                    fontWeight = FontWeight.Normal,
                ),
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .fillMaxWidth()
                    .fadeUpOnAppear(delayMs = 1200),
            )
            Spacer(Modifier.height(10.dp))
            Text(
                text = formatSemesterSummary(
                    context = context,
                    classCount = state.classCount,
                    totalCredits = state.totalCredits,
                    semesterCode = state.semesterCode,
                ),
                style = MaterialTheme.typography.bodyLarge.copy(
                    fontSize = 15.sp,
                    lineHeight = 22.sp,
                    letterSpacing = (-0.08).sp,
                ),
                color = ink3,
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .fillMaxWidth()
                    .fadeUpOnAppear(delayMs = 1300, durationMs = 500),
            )
            Spacer(Modifier.height(22.dp))

            // Preview card — only rendered when there's a real next class.
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                contentAlignment = Alignment.Center,
            ) {
                val next = state.next
                if (next != null) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .fadeUpOnAppear(delayMs = 1400)
                            .clip(RoundedCornerShape(22.dp))
                            .background(surface)
                            .border(1.dp, line, RoundedCornerShape(22.dp))
                            .padding(horizontal = 18.dp, vertical = 16.dp),
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Text(
                                text = stringResource(R.string.onboarding_ready_next_class_label),
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontSize = 10.sp,
                                    letterSpacing = 1.5.sp,
                                    fontFamily = FontFamily.Monospace,
                                ),
                                color = ink3,
                            )
                            Text(
                                text = formatCountdown(context, next.startsInMinutes),
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontSize = 11.sp,
                                    fontFamily = FontFamily.Monospace,
                                ),
                                color = accent,
                            )
                        }
                        Spacer(Modifier.height(10.dp))
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                        ) {
                            Box(
                                Modifier
                                    .size(48.dp)
                                    .clip(RoundedCornerShape(14.dp)),
                            ) {
                                Mesh(
                                    variant = MeshVariant.Cool,
                                    modifier = Modifier.fillMaxSize(),
                                )
                            }
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = next.disciplineName,
                                    style = MaterialTheme.typography.headlineSmall.copy(
                                        fontSize = 20.sp,
                                        lineHeight = 22.sp,
                                        letterSpacing = (-0.2).sp,
                                    ),
                                    color = ink,
                                )
                                Spacer(Modifier.height(2.dp))
                                Text(
                                    text = formatNextClassMeta(
                                        context = context,
                                        timeRaw = next.timeRaw,
                                        room = next.spaceLocation,
                                        teacherFirstName = next.teacherFirstName,
                                    ),
                                    style = MaterialTheme.typography.bodyMedium.copy(fontSize = 13.sp),
                                    color = ink3,
                                )
                            }
                        }
                    }
                }
            }

            MelonPrimaryButton(
                text = stringResource(R.string.onboarding_ready_cta),
                onClick = onEnter,
                modifier = Modifier.fadeUpOnAppear(delayMs = 1500),
            )
        }
    }
}

@Composable
private fun CheckMedallion(ringProgress: Float, checkProgress: Float) {
    Canvas(modifier = Modifier.size(54.dp)) {
        val sw = 2.dp.toPx()
        val cx = size.width / 2f
        val cy = size.height / 2f
        val r = (size.minDimension - sw) / 2f
        // ring
        drawArc(
            color = SurfaceLight,
            startAngle = -90f,
            sweepAngle = 360f * ringProgress,
            useCenter = false,
            topLeft = Offset(sw / 2f, sw / 2f),
            size = Size(size.width - sw, size.height - sw),
            style = Stroke(width = sw, cap = StrokeCap.Round),
        )
        // check
        if (checkProgress > 0f) {
            val checkPath = Path().apply {
                moveTo(size.width * 0.32f, size.height * 0.5f)
                lineTo(size.width * 0.45f, size.height * 0.63f)
                lineTo(size.width * 0.71f, size.height * 0.37f)
            }
            // Animate as a length-based reveal by intersecting against the
            // checkProgress fraction. PathMeasure would be cleaner but the
            // straight-line check is short enough that two segments suffice.
            val pathEffect = PathEffect.cornerPathEffect(2.dp.toPx())
            drawPath(
                path = checkPath,
                color = SurfaceLight,
                style = Stroke(
                    width = 3.dp.toPx() * checkProgress,
                    cap = StrokeCap.Round,
                    join = StrokeJoin.Round,
                    pathEffect = pathEffect,
                ),
            )
        }
    }
}

@Composable
private fun readyHeadline(firstName: String, ink: Color, accent: Color): AnnotatedString {
    val top = stringResource(R.string.onboarding_ready_headline_top)
    val fallback = stringResource(R.string.onboarding_ready_default_user)
    return buildAnnotatedString {
        withStyle(SpanStyle(color = ink)) { append("$top\n") }
        withStyle(SpanStyle(color = accent, fontStyle = FontStyle.Italic)) {
            append(firstName.ifBlank { fallback })
            append(".")
        }
    }
}

private fun formatSemesterSummary(
    context: android.content.Context,
    classCount: Int,
    totalCredits: Int,
    semesterCode: String?,
): String {
    val classes = context.resources.getQuantityString(
        R.plurals.onboarding_ready_classes,
        classCount,
        classCount,
    )
    val credits = context.resources.getQuantityString(
        R.plurals.onboarding_ready_credits,
        totalCredits,
        totalCredits,
    )
    return if (semesterCode.isNullOrBlank()) {
        context.getString(R.string.onboarding_ready_semester_summary_no_code, classes, credits)
    } else {
        context.getString(
            R.string.onboarding_ready_semester_summary_format,
            classes,
            credits,
            semesterCode,
        )
    }
}

private fun formatCountdown(context: android.content.Context, minutesUntil: Int): String {
    if (minutesUntil <= 0) return context.getString(R.string.onboarding_ready_starts_in_now)
    val days = minutesUntil / (60 * 24)
    val hoursTotal = minutesUntil / 60
    val hoursOfDay = hoursTotal % 24
    val minutesOfHour = minutesUntil % 60
    return when {
        days >= 1 && hoursOfDay > 0 -> context.getString(
            R.string.onboarding_ready_starts_in_day_hour,
            days,
            hoursOfDay,
        )
        days >= 1 -> context.getString(R.string.onboarding_ready_starts_in_days, days)
        hoursTotal >= 1 && minutesOfHour > 0 -> context.getString(
            R.string.onboarding_ready_starts_in_hour_min,
            hoursTotal,
            minutesOfHour,
        )
        hoursTotal >= 1 -> context.getString(R.string.onboarding_ready_starts_in_hours, hoursTotal)
        else -> context.getString(R.string.onboarding_ready_starts_in_min, minutesUntil)
    }
}

@Preview
@Composable
private fun ReadyScreenPreview() {
    MelonTheme {
        ReadyContent(
            firstName = "Joana",
            state = ReadyUiState(
                loading = false,
                semesterCode = "2026.1",
                classCount = 6,
                totalCredits = 24,
                next = NextClassDisplay(
                    disciplineName = "Cálculo III",
                    timeRaw = "08:00",
                    spaceLocation = "Sala 304",
                    teacherFirstName = "Marina",
                    startsInMinutes = 45,
                ),
            ),
            onEnter = {},
        )
    }
}

private fun formatNextClassMeta(
    context: android.content.Context,
    timeRaw: String,
    room: String?,
    teacherFirstName: String?,
): String {
    // iOS trims to "HH:mm". The shared model already returns "HH:mm[:ss]" — drop seconds if present.
    val time = timeRaw.split(':').take(2).joinToString(":")
    return when {
        !room.isNullOrBlank() && !teacherFirstName.isNullOrBlank() ->
            context.getString(R.string.onboarding_ready_next_class_meta_full, time, room, teacherFirstName)
        !teacherFirstName.isNullOrBlank() ->
            context.getString(R.string.onboarding_ready_next_class_meta_no_room, time, teacherFirstName)
        !room.isNullOrBlank() ->
            context.getString(R.string.onboarding_ready_next_class_meta_no_teacher, time, room)
        else ->
            context.getString(R.string.onboarding_ready_next_class_meta_time_only, time)
    }
}
