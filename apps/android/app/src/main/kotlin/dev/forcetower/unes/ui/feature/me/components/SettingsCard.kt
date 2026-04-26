package dev.forcetower.unes.ui.feature.me.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.forcetower.unes.designsystem.theme.melon
import dev.forcetower.unes.ui.feature.me.SettingsRow
import dev.forcetower.unes.ui.feature.me.SettingsRowKind

// Quiet services list under the shortcut constellation. Each row: small icon
// tile, label/hint stack, and a chevron. Mirrors `SettingsCard` on iOS and
// the JSX prototype's grouped settings list.
@Composable
internal fun SettingsCard(
    rows: List<SettingsRow>,
    onSelect: (SettingsRowKind) -> Unit,
    modifier: Modifier = Modifier,
) {
    val card = MaterialTheme.melon.surface.card
    val cardLine = MaterialTheme.melon.surface.cardLine
    val line = MaterialTheme.melon.surface.line

    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(22.dp))
            .background(card)
            .border(1.dp, cardLine, RoundedCornerShape(22.dp)),
    ) {
        rows.forEachIndexed { index, row ->
            SettingsRowItem(row = row, onClick = { onSelect(row.id) })
            if (index < rows.size - 1) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(1.dp)
                        .background(line),
                )
            }
        }
    }
}

@Composable
private fun SettingsRowItem(row: SettingsRow, onClick: () -> Unit) {
    val ink = MaterialTheme.colorScheme.onBackground
    val ink2 = MaterialTheme.colorScheme.onSurface
    val ink4 = MaterialTheme.colorScheme.outlineVariant
    val tile = MaterialTheme.colorScheme.surfaceVariant

    val label = stringResource(row.labelRes)

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(role = Role.Button, onClick = onClick, onClickLabel = label)
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(13.dp),
    ) {
        Box(
            modifier = Modifier
                .size(30.dp)
                .clip(RoundedCornerShape(9.dp))
                .background(tile),
            contentAlignment = Alignment.Center,
        ) {
            MeSettingsIconBox(icon = row.icon, color = ink2, modifier = Modifier.size(16.dp))
        }
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = label,
                style = MaterialTheme.typography.bodyMedium.copy(
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium,
                    letterSpacing = (-0.07).sp,
                ),
                color = ink,
            )
            Box(modifier = Modifier.height(2.dp))
            Text(
                text = stringResource(row.hintRes),
                style = MaterialTheme.typography.labelSmall.copy(
                    fontSize = 9.5.sp,
                    letterSpacing = 0.38.sp,
                    fontWeight = FontWeight.Medium,
                ),
                color = ink4,
            )
        }
        MeChevronGlyph(color = ink4, modifier = Modifier.size(12.dp))
    }
}
