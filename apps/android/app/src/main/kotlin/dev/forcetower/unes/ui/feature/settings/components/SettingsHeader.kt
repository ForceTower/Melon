package dev.forcetower.unes.ui.feature.settings.components

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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.forcetower.unes.R
import dev.forcetower.unes.designsystem.theme.melon
import androidx.compose.ui.res.stringResource

// Top chrome of the Settings screen. Mirrors `SettingsHeader.swift`: back
// chevron pill on the left, monospace sync stamp on the right, then the
// "Configurações" editorial title with the italic accent suffix and a short
// subtitle. Same back-button affordance as the discipline detail screen so the
// chrome reads consistently.
@Composable
internal fun SettingsHeader(
    onBack: () -> Unit,
    lastSyncLabel: String,
    appVersion: String,
    modifier: Modifier = Modifier,
) {
    val ink = MaterialTheme.colorScheme.onBackground
    val ink3 = MaterialTheme.colorScheme.onSurfaceVariant
    val ink4 = MaterialTheme.colorScheme.outlineVariant
    val accent = MaterialTheme.colorScheme.primary

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp)
            .padding(top = 8.dp, bottom = 18.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            BackPill(onBack = onBack)
            Spacer(modifier = Modifier.weight(1f))
            Text(
                text = stringResource(R.string.settings_last_sync_format, lastSyncLabel),
                style = MaterialTheme.typography.labelSmall.copy(
                    fontFamily = FontFamily.Monospace,
                    fontSize = 9.5.sp,
                    letterSpacing = 1.33.sp,
                ),
                color = ink4,
            )
        }

        Spacer(modifier = Modifier.height(22.dp))

        Text(
            text = stringResource(R.string.settings_eyebrow_format, appVersion),
            style = MaterialTheme.typography.labelSmall.copy(
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium,
                letterSpacing = 1.44.sp,
            ),
            color = ink3,
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = titleAnnotated(ink = ink, accent = accent),
            style = MaterialTheme.typography.displaySmall.copy(
                fontSize = 40.sp,
                lineHeight = 40.sp,
                letterSpacing = (-0.8).sp,
            ),
        )

        Spacer(modifier = Modifier.height(10.dp))

        Text(
            text = stringResource(R.string.settings_subtitle),
            style = MaterialTheme.typography.bodyMedium.copy(
                fontSize = 13.sp,
                lineHeight = 18.sp,
            ),
            color = ink3,
            modifier = Modifier.fillMaxWidth(),
        )
    }
}

@Composable
private fun titleAnnotated(
    ink: androidx.compose.ui.graphics.Color,
    accent: androidx.compose.ui.graphics.Color,
): AnnotatedString {
    val lead = stringResource(R.string.settings_title_lead)
    val tail = stringResource(R.string.settings_title_accent)
    return buildAnnotatedString {
        withStyle(SpanStyle(color = ink)) { append(lead) }
        withStyle(SpanStyle(color = accent, fontStyle = FontStyle.Italic)) { append(tail) }
    }
}

@Composable
private fun BackPill(onBack: () -> Unit) {
    val card = MaterialTheme.melon.surface.card
    val cardLine = MaterialTheme.melon.surface.cardLine
    val ink = MaterialTheme.colorScheme.onBackground
    val description = stringResource(R.string.settings_back)
    Box(
        modifier = Modifier
            .size(40.dp)
            .clip(CircleShape)
            .background(card)
            .border(1.dp, cardLine, CircleShape)
            .clickable(onClick = onBack)
            .semantics {
                role = Role.Button
                contentDescription = description
            },
        contentAlignment = Alignment.Center,
    ) {
        SettingsIcon(glyph = SettingsGlyph.ChevronLeft, color = ink, modifier = Modifier.size(17.dp))
    }
}

// "◦ capítulo 1" eyebrow + serif "Conta" + optional monospace meta chip on
// the right. Mirrors `SettingsSectionHeader` (Swift) and `CfgSection` (JSX).
@Composable
internal fun SettingsSectionHeader(
    eyebrow: String,
    title: String,
    meta: String? = null,
    modifier: Modifier = Modifier,
) {
    val ink = MaterialTheme.colorScheme.onBackground
    val ink3 = MaterialTheme.colorScheme.onSurfaceVariant
    val ink4 = MaterialTheme.colorScheme.outlineVariant

    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 4.dp)
            .padding(bottom = 10.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.Top,
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(
                text = "◦ ${eyebrow.uppercase()}",
                style = MaterialTheme.typography.labelSmall.copy(
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium,
                    letterSpacing = 1.44.sp,
                ),
                color = ink3,
            )
            Text(
                text = title,
                style = MaterialTheme.typography.headlineSmall.copy(
                    fontSize = 22.sp,
                    lineHeight = 22.sp,
                    letterSpacing = (-0.33).sp,
                ),
                color = ink,
            )
        }
        if (meta != null) {
            Text(
                text = meta.uppercase(),
                style = MaterialTheme.typography.labelSmall.copy(
                    fontFamily = FontFamily.Monospace,
                    fontSize = 9.5.sp,
                    letterSpacing = 0.76.sp,
                ),
                color = ink4,
            )
        }
    }
}
