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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.automirrored.filled.TrendingUp
import androidx.compose.material.icons.filled.WaterDrop
import androidx.compose.material3.Icon
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
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathMeasure
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
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
import dev.forcetower.unes.designsystem.foundation.scaleInOnAppear
import dev.forcetower.unes.designsystem.theme.MelonTheme
import dev.forcetower.unes.designsystem.theme.melon
import dev.forcetower.unes.ui.feature.disciplines.formatGrade
import dev.forcetower.unes.ui.feature.onboarding.components.LivePulseDot
import dev.forcetower.unes.ui.feature.onboarding.components.OnboardingPillButton
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

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
    val pageBg = MaterialTheme.colorScheme.background
    val success = MaterialTheme.melon.fixed.success

    Box(
        Modifier
            .fillMaxSize()
            .background(pageBg),
    ) {
        // Green-tinted mesh crown fading into the page background.
        Box(
            Modifier
                .fillMaxWidth()
                .height(230.dp),
        ) {
            Mesh(variant = MeshVariant.Fresh, intensity = 0.5f, modifier = Modifier.fillMaxSize())
            Box(
                Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            0f to Color.Transparent,
                            0.92f to pageBg,
                            1f to pageBg,
                        ),
                    ),
            )
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(start = 20.dp, end = 20.dp, top = 92.dp, bottom = 34.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            CheckBadge(accent = accent)

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier
                    .padding(top = 14.dp)
                    .fadeUpOnAppear(delayMs = 850, durationMs = 500),
            ) {
                LivePulseDot(color = success)
                Text(
                    text = stringResource(R.string.onboarding_ready_eyebrow).uppercase(),
                    style = MaterialTheme.typography.labelMedium.copy(
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 0.78.sp,
                    ),
                    color = success,
                )
            }

            Text(
                text = readyHeadline(firstName, ink, accent),
                style = MaterialTheme.typography.displayLarge.copy(
                    fontSize = 38.sp,
                    lineHeight = 39.sp,
                    letterSpacing = (-1.52).sp,
                    fontWeight = FontWeight.ExtraBold,
                ),
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 12.dp)
                    .fadeUpOnAppear(delayMs = 950),
            )
            Text(
                text = formatSemesterSummary(
                    context = context,
                    classCount = state.classCount,
                    totalCredits = state.totalCredits,
                    semesterCode = state.semesterCode,
                ),
                style = MaterialTheme.typography.bodyLarge.copy(
                    fontSize = 15.sp,
                    fontWeight = FontWeight.SemiBold,
                    letterSpacing = (-0.08).sp,
                ),
                color = ink3,
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 6.dp)
                    .fadeUpOnAppear(delayMs = 1020),
            )

            Spacer(Modifier.height(22.dp))

            val next = state.next
            if (next != null) {
                NextClassHero(
                    next = next,
                    modifier = Modifier.scaleInOnAppear(delayMs = 1120, durationMs = 600),
                )
            }

            val showScore = state.score != null
            val showAttendance = state.attendancePercent != null
            if (showScore || showAttendance) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 12.dp)
                        .fadeUpOnAppear(delayMs = 1220),
                ) {
                    if (showScore) {
                        ScoreStatCard(
                            score = state.score,
                            spark = state.scoreSpark,
                            modifier = Modifier.weight(1f),
                        )
                    }
                    if (showAttendance) {
                        AttendanceStatCard(
                            percent = state.attendancePercent ?: 0,
                            modifier = Modifier.weight(1f),
                        )
                    }
                }
            }

            Spacer(Modifier.height(28.dp))

            OnboardingPillButton(
                text = stringResource(R.string.onboarding_ready_cta),
                onClick = onEnter,
                showArrow = true,
                arrowIcon = Icons.AutoMirrored.Filled.ArrowForward,
                modifier = Modifier.fadeUpOnAppear(delayMs = 1340),
            )
        }
    }
}

// ───────── Check badge ─────────

