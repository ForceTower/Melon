package dev.forcetower.unes.ui.feature.enrollment.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.forcetower.melon.feature.enrollment.domain.model.EnrollmentWindow
import dev.forcetower.melon.feature.enrollment.domain.model.EnrollmentWindowState
import dev.forcetower.unes.R
import dev.forcetower.unes.designsystem.foundation.Mesh
import dev.forcetower.unes.designsystem.foundation.MeshVariant
import dev.forcetower.unes.designsystem.theme.melon
import dev.forcetower.unes.ui.feature.enrollment.EnrollmentFormat

// Status-hub mesh hero (dc `MatriculaScreen` live-activity card): always-dark
// warm brand field, state eyebrow with the pulsing live dot, the deadline
// headline, the days-left ring and the abertura/encerramento strip.
@Composable
internal fun EnrollmentHero(window: EnrollmentWindow, nowMillis: Long, modifier: Modifier = Modifier) {
    val fixed = MaterialTheme.melon.fixed
    val onHero = fixed.onHero
    val shape = RoundedCornerShape(28.dp)
    val start = EnrollmentFormat.parseDate(window.startDate)
    val end = EnrollmentFormat.parseDate(window.endDate)

    Box(
        modifier = modifier
            .fillMaxWidth()
            .shadow(14.dp, shape, spotColor = fixed.heroNight, ambientColor = fixed.heroNight)
            .clip(shape)
            .background(fixed.heroNight),
    ) {
        Mesh(variant = MeshVariant.Hero, modifier = Modifier.matchParentSize())
        Box(
            modifier = Modifier
                .matchParentSize()
                .background(
                    Brush.linearGradient(
                        colors = listOf(
                            fixed.heroVeil.copy(alpha = 0.14f),
                            fixed.heroVeil.copy(alpha = 0.60f),
                        ),
                        start = Offset.Zero,
                        end = Offset.Infinite,
                    ),
                ),
        )

        Column(modifier = Modifier.padding(horizontal = 20.dp, vertical = 18.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .clip(CircleShape)
                            .background(
                                when (window.state) {
                                    EnrollmentWindowState.Open -> fixed.live
                                    EnrollmentWindowState.Upcoming -> MaterialTheme.melon.brand.amber
                                    else -> onHero.copy(alpha = 0.5f)
                                },
                            ),
                    )
                    Text(
                        text = stringResource(
                            when (window.state) {
                                EnrollmentWindowState.Open -> R.string.enrollment_hero_state_open
                                EnrollmentWindowState.Upcoming -> R.string.enrollment_hero_state_upcoming
                                else -> R.string.enrollment_hero_state_closed
                            },
                        ).uppercase(),
                        style = MaterialTheme.typography.labelMedium.copy(
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.2.sp,
                        ),
                        color = onHero.copy(alpha = 0.92f),
                    )
                }
                Text(
                    text = window.semester,
                    style = MaterialTheme.typography.labelMedium.copy(
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold,
                    ),
                    color = onHero.copy(alpha = 0.62f),
                )
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 16.dp),
                verticalAlignment = Alignment.Bottom,
                horizontalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = heroTitle(window, end),
                        style = MaterialTheme.typography.headlineMedium.copy(
                            fontSize = 25.sp,
                            lineHeight = 27.sp,
                            fontWeight = FontWeight.ExtraBold,
                            letterSpacing = (-0.75).sp,
                        ),
                        color = onHero,
                    )
                    Text(
                        text = stringResource(
                            when (window.state) {
                                EnrollmentWindowState.Open -> R.string.enrollment_hero_body_open
                                EnrollmentWindowState.Upcoming -> R.string.enrollment_hero_body_upcoming
                                else -> R.string.enrollment_hero_body_closed
                            },
                        ),
                        style = MaterialTheme.typography.bodySmall.copy(
                            fontSize = 13.5.sp,
                            lineHeight = 19.sp,
                        ),
                        color = onHero.copy(alpha = 0.8f),
                        modifier = Modifier
                            .padding(top = 8.dp)
                            .widthIn(max = 210.dp),
                    )
                }
                when (window.state) {
                    EnrollmentWindowState.Open -> if (start != null && end != null) {
                        DaysLeftRing(
                            days = EnrollmentFormat.daysLeft(end, nowMillis),
                            fraction = EnrollmentFormat.remainingFraction(start, end, nowMillis),
                            onHero = onHero,
                        )
                    }
                    EnrollmentWindowState.Upcoming -> if (start != null) {
                        DaysLeftRing(
                            days = EnrollmentFormat.daysLeft(start, nowMillis),
                            fraction = 1f,
                            onHero = onHero,
                        )
                    }
                    else -> SubmittedBadge(onHero = onHero)
                }
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 18.dp)
                    .drawBehind {
                        drawLine(
                            color = onHero.copy(alpha = 0.16f),
                            start = Offset.Zero,
                            end = Offset(size.width, 0f),
                            strokeWidth = 1.dp.toPx(),
                        )
                    }
                    .padding(top = 14.dp),
            ) {
                HeroDateCell(
                    label = stringResource(R.string.enrollment_hero_start_label),
                    value = start?.let { EnrollmentFormat.shortDate(it) },
                    onHero = onHero,
                    modifier = Modifier.weight(1f),
                )
                Box(
                    modifier = Modifier
                        .padding(horizontal = 16.dp)
                        .size(width = 1.dp, height = 34.dp)
                        .background(onHero.copy(alpha = 0.18f)),
                )
                HeroDateCell(
                    label = stringResource(R.string.enrollment_hero_end_label),
                    value = end?.let {
                        stringResource(
                            R.string.enrollment_hero_end_value_format,
                            EnrollmentFormat.shortDate(it),
                            EnrollmentFormat.timeLabel(it),
                        )
                    },
                    onHero = onHero,
                    modifier = Modifier.weight(1f),
                )
            }
        }
    }
}

