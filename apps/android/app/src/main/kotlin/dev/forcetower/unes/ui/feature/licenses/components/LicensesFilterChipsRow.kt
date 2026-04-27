package dev.forcetower.unes.ui.feature.licenses.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.forcetower.unes.R
import dev.forcetower.unes.designsystem.theme.melon
import dev.forcetower.unes.ui.feature.licenses.LicenseBreakdown
import dev.forcetower.unes.ui.feature.licenses.LicenseFamily
import dev.forcetower.unes.ui.feature.licenses.LicenseFilter

// "todos" + one chip per license family present in the loaded set. Mirrors
// `LicensesFilterChipsRow` (iOS) and the JSX filters strip — the chip row is
// horizontally scrollable, active chip is filled with its tone color (or ink
// for "todos"), inactive chips are neutral pills.
@Composable
internal fun LicensesFilterChipsRow(
    breakdown: List<LicenseBreakdown>,
    filter: LicenseFilter,
    onFilterChange: (LicenseFilter) -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .horizontalScroll(rememberScrollState())
            .padding(horizontal = 2.dp, vertical = 2.dp),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        FilterChip(
            label = stringResource(R.string.licenses_filter_all),
            isActive = filter is LicenseFilter.All,
            activeBackground = MaterialTheme.colorScheme.onBackground,
            activeForeground = MaterialTheme.colorScheme.surface,
            onClick = { onFilterChange(LicenseFilter.All) },
        )
        breakdown.forEach { row ->
            val active = (filter as? LicenseFilter.Family)?.value == row.family
            FilterChip(
                label = row.family.displayName,
                isActive = active,
                activeBackground = row.family.toneBackground(),
                activeForeground = row.family.toneForeground(),
                onClick = {
                    onFilterChange(
                        if (active) LicenseFilter.All else LicenseFilter.Family(row.family),
                    )
                },
            )
        }
    }
}

@Composable
private fun FilterChip(
    label: String,
    isActive: Boolean,
    activeBackground: Color,
    activeForeground: Color,
    onClick: () -> Unit,
) {
    val inactiveBg = MaterialTheme.colorScheme.surfaceVariant
    val inactiveFg = MaterialTheme.colorScheme.onSurface
    val inactiveLine = MaterialTheme.melon.surface.line

    val bg by animateColorAsState(
        targetValue = if (isActive) activeBackground else inactiveBg,
        animationSpec = tween(durationMillis = 150),
        label = "chip-bg",
    )
    val fg by animateColorAsState(
        targetValue = if (isActive) activeForeground else inactiveFg,
        animationSpec = tween(durationMillis = 150),
        label = "chip-fg",
    )
    val border by animateColorAsState(
        targetValue = if (isActive) activeBackground else inactiveLine,
        animationSpec = tween(durationMillis = 150),
        label = "chip-border",
    )

    Row(
        modifier = Modifier
            .clip(CircleShape)
            .background(bg)
            .border(1.dp, border, CircleShape)
            .clickable(onClick = onClick)
            .padding(horizontal = 11.dp, vertical = 6.dp)
            .semantics {
                role = Role.Tab
                selected = isActive
            },
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall.copy(
                fontFamily = FontFamily.Monospace,
                fontSize = 10.sp,
                letterSpacing = 0.4.sp,
                fontWeight = FontWeight.Medium,
            ),
            color = fg,
            maxLines = 1,
        )
    }
}

// Smaller fixed-tone chip used inside group headers and rows. Always shows
// the family display name on its branded background.
@Composable
internal fun LicenseFamilyChip(family: LicenseFamily, compact: Boolean = true) {
    Row(
        modifier = Modifier
            .clip(androidx.compose.foundation.shape.RoundedCornerShape(6.dp))
            .background(family.toneBackground())
            .padding(
                horizontal = if (compact) 6.dp else 9.dp,
                vertical = if (compact) 2.dp else 4.dp,
            ),
    ) {
        Text(
            text = family.displayName,
            style = MaterialTheme.typography.labelSmall.copy(
                fontFamily = FontFamily.Monospace,
                fontSize = if (compact) 9.sp else 10.sp,
                letterSpacing = 0.4.sp,
                fontWeight = FontWeight.Medium,
            ),
            color = family.toneForeground(),
            maxLines = 1,
        )
    }
}
