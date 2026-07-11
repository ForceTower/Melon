package dev.forcetower.unes.ui.feature.me.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Flag
import androidx.compose.material.icons.filled.SportsScore
import androidx.compose.material.icons.outlined.Flag
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.forcetower.unes.R
import dev.forcetower.unes.designsystem.theme.MelonMotion
import dev.forcetower.unes.designsystem.theme.MelonTheme
import dev.forcetower.unes.designsystem.theme.melon
import dev.forcetower.unes.ui.feature.me.MeFixtures
import dev.forcetower.unes.ui.feature.me.ProfileIdentity

// Semester week counter as an M3-style linear progress card — dc `EuScreen`
// "Semana X de Y" block. Header carries the accent week number, the "Última"
// flag chip on the closing week, and the elapsed percent; the bar fills in
// on entrance; the footer pins the semester's start/end dates.
@Composable
internal fun SemesterProgressCard(identity: ProfileIdentity, modifier: Modifier = Modifier) {
    val shape = RoundedCornerShape(24.dp)
    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(shape)
            .background(MaterialTheme.melon.surface.card)
            .border(1.dp, MaterialTheme.melon.surface.line, shape)
            .padding(start = 18.dp, end = 18.dp, top = 18.dp, bottom = 16.dp),
    ) {
        Header(identity)
        Spacer(Modifier.height(14.dp))
        ProgressBar(percent = identity.progressPct)
        Spacer(Modifier.height(12.dp))
        Footer(identity)
    }
}

@Composable
private fun Header(identity: ProfileIdentity) {
    val accent = MaterialTheme.colorScheme.primary
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = buildAnnotatedString {
                append(stringResource(R.string.me_progress_week_prefix))
                withStyle(SpanStyle(color = accent)) {
                    append(identity.semesterWeek.toString())
                }
                append(stringResource(R.string.me_progress_week_suffix_format, identity.semesterTotalWeeks))
            },
            style = MaterialTheme.typography.titleSmall.copy(
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold,
            ),
            color = MaterialTheme.colorScheme.onBackground,
            maxLines = 1,
        )
        if (identity.semesterWeek >= identity.semesterTotalWeeks) {
            Spacer(Modifier.size(9.dp))
            LastWeekChip()
        }
        Spacer(Modifier.weight(1f))
        Text(
            text = stringResource(R.string.me_progress_percent_format, identity.progressPct),
            style = MaterialTheme.typography.titleSmall.copy(
                fontSize = 15.sp,
                fontWeight = FontWeight.ExtraBold,
            ),
            color = accent,
        )
    }
}

@Composable
private fun LastWeekChip() {
    val accent = MaterialTheme.colorScheme.primary
    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(50))
            .background(accent.copy(alpha = 0.16f))
            .padding(start = 7.dp, end = 9.dp, top = 3.dp, bottom = 3.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            imageVector = Icons.Filled.Flag,
            contentDescription = null,
            tint = accent,
            modifier = Modifier.size(13.dp),
        )
        Text(
            text = stringResource(R.string.me_progress_last_week),
            style = MaterialTheme.typography.labelSmall.copy(
                fontSize = 11.sp,
                letterSpacing = 0.22.sp,
            ),
            color = accent,
        )
    }
}

@Composable
private fun ProgressBar(percent: Int) {
    val accent = MaterialTheme.colorScheme.primary
    // Fill sweeps in from zero on entrance (dc `barFill`), delayed so it
    // starts once the card itself has landed.
    val fill = remember { Animatable(0f) }
    LaunchedEffect(percent) {
        fill.animateTo(
            targetValue = (percent / 100f).coerceIn(0f, 1f),
            animationSpec = tween(
                durationMillis = 1000,
                delayMillis = 400,
                easing = MelonMotion.EmphasizedEasing,
            ),
        )
    }
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(12.dp)
            .clip(RoundedCornerShape(6.dp))
            .background(accent.copy(alpha = 0.20f)),
    ) {
        if (fill.value > 0f) {
            Box(
                modifier = Modifier
                    .fillMaxWidth(fill.value)
                    .fillMaxHeight()
                    .clip(RoundedCornerShape(6.dp))
                    .background(accent),
            )
        }
    }
}

@Composable
private fun Footer(identity: ProfileIdentity) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            imageVector = Icons.Outlined.Flag,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.outlineVariant,
            modifier = Modifier.size(15.dp),
        )
        Spacer(Modifier.size(6.dp))
        BoundaryLabel(
            prefix = stringResource(R.string.me_progress_start_prefix),
            date = identity.semesterStart,
        )
        Spacer(Modifier.weight(1f))
        BoundaryLabel(
            prefix = stringResource(R.string.me_progress_end_prefix),
            date = identity.semesterEnd,
        )
        Spacer(Modifier.size(6.dp))
        Icon(
            imageVector = Icons.Filled.SportsScore,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(15.dp),
        )
    }
}

@Composable
private fun BoundaryLabel(prefix: String, date: String) {
    Text(
        text = buildAnnotatedString {
            append(prefix)
            withStyle(
                SpanStyle(
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontWeight = FontWeight.Bold,
                ),
            ) { append(date) }
        },
        style = MaterialTheme.typography.bodySmall.copy(fontSize = 12.sp),
        color = MaterialTheme.colorScheme.outline,
        maxLines = 1,
    )
}

@Preview
@Composable
private fun SemesterProgressCardPreview() {
    MelonTheme {
        SemesterProgressCard(identity = MeFixtures.identity, modifier = Modifier.padding(20.dp))
    }
}
