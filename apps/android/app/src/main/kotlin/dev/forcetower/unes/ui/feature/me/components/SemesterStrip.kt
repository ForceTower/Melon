package dev.forcetower.unes.ui.feature.me.components

import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.forcetower.unes.R
import dev.forcetower.unes.designsystem.theme.melon
import dev.forcetower.unes.ui.feature.me.ProfileIdentity
import java.util.Locale

// Card showing the active semester week-by-week. Past weeks fade up as a
// gradient; the current week pulses in the accent color; remaining weeks are
// a flat surface3 fill. Mirrors `SemesterStrip` on iOS and the JSX prototype.
@Composable
internal fun SemesterStrip(identity: ProfileIdentity, modifier: Modifier = Modifier) {
    val cardLine = MaterialTheme.melon.surface.cardLine
    val card = MaterialTheme.melon.surface.card
    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(22.dp))
            .background(card)
            .border(1.dp, cardLine, RoundedCornerShape(22.dp))
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Header(identity = identity)
        WeekBlocks(
            currentWeek = identity.semesterWeek,
            totalWeeks = identity.semesterTotalWeeks,
        )
        Footer(identity = identity)
    }
}

@Composable
private fun Header(identity: ProfileIdentity) {
    val ink = MaterialTheme.colorScheme.onBackground
    val ink3 = MaterialTheme.colorScheme.onSurfaceVariant
    val accent = MaterialTheme.colorScheme.primary
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.Top,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = stringResource(R.string.me_semester_strip_eyebrow_format, identity.semester)
                    .uppercase(Locale.ROOT),
                style = MaterialTheme.typography.labelMedium.copy(
                    fontSize = 10.sp,
                    letterSpacing = 1.2.sp,
                    fontWeight = FontWeight.Medium,
                ),
                color = ink3,
            )
            Spacer(Modifier.height(2.dp))
            Text(
                text = buildAnnotatedString {
                    withStyle(SpanStyle(color = ink)) {
                        append(stringResource(R.string.me_semester_strip_week_prefix))
                    }
                    withStyle(
                        SpanStyle(color = accent, fontStyle = FontStyle.Italic),
                    ) { append(identity.semesterWeek.toString()) }
                    withStyle(SpanStyle(color = ink)) {
                        append(stringResource(
                            R.string.me_semester_strip_week_suffix_format,
                            identity.semesterTotalWeeks,
                        ))
                    }
                },
                style = MaterialTheme.typography.headlineSmall.copy(
                    fontSize = 19.sp,
                    lineHeight = 19.sp,
                    letterSpacing = (-0.19).sp,
                    fontWeight = FontWeight.Normal,
                ),
            )
        }
        Text(
            text = "${identity.progressPct}%",
            style = MaterialTheme.typography.labelMedium.copy(
                fontSize = 10.sp,
                letterSpacing = 0.6.sp,
                fontWeight = FontWeight.Medium,
            ),
            color = ink3,
        )
    }
}

@Composable
private fun WeekBlocks(currentWeek: Int, totalWeeks: Int) {
    if (totalWeeks <= 0) return
    val ink = MaterialTheme.colorScheme.onBackground
    val accent = MaterialTheme.colorScheme.primary
    val upcoming = MaterialTheme.colorScheme.surfaceContainerHigh
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(3.dp),
    ) {
        for (i in 0 until totalWeeks) {
            val phase = when {
                i < currentWeek - 1 -> Phase.Done
                i == currentWeek - 1 -> Phase.Current
                else -> Phase.Upcoming
            }
            val fill = when (phase) {
                Phase.Done -> ink
                Phase.Current -> accent
                Phase.Upcoming -> upcoming
            }
            // Past weeks fade from dim → recent so the gradient tracks how
            // close we are to "now". Same trick as JSX/iOS.
            val opacity = if (phase == Phase.Done) {
                (0.35f + (i.toFloat() / totalWeeks) * 0.55f).coerceIn(0f, 1f)
            } else {
                1f
            }
            Box(
                modifier = Modifier
                    .weight(1f)
                    .height(22.dp),
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .graphicsLayer { alpha = opacity }
                        .clip(RoundedCornerShape(4.dp))
                        .background(fill),
                )
                if (phase == Phase.Current) {
                    PulseRing(color = accent)
                }
            }
        }
    }
}

@Composable
private fun PulseRing(color: androidx.compose.ui.graphics.Color) {
    val transition = rememberInfiniteTransition(label = "current-week-pulse")
    val phase by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1600),
        ),
        label = "phase",
    )
    // `Math.PI` is Double; force the whole product to Float before passing to
    // `cos` so the rest of the expression chain stays Float (Compose's `scale`
    // and `Color.copy(alpha=...)` both want Float, and Kotlin would otherwise
    // widen the result to Double via the cos overload pick).
    val angle = (phase * 2f * Math.PI).toFloat()
    val ease = (1f - kotlin.math.cos(angle)) / 2f
    val alpha = 0.55f - 0.3f * ease
    val pulseScale = 1.0f - 0.05f * ease
    // Draw the ring at the block's bounds, then scale it up via graphicsLayer
    // so it bleeds slightly outside without disturbing layout. The base
    // 1.06× compensates for the JSX prototype's `inset: -3` outer ring.
    Box(
        modifier = Modifier
            .fillMaxSize()
            .scale(pulseScale * RingExpansion)
            .border(
                width = 1.5.dp,
                brush = SolidColor(color.copy(alpha = alpha)),
                shape = RoundedCornerShape(6.dp),
            ),
    )
}

private const val RingExpansion = 1.06f

@Composable
private fun Footer(identity: ProfileIdentity) {
    val ink4 = MaterialTheme.colorScheme.outlineVariant
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(
            text = identity.semesterStart.uppercase(Locale.ROOT),
            style = MaterialTheme.typography.labelSmall.copy(
                fontSize = 9.sp,
                letterSpacing = 0.72.sp,
                fontWeight = FontWeight.Medium,
            ),
            color = ink4,
        )
        Text(
            text = identity.semesterEnd.uppercase(Locale.ROOT),
            style = MaterialTheme.typography.labelSmall.copy(
                fontSize = 9.sp,
                letterSpacing = 0.72.sp,
                fontWeight = FontWeight.Medium,
            ),
            color = ink4,
        )
    }
}

private enum class Phase { Done, Current, Upcoming }
