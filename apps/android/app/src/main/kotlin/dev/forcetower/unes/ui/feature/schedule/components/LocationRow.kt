package dev.forcetower.unes.ui.feature.schedule.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.forcetower.unes.R
import dev.forcetower.unes.designsystem.theme.melon
import dev.forcetower.unes.ui.feature.schedule.ScheduleClass

internal sealed interface LocationRowStyle {
    data class Normal(val dim: Boolean) : LocationRowStyle
    data object Inverted : LocationRowStyle
}

private enum class LocPartKind { Modulo, Room, Campus }
private data class LocPart(val kind: LocPartKind, val value: String)

@Composable
internal fun ScheduleLocationRow(
    cls: ScheduleClass,
    style: LocationRowStyle,
    modifier: Modifier = Modifier,
) {
    val parts = buildList {
        cls.modulo?.let { add(LocPart(LocPartKind.Modulo, it)) }
        cls.room?.let { add(LocPart(LocPartKind.Room, it)) }
        cls.campus?.let { add(LocPart(LocPartKind.Campus, it)) }
    }

    when (style) {
        is LocationRowStyle.Normal -> NormalLocationRow(cls, parts, dim = style.dim, modifier = modifier)
        LocationRowStyle.Inverted -> InvertedLocationRow(cls, parts, modifier = modifier)
    }
}

@Composable
private fun NormalLocationRow(
    cls: ScheduleClass,
    parts: List<LocPart>,
    dim: Boolean,
    modifier: Modifier,
) {
    val ink = MaterialTheme.colorScheme.onBackground
    val ink3 = MaterialTheme.colorScheme.onSurfaceVariant
    val rowModifier = modifier.alpha(if (dim) 0.5f else 1f)

    if (parts.isEmpty()) {
        Row(
            modifier = rowModifier,
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            WarningGlyph(color = ink3.copy(alpha = 0.8f))
            Text(
                text = stringResource(R.string.schedule_location_unknown),
                style = MaterialTheme.typography.bodySmall.copy(
                    fontSize = 12.sp,
                    fontStyle = FontStyle.Italic,
                ),
                color = ink3,
                maxLines = 1,
            )
        }
        return
    }

    if (cls.campus == "Online" && cls.modulo == null && cls.room == null) {
        Row(
            modifier = rowModifier,
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            CampusGlyph(color = ink.copy(alpha = 0.7f))
            MonoLocationText(
                text = stringResource(R.string.schedule_location_online),
                color = ink,
                weight = FontWeight.Medium,
            )
        }
        return
    }

    if (parts.size == 1) {
        val only = parts[0]
        Row(
            modifier = rowModifier,
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            LocPartGlyph(only.kind, color = ink.copy(alpha = 0.7f))
            MonoLocationText(
                text = only.value.uppercase(),
                color = ink,
                weight = FontWeight.Medium,
            )
        }
        return
    }

    val physical = parts.filter { it.kind != LocPartKind.Campus }
    val campus = parts.firstOrNull { it.kind == LocPartKind.Campus }
    Row(
        modifier = rowModifier,
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        PinGlyph(color = ink.copy(alpha = 0.7f))
        physical.forEachIndexed { index, part ->
            if (index > 0) {
                Text(
                    text = "·",
                    color = ink.copy(alpha = 0.35f),
                    style = MaterialTheme.typography.labelSmall.copy(fontFamily = FontFamily.Monospace),
                )
            }
            MonoLocationText(
                text = part.value.uppercase(),
                color = ink,
                weight = if (part.kind == LocPartKind.Room) FontWeight.SemiBold else FontWeight.Medium,
            )
        }
        campus?.let {
            Spacer(Modifier.width(2.dp))
            CampusChip(
                text = it.value,
                background = MaterialTheme.melon.surface.card,
                foreground = ink3,
            )
        }
    }
}

@Composable
private fun InvertedLocationRow(
    cls: ScheduleClass,
    parts: List<LocPart>,
    modifier: Modifier,
) {
    val onAlwaysDark = Color(0xFFFBF7F2)
    val physical = parts.filter { it.kind != LocPartKind.Campus }

    if (physical.isEmpty() && cls.campus == null) {
        Text(
            text = stringResource(R.string.schedule_location_unknown),
            style = MaterialTheme.typography.bodySmall.copy(
                fontSize = 12.sp,
                fontStyle = FontStyle.Italic,
            ),
            color = onAlwaysDark.copy(alpha = 0.75f),
            modifier = modifier,
            maxLines = 1,
        )
        return
    }

    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        PinGlyph(color = onAlwaysDark.copy(alpha = 0.75f))
        physical.forEachIndexed { index, part ->
            if (index > 0) {
                Text(
                    text = "·",
                    color = onAlwaysDark.copy(alpha = 0.5f),
                    style = MaterialTheme.typography.labelSmall.copy(fontFamily = FontFamily.Monospace),
                )
            }
            MonoLocationText(
                text = part.value.uppercase(),
                color = onAlwaysDark,
                weight = if (index == physical.lastIndex) FontWeight.SemiBold else FontWeight.Medium,
            )
        }
        cls.campus?.let { campus ->
            Spacer(Modifier.width(2.dp))
            CampusChip(
                text = campus,
                background = onAlwaysDark.copy(alpha = 0.2f),
                foreground = onAlwaysDark,
            )
        }
    }
}

@Composable
private fun MonoLocationText(text: String, color: Color, weight: FontWeight) {
    Text(
        text = text,
        style = MaterialTheme.typography.labelSmall.copy(
            fontFamily = FontFamily.Monospace,
            fontSize = 11.sp,
            fontWeight = weight,
            letterSpacing = 0.44.sp,
        ),
        color = color,
        maxLines = 1,
    )
}

@Composable
private fun CampusChip(text: String, background: Color, foreground: Color) {
    Text(
        text = text,
        style = MaterialTheme.typography.labelSmall.copy(
            fontFamily = FontFamily.Monospace,
            fontSize = 9.5.sp,
            fontWeight = FontWeight.Medium,
            letterSpacing = 0.76.sp,
        ),
        color = foreground,
        modifier = Modifier
            .clip(RoundedCornerShape(5.dp))
            .background(background)
            .padding(horizontal = 6.dp, vertical = 2.dp),
        maxLines = 1,
    )
}

@Composable
private fun LocPartGlyph(kind: LocPartKind, color: Color) {
    when (kind) {
        LocPartKind.Modulo -> ModuloGlyph(color = color)
        LocPartKind.Room -> RoomGlyph(color = color)
        LocPartKind.Campus -> CampusGlyph(color = color)
    }
}
