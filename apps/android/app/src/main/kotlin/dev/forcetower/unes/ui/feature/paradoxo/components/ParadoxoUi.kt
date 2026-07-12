package dev.forcetower.unes.ui.feature.paradoxo.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.forcetower.melon.feature.paradoxo.domain.model.ParadoxoPulseKind
import dev.forcetower.melon.feature.paradoxo.domain.model.ParadoxoShapeKind
import dev.forcetower.melon.feature.paradoxo.domain.model.ParadoxoTier
import dev.forcetower.unes.R
import dev.forcetower.unes.designsystem.foundation.MeshVariant
import dev.forcetower.unes.designsystem.theme.melon
import dev.forcetower.unes.ui.feature.paradoxo.ParadoxoFormat

// Tone + copy mappings and the small shared pieces (score tile, tier chip,
// outcomes bar, card chrome, loading/failure). Mirrors iOS
// `ParadoxoComponents.swift` with the dc `ParadoxoScreen` visuals.

// Grade-severity tone, shared by tiles, charts and chips.
@Composable
internal fun paradoxoTone(mean: Double): Color = when (ParadoxoTier.of(mean)) {
    ParadoxoTier.Angel -> MaterialTheme.melon.status.ok
    ParadoxoTier.Fair -> MaterialTheme.melon.palette.teal
    ParadoxoTier.Balanced -> MaterialTheme.melon.palette.orange
    ParadoxoTier.Demanding -> MaterialTheme.melon.status.warn
    ParadoxoTier.Relentless -> MaterialTheme.melon.status.bad
}

@Composable
internal fun paradoxoTierLabel(tier: ParadoxoTier): String = stringResource(
    when (tier) {
        ParadoxoTier.Angel -> R.string.paradoxo_tier_angel
        ParadoxoTier.Fair -> R.string.paradoxo_tier_fair
        ParadoxoTier.Balanced -> R.string.paradoxo_tier_balanced
        ParadoxoTier.Demanding -> R.string.paradoxo_tier_demanding
        ParadoxoTier.Relentless -> R.string.paradoxo_tier_relentless
    },
)

@Composable
internal fun paradoxoShapeLabel(shape: ParadoxoShapeKind): String = stringResource(
    when (shape) {
        ParadoxoShapeKind.Bimodal -> R.string.paradoxo_shape_bimodal
        ParadoxoShapeKind.Strict -> R.string.paradoxo_shape_strict
        ParadoxoShapeKind.Lenient -> R.string.paradoxo_shape_lenient
        ParadoxoShapeKind.Balanced -> R.string.paradoxo_shape_balanced
        ParadoxoShapeKind.Regular -> R.string.paradoxo_shape_regular
    },
)

@Composable
internal fun paradoxoPulseLabel(kind: ParadoxoPulseKind): String = stringResource(
    when (kind) {
        ParadoxoPulseKind.Brutal -> R.string.paradoxo_pulse_brutal
        ParadoxoPulseKind.Kind -> R.string.paradoxo_pulse_kind
        ParadoxoPulseKind.Trend -> R.string.paradoxo_pulse_trend
        ParadoxoPulseKind.Gap -> R.string.paradoxo_pulse_gap
        ParadoxoPulseKind.Rising -> R.string.paradoxo_pulse_rising
        ParadoxoPulseKind.Surprise -> R.string.paradoxo_pulse_surprise
        ParadoxoPulseKind.Signature -> R.string.paradoxo_pulse_signature
    },
)

@Composable
internal fun paradoxoPulseTone(kind: ParadoxoPulseKind): Color = when (kind) {
    ParadoxoPulseKind.Brutal -> MaterialTheme.melon.status.bad
    ParadoxoPulseKind.Kind -> MaterialTheme.melon.status.ok
    ParadoxoPulseKind.Trend -> MaterialTheme.melon.status.warn
    ParadoxoPulseKind.Gap, ParadoxoPulseKind.Signature -> MaterialTheme.melon.palette.magenta
    ParadoxoPulseKind.Rising -> MaterialTheme.melon.palette.teal
    ParadoxoPulseKind.Surprise -> MaterialTheme.melon.palette.amber
}

internal fun paradoxoPulseMesh(kind: ParadoxoPulseKind): MeshVariant = when (kind) {
    ParadoxoPulseKind.Brutal, ParadoxoPulseKind.Gap, ParadoxoPulseKind.Signature -> MeshVariant.Rose
    ParadoxoPulseKind.Kind -> MeshVariant.Fresh
    ParadoxoPulseKind.Trend, ParadoxoPulseKind.Surprise -> MeshVariant.Sun
    ParadoxoPulseKind.Rising -> MeshVariant.Cool
}

// Colored square with the truncated mean — the visual anchor of every list
// row (dc `tileStyle`: radius 28% of the side, white bold grade).
@Composable
internal fun ParadoxoScoreTile(mean: Double, size: Dp, modifier: Modifier = Modifier) {
    val fontSize = (size.value * 0.4f).sp
    Box(
        modifier = modifier
            .size(size)
            .clip(RoundedCornerShape(size * 0.28f))
            .background(paradoxoTone(mean)),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = ParadoxoFormat.grade(mean),
            style = MaterialTheme.typography.titleMedium.copy(
                fontSize = fontSize,
                fontWeight = FontWeight.Bold,
                letterSpacing = -(fontSize * 0.03f),
            ),
            color = MaterialTheme.melon.fixed.onHero,
        )
    }
}

