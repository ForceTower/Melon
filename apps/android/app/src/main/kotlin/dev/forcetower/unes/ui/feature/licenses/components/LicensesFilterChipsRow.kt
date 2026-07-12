package dev.forcetower.unes.ui.feature.licenses.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import dev.forcetower.unes.R
import dev.forcetower.unes.designsystem.theme.melon
import dev.forcetower.unes.ui.feature.licenses.LicenseBreakdown
import dev.forcetower.unes.ui.feature.licenses.LicenseFilter

// "Tudo" + one M3 `FilterChip` per license family present in the bundled set.
// Selecting a chip filters the group list; the active chip picks up the accent
// tint and a leading check, matching the dc `LicensesScreen`. Full-bleed: the
// row scrolls edge-to-edge with a 20.dp content inset so chips travel to the
// screen edge rather than clipping at a padded container.
@Composable
internal fun LicensesFilterChipsRow(
    breakdown: List<LicenseBreakdown>,
    filter: LicenseFilter,
    onFilterChange: (LicenseFilter) -> Unit,
    modifier: Modifier = Modifier,
) {
    LazyRow(
        modifier = modifier.fillMaxWidth(),
        contentPadding = PaddingValues(horizontal = 20.dp, vertical = 2.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        item(key = "all") {
            LicenseChip(
                label = stringResource(R.string.licenses_filter_all),
                selected = filter is LicenseFilter.All,
                onClick = { onFilterChange(LicenseFilter.All) },
            )
        }
        items(breakdown, key = { it.family.name }) { row ->
            val selected = (filter as? LicenseFilter.Family)?.value == row.family
            LicenseChip(
                label = row.family.displayName,
                selected = selected,
                onClick = {
                    onFilterChange(
                        if (selected) LicenseFilter.All else LicenseFilter.Family(row.family),
                    )
                },
            )
        }
    }
}

@Composable
private fun LicenseChip(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
) {
    val accent = MaterialTheme.colorScheme.primary
    val line = MaterialTheme.melon.surface.line

    FilterChip(
        selected = selected,
        onClick = onClick,
        label = { Text(text = label) },
        shape = RoundedCornerShape(10.dp),
        leadingIcon = if (selected) {
            {
                Icon(
                    imageVector = Icons.Filled.Check,
                    contentDescription = null,
                    modifier = Modifier.size(FilterChipDefaults.IconSize),
                )
            }
        } else {
            null
        },
        colors = FilterChipDefaults.filterChipColors(
            containerColor = Color.Transparent,
            labelColor = MaterialTheme.colorScheme.onSurface,
            selectedContainerColor = accent.copy(alpha = 0.20f),
            selectedLabelColor = accent,
            selectedLeadingIconColor = accent,
        ),
        border = FilterChipDefaults.filterChipBorder(
            enabled = true,
            selected = selected,
            borderColor = line,
            selectedBorderColor = Color.Transparent,
        ),
    )
}
