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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.forcetower.unes.R
import dev.forcetower.unes.designsystem.theme.MelonMotion
import dev.forcetower.unes.designsystem.theme.melon
import dev.forcetower.unes.ui.feature.finalcountdown.FCRow
import dev.forcetower.unes.ui.feature.finalcountdown.FinalCountdownMath

// "Composição" card — one bar per evaluation colored by its band (ok ≥ 7,
// warn ≥ 3, bad < 3; hatched while pending), plus the three UEFS rule chips.
// Mirrors the dc composition block.
@Composable
internal fun FCComposition(
    rows: List<FCRow>,
    weighted: Boolean,
    modifier: Modifier = Modifier,
) {
    val ink = MaterialTheme.colorScheme.onBackground
    val ink4 = MaterialTheme.colorScheme.outlineVariant
    val line = MaterialTheme.melon.surface.line

    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(MaterialTheme.melon.surface.card)
            .border(1.dp, line, RoundedCornerShape(20.dp))
            .padding(start = 16.dp, end = 16.dp, top = 15.dp, bottom = 14.dp),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text(
                text = stringResource(R.string.final_countdown_composition_title),
                style = MaterialTheme.typography.titleMedium.copy(
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = (-0.3).sp,
                ),
                color = ink,
            )
            Text(
                text = stringResource(
                    if (weighted) R.string.final_countdown_composition_header_weighted
                    else R.string.final_countdown_composition_header_simple,
                ).uppercase(),
                style = MaterialTheme.typography.labelSmall.copy(
                    fontSize = 11.5.sp,
                    fontWeight = FontWeight.SemiBold,
                    letterSpacing = 0.34.sp,
                ),
                color = ink4,
            )
        }

        Column(verticalArrangement = Arrangement.spacedBy(11.dp)) {
            rows.forEach { row ->
                CompositionBar(row = row, weighted = weighted)
            }
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 14.dp)
                .drawBehind {
                    drawLine(
                        color = line,
                        start = Offset.Zero,
                        end = Offset(size.width, 0f),
                        strokeWidth = 1.dp.toPx(),
                    )
                }
                .padding(top = 13.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            RuleChip(
                label = stringResource(R.string.final_countdown_rule_floor_label),
                value = stringResource(R.string.final_countdown_rule_floor_value),
                valueColor = MaterialTheme.melon.status.bad,
                modifier = Modifier.weight(1f),
            )
            RuleChip(
                label = stringResource(R.string.final_countdown_rule_pass_label),
                value = stringResource(R.string.final_countdown_rule_pass_value),
                valueColor = MaterialTheme.melon.status.ok,
                modifier = Modifier.weight(1f),
            )
            RuleChip(
                label = stringResource(R.string.final_countdown_rule_formula_label),
                value = stringResource(R.string.final_countdown_rule_formula_value),
                valueColor = MaterialTheme.melon.palette.jade,
                modifier = Modifier.weight(1f),
            )
        }
    }
}

@Composable
private fun CompositionBar(row: FCRow, weighted: Boolean) {
    val status = MaterialTheme.melon.status
    val surface3 = MaterialTheme.colorScheme.surfaceContainerHigh
    val ink = MaterialTheme.colorScheme.onBackground
    val ink2 = MaterialTheme.colorScheme.onSurfaceVariant
    val ink4 = MaterialTheme.colorScheme.outlineVariant

    val score = row.score
    val fillColor = when {
        score == null -> ink4
        score >= FinalCountdownMath.PassThreshold -> status.ok
        score >= FinalCountdownMath.FailThreshold -> status.warn
        else -> status.bad
    }
    val fraction by animateFloatAsState(
        targetValue = score?.let { (it / 10.0).toFloat().coerceIn(0f, 1f) } ?: 0f,
        animationSpec = MelonMotion.ease(),
        label = "fcCompFill",
    )

    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text(
            text = row.label,
            style = MaterialTheme.typography.labelMedium.copy(
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold,
            ),
            color = ink2,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.width(40.dp),
        )
        Box(
            modifier = Modifier
                .weight(1f)
                .height(7.dp)
                .clip(RoundedCornerShape(4.dp))
                .background(surface3),
        ) {
            if (score != null) {
                Box(
                    modifier = Modifier
                        .fillMaxHeight()
                        .fillMaxWidth(fraction)
                        .clip(RoundedCornerShape(4.dp))
                        .background(fillColor),
                )
            } else {
                PendingHatch(color = surface3, modifier = Modifier.matchParentSize())
            }
        }
        Row(
            modifier = Modifier.width(60.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.End,
        ) {
            Text(
                text = FinalCountdownMath.formatGrade(score),
                style = MaterialTheme.typography.bodyMedium.copy(
                    fontSize = 15.sp,
                    fontWeight = FontWeight.SemiBold,
                ),
                color = if (score != null) ink else ink4,
                textAlign = TextAlign.End,
            )
            if (weighted) {
                Text(
                    text = stringResource(R.string.final_countdown_weight_format, row.weight),
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold,
                    ),
                    color = ink4,
                    modifier = Modifier.padding(start = 4.dp),
                )
            }
        }
    }
}

// 45° hatch for evaluations that haven't happened yet (dc's repeating
// linear-gradient placeholder).
@Composable
private fun PendingHatch(color: Color, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier.drawBehind {
            val step = 8.dp.toPx()
            val stroke = 4.dp.toPx()
            var x = -size.height
            while (x < size.width) {
                drawLine(
                    color = color.copy(alpha = 0.7f),
                    start = Offset(x, size.height),
                    end = Offset(x + size.height, 0f),
                    strokeWidth = stroke,
                )
                x += step
            }
        },
    )
}

@Composable
private fun RuleChip(
    label: String,
    value: String,
    valueColor: Color,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .background(MaterialTheme.colorScheme.surfaceContainer)
            .padding(horizontal = 10.dp, vertical = 8.dp),
    ) {
        Text(
            text = label.uppercase(),
            style = MaterialTheme.typography.labelSmall.copy(
                fontSize = 10.sp,
                fontWeight = FontWeight.SemiBold,
                letterSpacing = 0.3.sp,
            ),
            color = MaterialTheme.colorScheme.outlineVariant,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        Text(
            text = value,
            style = MaterialTheme.typography.titleMedium.copy(
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = (-0.3).sp,
            ),
            color = valueColor,
            maxLines = 1,
            modifier = Modifier.padding(top = 3.dp),
        )
    }
}