@Composable
private fun CheckBadge(accent: Color) {
    val onHero = MaterialTheme.melon.fixed.onHero
    val circleScale = remember { Animatable(0.5f) }
    val circleAlpha = remember { Animatable(0f) }
    val checkProgress = remember { Animatable(0f) }

    LaunchedEffect(Unit) {
        delay(100)
        coroutineScope {
            launch {
                circleScale.animateTo(1f, tween(600, easing = CubicBezierEasing(0.2f, 0.9f, 0.3f, 1.5f)))
            }
            launch { circleAlpha.animateTo(1f, tween(300)) }
            launch {
                delay(400)
                checkProgress.animateTo(1f, tween(450, easing = CubicBezierEasing(0.4f, 0f, 0.2f, 1f)))
            }
        }
    }

    Box(
        modifier = Modifier
            .scale(circleScale.value)
            .shadow(12.dp, CircleShape, spotColor = accent.copy(alpha = 0.4f))
            .size(68.dp)
            .clip(CircleShape)
            .background(accent.copy(alpha = circleAlpha.value)),
        contentAlignment = Alignment.Center,
    ) {
        Canvas(Modifier.size(34.dp)) {
            val checkPath = Path().apply {
                moveTo(size.width * 0.26f, size.height * 0.51f)
                lineTo(size.width * 0.41f, size.height * 0.66f)
                lineTo(size.width * 0.74f, size.height * 0.32f)
            }
            val measure = PathMeasure().apply { setPath(checkPath, false) }
            val partial = Path()
            if (checkProgress.value > 0f) {
                measure.getSegment(0f, measure.length * checkProgress.value, partial, true)
                drawPath(
                    path = partial,
                    color = onHero,
                    style = Stroke(
                        width = 3.2.dp.toPx(),
                        cap = StrokeCap.Round,
                        join = StrokeJoin.Round,
                    ),
                )
            }
        }
    }
}

// ───────── Next class hero ─────────

@Composable
private fun NextClassHero(next: NextClassDisplay, modifier: Modifier = Modifier) {
    val night = MaterialTheme.melon.fixed.night
    val veil = MaterialTheme.melon.fixed.nightVeil
    val onHero = MaterialTheme.melon.fixed.onHero
    val live = MaterialTheme.melon.fixed.live
    val context = LocalContext.current

    Box(
        modifier = modifier
            .fillMaxWidth()
            .shadow(16.dp, RoundedCornerShape(26.dp))
            .clip(RoundedCornerShape(26.dp))
            .background(night),
    ) {
        Mesh(variant = MeshVariant.Cool, modifier = Modifier.fillMaxSize())
        Box(
            Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        0f to veil.copy(alpha = 0.15f),
                        1f to veil.copy(alpha = 0.6f),
                    ),
                ),
        )

        Column(Modifier.padding(start = 18.dp, end = 18.dp, top = 16.dp, bottom = 17.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(7.dp),
                ) {
                    LivePulseDot(color = live, size = 6.5.dp)
                    Text(
                        text = stringResource(R.string.onboarding_ready_next_class_label).uppercase(),
                        style = MaterialTheme.typography.labelMedium.copy(
                            fontSize = 11.5.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 0.23.sp,
                        ),
                        color = onHero.copy(alpha = 0.9f),
                    )
                }
                Text(
                    text = formatTimeRange(context, next.startRaw, next.endRaw),
                    style = MaterialTheme.typography.labelLarge.copy(
                        fontSize = 12.5.sp,
                        fontWeight = FontWeight.SemiBold,
                    ),
                    color = onHero.copy(alpha = 0.6f),
                )
            }
            Spacer(Modifier.height(13.dp))
            Row(verticalAlignment = Alignment.Bottom, horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                Column(Modifier.weight(1f)) {
                    Text(
                        text = next.disciplineName,
                        style = MaterialTheme.typography.headlineMedium.copy(
                            fontSize = 26.sp,
                            lineHeight = 27.sp,
                            letterSpacing = (-0.78).sp,
                            fontWeight = FontWeight.ExtraBold,
                        ),
                        color = onHero,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Spacer(Modifier.height(6.dp))
                    Text(
                        text = formatNextClassMeta(
                            context = context,
                            room = next.spaceLocation,
                            teacherName = next.teacherName,
                        ),
                        style = MaterialTheme.typography.bodyMedium.copy(fontSize = 13.sp),
                        color = onHero.copy(alpha = 0.8f),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
                CountdownRing(minutesUntil = next.startsInMinutes)
            }
        }
    }
}

