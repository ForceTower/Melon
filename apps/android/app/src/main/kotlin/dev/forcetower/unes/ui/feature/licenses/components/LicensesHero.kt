package dev.forcetower.unes.ui.feature.licenses.components

import androidx.compose.animation.core.Animatable
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.VolunteerActivism
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.forcetower.unes.R
import dev.forcetower.unes.designsystem.theme.MelonMotion
import dev.forcetower.unes.designsystem.theme.melon
import dev.forcetower.unes.ui.feature.licenses.LicenseBreakdown

// "Código aberto" hero — the M3 container that opens the screen: total package
// count, the volunteer glyph, a one-line thank-you, then a segmented M3
// distribution bar with a legend. Mirrors the dc `LicensesScreen` hero. The bar
// grows in from the left on first appear (barGrow); segments are weighted by
// each family's share of the bundled set.
@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun LicensesHero(
    totalPackages: Int,
    breakdown: List<LicenseBreakdown>,
    modifier: Modifier = Modifier,
) {
    val heroBg = MaterialTheme.colorScheme.surface
    val line = MaterialTheme.melon.surface.line
    val ink = MaterialTheme.colorScheme.onBackground
    val ink2 = MaterialTheme.colorScheme.onSurface
    val ink3 = MaterialTheme.colorScheme.onSurfaceVariant
    val accent = MaterialTheme.colorScheme.primary
    val ok = MaterialTheme.melon.status.ok
    val shape = RoundedCornerShape(28.dp)

    val grow = remember { Animatable(0f) }
    LaunchedEffect(Unit) { grow.animateTo(1f, MelonMotion.easeSlow()) }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(shape)
            .background(heroBg)
            .border(1.dp, line, shape),
    ) {
        // Soft accent glow bleeding in from the top-right corner.
        Box(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .offset(x = 30.dp, y = (-70).dp)
                .size(220.dp)
                .background(
                    Brush.radialGradient(listOf(accent.copy(alpha = 0.18f), Color.Transparent)),
                ),
        )

        Column(modifier = Modifier.padding(20.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .clip(RoundedCornerShape(4.dp))
                        .background(ok),
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = stringResource(R.string.licenses_hero_eyebrow).uppercase(),
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontSize = 11.sp,
                        fontWeight = FontWeight.ExtraBold,
                        letterSpacing = 1.4.sp,
                    ),
                    color = ink3,
                )
            }

            Spacer(modifier = Modifier.height(14.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.Bottom,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Row(verticalAlignment = Alignment.Bottom) {
                    Text(
                        text = totalPackages.toString(),
                        style = MaterialTheme.typography.displayLarge.copy(
                            fontSize = 58.sp,
                            lineHeight = 50.sp,
                            letterSpacing = (-2.3).sp,
                        ),
                        color = ink,
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = pluralStringResource(R.plurals.licenses_hero_packages, totalPackages),
                        style = MaterialTheme.typography.titleMedium.copy(fontSize = 18.sp),
                        color = ink3,
                        modifier = Modifier.padding(bottom = 6.dp),
                    )
                }
                Icon(
                    imageVector = Icons.Filled.VolunteerActivism,
                    contentDescription = null,
                    tint = lerp(ink3, accent, 0.7f),
                    modifier = Modifier
                        .padding(bottom = 4.dp)
                        .size(34.dp),
                )
            }

            Spacer(modifier = Modifier.height(14.dp))

            Text(
                text = stringResource(R.string.licenses_hero_caption),
                style = MaterialTheme.typography.bodyMedium.copy(fontSize = 13.sp, lineHeight = 18.sp),
                color = ink2,
            )

            if (breakdown.isNotEmpty()) {
                Spacer(modifier = Modifier.height(18.dp))
                DistributionBar(breakdown = breakdown, grow = grow.value)
                Spacer(modifier = Modifier.height(14.dp))
                FlowRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(18.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    breakdown.forEach { row ->
                        LegendItem(row = row, ink = ink, ink2 = ink2)
                    }
                }
            }
        }
    }
}

@Composable
private fun DistributionBar(breakdown: List<LicenseBreakdown>, grow: Float) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(12.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        breakdown.forEach { row ->
            Box(
                modifier = Modifier
                    .weight(row.count.toFloat())
                    .fillMaxHeight()
                    .graphicsLayer {
                        scaleX = grow
                        transformOrigin = TransformOrigin(0f, 0.5f)
                    }
                    .clip(RoundedCornerShape(6.dp))
                    .background(row.family.toneBackground()),
            )
        }
    }
}

@Composable
private fun LegendItem(row: LicenseBreakdown, ink: Color, ink2: Color) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
            modifier = Modifier
                .size(9.dp)
                .clip(RoundedCornerShape(3.dp))
                .background(row.family.toneBackground()),
        )
        Spacer(modifier = Modifier.width(7.dp))
        Text(
            text = row.family.displayName,
            style = MaterialTheme.typography.bodySmall.copy(
                fontFamily = FontFamily.Monospace,
                fontSize = 13.sp,
            ),
            color = ink2,
        )
        Spacer(modifier = Modifier.width(7.dp))
        Text(
            text = row.count.toString(),
            style = MaterialTheme.typography.labelLarge.copy(
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
            ),
            color = ink,
        )
    }
}
