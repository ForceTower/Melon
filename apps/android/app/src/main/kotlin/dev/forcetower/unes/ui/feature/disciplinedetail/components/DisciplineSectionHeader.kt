package dev.forcetower.unes.ui.feature.disciplinedetail.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.forcetower.unes.designsystem.theme.melon
import java.util.Locale

// Section header used inside the detail screen. Left side is the serif label
// ("Notas", "Aulas", …) in italic; right side carries an optional monospaced
// trailing slot. Mirrors iOS `DisciplineSectionHeader`.
@Composable
internal fun DisciplineSectionHeader(
    title: String,
    modifier: Modifier = Modifier,
    trailing: @Composable () -> Unit = {},
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 4.dp)
            .padding(bottom = 10.dp),
        verticalAlignment = Alignment.Bottom,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.headlineSmall.copy(
                fontSize = 22.sp,
                lineHeight = 24.sp,
                fontStyle = FontStyle.Italic,
                letterSpacing = (-0.22).sp,
            ),
            color = MaterialTheme.colorScheme.onBackground,
            modifier = Modifier.weight(1f, fill = true),
        )
        trailing()
    }
}

// Small stat card used at the top of the detail screen — reproduces iOS
// `DetailStatCard`. `value` is rendered serif large, the optional `sub` lives
// underneath in body-12 muted ink, and the icon slot anchors the eyebrow row.
@Composable
internal fun DetailStatCard(
    label: String,
    value: String,
    modifier: Modifier = Modifier,
    sub: String? = null,
    valueColor: Color = MaterialTheme.colorScheme.onBackground,
    icon: @Composable () -> Unit = {},
) {
    val card = MaterialTheme.melon.surface.card
    val cardLine = MaterialTheme.melon.surface.cardLine
    val shape = RoundedCornerShape(18.dp)
    Column(
        modifier = modifier
            .clip(shape)
            .background(card)
            .border(1.dp, cardLine, shape)
            .padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(3.dp),
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            icon()
            Text(
                text = label.uppercase(Locale.ROOT),
                style = MaterialTheme.typography.labelSmall.copy(
                    fontFamily = FontFamily.Monospace,
                    fontSize = 9.sp,
                    fontWeight = FontWeight.SemiBold,
                    letterSpacing = 1.08.sp,
                ),
                color = MaterialTheme.colorScheme.outlineVariant,
            )
        }
        Text(
            text = value,
            style = MaterialTheme.typography.headlineSmall.copy(
                fontSize = 26.sp,
                lineHeight = 28.sp,
                letterSpacing = (-0.52).sp,
            ),
            color = valueColor,
            modifier = Modifier.padding(top = 3.dp),
        )
        if (sub != null) {
            Text(
                text = sub,
                style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

// Small clock glyph for the "Carga horária" stat card. Avoids pulling in the
// material-icons-extended dependency for two icons.
@Composable
internal fun ClockIcon(tint: Color, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier.size(11.dp),
        contentAlignment = Alignment.Center,
    ) {
        androidx.compose.foundation.Canvas(modifier = Modifier.size(11.dp)) {
            val strokePx = 1.2.dp.toPx()
            val r = size.minDimension / 2f - strokePx / 2f
            drawCircle(
                color = tint,
                radius = r,
                style = androidx.compose.ui.graphics.drawscope.Stroke(width = strokePx),
            )
            // Hour and minute hands — pointing to "2:30" so the glyph reads
            // unambiguously as a clock at small sizes.
            val cx = size.width / 2f
            val cy = size.height / 2f
            drawLine(
                color = tint,
                start = androidx.compose.ui.geometry.Offset(cx, cy),
                end = androidx.compose.ui.geometry.Offset(cx, cy - r * 0.55f),
                strokeWidth = strokePx,
                cap = androidx.compose.ui.graphics.StrokeCap.Round,
            )
            drawLine(
                color = tint,
                start = androidx.compose.ui.geometry.Offset(cx, cy),
                end = androidx.compose.ui.geometry.Offset(cx + r * 0.45f, cy),
                strokeWidth = strokePx,
                cap = androidx.compose.ui.graphics.StrokeCap.Round,
            )
        }
    }
}

// Small "list rectangle" glyph for the "Faltas" stat card. Same stroke
// thickness as the clock so the eyebrow row stays balanced.
@Composable
internal fun AbsencesIcon(tint: Color, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier.size(11.dp),
        contentAlignment = Alignment.Center,
    ) {
        androidx.compose.foundation.Canvas(modifier = Modifier.size(11.dp)) {
            val strokePx = 1.2.dp.toPx()
            val pad = 1.dp.toPx()
            val left = pad
            val top = pad
            val right = size.width - pad
            val bottom = size.height - pad
            // Outline.
            val cornerPx = 1.dp.toPx()
            drawRoundRect(
                color = tint,
                topLeft = androidx.compose.ui.geometry.Offset(left, top),
                size = androidx.compose.ui.geometry.Size(right - left, bottom - top),
                cornerRadius = androidx.compose.ui.geometry.CornerRadius(cornerPx, cornerPx),
                style = androidx.compose.ui.graphics.drawscope.Stroke(width = strokePx),
            )
            // Three rule lines (matches the iOS list-bullet glyph).
            val rowGap = (bottom - top - strokePx) / 4f
            for (i in 1..3) {
                val y = top + rowGap * i
                drawLine(
                    color = tint,
                    start = androidx.compose.ui.geometry.Offset(left + pad, y),
                    end = androidx.compose.ui.geometry.Offset(right - pad, y),
                    strokeWidth = strokePx * 0.85f,
                    cap = androidx.compose.ui.graphics.StrokeCap.Round,
                )
            }
        }
    }
}