@Composable
private fun CountdownRing(minutesUntil: Int) {
    val onHero = MaterialTheme.melon.fixed.onHero

    val (value, unitRes, fraction) = when {
        minutesUntil < 60 -> Triple(
            minutesUntil.coerceAtLeast(0).toString(),
            R.string.onboarding_ready_ring_unit_minutes,
            (60 - minutesUntil).coerceAtLeast(0) / 60f,
        )
        minutesUntil < 60 * 24 -> Triple(
            (minutesUntil / 60).toString(),
            R.string.onboarding_ready_ring_unit_hours,
            (24 - minutesUntil / 60) / 24f,
        )
        else -> Triple(
            (minutesUntil / (60 * 24)).toString(),
            R.string.onboarding_ready_ring_unit_days,
            0.1f,
        )
    }

    Box(Modifier.size(66.dp), contentAlignment = Alignment.Center) {
        Canvas(Modifier.size(66.dp)) {
            val sw = 5.dp.toPx()
            val inset = sw / 2f
            drawCircle(
                color = onHero.copy(alpha = 0.16f),
                radius = (size.minDimension - sw) / 2f,
                style = Stroke(width = sw),
            )
            drawArc(
                color = onHero,
                startAngle = -90f,
                sweepAngle = 360f * fraction.coerceIn(0.02f, 1f),
                useCenter = false,
                topLeft = Offset(inset, inset),
                size = Size(size.width - sw, size.height - sw),
                style = Stroke(width = sw, cap = StrokeCap.Round),
            )
        }
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = value,
                style = MaterialTheme.typography.headlineSmall.copy(
                    fontSize = 21.sp,
                    lineHeight = 21.sp,
                    letterSpacing = (-0.63).sp,
                    fontWeight = FontWeight.ExtraBold,
                ),
                color = onHero,
            )
            Text(
                text = stringResource(unitRes),
                style = MaterialTheme.typography.labelSmall.copy(
                    fontSize = 8.5.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 0.34.sp,
                ),
                color = onHero.copy(alpha = 0.65f),
            )
        }
    }
}

// ───────── Stat cards ─────────

@Composable
private fun StatCard(
    icon: ImageVector,
    label: String,
    tint: Color,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    Column(
        modifier = modifier
            .height(118.dp)
            .clip(RoundedCornerShape(20.dp))
            .background(MaterialTheme.melon.surface.card)
            .border(1.dp, MaterialTheme.melon.surface.line, RoundedCornerShape(20.dp))
            .padding(14.dp),
        verticalArrangement = Arrangement.SpaceBetween,
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = tint,
                modifier = Modifier.size(16.dp),
            )
            Text(
                text = label,
                style = MaterialTheme.typography.labelLarge.copy(
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                ),
                color = tint,
            )
        }
        content()
    }
}

@Composable
private fun ScoreStatCard(score: Double?, spark: List<Double>, modifier: Modifier = Modifier) {
    val warn = MaterialTheme.melon.status.warn
    val ink = MaterialTheme.colorScheme.onBackground

    StatCard(
        icon = Icons.AutoMirrored.Filled.TrendingUp,
        label = stringResource(R.string.onboarding_ready_stat_score),
        tint = warn,
        modifier = modifier,
    ) {
        Column {
            Text(
                text = formatGrade(score),
                style = MaterialTheme.typography.headlineMedium.copy(
                    fontSize = 30.sp,
                    lineHeight = 30.sp,
                    letterSpacing = (-1.2).sp,
                    fontWeight = FontWeight.ExtraBold,
                ),
                color = ink,
            )
            if (spark.size >= 2) {
                Spacer(Modifier.height(5.dp))
                Sparkline(
                    points = spark,
                    color = warn,
                    modifier = Modifier
                        .width(90.dp)
                        .height(26.dp),
                )
            }
        }
    }
}