@Composable
internal fun ParadoxoTierChip(mean: Double, modifier: Modifier = Modifier) {
    val tone = paradoxoTone(mean)
    Row(
        modifier = modifier
            .clip(RoundedCornerShape(20.dp))
            .background(tone.copy(alpha = 0.18f))
            .padding(start = 8.dp, end = 10.dp, top = 4.dp, bottom = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Box(
            modifier = Modifier
                .size(6.dp)
                .clip(RoundedCornerShape(3.dp))
                .background(tone),
        )
        Text(
            text = paradoxoTierLabel(ParadoxoTier.of(mean)),
            style = MaterialTheme.typography.labelMedium.copy(
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
            ),
            color = tone,
        )
    }
}

@Composable
internal fun ParadoxoShapeChip(
    shape: ParadoxoShapeKind,
    tone: Color,
    modifier: Modifier = Modifier,
) {
    Text(
        text = paradoxoShapeLabel(shape),
        style = MaterialTheme.typography.labelMedium.copy(
            fontSize = 11.5.sp,
            fontWeight = FontWeight.Bold,
        ),
        color = tone,
        modifier = modifier
            .clip(RoundedCornerShape(8.dp))
            .background(tone.copy(alpha = 0.14f))
            .padding(horizontal = 9.dp, vertical = 3.dp),
    )
}

// Stacked approval/failure/quit bar + the three dotted counts underneath.
@Composable
internal fun ParadoxoOutcomes(
    approved: Int,
    failed: Int,
    quit: Int,
    modifier: Modifier = Modifier,
) {
    val ok = MaterialTheme.melon.status.ok
    val bad = MaterialTheme.melon.status.bad
    val neutral = MaterialTheme.colorScheme.outlineVariant
    Column(modifier = modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(10.dp)
                .clip(RoundedCornerShape(6.dp)),
            horizontalArrangement = Arrangement.spacedBy(2.dp),
        ) {
            val total = (approved + failed + quit).coerceAtLeast(1)
            listOf(approved to ok, failed to bad, quit to neutral).forEach { (value, color) ->
                if (value > 0) {
                    Box(
                        modifier = Modifier
                            .weight(value.toFloat() / total)
                            .height(10.dp)
                            .background(color),
                    )
                }
            }
        }
        Row(modifier = Modifier.padding(top = 12.dp)) {
            OutcomeCount(approved, stringResource(R.string.paradoxo_outcome_approved), ok)
            OutcomeCount(failed, stringResource(R.string.paradoxo_outcome_failed), bad)
            OutcomeCount(quit, stringResource(R.string.paradoxo_outcome_quit), neutral)
        }
    }
}

@Composable
private fun RowScope.OutcomeCount(value: Int, label: String, dot: Color) {
    Column(
        modifier = Modifier.weight(1f),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(5.dp),
        ) {
            Box(
                modifier = Modifier
                    .size(7.dp)
                    .clip(RoundedCornerShape(4.dp))
                    .background(dot),
            )
            Text(
                text = ParadoxoFormat.count(value),
                style = MaterialTheme.typography.titleMedium.copy(
                    fontSize = 17.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = (-0.34).sp,
                ),
                color = MaterialTheme.colorScheme.onBackground,
            )
        }
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall.copy(
                fontSize = 11.5.sp,
                fontWeight = FontWeight.Medium,
            ),
            color = MaterialTheme.colorScheme.outline,
            modifier = Modifier.padding(top = 2.dp),
        )
    }
}

// Standard card plate (dc: card bg, hairline border, radius 22).
@Composable
internal fun ParadoxoCard(
    modifier: Modifier = Modifier,
    cornerRadius: Dp = 22.dp,
    content: @Composable ColumnScope.() -> Unit,
) {
    val shape = RoundedCornerShape(cornerRadius)
    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(shape)
            .background(MaterialTheme.melon.surface.card)
            .border(1.dp, MaterialTheme.melon.surface.line, shape),
        content = content,
    )
}

@Composable
internal fun ParadoxoLoading(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 64.dp),
        contentAlignment = Alignment.Center,
    ) {
        CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
    }
}

@Composable
internal fun ParadoxoFailure(onRetry: () -> Unit, modifier: Modifier = Modifier) {
    val retry = stringResource(R.string.paradoxo_error_retry)
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 48.dp, horizontal = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text = stringResource(R.string.paradoxo_error_title),
            style = MaterialTheme.typography.titleMedium.copy(
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
            ),
            color = MaterialTheme.colorScheme.onBackground,
            textAlign = TextAlign.Center,
        )
        Text(
            text = retry,
            style = MaterialTheme.typography.labelLarge.copy(
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold,
            ),
            color = MaterialTheme.melon.fixed.onHero,
            modifier = Modifier
                .padding(top = 14.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.primary)
                .clickable(role = Role.Button, onClickLabel = retry, onClick = onRetry)
                .padding(horizontal = 18.dp, vertical = 9.dp),
        )
    }
}
