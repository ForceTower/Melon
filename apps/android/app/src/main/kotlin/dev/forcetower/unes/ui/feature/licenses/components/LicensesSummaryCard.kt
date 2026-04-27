package dev.forcetower.unes.ui.feature.licenses.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.forcetower.unes.R
import dev.forcetower.unes.designsystem.theme.melon
import dev.forcetower.unes.ui.feature.licenses.LicenseBreakdown

// Distribution-by-license card. Stacked horizontal bar on top, two-column
// legend underneath. Mirrors `LicensesSummaryCard` (iOS) and `LicSummary`
// (JSX). The breakdown is computed from the loaded packages — empty input
// produces a single neutral bar so the card never collapses.
@Composable
internal fun LicensesSummaryCard(breakdown: List<LicenseBreakdown>, modifier: Modifier = Modifier) {
    val card = MaterialTheme.melon.surface.card
    val cardLine = MaterialTheme.melon.surface.cardLine
    val ink = MaterialTheme.colorScheme.onBackground
    val ink2 = MaterialTheme.colorScheme.onSurface
    val ink3 = MaterialTheme.colorScheme.onSurfaceVariant
    val ink4 = MaterialTheme.colorScheme.outlineVariant
    val surface2 = MaterialTheme.colorScheme.surfaceVariant
    val total = breakdown.sumOf { it.count }
    val shape = RoundedCornerShape(22.dp)

    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(shape)
            .background(card)
            .border(1.dp, cardLine, shape)
            .padding(start = 16.dp, end = 16.dp, top = 14.dp, bottom = 16.dp),
    ) {
        Row(verticalAlignment = Alignment.Top, modifier = Modifier.fillMaxWidth()) {
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                Text(
                    text = stringResource(R.string.licenses_summary_eyebrow),
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Medium,
                        letterSpacing = 1.2.sp,
                    ),
                    color = ink3,
                )
                Row(verticalAlignment = Alignment.Bottom, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(
                        text = total.toString(),
                        style = MaterialTheme.typography.headlineSmall.copy(
                            fontSize = 22.sp,
                            lineHeight = 22.sp,
                            letterSpacing = (-0.33).sp,
                        ),
                        color = ink,
                    )
                    Text(
                        text = pluralStringResource(R.plurals.licenses_summary_packages, total),
                        style = MaterialTheme.typography.headlineSmall.copy(
                            fontSize = 18.sp,
                            lineHeight = 22.sp,
                            letterSpacing = (-0.18).sp,
                            fontStyle = FontStyle.Italic,
                        ),
                        color = ink3,
                    )
                }
            }
            if (breakdown.isNotEmpty()) {
                Text(
                    text = pluralStringResource(R.plurals.licenses_summary_families, breakdown.size, breakdown.size),
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontFamily = FontFamily.Monospace,
                        fontSize = 9.5.sp,
                        letterSpacing = 0.76.sp,
                    ),
                    color = ink4,
                )
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        StackedBar(breakdown = breakdown, total = total, fallback = surface2)

        Spacer(modifier = Modifier.height(12.dp))

        Legend(breakdown = breakdown, ink = ink, ink2 = ink2)
    }
}

@Composable
private fun StackedBar(
    breakdown: List<LicenseBreakdown>,
    total: Int,
    fallback: androidx.compose.ui.graphics.Color,
) {
    val barShape = RoundedCornerShape(5.dp)
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(10.dp)
            .clip(barShape)
            .background(fallback),
    ) {
        if (breakdown.isNotEmpty() && total > 0) {
            Row(modifier = Modifier.fillMaxWidth()) {
                breakdown.forEach { row ->
                    Box(
                        modifier = Modifier
                            .weight(row.count.toFloat())
                            .fillMaxWidth()
                            .height(10.dp)
                            .background(row.family.toneBackground()),
                    )
                }
            }
        }
    }
}

@Composable
private fun Legend(
    breakdown: List<LicenseBreakdown>,
    ink: androidx.compose.ui.graphics.Color,
    ink2: androidx.compose.ui.graphics.Color,
) {
    if (breakdown.isEmpty()) return
    // Two-column flow — same arrangement as the JSX 1fr 1fr grid. Items are
    // chunked into pairs and laid out as two equal-width columns.
    val pairs = breakdown.chunked(2)
    Column(verticalArrangement = Arrangement.spacedBy(7.dp)) {
        pairs.forEach { pair ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(14.dp),
            ) {
                pair.forEach { row ->
                    LegendCell(
                        row = row,
                        ink = ink,
                        ink2 = ink2,
                        modifier = Modifier.weight(1f),
                    )
                }
                if (pair.size == 1) Spacer(modifier = Modifier.weight(1f))
            }
        }
    }
}

@Composable
private fun LegendCell(
    row: LicenseBreakdown,
    ink: androidx.compose.ui.graphics.Color,
    ink2: androidx.compose.ui.graphics.Color,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Box(
            modifier = Modifier
                .size(7.dp)
                .clip(RoundedCornerShape(2.dp))
                .background(row.family.toneBackground()),
        )
        Text(
            text = row.family.displayName,
            style = MaterialTheme.typography.labelSmall.copy(
                fontFamily = FontFamily.Monospace,
                fontSize = 10.5.sp,
                letterSpacing = 0.21.sp,
            ),
            color = ink2,
            maxLines = 1,
            modifier = Modifier.weight(1f),
        )
        Text(
            text = row.count.toString(),
            style = MaterialTheme.typography.headlineSmall.copy(
                fontSize = 14.sp,
                lineHeight = 14.sp,
                letterSpacing = (-0.14).sp,
            ),
            color = ink,
        )
    }
}
