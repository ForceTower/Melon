package dev.forcetower.unes.ui.feature.settings.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.forcetower.unes.R
import dev.forcetower.unes.designsystem.theme.MelonMotion
import dev.forcetower.unes.designsystem.theme.melon
import dev.forcetower.unes.ui.feature.settings.SettingsTone
import dev.forcetower.unes.ui.feature.settings.SpoilerMode

// Expandable spoiler picker. Collapsed: icon + label + current pill. Tapping
// the row reveals the three options as radio rows. Mirrors `SpoilerPickerRow`
// on iOS and the JSX `PickerRow`.
@Composable
internal fun SpoilerPickerRow(
    value: SpoilerMode,
    onChange: (SpoilerMode) -> Unit,
    modifier: Modifier = Modifier,
) {
    val card = MaterialTheme.melon.surface.card
    val cardLine = MaterialTheme.melon.surface.cardLine
    val tone = resolveTone(SettingsTone.Coral)
    var open by rememberSaveable { mutableStateOf(false) }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(22.dp))
            .background(card)
            .border(1.dp, cardLine, RoundedCornerShape(22.dp)),
    ) {
        CollapsedRow(
            value = value,
            tone = tone,
            isOpen = open,
            onToggle = { open = !open },
        )
        AnimatedVisibility(
            visible = open,
            enter = fadeIn() + expandVertically(),
            exit = fadeOut() + shrinkVertically(),
        ) {
            Column(
                modifier = Modifier
                    .padding(start = 60.dp, end = 14.dp, bottom = 14.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                SpoilerMode.entries.forEach { option ->
                    OptionRow(
                        option = option,
                        active = option == value,
                        tone = tone,
                        onSelect = {
                            onChange(option)
                            open = false
                        },
                    )
                }
            }
        }
    }
}

@Composable
private fun CollapsedRow(
    value: SpoilerMode,
    tone: ResolvedSettingsTone,
    isOpen: Boolean,
    onToggle: () -> Unit,
) {
    val ink = MaterialTheme.colorScheme.onBackground
    val ink3 = MaterialTheme.colorScheme.onSurfaceVariant
    val ink4 = MaterialTheme.colorScheme.outlineVariant
    val surface2 = MaterialTheme.colorScheme.surfaceVariant
    val line = MaterialTheme.melon.surface.line
    val rotation by animateFloatAsState(
        targetValue = if (isOpen) 90f else 0f,
        animationSpec = MelonMotion.ease(),
        label = "spoiler-chevron",
    )

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onToggle)
            .padding(horizontal = 16.dp, vertical = 13.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Box(
            modifier = Modifier
                .size(32.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(tone.background),
            contentAlignment = Alignment.Center,
        ) {
            SettingsIcon(glyph = SettingsGlyph.Shield, color = tone.foreground, modifier = Modifier.size(15.dp))
        }
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = stringResource(R.string.settings_spoiler_label),
                style = MaterialTheme.typography.bodyMedium.copy(
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium,
                    letterSpacing = (-0.07).sp,
                ),
                color = ink,
            )
            Text(
                text = stringResource(R.string.settings_spoiler_hint),
                style = MaterialTheme.typography.labelSmall.copy(
                    fontFamily = FontFamily.Monospace,
                    fontSize = 9.5.sp,
                    letterSpacing = 0.48.sp,
                ),
                color = ink4,
            )
        }
        Row(
            modifier = Modifier
                .clip(CircleShape)
                .background(surface2)
                .border(1.dp, line, CircleShape)
                .padding(horizontal = 11.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            Text(
                text = stringResource(value.labelRes()),
                style = MaterialTheme.typography.labelMedium.copy(
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium,
                    letterSpacing = (-0.06).sp,
                ),
                color = ink,
            )
            SettingsIcon(
                glyph = SettingsGlyph.Chevron,
                color = ink3,
                modifier = Modifier.size(11.dp).rotate(rotation),
            )
        }
    }
}

@Composable
private fun OptionRow(
    option: SpoilerMode,
    active: Boolean,
    tone: ResolvedSettingsTone,
    onSelect: () -> Unit,
) {
    val ink = MaterialTheme.colorScheme.onBackground
    val ink4 = MaterialTheme.colorScheme.outlineVariant
    val surface2 = MaterialTheme.colorScheme.surfaceVariant
    val line = MaterialTheme.melon.surface.line

    val bg = if (active) tone.background else surface2
    val borderColor = if (active) tone.background else line
    val textColor = if (active) tone.foreground else ink
    val hintColor = if (active) tone.foreground.copy(alpha = 0.7f) else ink4
    val radioColor = if (active) tone.foreground else ink4

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(bg)
            .border(1.dp, borderColor, RoundedCornerShape(12.dp))
            .clickable(onClick = onSelect)
            .padding(horizontal = 12.dp, vertical = 9.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        RadioDot(active = active, color = radioColor, fill = if (active) tone.foreground else Color.Transparent)
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = stringResource(option.labelRes()),
                style = MaterialTheme.typography.labelMedium.copy(
                    fontSize = 12.5.sp,
                    fontWeight = FontWeight.Medium,
                    letterSpacing = (-0.06).sp,
                ),
                color = textColor,
            )
            Text(
                text = stringResource(option.hintRes()),
                style = MaterialTheme.typography.labelSmall.copy(
                    fontFamily = FontFamily.Monospace,
                    fontSize = 9.sp,
                    letterSpacing = 0.54.sp,
                ),
                color = hintColor,
            )
        }
    }
}

@Composable
private fun RadioDot(active: Boolean, color: Color, fill: Color) {
    Box(
        modifier = Modifier
            .size(14.dp)
            .border(1.5.dp, color, CircleShape),
        contentAlignment = Alignment.Center,
    ) {
        if (active) {
            Box(modifier = Modifier.size(6.dp).clip(CircleShape).background(fill))
        }
    }
}

private fun SpoilerMode.labelRes(): Int = when (this) {
    SpoilerMode.Value -> R.string.settings_spoiler_option_value_label
    SpoilerMode.Comment -> R.string.settings_spoiler_option_comment_label
    SpoilerMode.Posted -> R.string.settings_spoiler_option_posted_label
}

private fun SpoilerMode.hintRes(): Int = when (this) {
    SpoilerMode.Value -> R.string.settings_spoiler_option_value_hint
    SpoilerMode.Comment -> R.string.settings_spoiler_option_comment_hint
    SpoilerMode.Posted -> R.string.settings_spoiler_option_posted_hint
}