@Composable
private fun heroTitle(window: EnrollmentWindow, end: java.time.OffsetDateTime?): String =
    when (window.state) {
        EnrollmentWindowState.Open ->
            end?.let { stringResource(R.string.enrollment_hero_open_title_format, EnrollmentFormat.shortDate(it)) }
                ?: stringResource(R.string.enrollment_hero_state_open)
        EnrollmentWindowState.Upcoming ->
            EnrollmentFormat.parseDate(window.startDate)
                ?.let { stringResource(R.string.enrollment_hero_upcoming_title_format, EnrollmentFormat.shortDate(it)) }
                ?: stringResource(R.string.enrollment_hero_state_upcoming)
        else -> stringResource(R.string.enrollment_hero_closed_title)
    }

@Composable
private fun HeroDateCell(label: String, value: String?, onHero: Color, modifier: Modifier = Modifier) {
    Column(modifier = modifier) {
        Text(
            text = label.uppercase(),
            style = MaterialTheme.typography.labelSmall.copy(
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.sp,
            ),
            color = onHero.copy(alpha = 0.6f),
        )
        Text(
            text = value ?: stringResource(R.string.enrollment_date_unknown),
            style = MaterialTheme.typography.titleSmall.copy(
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
            ),
            color = onHero,
            modifier = Modifier.padding(top = 3.dp),
        )
    }
}

// dc hero ring: full circle, 6dp stroke, fill = remaining fraction of the
// window, the big day count in the middle.
@Composable
private fun DaysLeftRing(days: Int, fraction: Float, onHero: Color) {
    Box(modifier = Modifier.size(78.dp), contentAlignment = Alignment.Center) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val stroke = Stroke(width = 6.dp.toPx(), cap = StrokeCap.Round)
            val inset = 3.dp.toPx()
            val arcSize = androidx.compose.ui.geometry.Size(size.width - inset * 2, size.height - inset * 2)
            val topLeft = Offset(inset, inset)
            drawArc(
                color = onHero.copy(alpha = 0.18f),
                startAngle = -90f,
                sweepAngle = 360f,
                useCenter = false,
                topLeft = topLeft,
                size = arcSize,
                style = stroke,
            )
            if (fraction > 0f) {
                drawArc(
                    color = onHero,
                    startAngle = -90f,
                    sweepAngle = 360f * fraction.coerceIn(0f, 1f),
                    useCenter = false,
                    topLeft = topLeft,
                    size = arcSize,
                    style = stroke,
                )
            }
        }
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = days.toString(),
                style = MaterialTheme.typography.headlineMedium.copy(
                    fontSize = 26.sp,
                    lineHeight = 26.sp,
                    fontWeight = FontWeight.ExtraBold,
                    letterSpacing = (-0.78).sp,
                ),
                color = onHero,
            )
            Text(
                text = stringResource(R.string.enrollment_hero_days_caption).uppercase(),
                style = MaterialTheme.typography.labelSmall.copy(
                    fontSize = 9.5.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 0.6.sp,
                ),
                color = onHero.copy(alpha = 0.72f),
            )
        }
    }
}

@Composable
private fun SubmittedBadge(onHero: Color) {
    Box(
        modifier = Modifier
            .size(64.dp)
            .clip(CircleShape)
            .background(onHero.copy(alpha = 0.14f)),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            imageVector = Icons.Filled.Check,
            contentDescription = null,
            tint = onHero,
            modifier = Modifier.size(30.dp),
        )
    }
}