@Composable
private fun Sparkline(points: List<Double>, color: Color, modifier: Modifier = Modifier) {
    Canvas(modifier) {
        val min = points.min()
        val max = points.max()
        val span = (max - min).takeIf { it > 0.0001 } ?: 1.0
        val stepX = size.width / (points.size - 1)
        val pad = 3.dp.toPx()
        fun yFor(value: Double): Float =
            pad + ((max - value) / span).toFloat() * (size.height - pad * 2)

        val path = Path()
        points.forEachIndexed { index, value ->
            val x = index * stepX
            val y = yFor(value)
            if (index == 0) path.moveTo(x, y) else path.lineTo(x, y)
        }
        drawPath(
            path = path,
            color = color,
            style = Stroke(width = 2.2.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round),
        )
        drawCircle(
            color = color,
            radius = 3.dp.toPx(),
            center = Offset(size.width, yFor(points.last())),
        )
    }
}

@Composable
private fun AttendanceStatCard(percent: Int, modifier: Modifier = Modifier) {
    val jade = MaterialTheme.melon.palette.jade
    val ink = MaterialTheme.colorScheme.onBackground
    val ink3 = MaterialTheme.colorScheme.onSurfaceVariant
    val track = MaterialTheme.colorScheme.surfaceContainerHighest

    StatCard(
        icon = Icons.Filled.WaterDrop,
        label = stringResource(R.string.onboarding_ready_stat_attendance),
        tint = jade,
        modifier = modifier,
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.Bottom,
        ) {
            val percentSign = stringResource(R.string.onboarding_ready_percent_sign)
            Text(
                text = buildAnnotatedString {
                    append(percent.toString())
                    withStyle(
                        SpanStyle(
                            fontSize = 15.sp,
                            color = ink3,
                            fontWeight = FontWeight.Bold,
                        ),
                    ) { append(percentSign) }
                },
                style = MaterialTheme.typography.headlineMedium.copy(
                    fontSize = 30.sp,
                    lineHeight = 30.sp,
                    letterSpacing = (-1.2).sp,
                    fontWeight = FontWeight.ExtraBold,
                ),
                color = ink,
            )
            Canvas(Modifier.size(46.dp)) {
                val sw = 6.dp.toPx()
                val inset = sw / 2f
                drawCircle(
                    color = track,
                    radius = (size.minDimension - sw) / 2f,
                    style = Stroke(width = sw),
                )
                drawArc(
                    color = jade,
                    startAngle = -90f,
                    sweepAngle = 360f * (percent / 100f),
                    useCenter = false,
                    topLeft = Offset(inset, inset),
                    size = Size(size.width - sw, size.height - sw),
                    style = Stroke(width = sw, cap = StrokeCap.Round),
                )
            }
        }
    }
}

// ───────── Formatting ─────────

@Composable
private fun readyHeadline(firstName: String, ink: Color, accent: Color): AnnotatedString {
    val top = stringResource(R.string.onboarding_ready_headline_top)
    val fallback = stringResource(R.string.onboarding_ready_default_user)
    return buildAnnotatedString {
        withStyle(SpanStyle(color = ink)) { append("$top ") }
        withStyle(SpanStyle(color = accent)) {
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

// KMP emits "HH:mm[:ss]" — trim to "HH:mm".
private fun trimTime(raw: String): String = raw.split(':').take(2).joinToString(":")

private fun formatTimeRange(context: android.content.Context, start: String, end: String?): String =
    if (end.isNullOrBlank()) {
        trimTime(start)
    } else {
        context.getString(R.string.onboarding_ready_time_range, trimTime(start), trimTime(end))
    }

private fun formatNextClassMeta(
    context: android.content.Context,
    room: String?,
    teacherName: String?,
): String = when {
    !room.isNullOrBlank() && !teacherName.isNullOrBlank() ->
        context.getString(R.string.onboarding_ready_next_class_meta_full, room, teacherName)
    !teacherName.isNullOrBlank() -> teacherName
    !room.isNullOrBlank() ->
        context.getString(R.string.onboarding_ready_next_class_meta_no_teacher, room)
    else -> context.getString(R.string.onboarding_ready_next_class_meta_empty)
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
                    disciplineName = "Cálculo II",
                    startRaw = "10:20:00",
                    endRaw = "12:00:00",
                    spaceLocation = "MT-14",
                    teacherName = "Adriana Matos",
                    startsInMinutes = 39,
                ),
                score = 8.5,
                scoreSpark = listOf(7.5, 7.4, 7.9, 7.8, 8.5),
                attendancePercent = 96,
            ),
            onEnter = {},
        )
    }
}
