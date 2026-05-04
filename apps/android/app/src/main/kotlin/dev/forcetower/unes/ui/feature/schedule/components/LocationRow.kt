package dev.forcetower.unes.ui.feature.schedule.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.forcetower.unes.R
import dev.forcetower.unes.ui.feature.schedule.ScheduleClass

internal sealed interface LocationRowStyle {
    data class Normal(val dim: Boolean) : LocationRowStyle
    data object Inverted : LocationRowStyle
}

private enum class LocPartKind { Modulo, Room }
private data class LocPart(val kind: LocPartKind, val value: String)

// Upstream stores a building/module label that's sometimes a short code
// ("MT", "PV", "Módulo 5") and sometimes descriptive prose ("Pavilhão de
// aula padrão 2° andar"). When it's prose we promote it to a multi-line
// headline and demote room to a small meta row beneath. Whitespace alone
// isn't a prose signal — "Módulo 5" should still render in CODE mode.
private fun isProseModulo(modulo: String?): Boolean =
    !modulo.isNullOrEmpty() && modulo.length > 11

@Composable
internal fun ScheduleLocationRow(
    cls: ScheduleClass,
    style: LocationRowStyle,
    modifier: Modifier = Modifier,
) {
    val parts = buildList {
        cls.modulo?.let { add(LocPart(LocPartKind.Modulo, it)) }
        cls.room?.let { add(LocPart(LocPartKind.Room, it)) }
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

    if (isProseModulo(cls.modulo)) {
        ProseLocationStack(
            headline = cls.modulo.orEmpty(),
            room = cls.room,
            headlineColor = ink,
            metaColor = ink3,
            modifier = rowModifier,
        )
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

    Row(
        modifier = rowModifier,
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        PinGlyph(color = ink.copy(alpha = 0.7f))
        parts.forEachIndexed { index, part ->
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
    }
}

@Composable
private fun InvertedLocationRow(
    cls: ScheduleClass,
    parts: List<LocPart>,
    modifier: Modifier,
) {
    val onAlwaysDark = Color(0xFFFBF7F2)

    if (parts.isEmpty()) {
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

    if (isProseModulo(cls.modulo)) {
        ProseLocationStack(
            headline = cls.modulo.orEmpty(),
            room = cls.room,
            headlineColor = onAlwaysDark,
            metaColor = onAlwaysDark.copy(alpha = 0.78f),
            modifier = modifier,
        )
        return
    }

    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        PinGlyph(color = onAlwaysDark.copy(alpha = 0.75f))
        parts.forEachIndexed { index, part ->
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
                weight = if (index == parts.lastIndex) FontWeight.SemiBold else FontWeight.Medium,
            )
        }
    }
}

// PROSE mode: long descriptive modulo (building) → 2-line sans headline + a
// small uppercase room code beneath. The meta row indents 17.dp so it aligns
// under the prose text rather than the pin icon.
@Composable
private fun ProseLocationStack(
    headline: String,
    room: String?,
    headlineColor: Color,
    metaColor: Color,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(3.dp),
    ) {
        Row(
            verticalAlignment = Alignment.Top,
            horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            PinGlyph(color = headlineColor.copy(alpha = 0.75f))
            Text(
                text = headline,
                style = MaterialTheme.typography.bodySmall.copy(
                    fontSize = 12.5.sp,
                    lineHeight = 16.sp,
                    fontWeight = FontWeight.Medium,
                ),
                color = headlineColor,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
        }

        if (room != null) {
            Text(
                text = room.uppercase(),
                style = MaterialTheme.typography.labelSmall.copy(
                    fontFamily = FontFamily.Monospace,
                    fontSize = 9.5.sp,
                    fontWeight = FontWeight.SemiBold,
                    letterSpacing = 0.95.sp,
                ),
                color = metaColor,
                modifier = Modifier.padding(start = 17.dp),
                maxLines = 1,
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
private fun LocPartGlyph(kind: LocPartKind, color: Color) {
    when (kind) {
        LocPartKind.Modulo -> ModuloGlyph(color = color)
        LocPartKind.Room -> RoomGlyph(color = color)
    }
}
