package dev.forcetower.unes.ui.feature.me.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.forcetower.unes.designsystem.theme.MelonTheme
import dev.forcetower.unes.designsystem.theme.melon
import dev.forcetower.unes.ui.feature.me.MeFixtures
import dev.forcetower.unes.ui.feature.me.SettingsRow
import dev.forcetower.unes.ui.feature.me.SettingsRowKind
import dev.forcetower.unes.ui.feature.me.hue

// "Definições" list — dc `EuScreen` M3-style grouped rows: tonal icon
// container, label/hint stack, chevron, hairline dividers. The About row's
// hint carries the live version/build.
@Composable
internal fun SettingsCard(
    rows: List<SettingsRow>,
    onSelect: (SettingsRowKind) -> Unit,
    modifier: Modifier = Modifier,
) {
    val shape = RoundedCornerShape(24.dp)
    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(shape)
            .background(MaterialTheme.melon.surface.card)
            .border(1.dp, MaterialTheme.melon.surface.line, shape),
    ) {
        rows.forEachIndexed { index, row ->
            SettingsRowItem(row = row, onClick = { onSelect(row.id) })
            if (index < rows.size - 1) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(1.dp)
                        .background(MaterialTheme.melon.surface.line),
                )
            }
        }
    }
}

@Composable
private fun SettingsRowItem(row: SettingsRow, onClick: () -> Unit) {
    val label = stringResource(row.labelRes)
    val appInfo = rememberAppInfo()
    val hint = when (row.id) {
        SettingsRowKind.About -> stringResource(row.hintRes, appInfo.version, appInfo.build)
        else -> stringResource(row.hintRes)
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(role = Role.Button, onClick = onClick, onClickLabel = label)
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        IconContainer(row)
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = label,
                style = MaterialTheme.typography.titleSmall.copy(fontSize = 15.sp),
                color = MaterialTheme.colorScheme.onBackground,
            )
            Spacer(Modifier.height(2.dp))
            Text(
                text = hint,
                style = MaterialTheme.typography.bodySmall.copy(fontSize = 12.sp),
                color = MaterialTheme.colorScheme.outline,
            )
        }
        Icon(
            imageVector = Icons.Filled.ChevronRight,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.outlineVariant,
            modifier = Modifier.size(20.dp),
        )
    }
}

@Composable
private fun IconContainer(row: SettingsRow) {
    val hue = row.tone?.hue()
    val background = hue?.copy(alpha = 0.16f) ?: MaterialTheme.colorScheme.surfaceContainer
    val tint = hue ?: MaterialTheme.colorScheme.outline
    Box(
        modifier = Modifier
            .size(40.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(background),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            imageVector = row.icon,
            contentDescription = null,
            tint = tint,
            modifier = Modifier.size(21.dp),
        )
    }
}

@Preview
@Composable
private fun SettingsCardPreview() {
    MelonTheme {
        SettingsCard(
            rows = MeFixtures.settingsRows,
            onSelect = {},
            modifier = Modifier.padding(20.dp),
        )
    }
}
