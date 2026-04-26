package dev.forcetower.unes.ui.feature.finalcountdown.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.forcetower.unes.R
import dev.forcetower.unes.designsystem.theme.MelonMotion
import dev.forcetower.unes.designsystem.theme.melon
import dev.forcetower.unes.ui.feature.finalcountdown.FCRow
import dev.forcetower.unes.ui.feature.finalcountdown.FCTone
import dev.forcetower.unes.ui.feature.finalcountdown.FinalCountdownMath
import dev.forcetower.unes.ui.feature.finalcountdown.fcToneBackground
import dev.forcetower.unes.ui.feature.finalcountdown.fcToneSoft

// "◦ composição" card — one bar per row, then the three rule pills (piso
// final / aprovação / fórmula final). Unfilled rows show a striped placeholder,
// wildcards render with the amber tint, and bars pick a color from the score
// band (red < 3, amber 3..7, green ≥ 7). Mirrors iOS `FCBreakdown`.
@Composable
internal fun FCBreakdown(
    rows: List<FCRow>,
    weighted: Boolean,
    modifier: Modifier = Modifier,
) {
    val card = MaterialTheme.melon.surface.card
    val cardLine = MaterialTheme.melon.surface.cardLine
    val ink3 = MaterialTheme.colorScheme.onSurfaceVariant
    val ink4 = MaterialTheme.colorScheme.outlineVariant
    val line = MaterialTheme.melon.surface.line

    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .background(card)
            .border(1.dp, cardLine, RoundedCornerShape(18.dp))
            .padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.Bottom,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text(
                text = stringResource(R.string.final_countdown_breakdown_title).uppercase(),
                style = MaterialTheme.typography.labelSmall.copy(
                    fontSize = 10.sp,
                    letterSpacing = 1.2.sp,
                    fontWeight = FontWeight.Medium,
                ),
                color = ink3,
            )
            Text(
                text = stringResource(R.string.final_countdown_breakdown_caption).uppercase(),
                style = MaterialTheme.typography.labelSmall.copy(
                    fontSize = 9.sp,
                    letterSpacing = 0.72.sp,
                    fontWeight = FontWeight.Medium,
                ),
                color = ink4,
            )
        }

        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            rows.forEach { row -> RowBar(row = row, weighted = weighted) }
        }

        HorizontalDivider(color = line, thickness = 1.dp)

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            RulePill(
                label = stringResource(R.string.final_countdown_rule_piso_label),
                value = stringResource(R.string.final_countdown_rule_piso_value),
                tone = FCTone.Coral,
                modifier = Modifier.weight(1f),
            )
            RulePill(
                label = stringResource(R.string.final_countdown_rule_pass_label),
                value = stringResource(R.string.final_countdown_rule_pass_value),
                tone = FCTone.Green,
                modifier = Modifier.weight(1f),
            )
            RulePill(
                label = stringResource(R.string.final_countdown_rule_formula_label),
                value = stringResource(R.string.final_countdown_rule_formula_value),
                tone = FCTone.Teal,
                modifier = Modifier.weight(1f),
            )
        }
    }
}

@Composable
private fun RowBar(row: FCRow, weighted: Boolean) {
    val ink = MaterialTheme.colorScheme.onBackground
    val ink2 = MaterialTheme.colorScheme.onSurface
    val ink4 = MaterialTheme.colorScheme.outlineVariant
    val track = MaterialTheme.colorScheme.surfaceContainerHigh
    val amber = MaterialTheme.melon.brand.amber
    val green = fcToneBackground(FCTone.Green)
    val coral = fcToneBackground(FCTone.Coral)

    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Row(
            modifier = Modifier.width(44.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(3.dp),
        ) {
            if (row.wildcard) {
                FCStar(color = amber, modifier = Modifier.size(9.dp))
            }
            Text(
                text = row.label,
                style = MaterialTheme.typography.labelSmall.copy(
                    fontSize = 10.sp,
                    letterSpacing = 0.2.sp,
                    fontWeight = FontWeight.SemiBold,
                ),
                color = ink2,
            )
        }

        val barColor = when {
            row.wildcard -> amber
            row.score == null -> ink4
            row.score >= 7.0 -> green
            row.score >= 3.0 -> amber
            else -> coral
        }
        BarSlot(
            score = row.score,
            track = track,
            fill = barColor,
            stripe = ink4,
            modifier = Modifier.weight(1f),
        )

        Row(
            modifier = Modifier.width(56.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp, Alignment.End),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = row.score?.let { FinalCountdownMath.formatGrade(it) } ?: "—",
                style = MaterialTheme.typography.labelMedium.copy(
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Medium,
                ),
                color = if (row.score != null) ink else ink4,
            )
            if (weighted) {
                Text(
                    text = "×${row.weight}",
                    style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp),
                    color = ink4,
                )
            }
        }
    }
}

@Composable
private fun BarSlot(
    score: Double?,
    track: Color,
    fill: Color,
    stripe: Color,
    modifier: Modifier = Modifier,
) {
    val target = if (score == null) 0f else (score.coerceIn(0.0, 10.0) / 10.0).toFloat()
    val animated by animateFloatAsState(
        targetValue = target,
        animationSpec = MelonMotion.spring(),
        label = "fc-bar-fill",
    )
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(6.dp)
            .clip(RoundedCornerShape(3.dp))
            .background(track),
    ) {
        if (score == null) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .fillMaxHeight()
                    .stripedFill(stripe),
            )
        } else {
            Box(
                modifier = Modifier
                    .fillMaxWidth(fraction = animated)
                    .fillMaxHeight()
                    .background(fill),
            )
        }
    }
}

@Composable
private fun RulePill(
    label: String,
    value: String,
    tone: FCTone,
    modifier: Modifier = Modifier,
) {
    val toneBg = fcToneBackground(tone)
    val soft = fcToneSoft(tone)
    val ink4 = MaterialTheme.colorScheme.outlineVariant
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(10.dp))
            .background(soft)
            .padding(horizontal = 9.dp, vertical = 7.dp),
        verticalArrangement = Arrangement.spacedBy(2.dp),
    ) {
        Text(
            text = label.uppercase(),
            style = MaterialTheme.typography.labelSmall.copy(
                fontSize = 8.sp,
                letterSpacing = 0.8.sp,
                fontWeight = FontWeight.Medium,
            ),
            color = ink4,
        )
        Text(
            text = value,
            style = MaterialTheme.typography.headlineSmall.copy(
                fontSize = 13.sp,
                lineHeight = 14.sp,
                letterSpacing = (-0.13).sp,
            ),
            color = toneBg,
        )
    }
}

// Diagonal stripe pattern for unfilled bars. Mirrors the iOS Canvas-based
// striped placeholder — 4dp on, 4dp off, 45° angle.
private fun Modifier.stripedFill(color: Color): Modifier = drawWithCache {
    val stripeColor = color.copy(alpha = 0.2f)
    val step = 8.dp.toPx()
    val stripeWidth = 4.dp.toPx()
    val h = size.height
    onDrawBehind {
        var x = -h
        while (x < size.width) {
            val path = Path().apply {
                moveTo(x, 0f)
                lineTo(x + stripeWidth, 0f)
                lineTo(x + stripeWidth + h, h)
                lineTo(x + h, h)
                close()
            }
            drawPath(path = path, color = stripeColor)
            x += step
        }
    }
}
